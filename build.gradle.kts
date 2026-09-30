import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.changelog")
    id("org.jetbrains.intellij.platform")
}

dependencies {
    testImplementation(libs.junit)
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.tukaani:xz:1.11")

    intellijPlatform {
        clion("2026.2.3")
        bundledPlugin("com.intellij.clion")
        bundledPlugin("com.intellij.cmake")
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    publishing {
        providers.gradleProperty("intellijPlatformPublishingToken").orNull?.let {
            token = it
        }

        val channel = providers.gradleProperty("publishChannel").orNull
        channels = if (channel == null || channel == "stable") {
            emptyList()
        } else {
            listOf(channel)
        }
    }
}

kotlin {
    jvmToolchain(25)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
