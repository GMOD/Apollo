#!/bin/bash
# End-to-end test for Apollo7
# Tests the full workflow: blank instance → admin login → add organism →
# create user → grant permissions → user annotates
#
# Suitable for CI: self-contained, cleans up after itself.

set -e

. "$(dirname "$0")/test-helpers.sh"

COOKIE_JAR=$(mktemp)
USER_COOKIE_JAR=$(mktemp)
TEST_DATA_DIR=""
APP_PID=""

cleanup() {
    rm -f "$COOKIE_JAR" "$USER_COOKIE_JAR"
    if [ -n "$TEST_DATA_DIR" ] && [ -d "$TEST_DATA_DIR" ]; then
        rm -rf "$TEST_DATA_DIR"
    fi
    stop_app
}
trap cleanup EXIT

# Auth params — bootstrap admin from application.yml
ADMIN_AUTH='"username":"admin@local.host","password":"password"'
USER_AUTH='"username":"testuser@example.com","password":"userpass123"'
CLIENT_TOKEN="e2e-test-$$"

start_app

# =========================================================================
# Phase 1: Verify blank instance
# =========================================================================

echo ""
echo "=== Phase 1: Blank instance ==="

echo ""
echo "=== Health endpoint ready ==="
RESPONSE=$(curl -s "$BASE_URL/health/index")
assert_contains "Health ready" '"status":"ready"' "$RESPONSE"

echo ""
echo "=== GWT UI loads ==="
RESPONSE=$(curl -s "$BASE_URL/annotator/index")
assert_contains "Annotator page loads" "annotator.nocache.js" "$RESPONSE"

echo ""
echo "=== Static resources serve correctly ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/annotator/annotator.nocache.js")
assert_eq "GWT JS serves" "200" "$HTTP_CODE"

# =========================================================================
# Phase 2: Admin login
# =========================================================================

echo ""
echo "=== Phase 2: Admin login ==="

echo ""
echo "=== Admin login via GWT endpoint ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/Login?operation=login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@local.host","password":"password"}' \
    -c "$COOKIE_JAR" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Admin login returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Admin session persists ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d '{}' \
    -b "$COOKIE_JAR")
assert_contains "Session persists" "admin@local.host" "$RESPONSE"
assert_not_contains "No error" "error" "$RESPONSE"

echo ""
echo "=== No organisms yet ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH}")
assert_eq "No organisms" "[]" "$RESPONSE"

# =========================================================================
# Phase 3: Add organism
# =========================================================================

echo ""
echo "=== Phase 3: Add organism ==="

echo ""
echo "=== Setup: Create test genome data ==="
TEST_DATA_DIR=$(mktemp -d)
mkdir -p "$TEST_DATA_DIR/seq"

python3 -c "
import random
random.seed(42)
seq = ''.join(random.choices('ACGT', k=50000))
print('>chr1')
for i in range(0, len(seq), 80):
    print(seq[i:i+80])
" > "$TEST_DATA_DIR/seq/chr1.fa"

python3 -c "print('chr1\t50000\t6\t80\t81')" > "$TEST_DATA_DIR/seq/chr1.fa.fai"

cat > "$TEST_DATA_DIR/seq/refSeqs.json" << 'REFSEQ'
[{"name":"chr1","start":0,"end":50000,"length":50000,"seqChunkSize":20000}]
REFSEQ

cat > "$TEST_DATA_DIR/trackList.json" << 'TRACKLIST'
{
  "tracks": [{
    "label": "DNA",
    "key": "Reference sequence",
    "type": "SequenceTrack",
    "storeClass": "JBrowse/Store/Sequence/IndexedFasta",
    "urlTemplate": "seq/chr1.fa",
    "faiUrlTemplate": "seq/chr1.fa.fai"
  }]
}
TRACKLIST
echo "  Created test data at $TEST_DATA_DIR"

echo ""
echo "=== Add organism ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/addOrganism" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"commonName\":\"TestOrganism\", \"directory\":\"$TEST_DATA_DIR\", \"genus\":\"Testus\", \"species\":\"testicus\"}")
assert_not_contains "addOrganism no error" "error" "$RESPONSE"
assert_contains "addOrganism returns name" "TestOrganism" "$RESPONSE"

ORGANISM_ID=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
for o in data:
    if o.get('commonName')=='TestOrganism':
        print(o['id'])
        break
" 2>/dev/null || echo "")
echo "  Organism ID: $ORGANISM_ID"

echo ""
echo "=== Organism appears in list ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH}")
assert_contains "Organism in list" "TestOrganism" "$RESPONSE"

echo ""
echo "=== Sequences loaded ==="
if [ -n "$ORGANISM_ID" ]; then
    RESPONSE=$(curl -s -b "$COOKIE_JAR" \
        "$BASE_URL/sequence/getSequences/?name=&start=0&length=50&sort=length&asc=false&clientToken=$CLIENT_TOKEN")
    assert_contains "chr1 in sequences" "chr1" "$RESPONSE"
    assert_contains "Sequence length" "50000" "$RESPONSE"
else
    echo "  SKIP: No organism ID"
    FAIL=$((FAIL + 2))
fi

# =========================================================================
# Phase 4: Create plain user and grant permissions
# =========================================================================

echo ""
echo "=== Phase 4: Create user and grant permissions ==="

echo ""
echo "=== Create plain user ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/createUser" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH, \"firstName\":\"Test\", \"lastName\":\"User\", \"email\":\"testuser@example.com\", \"newPassword\":\"userpass123\", \"role\":\"USER\"}")
HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
assert_eq "createUser no error" "no" "$HAS_ERROR"
assert_contains "createUser returns username" "testuser@example.com" "$RESPONSE"

