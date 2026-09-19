#!/usr/bin/env bash
# CallBlocker — build manual sem Gradle
# Requisitos: bash, curl, unzip, zip, JDK (jdk17-openjdk no Arch)
#   sudo pacman -S jdk17-openjdk unzip zip
set -euo pipefail

SDK_VER=30
BUILD_TOOLS=30.0.3
ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK_DIR="$ROOT/sdk"
BUILD_DIR="$ROOT/build"
BT_DIR="$(echo "$SDK_DIR"/build-tools/*/ 2>/dev/null | grep -o '[^ ]*' | head -1)"
ANDROID_JAR="$(echo "$SDK_DIR"/platforms/*/android.jar 2>/dev/null | grep -o '[^ ]*' | head -1)"
KEYSTORE="$BUILD_DIR/keystore.jks"
KEYSTORE_PASS=callblocker
KEYSTORE_ALIAS=callblocker
OUT_APK="$BUILD_DIR/CallBlocker.apk"

mkdir -p "$SDK_DIR" "$BUILD_DIR"

log() { echo "==> $*"; }

# ---------- SDK ----------
if [ ! -x "$BT_DIR/aapt2" ]; then
    log "Baixando build-tools $BUILD_TOOLS..."
    curl -fsSL -o "$SDK_DIR/bt.zip" \
        "https://dl.google.com/android/repository/build-tools_r${BUILD_TOOLS}-linux.zip"
    unzip -q -o "$SDK_DIR/bt.zip" -d "$SDK_DIR/build-tools"
    rm -f "$SDK_DIR/bt.zip"
fi

if [ ! -f "$ANDROID_JAR" ]; then
    log "Baixando platform android-$SDK_VER..."
    curl -fsSL -o "$SDK_DIR/pf.zip" \
        "https://dl.google.com/android/repository/platform-${SDK_VER}_r03.zip"
    unzip -q -o "$SDK_DIR/pf.zip" -d "$SDK_DIR/platforms"
    rm -f "$SDK_DIR/pf.zip"
fi

# ---------- 1. aapt2 link (sem recursos proprios; usa so o framework) ----------
log "aapt2 link..."
"$BT_DIR/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$ROOT/AndroidManifest.xml" \
    --min-sdk-version "$SDK_VER" \
    --target-sdk-version "$SDK_VER" \
    -o "$BUILD_DIR/base.apk"

# ---------- 2. javac ----------
log "javac..."
rm -rf "$BUILD_DIR/classes"
mkdir -p "$BUILD_DIR/classes"
find "$ROOT/src" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -source 8 -target 8 \
    -bootclasspath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.txt"

# ---------- 3. d8 (dex) ----------
log "d8..."
"$BT_DIR/d8" --release \
    --lib "$ANDROID_JAR" \
    --min-api "$SDK_VER" \
    --output "$BUILD_DIR" \
    "$BUILD_DIR/classes"/com/nospam/blocker/*.class

# ---------- 4. empacotar classes.dex no apk + zipalign ----------
log "empacotando..."
(cd "$BUILD_DIR" && zip -q base.apk classes.dex)
"$BT_DIR/zipalign" -f 4 "$BUILD_DIR/base.apk" "$BUILD_DIR/aligned.apk"

# ---------- 5. assinar ----------
if [ ! -f "$KEYSTORE" ]; then
    log "Gerando keystore..."
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -alias "$KEYSTORE_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass "$KEYSTORE_PASS" \
        -keypass "$KEYSTORE_PASS" \
        -dname "CN=CallBlocker, OU=Personal, O=Personal, C=BR"
fi
log "Assinando..."
"$BT_DIR/apksigner" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias "$KEYSTORE_ALIAS" \
    --ks-pass "pass:$KEYSTORE_PASS" \
    --key-pass "pass:$KEYSTORE_PASS" \
    --out "$OUT_APK" \
    "$BUILD_DIR/aligned.apk"

log "OK: $OUT_APK ($(du -h "$OUT_APK" | cut -f1))"