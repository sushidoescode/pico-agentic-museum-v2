package dev.local.picopolish

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.isUnspecified
import com.pico.spatial.ui.foundation.vibrant.Vibrant
import com.pico.spatial.ui.foundation.vibrant.containsVibrant
import com.pico.spatial.ui.foundation.vibrant.obtainVibrant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2 contrast, for the cases where an app can actually compute it — and an explicit refusal for
 * the cases where it cannot.
 *
 * ## Read this before using any number it returns
 *
 * On a PICO device with vibrancy on, **10 of the 16 `ColorScheme` roles are not colours**. They are
 * `Color.Vibrant.withVibrant(level)` sentinels: `Color.Black` with a level written into bits 24–31
 * of the `ULong`, bits that `Color.red/green/blue` and `toArgb()` never read. All ten therefore read
 * back as `#FF000000` in the app process, and the real pixel is produced out of process by the
 * system compositor (`ColorVibrantEncoder.kt:24, 65-81`; `Color.kt:53-58` — *"This is NOT a standard
 * RGBA color"*). `fillPrimary`, `fillSecondary`, `fillTertiary`, `fillLight`, `labelPrimary`,
 * `labelSecondary`, `labelTertiary`, `labelQuaternary`, `lightenHover` and `lightenPressed` are all
 * in that set.
 *
 * A contrast ratio computed from two sentinels is not a wrong number — it is a number about the
 * wrong thing. It would report black on black, 1.00:1, for a pairing the compositor renders
 * perfectly legibly. So this object **refuses** rather than returning it: see
 * [PolishContrastResult.Undecidable].
 *
 * ## Where it is valid
 *
 * - **Off the spatial platform** — a JVM or Robolectric test, a preview, a non-PICO build. There
 *   `systemColorScheme` resolves to `defaultColorScheme()`, whose 16 roles are all literal ARGB, and
 *   every ratio here is exact. This is the lane the kit's own tests use.
 * - **For the 6 roles whose channels survive the compositor**: `error`, `alert`, `passable` and
 *   `interaction` are literal in both schemes; `labelPrimaryLight` and `dividerLine` carry a
 *   `Vibrant.None` tag in the vibrant scheme, which disables the effect and leaves their RGB
 *   authoritative — see [hasUnresolvedVibrancy], which is why they are no longer refused for it.
 *
 *   "Survives the compositor" is not the same as "measurable directly". `dividerLine` is
 *   `SubBlack12` at 12.2 % alpha, so [ratio] still refuses it as [PolishContrastLimit.TranslucentForeground]
 *   and you must composite it first with [ratioOver]. That refusal is the alpha rule working, not the
 *   vibrancy bug returning, and `theSixSupposedlyLiteralRolesAreActuallyReadable` pins the difference.
 *
 *   The four semantic ones carry a DisplayP3 colour space, and this object converts nothing — treat
 *   their ratios as sRGB approximations.
 * - **For app-owned colours** an app supplies itself.
 *
 * ## Where it is not, and what to use instead
 *
 * On device, against the compositor's glass, contrast has to be **measured from rendered pixels**.
 * That is what `evidence/.../tooling/polish_contrast.py` does against an admitted capture, and why
 * the kit's showcase renders uniform probe swatches for it to sample. Nothing computed here
 * substitutes for that; the two lanes answer different questions and are reported separately.
 */
object PolishContrast {

    /**
     * Whether this colour's channels are meaningless because the compositor will replace them.
     *
     * Not simply "does it carry a vibrancy encoding". `vibrantColorScheme()` builds two of its
     * sixteen roles as `<a real colour>.withVibrant(Vibrant.None)` — `labelPrimaryLight` is
     * `SubWhite100` and `dividerLine` is `SubBlack12`, both tagged with the level that **disables**
     * the vibrant effect (`ColorScheme.kt:235, :246`; `Vibrant.None` is `MaxVibrantType`,
     * `Vibrant.kt:102`). Their RGB is authoritative; the tag says so.
     *
     * The kit's first version asked only `containsVibrant()` and therefore refused those two roles as
     * undecidable — on a device, silently, and for exactly the two roles its own documentation
     * promised were measurable. A test now pins which roles carry an encoding
     * (`PolishDesignSystemTest.theSixSupposedlyLiteralRolesAreActuallyReadable`), and this predicate
     * asks the question that actually matters: is there a level that will *replace* the channels?
     */
    private fun Color.hasUnresolvedVibrancy(): Boolean =
        containsVibrant() && obtainVibrant() != Vibrant.None

    /**
     * How close to its target a ratio may land and still be treated as meeting it.
     *
     * Float arithmetic on linearised channels lands a genuine 4.50 on 4.4999995. The tolerance is
     * one part in ten thousand of a ratio point — far below any perceptual difference, and far below
     * the step between two adjacent role choices.
     */
    private const val Tolerance = 1e-4f

    /** WCAG's threshold for text below the large-text size, and the kit's default. */
    const val NormalText: Float = 4.5f

    /**
     * WCAG's threshold for large text, and for the visual boundary of a control.
     *
     * The second use is the one that matters most here: a switch track, a selection edge, a field
     * border and a focus ring are *graphical objects*, and 3:1 is their bar, not 4.5:1.
     */
    const val LargeText: Float = 3.0f

    /** Same value as [LargeText], named for the case where the thing measured is not text at all. */
    const val NonText: Float = 3.0f

    /**
     * Whether a text style qualifies for the [LargeText] allowance.
     *
     * WCAG states large text as **18 pt, or 14 pt bold**. Android has no pt; the conventional
     * Android reading — the one Material's own accessibility guidance uses — is **18 sp, or 14 sp
     * bold**, and that is what this implements. It is a convention, not a derivation: a strict
     * physical conversion from 18 pt at 160 dpi would give a much larger sp figure. The convention
     * is the conservative choice in practice and is the one a reviewer will expect.
     *
     * "Bold" is taken as [FontWeight.Bold] (700) or heavier. The SDK's `title*` and `label*` roles
     * are W600, which is SemiBold; they do **not** qualify, so they are held to [NormalText]. That is
     * deliberately stricter than counting 600 as bold.
     *
     * A style whose size is unspecified or is expressed in em rather than sp cannot be judged, and
     * returns `false` — the stricter answer.
     */
    fun isLargeText(style: TextStyle): Boolean {
        val size = style.fontSize
        if (size.isUnspecified || size.type != TextUnitType.Sp) return false
        val sp = size.value
        val bold = (style.fontWeight ?: FontWeight.Normal) >= FontWeight.Bold
        return sp >= LargeTextSp || (bold && sp >= LargeBoldTextSp)
    }

    /** The target a given text style must meet: [LargeText] if [isLargeText], else [NormalText]. */
    fun targetFor(style: TextStyle): Float = if (isLargeText(style)) LargeText else NormalText

    /**
     * Source-over composite of [foreground] onto an opaque [background].
     *
     * Use this before [ratio] whenever a role is translucent — and several are: `fillSecondary` is
     * 40 % white, `fillTertiary` is 3.9 % black, `fillLight` is 14.9 % white, `labelSecondary` 80 %
     * black, `labelTertiary` 65 %, `labelQuaternary` 40 %, `dividerLine` 12.2 %. Measuring one of
     * those against a backdrop without compositing it first ignores its alpha entirely and reports a
     * ratio the screen never shows.
     */
    fun composite(foreground: Color, background: Color): Color {
        val a = foreground.alpha
        if (a >= 1f) return foreground.copy(alpha = 1f)
        if (a <= 0f) return background.copy(alpha = 1f)
        return Color(
            red = foreground.red * a + background.red * (1f - a),
            green = foreground.green * a + background.green * (1f - a),
            blue = foreground.blue * a + background.blue * (1f - a),
            alpha = 1f,
        )
    }

    /**
     * WCAG relative luminance of an **opaque** colour.
     *
     * Channels are linearised with the WCAG 2 transfer function and weighted 0.2126 / 0.7152 /
     * 0.0722. Alpha is ignored, which is exactly why [ratio] refuses a translucent input rather than
     * calling this on one.
     */
    fun relativeLuminance(color: Color): Float {
        fun channel(v: Float): Float =
            if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()
        return 0.2126f * channel(color.red) +
            0.7152f * channel(color.green) +
            0.0722f * channel(color.blue)
    }

    /**
     * The contrast ratio of [foreground] against [background], or a stated reason it is undecidable.
     *
     * Refuses, in this order:
     *  1. either colour carries a vibrancy encoding — the value in hand is a sentinel, not a pixel;
     *  2. [background] is translucent — there is no defined backdrop to composite it onto, and
     *     guessing one (white? the window material?) would invent the answer;
     *  3. [foreground] is translucent — composite it yourself with [composite], or use [ratioOver].
     *
     * @param target what this pairing has to reach; defaults to [NormalText].
     */
    fun ratio(
        foreground: Color,
        background: Color,
        target: Float = NormalText,
    ): PolishContrastResult {
        if (foreground.hasUnresolvedVibrancy()) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.VibrantForeground)
        }
        if (background.hasUnresolvedVibrancy()) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.VibrantBackground)
        }
        if (background.alpha < 1f) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentBackground)
        }
        if (foreground.alpha < 1f) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentForeground)
        }
        val lf = relativeLuminance(foreground)
        val lb = relativeLuminance(background)
        val ratio = (max(lf, lb) + 0.05f) / (min(lf, lb) + 0.05f)
        return PolishContrastResult.Measured(ratio = ratio, target = target)
    }

    /**
     * As [ratio], but composites both colours onto an opaque [backdrop] first.
     *
     * This is the form to use for a real pairing in this design system, because a role fill is
     * usually translucent and the text sits on top of it: the backdrop is what is behind the surface,
     * the background is the surface's own fill, and the foreground is the label.
     */
    fun ratioOver(
        foreground: Color,
        background: Color,
        backdrop: Color,
        target: Float = NormalText,
    ): PolishContrastResult {
        if (foreground.hasUnresolvedVibrancy()) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.VibrantForeground)
        }
        if (background.hasUnresolvedVibrancy()) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.VibrantBackground)
        }
        if (backdrop.hasUnresolvedVibrancy()) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.VibrantBackdrop)
        }
        if (backdrop.alpha < 1f) {
            return PolishContrastResult.Undecidable(PolishContrastLimit.TranslucentBackdrop)
        }
        val surface = composite(background, backdrop)
        val label = composite(foreground, surface)
        return ratio(label, surface, target)
    }

    /** WCAG's large-text size in the conventional Android reading. See [isLargeText]. */
    private const val LargeTextSp = 18f

    /** WCAG's large-bold-text size in the conventional Android reading. See [isLargeText]. */
    private const val LargeBoldTextSp = 14f

    /** [PolishContrastResult.Measured.meets], factored out so the tolerance has one home. */
    internal fun meets(ratio: Float, target: Float): Boolean = ratio + Tolerance >= target
}

