package eu.kanade.tachiyomi.ui.reader.tts

import eu.kanade.tachiyomi.data.ocr.OcrPageSourceResolver
import eu.kanade.tachiyomi.util.ocr.toOcrImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import mihon.domain.ocr.interactor.GetCachedPageOcr
import mihon.domain.ocr.interactor.ScanPageOcr
import mihon.domain.ocr.interactor.WithOcrScanSession
import mihon.domain.tts.TtsAdvanceAction
import mihon.domain.tts.TtsAdvancePolicy
import mihon.domain.tts.TtsSentence
import mihon.domain.tts.engine.TtsEngine
import mihon.domain.tts.engine.TtsFocusEvent
import mihon.domain.tts.service.TtsPreferences
import mihon.domain.tts.toTtsSentences
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

enum class TtsPhase {
    Idle,
    Preparing,
    LoadingPage,
    Playing,
    Paused,
    Finished,
    Error,
}

enum class TtsError {
    NoJapaneseVoice,
    EngineError,
    OcrError,
    NoTextFound,
}

data class TtsPlaybackState(
    val phase: TtsPhase = TtsPhase.Idle,
    val pageIndex: Int = -1,
    val sentenceIndex: Int = -1,
    val sentenceCount: Int = 0,
    val sentenceText: String = "",
    val error: TtsError? = null,
)

sealed interface TtsEvent {
    data class AdvancePage(val pageIndex: Int) : TtsEvent
    data object AdvanceChapter : TtsEvent
    data class Failed(val error: TtsError) : TtsEvent
}

/** Everything the controller needs to play one chapter; supplied by ReaderViewModel. */
data class TtsChapterContext(
    val manga: Manga,
    val chapter: Chapter,
    val totalPages: Int,
    val hasNextChapter: Boolean,
)

/**
 * Orchestrates read-aloud playback: OCR text acquisition → sentence segmentation →
 * speech → advance policy. Never touches the viewer directly; page/chapter movement
 * is requested via [TtsEvent]s and confirmed back through [onPageSelected], so user
 * navigation always wins over automatic advancement.
 */
