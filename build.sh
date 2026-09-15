#!/data/data/com.termux/files/usr/bin/sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SDK_DIR=/data/data/com.termux/files/home/.cache/android-api/android-35
RESOURCE_SDK_DIR=/data/data/com.termux/files/home/.cache/android-api/android-9
TOOLS_DIR=/data/data/com.termux/files/usr/bin
OUT="$PROJECT_DIR/build"
RES_COMPILED="$OUT/res-compiled.zip"
RES_APK="$OUT/resources.apk"
GEN="$OUT/gen"
CLASSES="$OUT/classes"
DEX="$OUT/dex"

rm -rf "$OUT"
mkdir -p "$GEN" "$CLASSES" "$DEX" "$OUT/tool-classes"

"$TOOLS_DIR/aapt2" compile --dir "$PROJECT_DIR/res" -o "$RES_COMPILED"
"$TOOLS_DIR/aapt2" link \
    -I "$RESOURCE_SDK_DIR/android.jar" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    --java "$GEN" \
    --min-sdk-version 26 \
    --target-sdk-version 35 \
    --version-code 1 \
    --version-name 1.0 \
    --auto-add-overlay \
    -o "$RES_APK" -R "$RES_COMPILED"

find "$PROJECT_DIR/src" "$GEN" -type f -name '*.java' -print > "$OUT/sources.list"
javac --release 8 -parameters -encoding UTF-8 \
    -classpath "$SDK_DIR/android.jar" \
    -d "$CLASSES" \
    @"$OUT/sources.list"

jar cf "$OUT/classes.jar" -C "$CLASSES" .

"$TOOLS_DIR/d8" --release --min-api 26 --lib "$SDK_DIR/android.jar" \
    --output "$DEX" "$OUT/classes.jar"

cp "$RES_APK" "$OUT/unsigned.apk"
jar uf "$OUT/unsigned.apk" -C "$DEX" classes.dex

"$TOOLS_DIR/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/WiFi-Brute-Lab-aligned.apk"

if [ ! -f "$OUT/wifi-brute-lab-release.keystore" ]; then
    keytool -genkeypair -noprompt \
        -keystore "$OUT/wifi-brute-lab-release.keystore" \
        -storepass wifibrutelab \
        -keypass wifibrutelab \
        -alias wifibrutelab \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Wi-Fi Brute Lab, OU=Faculdade, O=Local, L=Local, ST=Local, C=BR"
fi

"$TOOLS_DIR/apksigner" sign \
    --ks "$OUT/wifi-brute-lab-release.keystore" \
    --ks-pass pass:wifibrutelab \
    --key-pass pass:wifibrutelab \
    --out "$OUT/WiFi-Brute-Lab.apk" \
    "$OUT/WiFi-Brute-Lab-aligned.apk"

"$TOOLS_DIR/apksigner" verify --verbose "$OUT/WiFi-Brute-Lab.apk"
printf 'APK criado: %s\n' "$OUT/WiFi-Brute-Lab.apk"
