package org.akuatech.ksupatcher.data

object UpdateConfig {
    const val appOwner = "akuatech"
    const val appRepo = "ksupatcher"

    const val ksuLkmOwner = "cyberknight777"
    const val ksuLkmRepo = "ksu-lkm"
    const val ksuModuleAsset = "kernelsu.ko"
    const val ksunModuleAsset = "kernelsu_next.ko"

    val supportedKmis = listOf(
        "android12-5.10",
        "android13-5.10",
        "android13-5.15",
        "android14-5.15",
        "android14-6.1",
        "android15-6.6",
        "android16-6.12"
    )
}
