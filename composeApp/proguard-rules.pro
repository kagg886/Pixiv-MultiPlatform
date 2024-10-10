-dontwarn org.slf4j.impl.StaticLoggerBinder
# 保留所有实现了 Screen 的子类及其成员和方法
-keep class * extends cafe.adriel.voyager.core.screen.Screen {
    <init>(...);
    <methods>;
}