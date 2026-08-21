package com.griff.keeper.application.appinfo

/** Version of the running build, supplied by the app module. */
data class AppVersion(
    val name: String,
    val code: Long,
)

/** Port implemented in the composition root, which is the only place that knows `BuildConfig`. */
interface AppVersionProvider {
    fun version(): AppVersion
}
