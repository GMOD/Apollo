#!/bin/bash
# Test script for Apollo7 login flow validation
# Prerequisites: App must be running at http://localhost:8080/apollo with a CLEAN database
# Start with: rm -f devDb.mv.db devDb.trace.db && ./gradlew bootRun

set -e

BASE_URL="${APOLLO_URL:-http://localhost:8080/apollo}"
COOKIE_JAR=$(mktemp)
PASS=0
FAIL=0

cleanup() {
    rm -f "$COOKIE_JAR"
}
trap cleanup EXIT

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

echo "=== Waiting for app to be ready ==="
for i in $(seq 1 60); do
    if curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/" | grep -q "200\|302"; then
        echo "  App is ready"
        break
    fi
    if [ "$i" -eq 60 ]; then
        echo "  FAIL: App not ready after 60 seconds"
        exit 1
    fi
    sleep 1
done

echo ""
echo "=== Test 1: Root URL redirects to annotator ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")
REDIRECT=$(curl -s -o /dev/null -w "%{redirect_url}" "$BASE_URL/")
assert_eq "Root returns 302" "302" "$HTTP_CODE"
assert_contains "Redirects to annotator" "annotator" "$REDIRECT"

echo ""
echo "=== Test 2: Login page is accessible ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/auth/login")
assert_eq "Login page returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Test 3: Login fails with bad credentials ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"nobody@test.com","password":"wrong"}')
assert_contains "Returns error for bad credentials" "error" "$RESPONSE"

echo ""
echo "=== Test 4: checkLogin with no users returns has_users=false ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/checkLogin" \
    -H 'Content-Type: application/json' \
    -d '{}')
assert_contains "has_users key present" "has_users" "$RESPONSE"

echo ""
echo "=== Test 5: Register admin user ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/registerAdmin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"testpass123","firstName":"Admin","lastName":"User"}' \
    -c "$COOKIE_JAR" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Register admin returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Test 6: Login with registered admin ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"testpass123"}' \
    -c "$COOKIE_JAR" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Login returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Test 7: Authenticated access to secured endpoint ==="
RESPONSE=$(curl -s "$BASE_URL/annotator/index" \
    -b "$COOKIE_JAR" \
    -o /dev/null -w "%{http_code}")
assert_eq "Annotator index accessible with session" "200" "$RESPONSE"

echo ""
echo "=== Test 8: loadUsers via webservice auth (JSON body credentials) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"testpass123"}')
assert_contains "loadUsers returns admin user" "admin@test.com" "$RESPONSE"
assert_contains "loadUsers includes role" "ADMIN" "$RESPONSE"
assert_not_contains "loadUsers has no error" "error" "$RESPONSE"

echo ""
echo "=== Test 9: checkLogin returns user info when authenticated ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/checkLogin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"testpass123"}')
assert_contains "checkLogin returns username" "admin@test.com" "$RESPONSE"
assert_contains "checkLogin returns has_users" "has_users" "$RESPONSE"

echo ""
echo "=== Test 10: WebSocket STOMP endpoint is available ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/stomp/info")
assert_eq "STOMP info returns 200" "200" "$HTTP_CODE"
RESPONSE=$(curl -s "$BASE_URL/stomp/info")
assert_contains "STOMP info has websocket flag" "websocket" "$RESPONSE"

echo ""
echo "=== Test 11: Unauthenticated registerAdmin rejected when users exist ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/registerAdmin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin2@test.com","password":"testpass456","firstName":"Admin2","lastName":"User2"}' \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Unauthenticated registerAdmin returns 500" "500" "$HTTP_CODE"

echo ""
echo "=== Test 12: Login with wrong password fails ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"wrongpassword"}')
assert_contains "Wrong password returns error" "error" "$RESPONSE"

echo ""
echo "==============================="
echo "Results: $PASS passed, $FAIL failed"
echo "==============================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
