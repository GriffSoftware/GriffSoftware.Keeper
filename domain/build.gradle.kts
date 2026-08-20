plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.lint)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
