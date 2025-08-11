#![cfg(feature = "jvm")]

use jni::objects::JObject;
use jni::sys::jstring;
use jni::{
    JNIEnv,
    objects::{JClass, JObjectArray, JString},
};
use jni_fn::jni_fn;
use rfd::{AsyncFileDialog, FileHandle};

pub struct FFIClosure {
    f: Box<dyn FnOnce() -> Option<FileHandle>>,
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn openFileSaver(mut env: JNIEnv, _class: JClass, suggested_name: JString, extension: JString, directory: JString) -> *mut FFIClosure {
    let suggested_name: String = env.get_string(&suggested_name).unwrap().into();
    let dir = if directory.is_null() { String::from("~") } else { env.get_string(&directory).unwrap().into() };
    let mut dialog = AsyncFileDialog::new().set_directory(dir).set_file_name(suggested_name);
    if !extension.is_null() {
        let ext: String = env.get_string(&extension).unwrap().into();
        dialog = dialog.add_filter("file", &[ext])
    }
    let fut = dialog.save_file();
    let f = Box::new(|| futures::executor::block_on(fut));
    let bo = Box::new(FFIClosure { f });
    Box::into_raw(bo)
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn awaitFileSaver(env: JNIEnv, _class: JClass, ptr: *mut FFIClosure) -> jstring {
    let f = unsafe { Box::from_raw(ptr) };
    let hnd = (f.f)().map(|x| x.path().display().to_string());
    hnd.map_or(*JObject::null(), |s| *JObject::from(env.new_string(s).unwrap()))
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn openFilePicker(mut env: JNIEnv, _class: JClass, ext: JObjectArray, title: JString, directory: JString) -> *mut FFIClosure {
    let dir = if directory.is_null() { String::from("~") } else { env.get_string(&directory).unwrap().into() };
    let mut dialog = AsyncFileDialog::new().set_directory(dir);
    if !ext.is_null() {
        let mut vec = Vec::new();
        let len = env.get_array_length(&ext).unwrap_or(0);
        for i in 0..len {
            let jstr = env.get_object_array_element(&ext, i).unwrap();
            let rust_str: String = env.get_string(&JString::from(jstr)).unwrap().into();
            vec.push(rust_str);
        }
        if !vec.is_empty() {
            dialog = dialog.add_filter("filter", &vec);
        }
    }
    if !title.is_null() {
        let title: String = env.get_string(&title).unwrap().into();
        dialog = dialog.set_title(title)
    }
    let fut = dialog.pick_file();
    let f = Box::new(|| futures::executor::block_on(fut));
    let bo = Box::new(FFIClosure { f });
    Box::into_raw(bo)
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn awaitFilePicker(env: JNIEnv, _class: JClass, ptr: *mut FFIClosure) -> jstring {
    let f = unsafe { Box::from_raw(ptr) };
    let hnd = (f.f)().map(|x| x.path().display().to_string());
    hnd.map_or(*JObject::null(), |s| *JObject::from(env.new_string(s).unwrap()))
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn openDictionaryPicker(mut env: JNIEnv, _class: JClass, title: JString, directory: JString) -> *mut FFIClosure {
    let dir = if directory.is_null() { String::from("~") } else { env.get_string(&directory).unwrap().into() };
    let mut dialog = AsyncFileDialog::new().set_directory(dir);
    if !title.is_null() {
        let title: String = env.get_string(&title).unwrap().into();
        dialog = dialog.set_title(title)
    }
    let fut = dialog.pick_folder();
    let f = Box::new(|| futures::executor::block_on(fut));
    let bo = Box::new(FFIClosure { f });
    Box::into_raw(bo)
}

#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn awaitDictionaryPicker(env: JNIEnv, _class: JClass, ptr: *mut FFIClosure) -> jstring {
    let f = unsafe { Box::from_raw(ptr) };
    let hnd = (f.f)().map(|x| x.path().display().to_string());
    hnd.map_or(*JObject::null(), |s| *JObject::from(env.new_string(s).unwrap()))
}
