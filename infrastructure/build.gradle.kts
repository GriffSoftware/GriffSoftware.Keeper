plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.griff.keeper.infrastructure"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Room's MigrationTestHelper reads the exported schemas from the test APK's assets.
    sourceSets.getByName("androidTest") {
        assets.srcDir("$projectDir/schemas")
    }


    lint {
        // Builds must not regress: any lint error fails the build.
        abortOnError = true
        checkDependencies = true
        // Depends on network access and on when the build runs, so it is not a useful gate.
        disable += "NewerVersionAvailable"
        warningsAsErrors = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":application"))

    implementation(libs.kotlinx.coroutines.core)
    // The backup payload is serialized as JSON before it is compressed and encrypted.
    implementation(libs.kotlinx.serialization.json)
    // Reminder notifications are built by a background worker, with no activity to inherit the
    // per-app locale from; AppCompatDelegate is what tells it which language the user picked.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(testFixtures(project(":domain")))
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(testFixtures(project(":domain")))
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
