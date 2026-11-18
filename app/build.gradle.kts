import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun signingProp(name: String): String? {
    findProperty(name)?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    for (fileName in listOf("local.properties", "keystore.properties")) {
        val propsFile = rootProject.file(fileName)
        if (propsFile.isFile) {
            val props = Properties()
            propsFile.inputStream().use { props.load(it) }
            props.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
    }
    return null
}

val releaseStoreFilePath = signingProp("RELEASE_STORE_FILE")
val releaseStorePassword = signingProp("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingProp("RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingProp("RELEASE_KEY_PASSWORD")
val releaseStoreFile = releaseStoreFilePath?.let { path ->
    val candidate = File(path)
    if (candidate.isAbsolute) candidate else rootProject.file(path)
}
val hasReleaseSigning = releaseStoreFile != null &&
    releaseStoreFile.isFile &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank()

if (!hasReleaseSigning) {
    logger.warn("Release signing config not found in local.properties/keystore.properties/env; release build will be unsigned.")
}

android {
    namespace = "io.github.xvsu.quickspacewidget"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.xvsu.quickspacewidget"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
