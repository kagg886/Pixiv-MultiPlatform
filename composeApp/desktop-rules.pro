# kcef
-keep class org.cef.** { *; }

# kotlinx serialization
-keepclasseswithmembers class ** { kotlinx.serialization.KSerializer serializer(...); }

# androidx sqlite
-keep class androidx.sqlite.SQLiteException

# dbus-java
-keep interface org.freedesktop.dbus.spi.transport.ITransportProvider
-keep interface org.freedesktop.dbus.interfaces.Properties { *; }

-ignorewarnings


-printmapping mappings-desktop-currentOS.txt

# 保护 Callback 接口不被混淆，因为它被 JNI 调用
-keep interface top.kagg886.filepicker.internal.Callback {
    public void onComplete(java.lang.String);
}

# 保护 NativeFilePicker 类的 JNI 方法
-keep class top.kagg886.filepicker.internal.NativeFilePicker {
    native <methods>;
}
