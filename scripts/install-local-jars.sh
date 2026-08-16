#!/usr/bin/env bash
# Repopulate the project's local-repo/ from libroguelike/lib/.
# slick-util and rlforj are not on Maven Central, so they live in the
# committed lib/ directory and are installed into a project-local Maven
# repository before the first build. Run once after cloning:
#
#   ./scripts/install-local-jars.sh
#   mvn package
#
# LWJGL 2 itself is no longer needed (M2 switched to LWJGL 3 from Central).
set -euo pipefail
cd "$(dirname "$0")/.."

install_local() {
  mvn -q install:install-file \
    -Dfile="$1" -DgroupId="$2" -DartifactId="$3" -Dversion="$4" \
    -Dpackaging=jar -DlocalRepositoryPath=local-repo -DcreateChecksum=true
}

install_local libroguelike/lib/slick-util/slick-util.jar       org.newdawn.slick slick-util 1.0.0
install_local libroguelike/lib/rlforj.0.2.jar                  net.sourceforge.rlforj rlforj 0.2

echo "Installed legacy jars into local-repo/"
