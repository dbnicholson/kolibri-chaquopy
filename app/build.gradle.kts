// Gradle build script
// https://docs.gradle.org/
// https://docs.gradle.org/current/kotlin-dsl/index.html

import com.android.build.api.dsl.ManagedVirtualDevice
import de.undercouch.gradle.tasks.download.Download

plugins {
    id("com.android.application")
    id("com.chaquo.python")
    id("de.undercouch.download")
}

val exploreVersion: String by project
val collectionsVersion: String by project

// Android (AGP) configuration
// https://developer.android.com/build/
// https://developer.android.com/reference/tools/gradle-api
android {
    namespace = "org.endlessos.testapp"

    compileSdk = 31

    defaultConfig {
        applicationId = "org.endlessos.testapp"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
        managedDevices {
            devices {
                maybeCreate<ManagedVirtualDevice>("pixel2api30").apply {
                    // Use device profiles you typically see in Android Studio.
                    device = "Pixel 2"
                    // Use only API levels 27 and higher.
                    apiLevel = 30
                    // To include Google services, use "google".
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

// Chaquopy configuration
// https://chaquo.com/chaquopy/doc/15.0/android.html
chaquopy {
    defaultConfig {
        version = "3.9"

        pip {
            install("https://github.com/learningequality/kolibri/releases/download/v0.16.0-beta5/kolibri-0.16.0b5-py2.py3-none-any.whl")
            install("kolibri-explore-plugin==${exploreVersion}")
        }

        // Django migrations and management commands work by looking for
        // modules in the filesystem, so any packages containing them
        // need to be extracted rather than loaded direcly from the
        // asset zip file.
        extractPackages("kolibri")
        extractPackages("kolibri_explore_plugin")
    }
}

dependencies {
    testImplementation("junit:junit:4.+")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.core:core-ktx:1.3.2")
}

// Enable Java deprecation warnings
tasks.withType<JavaCompile>().configureEach {
    options.setDeprecation(true)
}

// Tasks
// https://docs.gradle.org/current/userguide/more_about_tasks.html

// Download and extract apps-bundle.zip into the python source
// directory. Chaquopy will automatically extract its data files to the
// filesystem at runtime.
val appsBundleDirectory: Directory = layout.projectDirectory.dir("src/main/python/testapp/apps")

val downloadAppsBundleTask = tasks.register<Download>("downloadAppsBundle") {
    src("https://github.com/endlessm/kolibri-explore-plugin/releases/download/v${exploreVersion}/apps-bundle.zip")
    dest(layout.buildDirectory.file("download/apps-bundle.zip"))
    onlyIfModified(true)
    useETag(true)
}

val extractAppsBundleTask = tasks.register<Copy>("extractAppsBundle") {
    from(zipTree(downloadAppsBundleTask.map { it.outputs.files.singleFile })) {
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(appsBundleDirectory)
}

val cleanAppsBundleTask = tasks.register<Delete>("cleanAppsBundle") {
    delete(appsBundleDirectory)
}

// Download and extract welcome-screen.zip into the build assets directory.
val downloadWelcomeScreenTask = tasks.register<Download>("downloadWelcomeScreen") {
    src("https://github.com/endlessm/kolibri-explore-plugin/releases/download/v${exploreVersion}/welcome-screen.zip")
    dest(layout.buildDirectory.file("download/welcome-screen.zip"))
    onlyIfModified(true)
    useETag(true)
}

// AGP's addGeneratedSourceDirectory wants a DirectoryProperty, but Copy
// doesn't provide one.
abstract class CopyDirectoryTask : Copy() {
    @get:Internal
    val outputDir: DirectoryProperty
        get() = project.getObjects().directoryProperty().fileValue(getDestinationDir())
}

val extractWelcomeScreenTask = tasks.register<CopyDirectoryTask>("extractWelcomeScreen") {
    from(zipTree(downloadWelcomeScreenTask.map { it.outputs.files.singleFile })) {
        // addGeneratedSourceDirectory takes the contents of the
        // directory, so prepend an additional directory in the output.
        eachFile {
            relativePath = relativePath.prepend("welcomeScreen")
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("welcomeScreen"))
}

// Download and extract collections.zip into the python source
// directory. Chaquopy will automatically extract its data files to the
// filesystem at runtime.
val collectionsDirectory: Directory = layout.projectDirectory.dir("src/main/python/testapp/collections")

val downloadCollectionsTask = tasks.register<Download>("downloadCollections") {
    src("https://github.com/endlessm/endless-key-collections/releases/download/v${collectionsVersion}/collections.zip")
    dest(layout.buildDirectory.file("download/collections.zip"))
    onlyIfModified(true)
    useETag(true)
}

val extractCollectionsTask = tasks.register<Copy>("extractCollections") {
    from(zipTree(downloadCollectionsTask.map { it.outputs.files.singleFile }))
    into(collectionsDirectory)
}

val cleanCollectionsTask = tasks.register<Delete>("cleanCollections") {
    delete(collectionsDirectory)
}

// Connect our tasks to external tasks.

// AGP extension API
// https://developer.android.com/build/extend-agp
androidComponents {
    onVariants { variant ->
        // Add extracted welcomeScreen assets directory.
        variant.sources.assets?.addGeneratedSourceDirectory(
            extractWelcomeScreenTask,
            CopyDirectoryTask::outputDir
        )
    }
}

// For some reason, extractWelcomeScreenTask isn't added as a dependency
// of the task that handles the generated source directory. Hook into
// preBuild to ensure it runs.
tasks.named("preBuild").configure {
    dependsOn(extractWelcomeScreenTask)
}

// In order to support older AGP versions, chaquopy creates its tasks
// from afterEvaluate. In order to hook into those, we need to use an
// action from an inner afterEvaluate so that it runs after all
// previously added afterEvaluate actions complete.
//
// https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api/-project/after-evaluate.html
project.afterEvaluate {
    project.afterEvaluate {
        // Add extracted apps-bundle and collections files as inputs to
        // extracting the local python files.
        tasks.named("extractPythonBuildPackages").configure {
            inputs.files(extractAppsBundleTask.map { it.outputs.files })
            inputs.files(extractCollectionsTask.map { it.outputs.files })
        }
    }
}

// Make the generic clean task depend on our custom clean tasks.
tasks.named("clean").configure {
    dependsOn(cleanAppsBundleTask)
    dependsOn(cleanCollectionsTask)
}
