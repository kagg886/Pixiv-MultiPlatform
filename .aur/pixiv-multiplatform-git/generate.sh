#!/bin/bash

_pkgname=pixiv-multiplatform
pkgname=pixiv-multiplatform-git

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
makedepends=(jdk21-openjdk rust-aarch64-gnu)
source=("git+https://github.com/kagg886/Pixiv-MultiPlatform.git")
sha512sums=(SKIP)

cat <<EOF > PKGBUILD
# Maintainer: Your Name <your.email@example.com>
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

build() {
  rustup toolchain install x86_64-unknown-linux-gnu
  cd Pixiv-MultiPlatform
  ./gradlew packageReleaseDistributionForCurrentOs
}

package() {
  cd Pixiv-MultiPlatform
  install -d \$pkgdir/opt/Pixiv-MultiPlatform/
  cp -ar composeApp/build/compose/binaries/main-release/app/Pixiv-MultiPlatform \$pkgdir/opt
  install -Dm755 Pixiv-MultiPlatform.desktop \$pkgdir/usr/share/applications/Pixiv-MultiPlatform.desktop
  install -Dm644 composeApp/icons/pixiv.png \$pkgdir/usr/share/icons/hicolor/256x256/apps/Pixiv-MultiPlatform.png
}
EOF

echo "PKGBUILD 文件已生成。"
