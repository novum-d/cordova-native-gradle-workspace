package dev.example.nativehost

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** 両APKのソースとしてコンパイルし、ホストごとのHiltコンポーネントを生成する共通Application。 */
@HiltAndroidApp
class NativeApplication : Application()
