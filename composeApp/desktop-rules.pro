# kcef
-keep class org.cef.** { *; }

# kotlinx serialization
-keepclasseswithmembers class ** { kotlinx.serialization.KSerializer serializer(...); }

# androidx sqlite
-keep class androidx.sqlite.SQLiteException

# dbus-java
-keep interface org.freedesktop.dbus.spi.transport.ITransportProvider
-keep interface org.freedesktop.dbus.interfaces.Properties { *; }
-keep class androidx.sqlite.driver.bundled.** { *; }

-ignorewarnings


# -printmapping mappings-desktop-currentOS.txt

-printconfiguration build/compose/binaries/main-release/proguard/configuration.txt
-printmapping build/compose/binaries/main-release/proguard/mapping.txt
-printseeds build/compose/binaries/main-release/proguard/seeds.txt
-printusage build/compose/binaries/main-release/proguard/usage.txt
