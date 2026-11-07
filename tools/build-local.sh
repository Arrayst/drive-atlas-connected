#!/bin/zsh
set -eu
if [[ -n "${DRIVE_ATLAS_ENV:-}" ]]; then source "$DRIVE_ATLAS_ENV"; fi
cd "${0:A:h}/.."
exec /bin/sh ./gradlew --offline "$@"
