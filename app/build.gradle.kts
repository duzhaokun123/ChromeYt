import java.util.Properties

plugins {
  id("com.android.application")
  id("com.google.devtools.ksp")
  id("androidx.room3")
}

val localProperties =
    Properties().apply {
      rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
    }

fun localOrEnv(name: String, env: String): String? =
    localProperties.getProperty(name) ?: System.getenv(env)

val releaseKeystore =
    localOrEnv("keystore", "KEYSTORE")?.let { rootProject.file(it) }

android {
  compileSdk = 37
  namespace = "org.matrix.chromext"

  defaultConfig {
    applicationId = "io.github.duzhaokun123.chromeyt"
    minSdk = 23
    targetSdk = 37
    versionCode = 19
    versionName = "4.1.0"

    buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    buildConfigField("String", "SCRIPT_HANDLER_NAME", "\"ChromeYt\"")
  }

  buildFeatures { buildConfig = true }

  signingConfigs {
    if (releaseKeystore != null) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = localOrEnv("storePassword", "KEY_STORE_PASSWORD")
        keyAlias = localOrEnv("keyAlias", "ALIAS")
        keyPassword = localOrEnv("keyPassword", "KEY_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isShrinkResources = true
      isMinifyEnabled = true
      proguardFiles("proguard-rules.pro")
      signingConfig =
          if (releaseKeystore != null) signingConfigs.getByName("release")
          else signingConfigs.getByName("debug")
    }
  }

  androidResources {
    additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x42")
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  lint {
    disable +=
        listOf(
            "Internationalization",
            "UnsafeIntentLaunch",
            "SetJavaScriptEnabled",
            "Usability:Icons",
        )
  }
}

dependencies {
  compileOnly("de.robv.android.xposed:api:82")
  implementation("org.luckypray:dexkit:2.3.0")
  implementation("androidx.annotation:annotation:1.11.0")
  implementation("com.github.iamironz:unsafe:0.0.6")
  implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
  implementation("androidx.room3:room3-runtime:3.0.3")
  ksp("androidx.room3:room3-compiler:3.0.3")
  implementation("androidx.core:core-ktx:1.19.1")
  implementation("io.ktor:ktor-client-core:3.6.1")
  implementation("io.ktor:ktor-client-android:3.6.1")
  implementation("androidx.browser:browser:1.10.0")
  implementation("com.hrakaroo:glob:0.9.0")

  testImplementation("junit:junit:4.13.2")
}

room3 {
  schemaDirectory("$projectDir/schemas")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xcollection-literals")
  }
}
