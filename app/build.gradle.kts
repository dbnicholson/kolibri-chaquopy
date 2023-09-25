// Gradle build script
// https://docs.gradle.org/
// https://docs.gradle.org/current/kotlin-dsl/index.html

import com.android.build.api.dsl.ManagedVirtualDevice
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantOutputConfiguration.OutputType
import de.undercouch.gradle.tasks.download.Download
import groovy.json.JsonSlurper
import java.io.FileInputStream
import java.time.Instant
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.chaquo.python")
    id("de.undercouch.download")
}

val exploreVersion: String by project
val collectionsVersion: String by project

// Configure the app's versionCode. We do this once here so that all
// variants use the same version.
var versionCode: Int
if (project.hasProperty("versionCode")) {
    println("Using versionCode property")
    val versionCodeProp = project.property("versionCode") as String
    versionCode = versionCodeProp.toInt()
} else {
    // Use the current time in seconds.
    println("Using current time for versionCode")
    versionCode = Instant.now().getEpochSecond().toInt()
}
println("versionCode is " + versionCode)

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

        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    signingConfigs {
        val uploadPropFile = rootProject.file("upload.properties")
        if (uploadPropFile.exists()) {
            val uploadProps = Properties()
            uploadProps.load(FileInputStream(uploadPropFile))

            create("upload") {
                storeFile = file(uploadProps["storeFile"] as String)
                storePassword = uploadProps["storePassword"] as String
                keyAlias = uploadProps["keyAlias"] as String
                keyPassword = uploadProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("upload")
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
    dest(layout.buildDirectory.file("download/apps-bundle-${exploreVersion}.zip"))
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

// Download and extract loading-screen.zip into the build assets directory.
val downloadLoadingScreenTask = tasks.register<Download>("downloadLoadingScreen") {
    src("https://github.com/endlessm/kolibri-explore-plugin/releases/download/v${exploreVersion}/loading-screen.zip")
    dest(layout.buildDirectory.file("download/loading-screen-${exploreVersion}.zip"))
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

val extractLoadingScreenTask = tasks.register<CopyDirectoryTask>("extractLoadingScreen") {
    from(zipTree(downloadLoadingScreenTask.map { it.outputs.files.singleFile })) {
        // addGeneratedSourceDirectory takes the contents of the
        // directory, so prepend an additional directory in the output.
        eachFile {
            relativePath = relativePath.prepend("loadingScreen")
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("loadingScreen"))
}

// Download and extract collections.zip into the python source
// directory. Chaquopy will automatically extract its data files to the
// filesystem at runtime.
val collectionsDirectory: Directory = layout.projectDirectory.dir("src/main/python/testapp/collections")

val downloadCollectionsTask = tasks.register<Download>("downloadCollections") {
    src("https://github.com/endlessm/endless-key-collections/releases/download/v${collectionsVersion}/collections.zip")
    dest(layout.buildDirectory.file("download/collections-${collectionsVersion}.zip"))
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
val variants = ArrayList<Variant>()

// AGP extension API
// https://developer.android.com/build/extend-agp
androidComponents {
    onVariants { variant ->
        // Keep track of the variant for use in afterEvalute.
        variants.add(variant)

        // Add extracted loadingScreen assets directory.
        variant.sources.assets?.addGeneratedSourceDirectory(
            extractLoadingScreenTask,
            CopyDirectoryTask::outputDir
        )

        // Set the versionCode.
        val taskVariant = variant.name.replaceFirstChar { it.uppercase() }
        val versionTask = tasks.register<Exec>("output${taskVariant}Version") {
            val pkgdir: Provider<Directory> = layout.buildDirectory.dir("python/pip/${variant.name}/common")
            val output: Provider<RegularFile> = layout.buildDirectory.file("outputs/version-${variant.name}.json")
            commandLine(
                "./scripts/versions.py",
                "--version-code",
                versionCode.toString(),
                "--pkgdir",
                pkgdir.get().asFile.path,
                "--output",
                output.get().asFile.path
            )
            inputs.dir(pkgdir)
            outputs.file(output)
        }
        variant.outputs
            .filter { it.outputType == OutputType.SINGLE }
            .forEach {
                it.versionCode.set(versionCode)
                it.versionName.set(
                    versionTask.map { task ->
                        val versionFile = task.outputs.files.singleFile
                        val slurper = JsonSlurper()
                        @Suppress("UNCHECKED_CAST")
                        val versionData = slurper.parse(versionFile) as Map<String, String>
                        versionData.getValue("versionName")
                    }
                )
            }
    }
}

// For some reason, extractLoadingScreenTask isn't added as a dependency
// of the task that handles the generated source directory. Hook into
// preBuild to ensure it runs.
tasks.named("preBuild").configure {
    dependsOn(extractLoadingScreenTask)
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

        variants.forEach { variant ->
            val taskVariant = variant.name.replaceFirstChar { it.uppercase() }
            val versionTask = tasks.named("output${taskVariant}Version")
            val requirementsTask = tasks.named("generate${taskVariant}PythonRequirements")
            versionTask.configure {
                dependsOn(requirementsTask)
            }
        }
    }
}

// Make the generic clean task depend on our custom clean tasks.
tasks.named("clean").configure {
    dependsOn(cleanAppsBundleTask)
    dependsOn(cleanCollectionsTask)
}