/** Why a contrast ratio could not be computed from the values in hand. */
enum class PolishContrastLimit {
    /** The foreground is a vibrancy sentinel; only the compositor knows its pixel. */
    VibrantForeground,

    /** The background is a vibrancy sentinel; only the compositor knows its pixel. */
    VibrantBackground,

    /** The backdrop passed to `ratioOver` is a vibrancy sentinel. */
    VibrantBackdrop,

    /** The background is translucent and no backdrop was supplied to composite it onto. */
    TranslucentBackground,

    /** The foreground is translucent; composite it first, or use `ratioOver`. */
    TranslucentForeground,

    /** The backdrop passed to `ratioOver` is itself translucent, so the stack has no floor. */
    TranslucentBackdrop,
}

/** The outcome of a contrast check: a number with its target, or a stated reason there is none. */
@Immutable
sealed interface PolishContrastResult {

    /** A real measurement. */
    @Immutable
    data class Measured(val ratio: Float, val target: Float) : PolishContrastResult {
        /** Whether [ratio] reaches [target], within the float tolerance the object documents. */
        val meets: Boolean
            get() = PolishContrast.meets(ratio, target)
    }

    /**
     * No measurement was possible, and this is why.
     *
     * Treat it as "measure this from a rendered frame instead", never as a pass and never as a
     * failure. It is the honest answer to a question the app process cannot answer.
     */
    @Immutable
    data class Undecidable(val limit: PolishContrastLimit) : PolishContrastResult
}
