package mihon.domain.tts.engine

interface TtsEngine {

    /**
     * Notified when audio focus owned by this engine is lost or regained.
     * Implementations must only report events; playback reactions stay with the caller.
     */
    var onFocusEvent: ((TtsFocusEvent) -> Unit)?

    /**
     * Prepares the underlying speech resources. Returns false on failure.
     * Must be called before any other member and is safe to call again.
     */
    suspend fun initialize(): Boolean

    val japaneseAvailable: Boolean

    /**
     * Speaks [text] and suspends until the utterance completes, fails or [stop] is
     * called. Returns false when the utterance did not complete successfully.
     */
    suspend fun speak(utteranceId: String, text: String): Boolean

    fun setSpeechRate(rate: Float)

    fun setPitch(pitch: Float)

    fun acquireFocus()

    fun abandonFocus()

    /** Interrupts any ongoing utterance; pending [speak] calls return immediately. */
    fun stop()

    /** Releases all underlying resources. The engine must not be used afterwards. */
    fun shutdown()
}

enum class TtsFocusEvent {
    TransientLoss,
    Regained,
    PermanentLoss,
}
