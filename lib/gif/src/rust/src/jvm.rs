#![cfg(feature = "jvm")]
use crate::encode_animated_image_unsafe;
use jni::{
    JNIEnv,
    objects::{JByteBuffer, JClass},
    sys::jint,
};
use jni_fn::jni_fn;

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("moe.tarsin.gif.NativeBridgeKt")]
pub fn encode(env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) {
    let ptr = env.get_direct_buffer_address(&buffer).unwrap();
    encode_animated_image_unsafe(ptr, limit);
}
