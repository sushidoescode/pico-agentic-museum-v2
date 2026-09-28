package dev.local.picopolish

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How much optical treatment the kit applies on top of the native PICO presentation.
 *
 * This is a project-owned contract, not a PICO API.
 *
 * - [Native] is the supported baseline and must already look finished: system window glass,
 *   built-in components, native hover/press/haptics, one material or one fill per surface.
 * - [Enhanced] adds a restrained extra layer on top of that baseline — a hairline edge, and a small
 *   depth lift on interactive custom surfaces. Every enhancement is built from an API that is itself
 *   a no-op where the platform does not support it, and [PolishOptions.resolve] downgrades the whole
 *   mode to [Native] when the host says the enhancement is unsupported. Turning it off never removes
 *   functionality.
 *
 *   Element-grade glass on small controls **used to** be an Enhanced-only addition. It is not any
 *   more: the native mode drew `fillTertiary` there instead, and a frame showed that a 3.9 %-black
 *   wash leaves its own text black-on-black over a dark background. Legibility is not a quality tier,
 *   so `Material.Thin` moved down into the baseline and Enhanced keeps the edge and the lift.
 */
enum class PolishQuality {
    Native,
    Enhanced,
}

/**
 * The accessibility and quality policy a screen runs under.
 *
 * The three accessibility flags are independent of [quality] and of each other: every one of the
 * sixteen combinations is a usable screen. None of them removes an action, a control or a state.
 *
 * @property quality the requested optical treatment; see [PolishQuality].
 * @property reduceMotion a policy this type *carries*; the kit ships no animations of its own, so
 *   setting it changes nothing until the consuming app routes its animations through
 *   [PolishTokens.motion]. It is app-owned on purpose: the SDK pins the Compose motion duration
 *   scale to 1 from a ContentProvider at process start, so a system "remove animations" preference
 *   never reaches SpatialUI animations. Unlike [reduceTransparency] and [increaseContrast], which
 *   the components honour themselves, this one needs a line of code from the app.
 * @property reduceTransparency surfaces stop using the compositor glass and take the kit's
 *   **literal** opaque ground, [PolishTokens.OpaqueGround], with the light label that pairs with it.
 *   Opacity is never stacked over glass.
 *
 *   It has to be a literal. The obvious choice — `ColorScheme.fillPrimary`, the scheme's "opaque"
 *   fill role — is a vibrancy sentinel that the compositor resolves by *darkening what is behind the
 *   surface*, so a busy background still reads through it. Four admitted frames showed it; the token's
 *   KDoc has them. A setting whose whole purpose is that the background stops showing through cannot
 *   be built on a role whose value the background decides.
 * @property increaseContrast surfaces always carry their edge, and the edge and secondary text move
 *   to stronger label roles.
 */
@Immutable
data class PolishOptions(
    val quality: PolishQuality = PolishQuality.Native,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val increaseContrast: Boolean = false,
) {
    /**
     * The options actually applied under [capabilities]: [PolishQuality.Enhanced] falls back to
     * [PolishQuality.Native] when the host reports the enhancement layer unsupported. The
     * accessibility flags are never altered by capability.
     */
    fun resolve(capabilities: PolishCapabilities): PolishOptions =
        if (quality == PolishQuality.Enhanced && !capabilities.enhancedSupported) {
            copy(quality = PolishQuality.Native)
        } else {
            this
        }
}

/**
 * What the host running this app can actually render.
 *
 * The kit does not probe the platform: the SDK's own support predicates are library-internal, and
 * guessing from a build fingerprint would be worse than asking. The consuming app decides — from a
 * setting, a remote flag, or a native check it has evidence for — and the kit obeys.
 *
 * Leaving [enhancedSupported] at its default is safe even off a spatial device, because every
 * enhancement the kit adds is drawn through an API that no-ops there; the flag exists so an app can
 * retire the enhanced mode deliberately (for example after a measurement shows it does not pay for
 * itself on a given device) without touching call sites.
 */
@Immutable
data class PolishCapabilities(val enhancedSupported: Boolean = true)

/**
 * Who owns the background of the window container this content sits in.
 *
 * The kit cannot detect this: a container's material is resolved when the container is *declared*
 * (manifest meta-data `pico.spatial.windowcontainer.materialbackground`, or the DSL's
 * `enableMaterialBackground`), and there is no runtime read-back. So the app states it, exactly as
 * it states [PolishCapabilities].
 *
 * It matters for one reason: the glass budget in [PolishTokens.MaxGlassDepth] counts the window's
 * own material. A window that already spends one layer leaves the app one; a window whose material
 * was switched off leaves the app two.
 */
enum class PolishWindowSurface {
    /**
     * The system draws the container's panel — the default, and what almost every app wants.
     * `DefaultWindowContainer` with `materialbackground="1"`, or a DSL container left at `null`.
     */
    SystemGlass,

    /**
     * The container's material was switched off at declaration time and the app owns the root
     * through [Modifier.polishWindowRoot], which re-applies `Material.Regular`.
     *
     * Still one glass layer — the app is drawing it instead of the system.
     */
    AppGlass,

    /**
     * The container's material was switched off and the app root is opaque: `reduceTransparency`, a
     * backdrop fixture, or a product that simply does not want glass.
     *
     * Spends no glass layer, so the app may place one glass surface *and* one more beneath the
     * budget than in the other two cases.
     */
    AppOpaque,
    ;

    /** How many of [PolishTokens.MaxGlassDepth] layers this window has already spent. */
    val baseGlassDepth: Int
        get() = if (this == AppOpaque) 0 else 1
}

/** The resolved options for the current subtree. Provided by `PolishTheme`. */
val LocalPolishOptions = compositionLocalOf { PolishOptions() }

/** The capabilities the app declared. Provided by `PolishTheme`. */
val LocalPolishCapabilities = staticCompositionLocalOf { PolishCapabilities() }

/** Who owns this window's background. Provided by `PolishTheme`. */
val LocalPolishWindowSurface = compositionLocalOf { PolishWindowSurface.SystemGlass }

/**
 * How many compositor-material layers are already stacked above this point, counting the window's.
 *
 * `PolishTheme` seeds it from [PolishWindowSurface.baseGlassDepth]; every [PolishSurface] that takes
 * a material increments it for its children. A surface whose depth has reached
 * [PolishTokens.MaxGlassDepth] takes its drawn role fill instead — see that constant for why.
 */
val LocalPolishGlassDepth = compositionLocalOf { PolishWindowSurface.SystemGlass.baseGlassDepth }
