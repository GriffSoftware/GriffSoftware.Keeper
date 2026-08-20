package com.griff.subscriptions.presentation.drawer

import androidx.lifecycle.ViewModel
import com.griff.subscriptions.application.appinfo.AppVersion
import com.griff.subscriptions.application.appinfo.GetAppVersionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Supplies the drawer with the real version of the running build. */
@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    getAppVersion: GetAppVersionUseCase,
) : ViewModel() {

    val appVersion: AppVersion = getAppVersion()
}
