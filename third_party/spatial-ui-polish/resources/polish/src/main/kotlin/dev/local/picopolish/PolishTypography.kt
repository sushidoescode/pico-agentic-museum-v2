package dev.local.picopolish

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import com.pico.spatial.ui.design.PicoTheme

/**
 * The kit's semantic type ladder: **nine jobs across the Spatial SDK's typography roles**.
 *
 * Nine jobs, eleven rungs: [bodyLead] and [body] each carry a single-line and a multiline variant, so
 * [ladder] returns eleven entries. A specimen renders all eleven.
 *
 * ## Why this exists
 *
 * `PicoTheme.typography` ships 18 roles in 6.1.9, and they are *alphabetical*, not a ramp. **Four**
 * of them are 16 sp (`bodyLarge`, `bodyLargeMultiline`, `headlineSmall`, `titleMedium`), **four** are
 * 14 sp (`bodyMedium`, `bodyMediumMultiline`, `labelLarge`, `titleSmall`), two are 12 sp
 * (`bodySmall`, `labelMedium`) and two are 20 sp (`headlineMedium`, `titleLarge`) — so "pick a role"
 * is not the same question as "pick a rank". Worse, two roles are byte-identical: `labelLarge` and
 * `titleSmall` are both 14 sp / 18 lh / W600 over the same base style, so a screen that uses one for
 * a row title and the other for a button label has expressed no distinction at all.
 *
 * The kit therefore does not invent sizes — inventing a size would fight the shell, which draws its
 * own chrome with these roles — it **chooses which role carries which job**, and states the step
 * between consecutive jobs so the choice is checkable.
 *
 * ## The ladder, and the step between each rung
 *
 * | Job | SDK role | size / weight / line height | step to the rung above |
 * |---|---|---|---|
 * | [display] | `displaySmall` | 28 sp / 700 / 34 | — |
 * | [windowTitle] | `headlineMedium` | 20 sp / 700 / 26 | 1.40× |
 * | [sectionTitle] | `headlineSmall` | 16 sp / 700 / 20 | 1.25× |
 * | [objectTitle] | `titleMedium` | 16 sp / 600 / 20 | same size, −100 weight |
 * | [bodyLead] | `bodyLarge` / `bodyLargeMultiline` | 16 sp / 500 / 20 or 24 | same size, −100 weight |
 * | [body] | `bodyMedium` / `bodyMediumMultiline` | 14 sp / 500 / 18 or 22 | 1.14× |
 * | [control] | `labelLarge` | 14 sp / 600 / 18 | same size, +100 weight |
 * | [caption] | `bodySmall` | 12 sp / 500 / 16 | 1.17× |
 * | [overline] | `labelMedium` | 12 sp / 600 / 16 | same size, +100 weight |
 *
 * Distinct sizes: **28 → 20 → 16 → 14 → 12**, with a deliberate floor at 12 sp — the SDK's
 * `labelSmall` (11 sp) and `bodyTiny` (10 sp) are not on this ladder at all. Weight does the rest: 700 for the two header
 * rungs, 600 for titles and control text, 500 for prose.
 *
 * ## The rule this encodes
 *
 * **Rank is carried by size *and* weight, never by one alone, and never by colour.** The kit's own
 * first version broke it: an object title was `titleSmall` (14/600) directly above body text at
 * `bodyMedium` (14/500) — a 100-unit weight step at identical size, which is the weakest distinction
 * the SDK can express. [objectTitle] is `titleMedium` (16 sp) for exactly that reason: it puts a
 * size step back between a row's subject and its prose.
 *
 * ## What a rendered frame can and cannot settle about this
 *
 * An emulator capture frames a whole room, so the window occupies a small part of it and **absolute
 * legibility at wearing distance is not decidable from one**. What a capture does settle is
 * *relative* rank: whether two rungs are visibly different. Treat every claim here as a claim about
 * rank until a headset frame and a wearer say otherwise.
 *
 * Source: SDK 6.1.9 `design/tokens/TypeScaleTokens.kt:148-203` (the numbers) and
 * `design/tokens/TypographyTokens.kt:33-140` (the assembly); role list `design/Typography.kt:45-62`.
 * No role carries a colour — every one leaves `TextStyle.color` unspecified — which is why
 * [PolishTheme] must provide `LocalContentColor` separately.
 */
