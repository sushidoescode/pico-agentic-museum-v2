// spatial-ui-polish — the canonical reusable SpatialUI polish module (Android library).
//
// Consumed by an app through `include(":polish")` + `project(":polish").projectDir = file(<path>)`
// (see the skill's reuse guide). The module pins nothing about the consumer's BOM: it declares the
// PICO Spatial BOM version it was verified against through the consumer's version catalog alias
// `libs.spatial.bom`, so a consumer that resolves a different SDK sees the mismatch in its own
// catalog, not in a hidden transitive pin. Verified resolution: com.pico.spatial:bom:6.1.9.
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.local.picopolish"
    compileSdk = 35

    defaultConfig {
        minSdk = 35
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures { compose = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric's Compose window capture needs the hardware PixelCopy path; without it
            // captureToImage() times out waiting for a redraw that software rendering never reports.
            it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            it.systemProperty("polish.render.out", System.getProperty("polish.render.out") ?: "")
        }
    }
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = catalog.findLibrary(alias).orElseThrow {
    IllegalStateException("spatial-ui-polish needs `$alias` in the consumer's libs.versions.toml")
}

dependencies {
    api(platform(lib("spatial-bom")))
    api(lib("spatial-core"))
    api(lib("spatial-ui-platform"))
    api(lib("spatial-ui-foundation"))
    api(lib("spatial-ui-design"))
    implementation(lib("androidx-annotation"))

    testImplementation(lib("junit"))
    // Host-side Compose behaviour tests: Robolectric runs the Compose runtime on the JVM; the
    // ui-test artifacts match the Compose base the PICO fork declares (runtime-android 1.10.4).
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.10.4")
    testImplementation("androidx.test:core-ktx:1.6.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.4")
}

// The PICO SpatialUI fork replaces these androidx artifacts; keep the same exclusions the official
// template applies so the library resolves the identical Compose classes as its consumer.
configurations.all {
    resolutionStrategy {
        exclude("androidx.compose.ui", "ui")
        exclude("androidx.compose.ui", "ui-graphics")
        exclude("androidx.compose.ui", "ui-text")
        exclude("androidx.compose.foundation", "foundation")
    }
}
