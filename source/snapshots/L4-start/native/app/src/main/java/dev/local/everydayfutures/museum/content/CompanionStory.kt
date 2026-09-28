package dev.local.everydayfutures.museum.content

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.StoryReducer
import dev.local.everydayfutures.museum.state.StoryState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WindowNavigator
import dev.local.everydayfutures.museum.story.SceneBuild
import dev.local.everydayfutures.museum.story.SceneStatus
import dev.local.everydayfutures.museum.story.StoryPresenter
import dev.local.picopolish.PolishButton
import dev.local.picopolish.PolishButtonTone
import dev.local.picopolish.PolishSurface
import dev.local.picopolish.PolishSurfaceKind
import dev.local.picopolish.PolishTokens
import dev.local.picopolish.PolishTypography

/**
 * The companion's story mode: the object's miniature on the window's own glass (no surface wraps the scene, so the volume
 * keeps its depth) and the chapter panel beside it. One layout for every object; the chapter copy is the catalogue's.
 * While the scene is being created the miniature region shows a readable preparing state naming the chapter; it clears
 * only when the story's miniature exists. If it cannot be created, the region says so and offers Try again (a fresh scene,
 * at most [SceneBuild.MAX_ATTEMPTS] attempts per story view, shared by every object the story shows in it) beside the
 * panel's own Return to object and Close.
 */
@Composable
fun CompanionStory(
    controller: MuseumController,
    artifact: Artifact,
    story: StoryState,
    reduceMotion: Boolean,
    navigator: WindowNavigator,
    modifier: Modifier = Modifier,
) {
    var attempt by remember { mutableIntStateOf(1) }
    var sceneBuild by remember(attempt) { mutableStateOf(SceneBuild()) }
    val status = sceneBuild.status(story.objectId)
    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
        // Weighted rather than fixed (about 436 dp at the default 880 dp width), so at the 800 dp minimum both halves shrink.
        Box(modifier = Modifier.weight(1.15f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            // A new attempt is a new composition: the old scene's entities are destroyed and a fresh scene is created.
            key(attempt) {
                StoryPresenter(
                    objectId = story.objectId,
                    chapter = story.chapter,
                    reduceMotion = reduceMotion,
                    attempt = attempt,
                    controller = controller,
                    onSceneBuild = { sceneBuild = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (status != SceneStatus.READY) {
                key(attempt) {
                    // When the card leaves the composition (the scene became ready, or the story or window closed):
                    // composition time, not a measured on-screen time.
                    val shownSince = remember { SystemClock.uptimeMillis() }
                    val cardAttempt = attempt
                    val latest by rememberUpdatedState(story)
                    DisposableEffect(Unit) {
                        onDispose {
                            controller.storySceneCardRemoved(latest.objectId, latest.chapter, cardAttempt, SystemClock.uptimeMillis() - shownSince)
                        }
                    }
                    SceneStatusCard(
                        status = status,
                        artifact = artifact,
                        story = story,
                        canRetry = SceneBuild.canRetry(attempt),
                        onRetry = { attempt += 1 },
                    )
                }
            }
        }
        ChapterPanel(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            controller = controller,
            artifact = artifact,
            story = story,
            navigator = navigator,
        )
    }
}

/**
 * What the miniature region says while the story's scene does not exist: preparing (with the chapter it will open at, so
 * a reopened story keeps its identity), or failed with the recovery the app supports. Text only, so it is the same with
 * Reduce motion on.
 */
@Composable
private fun SceneStatusCard(status: SceneStatus, artifact: Artifact, story: StoryState, canRetry: Boolean, onRetry: () -> Unit) {
    val count = StoryReducer.chapterCount(story.objectId)
    val title = artifact.chapters.getOrNull(story.chapter)?.title.orEmpty()
    PolishSurface(modifier = Modifier.widthIn(max = 320.dp), kind = PolishSurfaceKind.Card) {
        Text(
            text = stringResource(R.string.story_scene_chapter, story.chapterNumber, count, title),
            style = PolishTypography.overline,
            color = supportingColor(),
        )
        if (status == SceneStatus.FAILED) {
            Text(text = stringResource(R.string.story_scene_failed), style = PolishTypography.objectTitle)
            Text(
                text = if (canRetry) stringResource(R.string.story_scene_failed_hint) else stringResource(R.string.story_scene_failed_final),
                style = PolishTypography.body(multiline = true),
            )
            if (canRetry) {
                PolishButton(onClick = onRetry, tone = PolishButtonTone.Emphasis) {
                    Text(text = stringResource(R.string.action_story_retry))
                }
            }
        } else {
            Text(text = stringResource(R.string.story_scene_preparing), style = PolishTypography.objectTitle)
        }
    }
}

@Composable
private fun ChapterPanel(
    modifier: Modifier,
    controller: MuseumController,
    artifact: Artifact,
    story: StoryState,
    navigator: WindowNavigator,
) {
    val count = StoryReducer.chapterCount(story.objectId)
    val chapter = artifact.chapters.getOrNull(story.chapter)
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(
            text = stringResource(R.string.story_overline, artifact.number, story.chapterNumber, count),
            style = PolishTypography.overline,
            color = supportingColor(),
        )
        Text(text = chapter?.title.orEmpty(), style = PolishTypography.display)
        Text(text = chapter?.text.orEmpty(), style = PolishTypography.body(multiline = true))
        Spacer(modifier = Modifier.weight(1f))
        // Two short rows: chapter steps with Close, then the two story-level actions (the L3 review found three labels in
        // one row too wide for the panel at the window's 800 dp minimum).
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.previousChapter(Surface.COMPANION) }, enabled = story.chapter > 0, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_story_back))
            }
            PolishButton(onClick = { controller.nextChapter(Surface.COMPANION) }, enabled = story.chapter < count - 1, tone = PolishButtonTone.Emphasis) {
                Text(text = stringResource(R.string.action_story_next))
            }
            Spacer(modifier = Modifier.weight(1f))
            PolishButton(onClick = { controller.closeCompanion(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_close_short))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.restartStory(Surface.COMPANION) }, enabled = story.chapter > 0, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_story_restart))
            }
            PolishButton(onClick = { controller.returnFromStory(Surface.COMPANION) }) {
                Text(text = stringResource(R.string.action_story_return))
            }
        }
    }
}
