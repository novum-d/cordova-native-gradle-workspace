plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

check(gradle.gradleVersion == libs.versions.gradle.get()) {
    "Use Gradle ${libs.versions.gradle.get()} from ./gradlew"
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.BIN
}

apply(from = "../verification/catalog-verification.gradle")

apply(from = "gradle/git-hooks.gradle.kts")
