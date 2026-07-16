#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
safe_root="$(mktemp -d /tmp/abstractcache-test-XXXXXX)"
cleanup() {
    rm -rf "$safe_root"
}
trap cleanup EXIT

mirror_dir="$safe_root/repo"
mkdir -p "$mirror_dir"

# Copy the tracked project tree into a colon-free path so javac classpath parsing works on Linux.
tar \
    --exclude='./.git' \
    --exclude='./target' \
    --exclude='./.idea' \
    -C "$repo_root" \
    -cf - . | tar -C "$mirror_dir" -xf -

cd "$mirror_dir"
exec mvn test "$@"
