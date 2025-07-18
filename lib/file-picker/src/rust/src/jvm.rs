#![cfg(feature = "jvm")]

use jni::objects::{JObject, JValue};
use jni::{
    JNIEnv,
    objects::{JClass, JObjectArray, JString},
};
use jni_fn::jni_fn;
use rfd::{AsyncFileDialog};
use std::thread;

/// JNI interface for NativeFilePicker.openFileSaver
///
/// Parameters:
/// - suggestedName: String - 建议的文件名
/// - extension: String? - 文件扩展名（可为null）
/// - directory: String? - 目录路径（可为null）
/// - callback: Callback - 回调接口
#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn openFileSaver(mut env: JNIEnv, _class: JClass, suggested_name: JString, extension: JString, directory: JString,callback: JObject) {
    // suggestedName: String => String
    let suggested_name: String = env.get_string(&suggested_name).unwrap().into();

    // extension: String? => Option<String>
    let extension: Option<String> = {
        let raw = extension.as_raw();
        if raw.is_null() { None } else { Some(env.get_string(&extension).unwrap().into()) }
    };

    // directory: String? => Option<String>
    let directory: Option<String> = {
        let raw = directory.as_raw();
        if raw.is_null() { Some(String::from("~")) } else { Some(env.get_string(&directory).unwrap().into()) }
    };

    // 构建对话框
    let mut dialog = AsyncFileDialog::new();

    if let Some(dir) = &directory {
        dialog = dialog.set_directory(dir);
    }

    if let Some(ext) = &extension {
        dialog = dialog.add_filter("file", &[ext]);
    }

    dialog = dialog.set_file_name(&suggested_name);

    // 弹出保存对话框
    let resultFuture = dialog.save_file();

    let jvm = env.get_java_vm().unwrap();
    let callbackGlobalRef = env.new_global_ref(callback).unwrap();
    execute(async move {
        let resultOptions = resultFuture.await;
        let mut env = jvm.attach_current_thread().unwrap(); // 新线程/任务内 attach

        let arg = match resultOptions {
            Some(p) => {
                let s = env.new_string(p.path().display().to_string()).unwrap();
                JObject::from(s)
            }
            None => JObject::null(),
        };


        // 2. 调用 Callback.onComplete(String)
        env.call_method(callbackGlobalRef.as_obj(), "onComplete", "(Ljava/lang/String;)V", &[JValue::from(&arg)]).unwrap().v().unwrap();
    });
}

/// JNI interface for NativeFilePicker.openFilePicker
///
/// Parameters:
/// - ext: Array<String>? - 文件扩展名数组（可为null）
/// - title: String? - 对话框标题（可为null）
/// - directory: String? - 目录路径（可为null）
/// - callback: Callback - 回调接口
#[unsafe(no_mangle)]
#[allow(non_snake_case)]
#[jni_fn("top.kagg886.filepicker.internal.NativeFilePicker")]
pub fn openFilePicker(mut env: JNIEnv, _class: JClass, ext: JObjectArray, title: JString, directory: JString,callback:JObject) {
    // ext: Array<String>? => Option<Vec<String>>
    let extensions = {
        let raw = ext.as_raw(); // jobjectArray
        if raw.is_null() {
            None
        } else {
            let mut vec = Vec::new();
            let len = env.get_array_length(&ext).unwrap_or(0);
            for i in 0..len {
                let jstr = env.get_object_array_element(&ext, i).unwrap();
                let rust_str: String = env.get_string(&JString::from(jstr)).unwrap().into();
                vec.push(rust_str);
            }
            Some(vec)
        }
    };

    // title: String? => Option<String>
    let title: Option<String> = {
        let raw = title.as_raw();
        if raw.is_null() { None } else { Some(env.get_string(&title).unwrap().into()) }
    };

    // directory: String? => Option<String>
    let directory: Option<String> = {
        let raw = directory.as_raw();
        if raw.is_null() { Some(String::from("~")) } else { Some(env.get_string(&directory).unwrap().into()) }
    };

    // === 弹出对话框 ===
    let mut dialog = AsyncFileDialog::new();

    if let Some(exts) = &extensions {
        if !exts.is_empty() {
            dialog = dialog.add_filter("filter", exts);
        }
    }

    if let Some(t) = &title {
        dialog = dialog.set_title(t);
    }

    if let Some(dir) = &directory {
        dialog = dialog.set_directory(dir);
    }

    let resultFuture = dialog.pick_file(); // Option<PathBuf>

    let jvm = env.get_java_vm().unwrap();
    let callbackGlobalRef = env.new_global_ref(callback).unwrap();
    execute(async move {
        let resultOptions = resultFuture.await;
        let mut env = jvm.attach_current_thread().unwrap(); // 新线程/任务内 attach

        let arg = match resultOptions {
            Some(p) => {
                let s = env.new_string(p.path().display().to_string()).unwrap();
                JObject::from(s)
            }
            None => JObject::null(),
        };


        // 2. 调用 Callback.onComplete(String)
        env.call_method(callbackGlobalRef.as_obj(), "onComplete", "(Ljava/lang/String;)V", &[JValue::from(&arg)]).unwrap().v().unwrap();
    });
}

fn execute<F: Future<Output = ()> + Send + 'static>(f: F) {
    // this is stupid... use any executor of your choice instead
    thread::spawn(move || futures::executor::block_on(f));
}
