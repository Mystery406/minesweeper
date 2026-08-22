# Prevent ProGuard from specializing Okio facade return types into invalid bytecode.
-keep,allowshrinking,allowobfuscation class okio.Okio__OkioKt { *; }
