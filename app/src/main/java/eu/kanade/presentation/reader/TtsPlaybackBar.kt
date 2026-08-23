package eu.kanade.presentation.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.tts.TtsError
import eu.kanade.tachiyomi.ui.reader.tts.TtsPhase
import eu.kanade.tachiyomi.ui.reader.tts.TtsPlaybackState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TtsPlaybackBar(
    state: TtsPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.phase != TtsPhase.Idle,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            when (state.phase) {
                TtsPhase.Preparing, TtsPhase.LoadingPage -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(14.dp).size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(
                                if (state.phase == TtsPhase.Preparing) {
                                    MR.strings.tts_preparing
                                } else {
                                    MR.strings.tts_loading_page
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                TtsPhase.Error -> {
                    ErrorContent(state = state, onRetry = onRetry, onStop = onStop)
                }
                else -> {
                    PlaybackContent(
                        state = state,
                        onTogglePlayPause = onTogglePlayPause,
                        onNextSentence = onNextSentence,
                        onPreviousSentence = onPreviousSentence,
                        onStop = onStop,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackContent(
    state: TtsPlaybackState,
    onTogglePlayPause: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit,
    onStop: () -> Unit,
) {
    val playing = state.phase == TtsPhase.Playing || state.phase == TtsPhase.LoadingPage
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (state.sentenceText.isNotBlank()) {
                Text(
                    text = state.sentenceText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = stringResource(
                        when (state.phase) {
                            TtsPhase.Paused -> MR.strings.tts_paused
                            TtsPhase.Finished -> MR.strings.tts_finished
                            else -> MR.strings.action_read_aloud
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.sentenceCount > 0 && state.sentenceIndex >= 0) {
                Text(
                    text = "${state.sentenceIndex + 1}/${state.sentenceCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        IconButton(onClick = onPreviousSentence, enabled = playing || state.phase == TtsPhase.Paused) {
            Icon(
                imageVector = Icons.Outlined.SkipPrevious,
                contentDescription = stringResource(MR.strings.tts_action_previous_sentence),
            )
        }
        IconButton(onClick = onTogglePlayPause, enabled = playing || state.phase == TtsPhase.Paused) {
            Icon(
                imageVector = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = stringResource(
                    if (playing) MR.strings.tts_action_pause else MR.strings.tts_action_play,
                ),
            )
        }
        IconButton(onClick = onNextSentence, enabled = playing || state.phase == TtsPhase.Paused) {
            Icon(
                imageVector = Icons.Outlined.SkipNext,
                contentDescription = stringResource(MR.strings.tts_action_next_sentence),
            )
        }
        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = stringResource(MR.strings.tts_action_stop),
            )
        }
    }
}

@Composable
private fun ErrorContent(
    state: TtsPlaybackState,
    onRetry: () -> Unit,
    onStop: () -> Unit,
) {
    val retryable = state.error != TtsError.NoJapaneseVoice
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        Text(
            text = errorMessage(state.error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (retryable) {
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(MR.strings.action_retry),
                )
            }
        }
        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = stringResource(MR.strings.tts_action_stop),
            )
        }
    }
}

@Composable
private fun errorMessage(error: TtsError?): String = stringResource(
    when (error) {
        TtsError.NoJapaneseVoice -> MR.strings.tts_error_no_japanese_voice
        TtsError.EngineError -> MR.strings.tts_error_engine
        TtsError.OcrError, null -> MR.strings.tts_error_ocr
        TtsError.NoTextFound -> MR.strings.no_results_found
    },
)