object PolishTypography {

    /** A hero line for a focus or detail window. One per app, at most. `displaySmall`, 28 sp / 700. */
    val display: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.displaySmall

    /** The window's own name. One per window. `headlineMedium`, 20 sp / 700. */
    val windowTitle: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.headlineMedium

    /** The header of a group of controls inside a window. `headlineSmall`, 16 sp / 700. */
    val sectionTitle: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.headlineSmall

    /**
     * The subject of one row, card or list item. `titleMedium`, 16 sp / 600.
     *
     * Deliberately a size step above [body] rather than the weight-only step the SDK's `titleSmall`
     * would have given at the same 14 sp.
     */
    val objectTitle: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.titleMedium

    /** Text inside a control: a button label, a segment, a chip. `labelLarge`, 14 sp / 600. */
    val control: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.labelLarge

    /** A supporting line under a title or a value. `bodySmall`, 12 sp / 500. */
    val caption: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.bodySmall

    /**
     * The small label naming a value in a details pane. `labelMedium`, 12 sp / 600.
     *
     * Not `labelSmall` (11 sp), and that is the ladder's **floor**: the SDK ships `labelSmall` at
     * 11 sp and `bodyTiny` at 10 sp, and this kit uses neither. A label that names a value is text
     * someone has to read at arm's length through a compositor, and the two smallest roles in the
     * scale are reserved for the shell's own dense chrome, not for app content.
     */
    val overline: TextStyle
        @Composable @ReadOnlyComposable get() = PicoTheme.typography.labelMedium

    /**
     * The lead paragraph of a panel — one size above [body].
     *
     * @param multiline `true` takes `bodyLargeMultiline`, which is the same 16 sp / 500 with the line
     *   height opened from 20 to 24. The SDK ships that pair for exactly this purpose, and the kit's
     *   first version never used either multiline role, so every wrapped paragraph in the showcase
     *   was set at the tight single-line leading.
     */
    @Composable
    @ReadOnlyComposable
    fun bodyLead(multiline: Boolean = false): TextStyle =
        if (multiline) PicoTheme.typography.bodyLargeMultiline else PicoTheme.typography.bodyLarge

    /**
     * Ordinary prose. `bodyMedium` at 14 sp / 500.
     *
     * @param multiline `true` takes `bodyMediumMultiline` — same size and weight, line height 18 → 22.
     *   Pass `true` whenever the text can wrap; a paragraph set at single-line leading is the most
     *   common reason a panel reads as a wall.
     */
    @Composable
    @ReadOnlyComposable
    fun body(multiline: Boolean = false): TextStyle =
        if (multiline) PicoTheme.typography.bodyMediumMultiline else PicoTheme.typography.bodyMedium

    /**
     * Every rung of the ladder, in rank order, paired with the name the kit uses for it.
     *
     * This is what the specimen screen renders and what the ladder test walks, so a rung that is
     * added to the object without being ranked here fails a test rather than quietly appearing.
     */
    @Composable
    @ReadOnlyComposable
    fun ladder(): List<Pair<String, TextStyle>> =
        listOf(
            "display" to display,
            "windowTitle" to windowTitle,
            "sectionTitle" to sectionTitle,
            "objectTitle" to objectTitle,
            "bodyLead" to bodyLead(),
            "bodyLead·multiline" to bodyLead(multiline = true),
            "body" to body(),
            "body·multiline" to body(multiline = true),
            "control" to control,
            "caption" to caption,
            "overline" to overline,
        )
}
