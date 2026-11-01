#!/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WORKSPACE_DIR=$(CDPATH= cd -- "$PROJECT_DIR/.." && pwd)
SDK_ROOT=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-"$HOME/Library/Android/sdk"}}
BUILD_TOOLS="$SDK_ROOT/build-tools/36.0.0"
ANDROID_JAR="$SDK_ROOT/platforms/android-36/android.jar"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_APK="$BUILD_DIR/Call-Text-Cleaner-debug.apk"

for tool in javac zip keytool adb; do
    command -v "$tool" >/dev/null 2>&1 || {
        printf 'Required tool is missing: %s\n' "$tool" >&2
        exit 1
    }
done
for tool in aapt d8 zipalign apksigner; do
    test -x "$BUILD_TOOLS/$tool" || {
        printf 'Android SDK build tool is missing: %s\n' "$BUILD_TOOLS/$tool" >&2
        exit 1
    }
done
test -f "$ANDROID_JAR" || {
    printf 'Android platform jar is missing: %s\n' "$ANDROID_JAR" >&2
    exit 1
}

export JAVA_TOOL_OPTIONS=-Xmx4g
mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/dex"

javac --release 11 -J-Xmx4g -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" "$PROJECT_DIR/src/com/calltextcleaner/MainActivity.java"

"$BUILD_TOOLS/aapt" package -f \
    -M "$PROJECT_DIR/AndroidManifest.xml" \
    -S "$PROJECT_DIR/res" \
    -A "$WORKSPACE_DIR/website" \
    -I "$ANDROID_JAR" \
    --min-sdk-version 23 \
    --target-sdk-version 28 \
    --version-code 1 \
    --version-name 1.0 \
    -F "$BUILD_DIR/resources.ap_"

"$BUILD_TOOLS/d8" --min-api 23 --lib "$ANDROID_JAR" \
    --output "$BUILD_DIR/dex" \
    "$BUILD_DIR/classes/com/calltextcleaner/MainActivity.class"

cp "$BUILD_DIR/resources.ap_" "$BUILD_DIR/unsigned.apk"
(
    cd "$BUILD_DIR/dex"
    zip -q -j "$BUILD_DIR/unsigned.apk" classes.dex
)
"$BUILD_TOOLS/zipalign" -f 4 "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/aligned.apk"

KEYSTORE="$HOME/.android/debug.keystore"
if [ ! -f "$KEYSTORE" ]; then
    mkdir -p "$HOME/.android"
    keytool -genkeypair -v \
        -keystore "$KEYSTORE" \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

"$BUILD_TOOLS/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$OUTPUT_APK" \
    "$BUILD_DIR/aligned.apk"
"$BUILD_TOOLS/apksigner" verify --verbose "$OUTPUT_APK"
printf 'Built APK: %s\n' "$OUTPUT_APK"

if [ "${1:-}" = "--install" ]; then
    adb install -r "$OUTPUT_APK"
    adb shell am start -n com.calltextcleaner/.MainActivity
fi
