package com.learntogether

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * LearnTogether Application class.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 * across the entire application.
 */
@HiltAndroidApp
class LearnTogetherApp : Application()