internal class TtsPlaybackController(
    private val scope: CoroutineScope,
    private val engine: TtsEngine,
    private val preferences: TtsPreferences,
    private val getCachedPageOcr: GetCachedPageOcr,
    private val scanPageOcr: ScanPageOcr,
    private val withOcrScanSession: WithOcrScanSession,
    private val pageSourceResolver: OcrPageSourceResolver,
) {

    private val mutableState = MutableStateFlow(TtsPlaybackState())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<TtsEvent>()
    val events = eventChannel.receiveAsFlow()

    /** Completed by [onPageSelected] with the index of the page actually shown. */
    private var pendingAdvance: CompletableDeferred<Int>? = null

    private var context: TtsChapterContext? = null

    private var queue: List<TtsSentence> = emptyList()
    private var queuePageIndex = -1

    /** Sentence to resume from when playback continues. */
    private var resumeIndex = 0

    @Volatile
    private var paused = false

    @Volatile
    private var stopped = false

    private var playbackJob: Job? = null

    private var prefetchJob: Job? = null

    init {
        engine.onFocusEvent = { event ->
            when (event) {
                TtsFocusEvent.TransientLoss, TtsFocusEvent.PermanentLoss -> pause()
                TtsFocusEvent.Regained -> Unit // resume stays manual for transient loss
            }
        }
        preferences.ttsSpeechRate().changes().onEach { engine.setSpeechRate(it) }.launchIn(scope)
        preferences.ttsPitch().changes().onEach { engine.setPitch(it) }.launchIn(scope)
    }

    fun start(context: TtsChapterContext, startPageIndex: Int) {
        resetSession(stoppedFlag = true)
        this.context = context
        stopped = false
        paused = false
        mutableState.value = TtsPlaybackState(phase = TtsPhase.Preparing, pageIndex = startPageIndex)
        playbackJob = scope.launch { runPlayback(startPageIndex) }
    }

    fun pause() {
        if (mutableState.value.phase != TtsPhase.Playing && !paused) return
        paused = true
        engine.stop() // interrupts the current utterance; speak() returns false
        mutableState.update { it.copy(phase = TtsPhase.Paused) }
    }

    fun resume() {
        if (!paused || stopped) return
        paused = false
        val pageIndex = mutableState.value.pageIndex
        playbackJob?.cancel()
        playbackJob = scope.launch {
            if (engine.initialize()) {
                engine.acquireFocus()
                runPlayback(pageIndex, startAtSentence = resumeIndex)
            } else {
                fail(TtsError.EngineError)
            }
        }
    }

    fun togglePlayPause() {
        if (paused || mutableState.value.phase == TtsPhase.Paused) resume() else pause()
    }

    fun stop() {
        context = null
        resetSession(stoppedFlag = true)
        mutableState.value = TtsPlaybackState(phase = TtsPhase.Idle)
    }

    fun nextSentence() = stepBy(1)

    fun previousSentence() = stepBy(-1)

    /**
     * Called by the host on every viewer page change. A mismatch against the page we
     * asked to advance to means the user navigated manually — user navigation wins.
     */
    fun onPageSelected(pageIndex: Int) {
        val pending = pendingAdvance
        if (pending != null) {
            pending.complete(pageIndex)
            return
        }
        val playing = mutableState.value.phase == TtsPhase.Playing ||
            mutableState.value.phase == TtsPhase.LoadingPage
        if (playing && !stopped && pageIndex != mutableState.value.pageIndex) {
            rebuildQueueForUserNavigation(pageIndex)
        }
    }

    private suspend fun awaitAdvanceConfirmation(target: Int): Int? {
        val deferred = CompletableDeferred<Int>()
        pendingAdvance = deferred
        try {
            eventChannel.send(TtsEvent.AdvancePage(target))
            val shown = withTimeoutOrNull(ADVANCE_CONFIRM_TIMEOUT_MS) { deferred.await() }
            if (shown != null && shown != target) {
                // User went somewhere else while the advance was in flight;
                // onPageSelected already rebuilt the queue for that page.
                return null
            }
            return shown
        } finally {
            pendingAdvance = null
        }
    }

    private fun rebuildQueueForUserNavigation(pageIndex: Int) {
        playbackJob?.cancel()
        prefetchJob?.cancel()
        engine.stop()
        paused = false
        mutableState.update { it.copy(pageIndex = pageIndex, phase = TtsPhase.LoadingPage) }
        playbackJob = scope.launch {
            if (engine.initialize()) runPlayback(pageIndex) else fail(TtsError.EngineError)
        }
    }

    private suspend fun runPlayback(startPageIndex: Int, startAtSentence: Int = 0) {
        var pageIndex = startPageIndex
        var sentenceIndex = startAtSentence

        if (!ensureInitialized()) return

        while (true) {
            val ctx = context ?: return finishIdle()

            val sentences = acquireSentences(pageIndex) ?: return // acquisition failed & reported
            queuePageIndex = pageIndex

            if (sentences.isEmpty()) {
                if (!advanceFromPolicy(ctx, pageIndex, pageHasText = false)) return
                pageIndex = mutableState.value.pageIndex
                sentenceIndex = 0
                continue
            }

            queue = sentences
            if (sentenceIndex >= sentences.size) sentenceIndex = 0
            schedulePrefetch(ctx, pageIndex + 1)

            while (sentenceIndex < sentences.size) {
                if (stopped) return finishIdle()
                if (!awaitWhilePaused()) return finishIdle()

                val sentence = sentences[sentenceIndex]
                resumeIndex = sentenceIndex
                mutableState.update {
                    it.copy(
                        phase = TtsPhase.Playing,
                        pageIndex = pageIndex,
                        sentenceIndex = sentenceIndex,
                        sentenceCount = sentences.size,
                        sentenceText = sentence.text,
                    )
                }
                val spoke = try {
                    engine.speak(utteranceId(pageIndex, sentenceIndex), sentence.text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "TTS speak failed" }
                    return fail(TtsError.EngineError)
                }
                if (!spoke) {
                    // Interrupted by pause(), stop(), or job replacement.
                    if (stopped) return finishIdle()
                    if (!awaitWhilePaused()) return finishIdle()
                }
                sentenceIndex++
            }

            resumeIndex = 0
            if (!advanceFromPolicy(ctx, pageIndex, pageHasText = true)) return
            pageIndex = mutableState.value.pageIndex
            sentenceIndex = 0
        }
    }

    /**
     * Runs the advance policy and executes its decision.
     * Returns true when the loop should continue (NextPage), false otherwise.
     */
    private suspend fun advanceFromPolicy(
        ctx: TtsChapterContext,
        pageIndex: Int,
        pageHasText: Boolean,
    ): Boolean {
        when (
            val action = TtsAdvancePolicy.computeAdvance(
                currentPageIndex = pageIndex,
                totalPages = ctx.totalPages,
                pageHasText = pageHasText,
                autoTurn = preferences.ttsAutoPageTurn().get(),
                autoNextChapter = preferences.ttsAutoNextChapter().get(),
                nextChapterExists = ctx.hasNextChapter,
            )
        ) {
            TtsAdvanceAction.NextPage -> {
                mutableState.update { it.copy(phase = TtsPhase.LoadingPage) }
                val confirmed = awaitAdvanceConfirmation(pageIndex + 1) ?: run {
                    if (pendingAdvance == null && !stopped) {
                        paused = true
                        mutableState.update { it.copy(phase = TtsPhase.Paused, sentenceText = "") }
                    }
                    return false
                }
                mutableState.update { it.copy(pageIndex = confirmed, phase = TtsPhase.LoadingPage) }
                return true
            }
            TtsAdvanceAction.NextChapter -> {
                mutableState.update { it.copy(phase = TtsPhase.Preparing, sentenceText = "") }
                engine.stop()
                engine.abandonFocus()
                eventChannel.send(TtsEvent.AdvanceChapter)
                // Host loads the next chapter and rebinds us via start().
                return false
            }
            TtsAdvanceAction.Finish -> {
                if (!pageHasText) eventChannel.send(TtsEvent.Failed(TtsError.NoTextFound))
                engine.stop()
                engine.abandonFocus()
                mutableState.update { it.copy(phase = TtsPhase.Finished, sentenceText = "") }
                return false
            }
            TtsAdvanceAction.PauseAtPageEnd -> {
                paused = true
                mutableState.update { it.copy(phase = TtsPhase.Paused, sentenceText = "") }
                return false
            }
        }
    }

    private suspend fun ensureInitialized(): Boolean {
        if (!engine.initialize()) {
            fail(TtsError.EngineError)
            return false
        }
        if (!engine.japaneseAvailable) {
            engine.shutdown()
            fail(TtsError.NoJapaneseVoice)
            return false
        }
        engine.acquireFocus()
        return true
    }

    /** Returns the page's sentences, an empty list when the page has no text, or null on failure. */
    private suspend fun acquireSentences(pageIndex: Int): List<TtsSentence>? = withIOContext {
        mutableState.update { it.copy(phase = TtsPhase.LoadingPage) }
        val ctx = context ?: return@withIOContext emptyList()
        val cached = try {
            getCachedPageOcr.await(ctx.chapter.id, pageIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "TTS cached OCR read failed" }
            null
        }
        val result = cached ?: scanOnDemand(ctx, pageIndex)
        result?.regions.orEmpty().toTtsSentences()
    }

    /** Cached-miss path: resolve the bitmap through the shared pipeline, scan, recycle. */
    private suspend fun scanOnDemand(ctx: TtsChapterContext, pageIndex: Int) = try {
        withOcrScanSession.await {
            val pages = pageSourceResolver.resolve(ctx.manga, ctx.chapter)
            pages.use { resolved ->
                val input = resolved.getPageInput(pageIndex) ?: return@use null
                val bitmap: android.graphics.Bitmap = input.openBitmap() ?: return@use null
                try {
                    scanPageOcr.await(ctx.chapter.id, pageIndex, bitmap.toOcrImage())
                } finally {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: OutOfMemoryError) {
        logcat(LogPriority.ERROR, e) { "OOM during TTS page scan" }
        fail(TtsError.OcrError)
        null
    } catch (e: Exception) {
        logcat(LogPriority.ERROR, e) { "TTS on-demand scan failed" }
        fail(TtsError.OcrError)
        null
    }

    private fun schedulePrefetch(ctx: TtsChapterContext, pageIndex: Int) {
        if (pageIndex >= ctx.totalPages) return
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            val cached = runCatching { getCachedPageOcr.await(ctx.chapter.id, pageIndex) }.getOrNull()
            if (cached != null) return@launch
            runCatching { scanOnDemand(ctx, pageIndex) }
        }
    }

    private suspend fun awaitWhilePaused(): Boolean {
        while (paused && !stopped) delay(RESUME_POLL_MS)
        return !stopped
    }

    private fun stepBy(delta: Int) {
        val current = mutableState.value
        if (current.phase != TtsPhase.Playing && current.phase != TtsPhase.Paused) return
        val newIndex = current.sentenceIndex + delta
        if (newIndex !in queue.indices) return // page boundary crossing stays manual in v1

        resumeIndex = newIndex
        if (current.phase == TtsPhase.Playing) {
            playbackJob?.cancel()
            engine.stop()
            paused = false
            playbackJob = scope.launch {
                if (engine.initialize()) {
                    runPlayback(
                        queuePageIndex,
                        startAtSentence = newIndex,
                    )
                } else {
                    fail(TtsError.EngineError)
                }
            }
        } else {
            mutableState.update {
                it.copy(
                    sentenceIndex = newIndex,
                    sentenceCount = queue.size,
                    sentenceText = queue.getOrNull(newIndex)?.text.orEmpty(),
                )
            }
        }
    }

    private fun resetSession(stoppedFlag: Boolean) {
        stopped = stoppedFlag
        playbackJob?.cancel()
        playbackJob = null
        prefetchJob?.cancel()
        prefetchJob = null
        pendingAdvance = null
        paused = false
        queue = emptyList()
        queuePageIndex = -1
        resumeIndex = 0
        engine.stop()
    }

    private fun finishIdle() {
        engine.abandonFocus()
        mutableState.update { it.copy(phase = TtsPhase.Idle, sentenceText = "") }
    }

    private fun fail(error: TtsError) {
        engine.stop()
        engine.abandonFocus()
        scope.launch { eventChannel.send(TtsEvent.Failed(error)) }
        mutableState.update { it.copy(phase = TtsPhase.Error, error = error) }
    }

    private fun utteranceId(pageIndex: Int, sentenceIndex: Int) = "p${pageIndex}_s$sentenceIndex"

    private companion object {
        const val ADVANCE_CONFIRM_TIMEOUT_MS = 10_000L
        const val RESUME_POLL_MS = 100L
    }
}
