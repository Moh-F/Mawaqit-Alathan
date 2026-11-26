#!/usr/bin/env bash
# Creates a signing keystore on YOUR machine + encrypted backup. Never run in the cloud / CI.
# Usage: ./create_keystore.sh <app-name> [output-dir]
set -euo pipefail
APP="${1:?usage: $0 <app-name> [output-dir]}"
OUT="${2:-$HOME/app-keystores/$APP}"
mkdir -p "$OUT"; chmod 700 "$OUT"
KS="$OUT/$APP.p12"
[ -e "$KS" ] && { echo "Keystore already exists: $KS (refusing to overwrite)"; exit 1; }
command -v keytool >/dev/null || { echo "Install a JDK (keytool) first"; exit 1; }
command -v openssl >/dev/null || { echo "Install openssl first"; exit 1; }
read -rsp "Choose keystore password (min 12 chars): " PASS; echo
[ ${#PASS} -ge 12 ] || { echo "Too short"; exit 1; }
ALIAS="${APP}-release"
keytool -genkeypair -v -storetype PKCS12 -keystore "$KS" -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 36500 -storepass "$PASS" -keypass "$PASS" \
  -dname "CN=$APP, O=Moh, C=DE"
cat > "$OUT/key.properties" <<P
storePassword=$PASS
keyPassword=$PASS
keyAlias=$ALIAS
storeFile=$KS
P
chmod 600 "$OUT/key.properties" "$KS"
# Encrypted backup (AES-256). You are asked for a backup passphrase.
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -salt -in "$KS" -out "$OUT/$APP.p12.enc"
base64 -w0 "$KS" > "$OUT/$APP.p12.b64" 2>/dev/null || base64 "$KS" > "$OUT/$APP.p12.b64"
keytool -list -v -keystore "$KS" -storepass "$PASS" | grep -E "SHA256|Alias|Valid" || true
cat <<M

DONE. Files in $OUT
 - $APP.p12          the keystore (NEVER commit, NEVER share)
 - $APP.p12.enc      encrypted backup -> copy to 2 separate places (password manager + offline USB)
 - $APP.p12.b64      for GitHub secret KEYSTORE_B64 (delete after uploading)
 - key.properties    copy to <flutter-project>/android/key.properties (git-ignored)
GitHub secrets: KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS=$ALIAS, KEY_PASSWORD
Restore backup: openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -in $APP.p12.enc -out $APP.p12
If you lose this key you can NEVER update installed copies of the app. Test the restore once.
M
