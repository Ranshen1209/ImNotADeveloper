import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseKeystorePath = providers.gradleProperty("imnotadeveloper.keystore.path")
    .orElse(providers.environmentVariable("IMNOTADEVELOPER_KEYSTORE_PATH"))
    .orElse(layout.projectDirectory.file("release-keystore.jks").asFile.absolutePath)
val releaseKeystoreFile = file(releaseKeystorePath.get())
val releaseStorePassword = providers.environmentVariable("storePassword")
val releaseKeyAlias = providers.environmentVariable("keyAlias")
val releaseKeyPassword = providers.environmentVariable("keyPassword")
val isReleasePackagingTaskRequested = gradle.startParameter.taskNames.any { taskName ->
    val normalized = taskName.substringAfterLast(':')
    normalized.startsWith("assembleRelease") ||
        normalized.startsWith("bundleRelease") ||
        normalized.startsWith("packageRelease")
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.auag0.imnotadeveloper"
    //noinspection GradleDependency
    compileSdk = 36
    ndkVersion = "30.0.14904198"

    defaultConfig {
        applicationId = "io.github.auag0.imnotadeveloper"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }
    signingConfigs {
        create("release") {
            storeFile = releaseKeystoreFile
            storePassword = releaseStorePassword.orNull
            keyAlias = releaseKeyAlias.orNull
            keyPassword = releaseKeyPassword.orNull
        }
    }
    buildTypes {
        release {
            if (isReleasePackagingTaskRequested) {
                if (!releaseKeystoreFile.exists()) {
                    throw GradleException("Release keystore not found. Set IMNOTADEVELOPER_KEYSTORE_PATH or -Pimnotadeveloper.keystore.path.")
                }
                if (releaseStorePassword.orNull.isNullOrBlank() ||
                    releaseKeyAlias.orNull.isNullOrBlank() ||
                    releaseKeyPassword.orNull.isNullOrBlank()
                ) {
                    throw GradleException("Release signing env vars storePassword, keyAlias, and keyPassword must be set.")
                }
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            merges += "META-INF/xposed/*"
            excludes += "/kotlin/**"
            excludes += "/kotlin-tooling-metadata.json"
        }
    }
    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")

    compileOnly("io.github.libxposed:api:101.0.1")
    implementation("io.github.libxposed:service:101.0.0")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
