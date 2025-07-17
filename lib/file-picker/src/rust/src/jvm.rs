#![cfg(feature = "jvm")]

use jni::sys::jstring;
use jni::{
    JNIEnv,
    objects::{JClass, JObjectArray, JString},
};
use jni_fn::jni_fn;
use rfd::FileDialog;

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
pub fn openFileSaver(mut env: JNIEnv, _class: JClass, suggested_name: JString, extension: JString, directory: JString) -> jstring {
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
    let mut dialog = FileDialog::new();

    if let Some(dir) = &directory {
        dialog = dialog.set_directory(dir);
    }

    if let Some(ext) = &extension {
        dialog = dialog.add_filter("file", &[ext]);
    }

    dialog = dialog.set_file_name(&suggested_name);

    // 弹出保存对话框
    let result = dialog.save_file();

    // 返回给 Java（null 或路径字符串）
    match result {
        Some(path) => {
            let path_str = path.display().to_string();
            env.new_string(path_str).ok().map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
        }
        None => std::ptr::null_mut(),
    }
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
pub fn openFilePicker(mut env: JNIEnv, _class: JClass, ext: JObjectArray, title: JString, directory: JString) -> jstring {
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
    let mut dialog = FileDialog::new();

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

    let result = dialog.pick_file(); // Option<PathBuf>

    // === 转换为 jstring 返回 ===
    match result {
        Some(path) => {
            let path_str = path.display().to_string();
            env.new_string(path_str).ok().map(|s| s.into_raw()).unwrap_or(std::ptr::null_mut())
        }
        None => std::ptr::null_mut(),
    }
}
