#!/bin/bash
set -e
BASE=${1:-"http://localhost:5173"}
PASS=0
FAIL=0

check() {
  local name=$1
  local url=$2
  local expected=$3
  local status
  status=$(curl -s -o /dev/null -w "%{http_code}" "$url")
  if [ "$status" = "$expected" ]; then
    echo "[PASS] $name ($status)"
    PASS=$((PASS+1))
  else
    echo "[FAIL] $name (expected $expected, got $status)"
    FAIL=$((FAIL+1))
  fi
}

echo "=== Smoke Test ==="
check "homepage" "$BASE/login" "200"
check "frontend mood diary" "$BASE/patient/mood-diary" "200"
check "backend login route" "http://localhost:8080/api/auth/login" "401"
check "backend doctor list unauth" "http://localhost:8080/api/patient/doctors" "401"

echo "=== Results: $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ] || exit 1
