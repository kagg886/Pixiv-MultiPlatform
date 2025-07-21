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
