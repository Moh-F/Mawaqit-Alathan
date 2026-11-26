# Windows PowerShell version. Run on YOUR machine. Requires JDK (keytool) and openssl (Git for Windows includes it).
param([Parameter(Mandatory=$true)][string]$App, [string]$Out = "$HOME\app-keystores\$App")
New-Item -ItemType Directory -Force -Path $Out | Out-Null
$ks = Join-Path $Out "$App.p12"
if (Test-Path $ks) { throw "Keystore already exists: $ks" }
$sec = Read-Host "Keystore password (min 12 chars)" -AsSecureString
$pass = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
if ($pass.Length -lt 12) { throw "Too short" }
$alias = "$App-release"
keytool -genkeypair -v -storetype PKCS12 -keystore $ks -alias $alias -keyalg RSA -keysize 4096 -validity 36500 -storepass $pass -keypass $pass -dname "CN=$App, O=Moh, C=DE"
@"
storePassword=$pass
keyPassword=$pass
keyAlias=$alias
storeFile=$($ks -replace '\\','/')
"@ | Set-Content (Join-Path $Out "key.properties")
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -salt -in $ks -out "$ks.enc"
[Convert]::ToBase64String([IO.File]::ReadAllBytes($ks)) | Set-Content "$ks.b64"
Write-Host "Done. Back up $ks.enc in 2 places. Never commit the .p12. Secrets: KEYSTORE_B64, KEYSTORE_PASSWORD, KEY_ALIAS=$alias, KEY_PASSWORD"
