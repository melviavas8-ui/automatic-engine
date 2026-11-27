#!/usr/bin/env bash
# Собирает исходники для 26.3: общий код + overlay (заменяет) - exclude (убирает).
set -euo pipefail
cd "$(dirname "$0")"

rm -rf assembled
mkdir -p assembled/java assembled/resources

cp -r ../src/main/java/. assembled/java/
cp -r ../src/main/resources/. assembled/resources/

[ -d overlay/java ] && cp -r overlay/java/. assembled/java/
[ -d overlay/resources ] && cp -r overlay/resources/. assembled/resources/

if [ -f exclude.txt ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%%#*}"
    line="$(printf '%s' "$line" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
    [ -z "$line" ] && continue
    rm -rf "assembled/$line"
    echo "excluded: $line"
  done < exclude.txt
fi

echo "assembled: $(find assembled/java -name '*.java' | wc -l) java files"
