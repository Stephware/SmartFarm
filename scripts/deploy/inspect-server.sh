#!/usr/bin/env bash
# Read-only shared-host inspection. Never prints environment variables or config contents.
set -u
id
uname -m
df -h / /var 2>/dev/null
free -m
printf '\nAvailable executables\n'
for app in dotnet docker caddy cloudflared sqlcmd; do command -v "$app" || true; done
if command -v dotnet >/dev/null; then dotnet --list-runtimes; fi
printf '\nApplication listeners\n'
ss -lnt 2>/dev/null || true
printf '\nExisting containers (no environment/secrets)\n'
if command -v docker >/dev/null; then docker ps --format '{{.Names}}\t{{.Image}}\t{{.Ports}}'; fi
printf '\nService state\n'
for service in caddy cloudflared docker; do systemctl is-active "$service" 2>/dev/null || true; done
printf '\nExisting repository locations\n'
find "$HOME" -maxdepth 4 -type d -name .git -print 2>/dev/null
printf '\nNoninteractive administrator authorization\n'
sudo -n -l 2>/dev/null || printf 'Administrator rights not available noninteractively. No escalation attempted.\n'
