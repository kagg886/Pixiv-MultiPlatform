package com.dokar.chiptextfield

actual suspend fun awaitFrame() {
    kotlinx.coroutines.android.awaitFrame()
}