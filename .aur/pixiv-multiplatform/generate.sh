#!/bin/bash

_pkgname=pixiv-multiplatform
pkgname=pixiv-multiplatform

if [[ -z "${APP_VERSION_NAME}" || "${APP_VERSION_NAME}" != v* ]]; then
  echo "错误：APP_VERSION_NAME 未设置或不符合首字母为 v 的格式。"
  exit 1
fi

pkgver=$(echo $APP_VERSION_NAME | sed 's/^v//')
pkgrel=1
pkgdesc="Pixiv Client"
url="https://pmf.kagg886.top"
license=(GPL3)
arch=(x86_64)
source=(
    "https://github.com/kagg886/Pixiv-MultiPlatform/releases/download/$APP_VERSION_NAME/linux.tar.gz"
    "https://raw.githubusercontent.com/kagg886/Pixiv-MultiPlatform/refs/tags/$APP_VERSION_NAME/.idea/icon.png"
)
depends=(mesa fontconfig)

# Download the linux.tar.gz file and calculate SHA512
if [ -f "linux.tar.gz" ]; then
  echo "File linux.tar.gz already exists, skipping download..."
else
  echo "Downloading linux.tar.gz to calculate SHA512..."
  curl -L -o linux.tar.gz "https://github.com/kagg886/Pixiv-MultiPlatform/releases/download/$APP_VERSION_NAME/linux.tar.gz"
fi

# Calculate SHA512 sum
echo "Calculating SHA512 sum..."
linux_sha512=$(sha512sum linux.tar.gz | cut -d' ' -f1)

if [ -z "$linux_sha512" ]; then
  echo "Error: Could not calculate SHA512 for linux.tar.gz"
  exit 1
fi
echo "Linux SHA512: $linux_sha512"


sha512sums=(
    "$linux_sha512"
    "0fc7f0d03cfb8bf97059c490784f4ad71da3e079936fc4c6a3d9ce41f3d3f411e72624590047c76dd136beed468bf1f56ee41e284b877d0c4a114ba6d1178510"
)

cat <<EOF > PKGBUILD
_pkgname=$_pkgname
pkgname=$pkgname
pkgver=$pkgver
pkgrel=$pkgrel
pkgdesc="$pkgdesc"
url="$url"
license=(${license[@]})
arch=(${arch[@]})
makedepends=(${makedepends[@]})
source=(${source[@]})
sha512sums=(${sha512sums[@]})
depends=(${depends[@]})

package() {
    # Create desktop file
    cat > Pixiv-MultiPlatform.desktop << DESKTOPEOF
[Desktop Entry]
Comment=an cross-multiplatform pixiv client
Exec=/opt/Pixiv-MultiPlatform/bin/Pixiv-MultiPlatform
Path=/opt/Pixiv-MultiPlatform/
Name=Pixiv MultiPlatform
Icon=Pixiv-MultiPlatform
Categories=Network;
NoDisplay=false
StartupNotify=true
Terminal=false
Type=Application
DESKTOPEOF

    install -d \$pkgdir/opt/Pixiv-MultiPlatform/
    cp -ar Pixiv-MultiPlatform \$pkgdir/opt/
    install -Dm755 Pixiv-MultiPlatform.desktop \$pkgdir/usr/share/applications/Pixiv-MultiPlatform.desktop
    install -Dm644 icon.png \$pkgdir/usr/share/icons/hicolor/256x256/apps/Pixiv-MultiPlatform.png
}
EOF

echo "PKGBUILD 文件已生成。"