USER_ID=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
print(data.get('userId') or data.get('id',''))
" 2>/dev/null || echo "")
echo "  User ID: $USER_ID"

echo ""
echo "=== User appears in loadUsers ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH}")
assert_contains "loadUsers shows new user" "testuser@example.com" "$RESPONSE"
USER_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")
assert_eq "Two users total" "2" "$USER_COUNT"

echo ""
echo "=== Grant user WRITE permission on organism ==="
if [ -n "$USER_ID" ] && [ -n "$ORGANISM_ID" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/user/updateOrganismPermission" \
        -H 'Content-Type: application/json' \
        -d "{$ADMIN_AUTH, \"userId\":$USER_ID, \"organism\":\"$ORGANISM_ID\", \"ADMINISTRATE\":false, \"WRITE\":true, \"READ\":true, \"EXPORT\":true}")
    assert_not_contains "updateOrganismPermission no error" "error" "$RESPONSE"
else
    echo "  SKIP: Missing user or organism ID"
    FAIL=$((FAIL + 1))
fi

# =========================================================================
# Phase 5: Plain user uses the system
# =========================================================================

echo ""
echo "=== Phase 5: Plain user workflow ==="

echo ""
echo "=== User login ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/Login?operation=login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"testuser@example.com","password":"userpass123"}' \
    -c "$USER_COOKIE_JAR" \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "User login returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== User session persists ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/checkLogin" \
    -H 'Content-Type: application/json' \
    -d '{}' \
    -b "$USER_COOKIE_JAR")
assert_contains "User session has username" "testuser@example.com" "$RESPONSE"

echo ""
echo "=== User can see organism ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$USER_AUTH}")
assert_contains "User sees organism" "TestOrganism" "$RESPONSE"

echo ""
echo "=== User can set current sequence ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/sequence/setCurrentSequence" \
    -H 'Content-Type: application/json' \
    -d "{$USER_AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"sequenceName\":\"chr1\"}")
echo "  INFO: setCurrentSequence: $(echo "$RESPONSE" | head -c 200)"

echo ""
echo "=== User can get features (empty) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$USER_AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
assert_contains "getFeatures returns features array" "features" "$RESPONSE"
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "?")
assert_eq "No features initially" "0" "$FEATURE_COUNT"

echo ""
echo "=== User adds a transcript ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addTranscript" \
    -H 'Content-Type: application/json' \
    -d "{
        $USER_AUTH,
        \"clientToken\":\"$CLIENT_TOKEN\",
        \"track\":\"chr1\",
        \"features\":[{
            \"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},
            \"location\":{\"fmin\":1000,\"fmax\":5000,\"strand\":1},
            \"children\":[
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":1000,\"fmax\":2000,\"strand\":1}},
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":3000,\"fmax\":5000,\"strand\":1}}
            ]
        }]
    }")
assert_contains "addTranscript returns features" "features" "$RESPONSE"
HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
assert_eq "addTranscript no error" "no" "$HAS_ERROR"

TRANSCRIPT_NAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    children = features[0].get('children',[])
    if children:
        print(children[0].get('uniquename',''))
    else:
        print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")
echo "  Transcript: $TRANSCRIPT_NAME"

echo ""
echo "=== Feature exists after creation ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$USER_AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "Features created" "true" "$([ "$FEATURE_COUNT" -ge 1 ] 2>/dev/null && echo true || echo false)"
assert_contains "Feature is mRNA" "mRNA" "$RESPONSE"

echo ""
echo "=== User can set feature name ==="
if [ -n "$TRANSCRIPT_NAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/setName" \
        -H 'Content-Type: application/json' \
        -d "{
            $USER_AUTH,
            \"clientToken\":\"$CLIENT_TOKEN\",
            \"track\":\"chr1\",
            \"features\":[{\"uniquename\":\"$TRANSCRIPT_NAME\",\"name\":\"my-gene-1\"}]
        }")
    assert_contains "setName returns name" "my-gene-1" "$RESPONSE"
else
    echo "  SKIP: No transcript uniquename"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "=== Admin can export GFF3 ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/IOService/write" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH, \"clientToken\":\"admin-$CLIENT_TOKEN\", \"type\":\"GFF3\", \"seqType\":\"genomic\", \"exportAllSequences\":true, \"output\":\"text\", \"exportGff3Fasta\":false, \"organism\":\"TestOrganism\"}")
assert_contains "GFF3 export has header" "##gff-version 3" "$RESPONSE"
assert_contains "GFF3 export has chr1" "chr1" "$RESPONSE"
assert_contains "GFF3 export has mRNA" "mRNA" "$RESPONSE"

echo ""
echo "=== Admin can see user's annotation ==="
# Admin must set current sequence first
curl -s -X POST "$BASE_URL/sequence/setCurrentSequence" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH, \"clientToken\":\"admin-$CLIENT_TOKEN\", \"sequenceName\":\"chr1\"}" > /dev/null
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$ADMIN_AUTH, \"clientToken\":\"admin-$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "Admin sees annotations" "true" "$([ "$FEATURE_COUNT" -ge 1 ] 2>/dev/null && echo true || echo false)"

echo ""
echo "=== User without permission is rejected ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addTranscript" \
    -H 'Content-Type: application/json' \
    -d "{
        \"username\":\"nobody@example.com\",\"password\":\"nopass\",
        \"clientToken\":\"$CLIENT_TOKEN\",
        \"track\":\"chr1\",
        \"features\":[{
            \"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},
            \"location\":{\"fmin\":10000,\"fmax\":12000,\"strand\":1},
            \"children\":[
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":10000,\"fmax\":12000,\"strand\":1}}
            ]
        }]
    }")
assert_contains "Unauthorized user rejected" "error" "$RESPONSE"

print_results

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
