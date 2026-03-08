#!/bin/bash
# Shared test helpers for Apollo7 test scripts
# Source this file: . "$(dirname "$0")/test-helpers.sh"

BASE_URL="${APOLLO_URL:-http://localhost:8080/apollo}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0

assert_eq() {
    local desc="$1" expected="$2" actual="$3"
    if [ "$expected" = "$actual" ]; then
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc (expected '$expected', got '$actual')"
        FAIL=$((FAIL + 1))
    fi
}

assert_contains() {
    local desc="$1" expected="$2" actual="$3"
    if echo "$actual" | grep -q "$expected"; then
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc (expected to contain '$expected', got '$actual')"
        FAIL=$((FAIL + 1))
    fi
}

assert_not_contains() {
    local desc="$1" unexpected="$2" actual="$3"
    if echo "$actual" | grep -q "$unexpected"; then
        echo "  FAIL: $desc (unexpectedly contains '$unexpected')"
        FAIL=$((FAIL + 1))
    else
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    fi
}

wait_for_health() {
    local max_wait="${1:-90}"
    echo "=== Waiting for app to be ready (health endpoint) ==="
    for i in $(seq 1 "$max_wait"); do
        local response
        response=$(curl -s "$BASE_URL/health/index" 2>/dev/null)
        if echo "$response" | grep -q '"status":"ready"'; then
            echo "  App is ready (${i}s)"
            return 0
        fi
        if [ "$i" -eq "$max_wait" ]; then
            echo "  FAIL: App not ready after ${max_wait} seconds"
            return 1
        fi
        sleep 1
    done
}

start_app() {
    echo "=== Stopping any existing instance ==="
    pkill -f 'apollo.*bootRun' 2>/dev/null || true
    pkill -f 'apollo.*GrailsApp' 2>/dev/null || true
    sleep 2

    echo "=== Cleaning database ==="
    rm -f "$SCRIPT_DIR"/devDb.mv.db "$SCRIPT_DIR"/devDb.trace.db

    echo "=== Starting app ==="
    cd "$SCRIPT_DIR"
    ./gradlew bootRun > /dev/null 2>&1 &
    APP_PID=$!

    wait_for_health || exit 1
}

stop_app() {
    if [ -n "$APP_PID" ]; then
        echo ""
        echo "=== Stopping app (PID $APP_PID) ==="
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
        APP_PID=""
    fi
}

print_results() {
    echo ""
    echo "==============================="
    echo "Results: $PASS passed, $FAIL failed"
    echo "==============================="
}
