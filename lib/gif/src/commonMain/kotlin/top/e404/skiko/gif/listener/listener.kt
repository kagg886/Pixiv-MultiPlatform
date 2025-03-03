package top.e404.skiko.gif.listener

fun interface GIFMakingListener {
    fun onProgress(data: GIFMakingStep)
}


sealed interface GIFMakingStep {
    data class CompressImage(val done: Int, val total: Int) : GIFMakingStep
    data class WritingData(val done: Int, val total: Int) : GIFMakingStep
}
