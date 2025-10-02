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

-keep class top.kagg886.pmf.String0_commonMainKt {
    *;
}

-keep class androidx.activity.result.ActivityResultRegistry$$ExternalSyntheticLambda0 {
    *;
}
