void encode_animated_image_unsafe(void*, int);

#include <jni.h>
JNIEXPORT void JNICALL Java_moe_tarsin_gif_NativeBridgeKt_encode(JNIEnv *env, jclass clazz, jobject buffer, jint limit) {
    void* ptr = (*env)->GetDirectBufferAddress(env, buffer);
    encode_animated_image_unsafe(ptr, limit);
}
