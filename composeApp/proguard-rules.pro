-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn androidx.test.platform.app.InstrumentationRegistry

# 保留所有实现了 Screen 的子类及其成员和方法
-keep class * extends cafe.adriel.voyager.core.screen.Screen {
    <init>(...);
    <methods>;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepattributes LineNumberTable
-allowaccessmodification
-repackageclasses
