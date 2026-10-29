plugins {
  id("com.android.application") version "9.4.1" apply false
  id("androidx.room3") version "3.0.3" apply false
  id("com.google.devtools.ksp") version "2.3.12"
}

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
  }
}