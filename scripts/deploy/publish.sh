#!/usr/bin/env bash
# Portable framework-dependent artifacts. Deploy only after inspecting the server runtime.
set -euo pipefail
output=${1:?Usage: publish.sh ABSOLUTE_OUTPUT_DIRECTORY}
[[ "$output" = /* ]] || { echo 'Use an absolute output directory.' >&2; exit 2; }
[[ ! -e "$output" ]] || { echo 'Output already exists; refusing to overwrite.' >&2; exit 2; }
mkdir -m 700 -p "$output"
for service in Web Api Worker; do
    dotnet publish "src/SmartFarm.$service/SmartFarm.$service.csproj" -c Release --no-self-contained -o "$output/$service"
done
if find "$output" -type f \( -name 'appsettings.Production.json' -o -name 'appsettings.Development.json' -o -name 'appsettings.Local.json' \) | read -r _; then
    echo 'Environment configuration found in artifact. Refusing to package secrets.' >&2
    exit 1
fi
