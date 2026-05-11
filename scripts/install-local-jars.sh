#!/usr/bin/env bash
# Repopulate the project's local-repo/ from libroguelike/lib/.
# Only needed if local-repo/ is removed. The committed local-repo means the
# Maven build is hermetic — no remote fetch required for these legacy jars.
set -euo pipefail
cd "$(dirname "$0")/.."

install_local() {
  mvn -q install:install-file \
    -Dfile="$1" -DgroupId="$2" -DartifactId="$3" -Dversion="$4" \
    -Dpackaging=jar -DlocalRepositoryPath=local-repo -DcreateChecksum=true
}

install_local libroguelike/lib/lwjgl-2.7.1/jar/lwjgl.jar       org.lwjgl.legacy lwjgl       2.7.1
install_local libroguelike/lib/lwjgl-2.7.1/jar/lwjgl_util.jar  org.lwjgl.legacy lwjgl_util  2.7.1
install_local libroguelike/lib/lwjgl-2.7.1/jar/jinput.jar      org.lwjgl.legacy jinput      2.7.1
install_local libroguelike/lib/slick-util/slick-util.jar       org.newdawn.slick slick-util 1.0.0
install_local libroguelike/lib/rlforj.0.2.jar                  net.sourceforge.rlforj rlforj 0.2

echo "Installed legacy jars into local-repo/"
