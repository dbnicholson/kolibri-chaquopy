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
    logger.info("Using versionCode property")
    val versionCodeProp = project.property("versionCode") as String
    versionCode = versionCodeProp.toInt()
} else {
    // Use the current time in seconds.
    logger.info("Using current time for versionCode")
    versionCode = Instant.now().getEpochSecond().toInt()
}
logger.quiet("Using versionCode {}", versionCode)

// Android (AGP) configuration
// https://developer.android.com/build/
// https://developer.android.com/reference/tools/gradle-api
android {
    namespace = "org.endlessos.testapp"

    compileSdk = 33

    defaultConfig {
        applicationId = "org.endlessos.testapp"
        minSdk = 24
        targetSdk = 33

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
            signingConfig = signingConfigs.findByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
            install("kolibri-zim-plugin==1.4.2")
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
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
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
    from(zipTree(downloadAppsBundleTask.map { it.dest })) {
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
abstract class CollectBuildAssetsTask : DefaultTask() {
    @get:InputFile
    abstract val loadingScreenZip: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val proj = getProject()
        val dest = outputDir.get()
        proj.delete(dest)
        proj.mkdir(dest)
        proj.copy {
            from(proj.zipTree(loadingScreenZip.get())) {
                eachFile {
                    relativePath = relativePath.prepend("loadingScreen")
                }
                includeEmptyDirs = false
            }
            into(dest)
        }
    }
}

val collectBuildAssetsTask = tasks.register<CollectBuildAssetsTask>("collectBuildAssets") {
    inputs.files(downloadLoadingScreenTask)
    loadingScreenZip.set(
        // Coerce the File into a RegularFileProperty.
        downloadLoadingScreenTask.flatMap {
            getObjects().fileProperty().fileValue(it.dest)
        }
    )
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
    from(zipTree(downloadCollectionsTask.map { it.dest }))
    into(collectionsDirectory)
}

val cleanCollectionsTask = tasks.register<Delete>("cleanCollections") {
    delete(collectionsDirectory)
}

// Create a task per variant that generates a JSON file with version
// names that will be used to set versionName below.
fun createVersionTask(variantName: String): TaskProvider<Exec> {
    val taskVariant = variantName.replaceFirstChar { it.uppercase() }
    return tasks.register<Exec>("output${taskVariant}Version") {
        val pkgdir = layout.buildDirectory.dir("python/pip/${variantName}/common")
        val output = layout.buildDirectory.file("outputs/version-${variantName}.json")
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
}

// Create a task per variant that prunes unwanted files from the
// extracted python packages.
fun createPruneTask(variantName: String): TaskProvider<Exec> {
    val taskVariant = variantName.replaceFirstChar { it.uppercase() }
    return tasks.register<Exec>("prune${taskVariant}PythonPackages") {
        val pkgroot = layout.buildDirectory.dir("python/pip/${variantName}")
        val report = layout.buildDirectory.file("outputs/logs/prune-${variantName}-report.txt")
        commandLine(
            "./scripts/prune.py",
            "--pkgroot",
            pkgroot.get().asFile.path,
            "--report",
            report.get().asFile.path
        )
    }
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
            collectBuildAssetsTask,
            CollectBuildAssetsTask::outputDir
        )

        // Set the versionCode.
        val versionTask = createVersionTask(variant.name)
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
            inputs.files(extractAppsBundleTask)
            inputs.files(extractCollectionsTask)
        }

        variants.forEach { variant ->
            val taskVariant = variant.name.replaceFirstChar { it.uppercase() }
            val requirementsTask = tasks.named("generate${taskVariant}PythonRequirements")
            val requirementsAssetsTask = tasks.named("generate${taskVariant}PythonRequirementsAssets")

            // Order the version task after the packages have been extracted.
            val versionTask = tasks.named("output${taskVariant}Version")
            versionTask.configure {
                inputs.files(requirementsTask)
            }

            // Order the pruning task after the packages have been
            // extracted but before they've been zipped into assets.
            val pruneTask = createPruneTask(variant.name)
            pruneTask.configure {
                inputs.files(requirementsTask)
            }
            requirementsAssetsTask.configure {
                // dependsOn is used here instead of wiring the prune
                // task outputs since there aren't any outputs.
                dependsOn(pruneTask)
            }
        }
    }
}

// Make the generic clean task depend on our custom clean tasks.
tasks.named("clean").configure {
    dependsOn(cleanAppsBundleTask)
    dependsOn(cleanCollectionsTask)
}
