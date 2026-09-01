package mihon.domain.tts.speech

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SpeechCleanerTest {

    private val options = SpeechCleanupOptions()

    @Test
    fun `standalone punctuation regions are skipped`() {
        SpeechCleaner.cleanRegionForSpeech("!!!", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("???", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("...", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("***", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("~~~~", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("‼⁇⁉", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("?!?!", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("   ", options) shouldBe null
    }

    @Test
    fun `meaningful dialogue with punctuation is kept intact`() {
        SpeechCleaner.cleanRegionForSpeech("What?!", options) shouldBe "What?!"
        SpeechCleaner.cleanRegionForSpeech("Hello.", options) shouldBe "Hello."
        SpeechCleaner.cleanRegionForSpeech("Wait, what? No way.", options) shouldBe "Wait, what? No way."
    }

    @Test
    fun `ellipsis becomes a spoken pause`() {
        SpeechCleaner.cleanRegionForSpeech("I don't know...", options) shouldBe "I don't know, "
        SpeechCleaner.cleanRegionForSpeech("Wait… what?", options) shouldBe "Wait,  what?"
        SpeechCleaner.cleanRegionForSpeech("done…", options) shouldBe "done, "
    }

    @Test
    fun `ellipsis pause can be disabled`() {
        val off = SpeechCleanupOptions(ellipsisToPause = false)
        SpeechCleaner.cleanRegionForSpeech("I don't know...", off) shouldBe "I don't know..."
        SpeechCleaner.cleanRegionForSpeech("...", off) shouldBe null
    }

    @Test
    fun `excessive punctuation is normalized but text kept`() {
        SpeechCleaner.cleanRegionForSpeech("WHAT?!?!?!", options) shouldBe "WHAT?!"
        SpeechCleaner.cleanRegionForSpeech("NO WAY!!!", options) shouldBe "NO WAY!!"
        SpeechCleaner.cleanRegionForSpeech("Really?????", options) shouldBe "Really??"
        SpeechCleaner.cleanRegionForSpeech("なんで！！！", options) shouldBe "なんで！！"
    }

    @Test
    fun `ocr garbage is skipped`() {
        SpeechCleaner.cleanRegionForSpeech("W@#R%!", options) shouldBe null
        SpeechCleaner.cleanRegionForSpeech("##$%&@#", options) shouldBe null
    }

    @Test
    fun `short emphatic text is never garbage`() {
        SpeechCleaner.cleanRegionForSpeech("AAAAH", options) shouldBe "AAAAH"
        SpeechCleaner.cleanRegionForSpeech("Bo", options) shouldBe "Bo"
    }

    @Test
    fun `bubble line breaks collapse to spaces for speech`() {
        SpeechCleaner.cleanRegionForSpeech("first line\nsecond   line", options) shouldBe "first line second line"
    }

    @Test
    fun `punctuation-only skip can be disabled`() {
        val off = SpeechCleanupOptions(skipPunctuationOnly = false)
        // ellipsisToPause still fires first: "..." -> ", "
        SpeechCleaner.cleanRegionForSpeech("...", off) shouldBe ", "
        SpeechCleaner.cleanRegionForSpeech("***", off) shouldBe "***"
    }

    @Test
    fun `garbage skip can be disabled`() {
        val off = SpeechCleanupOptions(skipOcrGarbage = false)
        SpeechCleaner.cleanRegionForSpeech("W@#R%!", off) shouldBe "W@#R%!"
    }

    @Test
    fun `normalization can be disabled`() {
        val off = SpeechCleanupOptions(normalizeExcessivePunctuation = false)
        SpeechCleaner.cleanRegionForSpeech("WHAT?!?!?!", off) shouldBe "WHAT?!?!?!"
    }
}
