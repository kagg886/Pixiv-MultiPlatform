#![cfg(feature = "jvm")]
use crate::encode_request_from_buffer;
use jni::{
    objects::{JByteBuffer, JClass},
    sys::jint,
    JNIEnv,
};
use jni_fn::jni_fn;

#[no_mangle]
#[allow(non_snake_case)]
#[jni_fn("moe.tarsin.gif.NativeBridgeKt")]
pub fn encode(env: JNIEnv, _class: JClass, buffer: JByteBuffer, limit: jint) {
    let ptr = env.get_direct_buffer_address(&buffer).unwrap();
    encode_request_from_buffer(ptr, limit);
}
