#!/usr/bin/env bash
# 把一次 CI 运行的输出（日志、截图）强制推送到单独的分支，方便不登录网页也能查看结果。
# 用法：bash ci/push-logs.sh <分支名> <目录>
set -euo pipefail
branch="$1"
dir="$2"
if [ ! -d "$dir" ]; then echo "没有 $dir，跳过"; exit 0; fi
tmp=$(mktemp -d)
cp -r "$dir"/. "$tmp"/
cd "$tmp"
git init -q -b "$branch"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git add -A
git commit -q -m "CI run ${GITHUB_RUN_NUMBER:-0}: ${GITHUB_JOB:-job}"
git push -q -f "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "$branch"
echo "已推送到 $branch 分支"
