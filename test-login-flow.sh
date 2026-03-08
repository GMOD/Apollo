#!/bin/bash
# Test script for Apollo7 login flow validation
# Self-sufficient: cleans database, starts app, runs tests, stops app
#
# The app bootstraps an admin user (admin@local.host / password) via
# the apollo.admin config in application.yml.

set -e

. "$(dirname "$0")/test-helpers.sh"

COOKIE_JAR=$(mktemp)
COOKIE_JAR2=$(mktemp)
APP_PID=""

cleanup() {
    rm -f "$COOKIE_JAR" "$COOKIE_JAR2"
    stop_app
}
trap cleanup EXIT

start_app

echo ""
echo "=== Root URL redirects to annotator ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/")
REDIRECT=$(curl -s -o /dev/null -w "%{redirect_url}" "$BASE_URL/")
assert_eq "Root returns 302" "302" "$HTTP_CODE"
assert_contains "Redirects to annotator" "annotator" "$REDIRECT"

echo ""
echo "=== Annotator index page loads ==="
RESPONSE=$(curl -s "$BASE_URL/annotator/index")
assert_contains "Page has annotator.nocache.js" "annotator.nocache.js" "$RESPONSE"
assert_contains "Page has Annotator title" "Annotator" "$RESPONSE"

echo ""
echo "=== Login fails with bad credentials ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/Login?operation=login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"nobody@test.com","password":"wrong"}')
assert_contains "Returns error for bad credentials" "error" "$RESPONSE"

echo ""
echo "=== Login with wrong password fails ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/Login?operation=login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"wrongpassword"}')
assert_contains "Wrong password returns error" "error" "$RESPONSE"

echo ""
echo "=== Login with bootstrap admin (GWT endpoint) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/Login?operation=login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"password"}' \
    -c "$COOKIE_JAR" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "GWT Login endpoint returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Session persistence — loadUsers with cookie only ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{}' \
    -b "$COOKIE_JAR")
assert_contains "Session persists — loadUsers works" "admin@local.host" "$RESPONSE"
assert_contains "loadUsers includes ADMIN role" "ADMIN" "$RESPONSE"
assert_not_contains "loadUsers has no error" "error" "$RESPONSE"

echo ""
echo "=== Login via direct controller endpoint ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"password"}' \
    -c "$COOKIE_JAR2" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Direct login returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Direct login session persistence ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{}' \
    -b "$COOKIE_JAR2")
assert_contains "Direct login session persists" "admin@local.host" "$RESPONSE"

echo ""
echo "=== loadUsers via inline credentials ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"password"}')
assert_contains "loadUsers with inline auth works" "admin@local.host" "$RESPONSE"

echo ""
echo "=== checkLogin returns user info ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/checkLogin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"password"}')
assert_contains "checkLogin returns username" "admin@local.host" "$RESPONSE"
assert_contains "checkLogin returns has_users" "has_users" "$RESPONSE"

echo ""
echo "=== WebSocket STOMP endpoint ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/stomp/info")
assert_eq "STOMP info returns 200" "200" "$HTTP_CODE"
RESPONSE=$(curl -s "$BASE_URL/stomp/info")
assert_contains "STOMP info has websocket flag" "websocket" "$RESPONSE"

echo ""
echo "=== Static resources — GWT JS ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/annotator/annotator.nocache.js")
assert_eq "annotator.nocache.js returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Static resources — GWT CSS ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/annotator/css/bootstrap-3.3.7.min.cache.css")
assert_eq "Bootstrap CSS returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Static resources — GWT images ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/annotator/clear.cache.gif")
assert_eq "clear.cache.gif returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Asset pipeline — JS ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/assets/application.js?compile=false")
assert_eq "application.js returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Asset pipeline — CSS ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/assets/annotator.css?compile=false")
assert_eq "annotator.css returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== getSequences returns empty when no organisms ==="
RESPONSE=$(curl -s -b "$COOKIE_JAR" \
    "$BASE_URL/sequence/getSequences/?name=&start=0&length=50&sort=length&asc=false&clientToken=test123")
assert_eq "getSequences returns empty array" "[]" "$RESPONSE"

echo ""
echo "=== Health endpoint ==="
RESPONSE=$(curl -s "$BASE_URL/health/index")
assert_contains "Health returns ready" '"status":"ready"' "$RESPONSE"

echo ""
echo "=== Logout clears session ==="
curl -s -X POST "$BASE_URL/login/logout" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host"}' \
    -b "$COOKIE_JAR" > /dev/null
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{}' \
    -b "$COOKIE_JAR")
assert_contains "loadUsers fails after logout" "error" "$RESPONSE"

print_results

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
