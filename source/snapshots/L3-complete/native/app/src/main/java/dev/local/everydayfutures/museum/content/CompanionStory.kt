package dev.local.everydayfutures.museum.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pico.spatial.ui.design.Text
import dev.local.everydayfutures.museum.R
import dev.local.everydayfutures.museum.catalogue.Artifact
import dev.local.everydayfutures.museum.state.MuseumController
import dev.local.everydayfutures.museum.state.StoryReducer
import dev.local.everydayfutures.museum.state.StoryState
import dev.local.everydayfutures.museum.state.Surface
import dev.local.everydayfutures.museum.state.WindowNavigator
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
    Row(modifier = modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(PolishTokens.SpaceMedium)) {
        // Weighted rather than fixed (about 436 dp at the default 880 dp width), so at the 800 dp minimum both halves shrink.
        Box(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
            StoryPresenter(
                objectId = story.objectId,
                chapter = story.chapter,
                reduceMotion = reduceMotion,
                controller = controller,
                modifier = Modifier.fillMaxSize(),
            )
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
