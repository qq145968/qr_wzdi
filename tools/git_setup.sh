#!/bin/bash
export PATH="/cmd:/bin:/mingw64/bin:/mingw64/libexec/git-core:$PATH"
export HOME="C:/Users/Administrator"
export GIT_CONFIG_NOSYSTEM=1

cd "e:/Git-Hub/扫码机器人"

echo "=== git init ==="
git init
echo "=== config ==="
git config user.name "qq145968"
git config user.email "qq145968@users.noreply.github.com"
echo "=== remote ==="
git remote remove origin 2>/dev/null
git remote add origin "https://qq145968:G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@github.com/qq145968/qr_wzdi.git"
echo "=== add ==="
git add -A
echo "=== commit ==="
git commit -m "Initial commit: ScanRobot Android app"
echo "=== push ==="
git push -u origin master --force
echo "=== DONE ==="
