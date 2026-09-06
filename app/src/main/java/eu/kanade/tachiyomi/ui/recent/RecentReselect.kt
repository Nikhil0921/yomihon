package eu.kanade.tachiyomi.ui.recent

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Opens the last-read manga's next chapter (old History-tab reselect
 * behavior), reusing the existing GetNextChapters history mechanism.
 */
internal suspend fun resumeLastReadChapter(context: Context, snackbarHostState: SnackbarHostState) {
    val chapter = withIOContext {
        Injekt.get<GetNextChapters>().await(onlyUnread = false).firstOrNull()
    }
    if (chapter != null) {
        context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
    } else {
        snackbarHostState.showSnackbar(context.stringResource(MR.strings.no_next_chapter))
    }
}
