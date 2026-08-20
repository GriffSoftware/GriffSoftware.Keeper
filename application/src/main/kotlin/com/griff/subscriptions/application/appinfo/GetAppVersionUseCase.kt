package com.griff.subscriptions.application.appinfo

import javax.inject.Inject

/** Returns the version shown in the navigation drawer. */
class GetAppVersionUseCase @Inject constructor(
    private val provider: AppVersionProvider,
) {
    operator fun invoke(): AppVersion = provider.version()
}
