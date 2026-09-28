package dev.local.picopolish

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.design.Text
import com.pico.spatial.ui.design.TextArea
import com.pico.spatial.ui.design.TextFieldColors
import com.pico.spatial.ui.design.TextFieldDefaults

/**
 * A labelled, multi-line-capable text field built on the built-in `TextArea`.
 *
 * `TextArea` owns the field shell, the focus behaviour, the text selection toolbar, the scroll
 * indicator and the hover (which it correctly suppresses while focused) — so the wrapper adds only
 * the label above and the validation state below, and never re-styles the shell.
 *
 * Error handling is a real state: the message is rendered in the `error` role **and** reported to
 * accessibility through the semantics error property, so the failure is not conveyed by colour
 * alone. When there is no error, [supportingText] keeps the same slot, so the layout does not jump
 * as validation toggles.
 *
 * @param errorMessage non-null puts the field in its error state and replaces the supporting line.
 */
@Composable
fun PolishField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    showScrollIndicator: Boolean = false,
    kind: PolishSurfaceKind = LocalPolishSurfaceKind.current,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val options = LocalPolishOptions.current
    val labelColor = PolishSurfaceDefaults.secondaryForegroundFor(kind, options)
    val isError = errorMessage != null
    val errorText = errorMessage.orEmpty()
    // `TextArea` does not read LocalContentColor: it paints the typed text with
    // `colors.textColor`, whose default is `labelPrimary` — the dark label. Inside a surface that
    // went opaque, that is black text on the dark fill, which is the exact defect this kit exists
    // to prevent, occurring inside the kit. So the field carries its own option-aware palette.
    val colors = PolishFieldDefaults.colorsFor(kind, options)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(PolishTokens.SpaceSmall),
    ) {
        Text(text = label, style = PolishTypography.overline, color = labelColor)
        TextArea(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.fillMaxWidth().semantics { if (isError) this.error(errorText) },
            placeholder = placeholder?.let { { Text(text = it, color = labelColor) } },
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            minLines = minLines,
            maxLines = maxLines,
            showScrollIndicator = showScrollIndicator,
            interactionSource = interactionSource,
            colors = colors,
        )
        val helper = errorMessage ?: supportingText
        if (helper != null) {
            Text(
                text = helper,
                style = PolishTypography.caption,
                color =
                    if (isError) PolishFieldDefaults.errorMessageColor(kind, options) else labelColor,
            )
        }
    }
}

/** Colour choices for [PolishField]. */
object PolishFieldDefaults {

    /**
     * The field's palette for the surface it sits on.
     *
     * On a light-foreground (dark, opaque) surface the shell takes `fillSecondary` — the light
     * role — and the typed text takes the **dark** label that pairs with it. The first version
     * paired a light shell with the light label, which measured 4.06:1 on a rendered frame and
     * missed the 4.5:1 target for normal text; `labelPrimary` on the same shell measures about
     * 5.1:1. The field then reads the way a form field on a dark panel normally reads: a light well
     * with dark text, which is also the pairing the design system's own roles imply.
     *
     * Everywhere else the design system's defaults are used unchanged.
     */
    @Composable
    fun colorsFor(kind: PolishSurfaceKind, options: PolishOptions): TextFieldColors =
        if (PolishSurfaceDefaults.usesLightForeground(kind, options)) {
            TextFieldDefaults.textFieldColors(
                textColor = PicoTheme.colorScheme.labelPrimary,
                backgroundColor = PicoTheme.colorScheme.fillSecondary,
                // labelTertiary measured 3.18-3.49:1 on rendered light surfaces — below the 4.5:1
                // a placeholder needs, since a placeholder is normal text. See
                // PolishSurfaceDefaults.secondaryForegroundFor for the frame and the numbers.
                placeholderColor = PicoTheme.colorScheme.labelSecondary,
            )
        } else {
            TextFieldDefaults.textFieldColors()
        }

    /**
     * The colour of the validation message under a field.
     *
     * The `error` role carries the state on the field's own border, where the 3:1 large-element
     * target governs and it measures about 4.1:1 against the opaque fill. The message is normal
     * text, where 4.5:1 governs and that same red does not reach it — so on a light-foreground
     * surface the message takes the readable label instead. The state is still not colour-only: the
     * border stays in the error role and the field carries the semantics error property.
     */
    @Composable
    fun errorMessageColor(kind: PolishSurfaceKind, options: PolishOptions): Color =
        if (PolishSurfaceDefaults.usesLightForeground(kind, options)) {
            PicoTheme.colorScheme.labelPrimaryLight
        } else {
            PicoTheme.colorScheme.error
        }
}
