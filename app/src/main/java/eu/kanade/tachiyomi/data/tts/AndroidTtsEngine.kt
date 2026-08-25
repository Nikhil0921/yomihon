package eu.kanade.tachiyomi.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mihon.domain.tts.engine.TtsEngine
import mihon.domain.tts.engine.TtsFocusEvent
import java.util.concurrent.atomic.AtomicReference

class AndroidTtsEngine(private val context: Context) : TtsEngine {

    private val mutex = Mutex()

    private val tts = AtomicReference<TextToSpeech?>(null)

    private val pendingUtterances = HashMap<String, CompletableDeferred<Boolean>>()

    private var audioFocusRequest: AudioFocusRequest? = null

    private var speechRate: Float = 1f

    private var pitch: Float = 1f

    override var onFocusEvent: ((TtsFocusEvent) -> Unit)? = null

    override suspend fun initialize(): Boolean = mutex.withLock {
        if (tts.get() != null) return@withLock true

        val readiness = CompletableDeferred<Boolean>()
        val engine = withContext(Dispatchers.Main) {
            TextToSpeech(context.applicationContext) { status ->
                readiness.complete(status == TextToSpeech.SUCCESS)
            }.apply {
                setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            completeUtterance(utteranceId, success = true)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            completeUtterance(utteranceId, success = false)
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            completeUtterance(utteranceId, success = false)
                        }

                        override fun onStop(utteranceId: String?, interrupted: Boolean) {
                            completeUtterance(utteranceId, success = false)
                        }
                    },
                )
                setSpeechRate(speechRate)
                setPitch(pitch)
            }
        }

        val success = try {
            readiness.await()
        } catch (e: CancellationException) {
            withContext(Dispatchers.Main) { engine.shutdown() }
            throw e
        }
        if (!success) {
            withContext(Dispatchers.Main) { engine.shutdown() }
            return@withLock false
        }

        tts.set(engine)
        true
    }

    override suspend fun speak(utteranceId: String, text: String): Boolean {
        val engine = tts.get() ?: return false

        val completion = CompletableDeferred<Boolean>()
        synchronized(pendingUtterances) { pendingUtterances[utteranceId] = completion }

        try {
            val accepted = withContext(Dispatchers.Main) {
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            }
            if (accepted != TextToSpeech.SUCCESS) return false

            return try {
                completion.await()
            } finally {
                if (!completion.isCompleted) stop()
            }
        } finally {
            synchronized(pendingUtterances) { pendingUtterances.remove(utteranceId) }
        }
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts.get()?.setSpeechRate(rate)
    }

    override fun setPitch(value: Float) {
        pitch = value
        tts.get()?.setPitch(value)
    }

    override fun acquireFocus() {
        val manager = audioManager() ?: return
        val request = audioFocusRequest ?: buildFocusRequest().also { audioFocusRequest = it }
        manager.requestAudioFocus(request)
    }

    override fun abandonFocus() {
        val request = audioFocusRequest ?: return
        audioManager()?.abandonAudioFocusRequest(request)
    }

    override fun stop() {
        val engine = tts.get() ?: return
        failPendingUtterances()
        engine.stop()
    }

    override fun shutdown() {
        val engine = tts.getAndSet(null) ?: return
        failPendingUtterances()
        abandonFocus()
        audioFocusRequest = null
        engine.shutdown()
    }

    private fun completeUtterance(utteranceId: String?, success: Boolean) {
        if (utteranceId == null) return
        val completion = synchronized(pendingUtterances) { pendingUtterances[utteranceId] } ?: return
        completion.complete(success)
    }

    private fun failPendingUtterances() {
        synchronized(pendingUtterances) {
            pendingUtterances.values.forEach { it.complete(false) }
            pendingUtterances.clear()
        }
    }

    private fun audioManager(): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun buildFocusRequest(): AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setOnAudioFocusChangeListener { change ->
                val event = when (change) {
                    AudioManager.AUDIOFOCUS_GAIN -> TtsFocusEvent.Regained
                    AudioManager.AUDIOFOCUS_LOSS -> TtsFocusEvent.PermanentLoss
                    else -> TtsFocusEvent.TransientLoss
                }
                onFocusEvent?.invoke(event)
            }
            .build()
}
