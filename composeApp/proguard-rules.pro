-dontwarn org.slf4j.impl.StaticLoggerBinder

-keep class * implements org.slf4j.ILoggerFactory {
    <init>(...);
    <methods>;
}
-dontwarn org.slf4j.impl.StaticLoggerBinder


# 保留所有实现了 Screen 的子类及其成员和方法
-keep class * extends cafe.adriel.voyager.core.screen.Screen {
    <init>(...);
    <methods>;
}

-keep class org.xmlpull.v1.XmlPullParser { *; }
-keep class * extends org.xmlpull.v1.XmlPullParser {
    <init>(...);
    <methods>;
}

-dontwarn org.xmlpull.v1.**
-dontwarn org.xmlpull.mxp1.**