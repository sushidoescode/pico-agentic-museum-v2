package dev.local.everydayfutures.museum.content

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import dev.local.everydayfutures.museum.story.StoryScenes
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
 * panel's own Return to object and Close. [compact] is the guide's 360 dp Subwindow prototype: the miniature region
 * above the chapter panel, at [StoryScenes.COMPACT_ROOT_SCALE], with Close in the guide's header instead of the panel;
 * the attempts, the status card and the scene's disposal are the same.
 */
@Composable
fun CompanionStory(
    controller: MuseumController,
    artifact: Artifact,
    story: StoryState,
    reduceMotion: Boolean,
    navigator: WindowNavigator,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    var attempt by remember { mutableIntStateOf(1) }
    var sceneBuild by remember(attempt) { mutableStateOf(SceneBuild()) }
    val status = sceneBuild.status(story.objectId)
    val miniature: @Composable (Modifier) -> Unit = { regionModifier ->
        Box(modifier = regionModifier, contentAlignment = Alignment.Center) {
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
                    rootScale = if (compact) StoryScenes.COMPACT_ROOT_SCALE else StoryScenes.ROOT_SCALE,
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
    }
    if (compact) {
        Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
            miniature(Modifier.weight(1f).fillMaxWidth())
            ChapterPanel(
                modifier = Modifier.fillMaxWidth(),
                controller = controller,
                artifact = artifact,
                story = story,
                reduceMotion = reduceMotion,
                navigator = navigator,
                compact = true,
            )
        }
        return
    }
    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
        // Weighted rather than fixed (about 436 dp at the default 880 dp width), so at the 800 dp minimum both halves shrink.
        miniature(Modifier.weight(1.15f).fillMaxHeight())
        ChapterPanel(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            controller = controller,
            artifact = artifact,
            story = story,
            reduceMotion = reduceMotion,
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

/**
 * The chapter copy and controls. While Reduce motion is on, a caption under the overline says chapters change at once.
 * [compact] (the Subwindow prototype): sized to its content below the miniature, so no weighted spacer, and no Close,
 * which the guide's header carries.
 */
@Composable
private fun ChapterPanel(
    modifier: Modifier,
    controller: MuseumController,
    artifact: Artifact,
    story: StoryState,
    reduceMotion: Boolean,
    navigator: WindowNavigator,
    compact: Boolean = false,
) {
    val count = StoryReducer.chapterCount(story.objectId)
    val chapter = artifact.chapters.getOrNull(story.chapter)
    PolishSurface(modifier = modifier, kind = PolishSurfaceKind.Panel) {
        Text(
            text = stringResource(R.string.story_overline, artifact.number, story.chapterNumber, count),
            style = PolishTypography.overline,
            color = supportingColor(),
        )
        if (reduceMotion) {
            Text(text = stringResource(R.string.story_motion_off), style = PolishTypography.caption, color = supportingColor())
        }
        Text(text = chapter?.title.orEmpty(), style = PolishTypography.display)
        Text(text = chapter?.text.orEmpty(), style = PolishTypography.body(multiline = true))
        if (!compact) Spacer(modifier = Modifier.weight(1f))
        // Two short rows: chapter steps with Close, then the two story-level actions (the L3 review found three labels in
        // one row too wide for the panel at the window's 800 dp minimum).
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall)) {
            PolishButton(onClick = { controller.previousChapter(Surface.COMPANION) }, enabled = story.chapter > 0, tone = PolishButtonTone.Quiet) {
                Text(text = stringResource(R.string.action_story_back))
            }
            PolishButton(onClick = { controller.nextChapter(Surface.COMPANION) }, enabled = story.chapter < count - 1, tone = PolishButtonTone.Emphasis) {
                Text(text = stringResource(R.string.action_story_next))
            }
            if (!compact) {
                Spacer(modifier = Modifier.weight(1f))
                PolishButton(onClick = { controller.closeCompanion(navigator, Surface.COMPANION) }, tone = PolishButtonTone.Quiet) {
                    Text(text = stringResource(R.string.action_close_short))
                }
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
