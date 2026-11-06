# Add project specific ProGuard rules here.
-keep class io.grpc.** { *; }
-keep class dev.mobile.dadb.** { *; }
-dontwarn io.grpc.**
-dontwarn javax.annotation.**

