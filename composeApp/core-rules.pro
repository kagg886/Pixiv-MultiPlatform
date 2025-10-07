-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn androidx.test.platform.app.InstrumentationRegistry

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

-keep class top.kagg886.pmf.res.** { *; }

-assumenosideeffects class top.kagg886.pmf.res.ActualResourceCollectorsKt {
    public static ** allStringResources_delegate$lambda$0(...);
}
