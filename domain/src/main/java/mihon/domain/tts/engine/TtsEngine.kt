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

    /**
     * Installed TTS engines on this device. Empty when the engine is not
     * initialized or discovery is unavailable.
     */
    suspend fun getEngines(): List<TtsEngineInfo>

    /**
     * Voices available from the active engine. Empty when the engine is not
     * initialized or exposes no voices.
     */
    suspend fun getVoices(): List<TtsVoiceInfo>

    /**
     * Selects the TTS engine used by the next [initialize]. Empty string
     * means system default engine. If the engine is currently initialized
     * with a different package, implementations must release it so the next
     * [initialize] rebuilds with the new engine.
     */
    fun setEnginePackage(pkg: String)
}

enum class TtsFocusEvent {
    TransientLoss,
    Regained,
    PermanentLoss,
}

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isSystemDefault: Boolean,
)

data class TtsVoiceInfo(
    val name: String,
    val languageTag: String,
    val displayName: String,
    val quality: Int,
    val latency: Int,
    val features: List<String>,
    val networkRequired: Boolean,
)
