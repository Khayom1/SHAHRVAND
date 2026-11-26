# Preserve metadata used by Kotlin, Compose and Android annotations.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep source/line information so optimized-build crash reports remain useful.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Do not add broad package keep-rules: R8 must be able to remove only code
# proven unreachable. ML Kit / CameraX consumer rules are supplied by their AARs.
