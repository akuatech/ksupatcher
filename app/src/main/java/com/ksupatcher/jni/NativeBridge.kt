package com.ksupatcher.jni

object NativeBridge {
    private var loaded = false

    fun tryLoad(): Result<Boolean> = runCatching {
        if (loaded) return@runCatching true
        System.loadLibrary("magiskboot")
        System.loadLibrary("ksud")
        loaded = true
        true
    }

    fun isLoaded(): Boolean = loaded
}
