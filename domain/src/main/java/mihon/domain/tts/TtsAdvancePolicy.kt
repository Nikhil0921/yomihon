package mihon.domain.tts

enum class TtsAdvanceAction {
    NextPage,
    NextChapter,
    Finish,
    PauseAtPageEnd,
}

object TtsAdvancePolicy {

    /**
     * Decides what playback should do once a page is exhausted (its last sentence
     * finished, or it never had text). Pages are 0-indexed; [currentPageIndex] is
     * the page just spoken and [totalPages] the size of the current chapter's
     * real page list. An empty page is skipped regardless of [autoTurn] because
     * there is nothing to pause for.
     */
    fun computeAdvance(
        currentPageIndex: Int,
        totalPages: Int,
        pageHasText: Boolean,
        autoTurn: Boolean,
        autoNextChapter: Boolean,
        nextChapterExists: Boolean,
    ): TtsAdvanceAction {
        if (!pageHasText) {
            return when {
                currentPageIndex < totalPages - 1 -> TtsAdvanceAction.NextPage
                autoNextChapter && nextChapterExists -> TtsAdvanceAction.NextChapter
                else -> TtsAdvanceAction.Finish
            }
        }
        return when {
            currentPageIndex < totalPages - 1 ->
                if (autoTurn) TtsAdvanceAction.NextPage else TtsAdvanceAction.PauseAtPageEnd
            autoNextChapter && nextChapterExists -> TtsAdvanceAction.NextChapter
            else -> TtsAdvanceAction.Finish
        }
    }
}
