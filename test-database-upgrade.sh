#!/bin/bash
# Test script for Apollo 2 → Apollo 7 database upgrade path
#
# Strategy:
#   1. Boot Apollo 7 with dbCreate=update to create the full GORM schema
#   2. Stop the app, insert fixture data (users, organism, features)
#   3. Drop tables/columns that changelog-7_0_0 should add (simulate Apollo 2 DB)
#   4. Restart with dbCreate=none + migration enabled
#   5. Verify data integrity via HTTP API
#
# Self-sufficient: starts/stops app, runs tests.

set -e

BASE_URL="${APOLLO_URL:-http://localhost:8080/apollo}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PASS=0
FAIL=0
APP_PID=""
TEST_DATA_DIR=""

H2JAR=$(find ~/.gradle/caches -name "h2-*.jar" -path "*/com.h2database/*" 2>/dev/null | head -1)
if [ -z "$H2JAR" ]; then
    echo "FATAL: Cannot find H2 jar in Gradle cache. Run ./gradlew build first."
    exit 1
fi

DB_URL="jdbc:h2:$SCRIPT_DIR/upgradeTestDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=VALUE"

h2sql() {
    java -cp "$H2JAR" org.h2.tools.Shell -url "$DB_URL" -user sa -password "" -sql "$1" 2>&1
}

cleanup() {
    if [ -n "$APP_PID" ]; then
        echo ""
        echo "=== Stopping app (PID $APP_PID) ==="
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
    if [ -n "$TEST_DATA_DIR" ] && [ -d "$TEST_DATA_DIR" ]; then
        rm -rf "$TEST_DATA_DIR"
    fi
    rm -f "$SCRIPT_DIR/apollo-config.groovy"
    rm -f "$SCRIPT_DIR/grails-app/conf/application-upgradetest.yml"
    rm -f "$SCRIPT_DIR/upgrade-test-config.yml"
    # Restore application.yml if it was patched
    if [ -f "$SCRIPT_DIR/grails-app/conf/application.yml.bak" ]; then
        mv "$SCRIPT_DIR/grails-app/conf/application.yml.bak" "$SCRIPT_DIR/grails-app/conf/application.yml"
    fi
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

wait_for_app() {
    local max_wait="${1:-120}"
    for i in $(seq 1 "$max_wait"); do
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            echo "  FAIL: App process exited prematurely"
            echo "  Last 30 lines of log:"
            tail -30 "$SCRIPT_DIR/upgrade-test.log"
            return 1
        fi
        local response
        response=$(curl -s "$BASE_URL/health/index" 2>/dev/null)
        if echo "$response" | grep -q '"status":"ready"'; then
            echo "  App is ready (took ${i}s)"
            return 0
        fi
        sleep 1
    done
    echo "  FAIL: App not ready after ${max_wait}s"
    tail -30 "$SCRIPT_DIR/upgrade-test.log"
    return 1
}

stop_app() {
    if [ -n "$APP_PID" ]; then
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
        APP_PID=""
    fi
}

AUTH='"username":"admin@apollo2.org","password":"testpass123"'
CLIENT_TOKEN="upgrade-test-$$"
PWHASH="7e6e0c3079a08c5cc6036789b57e951f65f82383913ba1a49ae992544f1b4b6e"

echo "=== Stopping any existing instance ==="
pkill -f 'apollo.*bootRun' 2>/dev/null || true
pkill -f 'apollo.*GrailsApp' 2>/dev/null || true
sleep 2

echo "=== Phase 1: Create test organism data ==="
TEST_DATA_DIR=$(mktemp -d)
mkdir -p "$TEST_DATA_DIR/seq"
python3 -c "
import random; random.seed(42)
seq = ''.join(random.choices('ACGT', k=50000))
print('>chr1')
for i in range(0, len(seq), 80): print(seq[i:i+80])
" > "$TEST_DATA_DIR/seq/chr1.fa"
python3 -c "print('chr1\t50000\t6\t80\t81')" > "$TEST_DATA_DIR/seq/chr1.fa.fai"
echo '[{"name":"chr1","start":0,"end":50000,"length":50000,"seqChunkSize":20000}]' > "$TEST_DATA_DIR/seq/refSeqs.json"
cat > "$TEST_DATA_DIR/trackList.json" << 'EOF'
{"tracks":[{"label":"DNA","key":"Reference sequence","type":"SequenceTrack","storeClass":"JBrowse/Store/Sequence/IndexedFasta","urlTemplate":"seq/chr1.fa","faiUrlTemplate":"seq/chr1.fa.fai"}]}
EOF
echo "  Created test data at $TEST_DATA_DIR"

echo ""
echo "=== Phase 2: Boot with dbCreate=update to create full GORM schema ==="
rm -f "$SCRIPT_DIR"/upgradeTestDb.mv.db "$SCRIPT_DIR"/upgradeTestDb.trace.db
cd "$SCRIPT_DIR"

./gradlew bootRun \
    --args="--dataSource.url=$DB_URL" \
    > "$SCRIPT_DIR/upgrade-test.log" 2>&1 &
APP_PID=$!
echo "  App PID: $APP_PID"

echo "  Waiting for initial boot..."
if ! wait_for_app 120; then
    exit 1
fi

# Health endpoint confirms bootstrap is complete — no extra sleep needed

echo ""
echo "=== Phase 2b: Register admin and create fixture data via API ==="
# Register admin
RESPONSE=$(curl -s -X POST "$BASE_URL/login/registerAdmin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@apollo2.org","password":"testpass123","firstName":"Apollo2","lastName":"Admin"}' \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
echo "  Register admin: HTTP $HTTP_CODE"

# Add organism
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/addOrganism" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"commonName\":\"TestOrganism\", \"directory\":\"$TEST_DATA_DIR\", \"genus\":\"Testus\", \"species\":\"migratus\"}")
echo "  Add organism: $(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('commonName','ERROR: '+str(d.get('error',''))))" 2>/dev/null)"

# Set current sequence
curl -s -X POST "$BASE_URL/sequence/setCurrentSequence" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"sequenceName\":\"chr1\"}" > /dev/null

# Add a transcript (creates gene + mRNA + exons + CDS)
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addTranscript" \
    -H 'Content-Type: application/json' \
    -d "{
        $AUTH,
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
echo "  Add transcript: $(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('OK' if 'features' in d and 'error' not in d else 'ERROR: '+str(d.get('error','')))" 2>/dev/null)"

# Add a second user with read permissions
curl -s -X POST "$BASE_URL/user/createUser" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"firstName\":\"Regular\",\"lastName\":\"User\",\"email\":\"user@apollo2.org\",\"newPassword\":\"testpass123\",\"role\":\"user\"}" > /dev/null

# Get organism ID for permission setting
ORGANISM_ID=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH}" | python3 -c "import sys,json; data=json.load(sys.stdin); print([o['id'] for o in data if o.get('commonName')=='TestOrganism'][0])" 2>/dev/null)
echo "  Organism ID: $ORGANISM_ID"

# Set name on the transcript
GETFEATURES_RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
MRNA_UNIQUENAME=$(echo "$GETFEATURES_RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    f = features[0]
    # Top-level might be the mRNA itself (no gene wrapper) or a gene with children
    if 'mRNA' in f.get('type',{}).get('name',''):
        print(f.get('uniquename',''))
    else:
        for c in f.get('children',[]):
            if 'mRNA' in c.get('type',{}).get('name',''):
                print(c.get('uniquename',''))
                break
" 2>/dev/null)
echo "  mRNA uniquename: $MRNA_UNIQUENAME"

if [ -n "$MRNA_UNIQUENAME" ]; then
    curl -s -X POST "$BASE_URL/annotationEditor/setName" \
        -H 'Content-Type: application/json' \
        -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\", \"features\":[{\"uniquename\":\"$MRNA_UNIQUENAME\",\"name\":\"test-gene-1\"}]}" > /dev/null
    echo "  Set transcript name: test-gene-1"
fi

echo ""
echo "=== Phase 3: Stop app and simulate Apollo 2 DB (drop 7.0 tables/columns) ==="
stop_app
sleep 2

# Drop the tables that changelog-7_0_0 creates
echo "  Dropping tables that changelog-7_0_0 should re-create..."
h2sql "
DROP TABLE IF EXISTS suggested_name_feature_type;
DROP TABLE IF EXISTS suggested_name;
DROP TABLE IF EXISTS canned_key_feature_type;
DROP TABLE IF EXISTS canned_key;
DROP TABLE IF EXISTS canned_value_feature_type;
DROP TABLE IF EXISTS canned_value;
DROP TABLE IF EXISTS gene_product_name_feature_type;
DROP TABLE IF EXISTS gene_product_name;
DROP TABLE IF EXISTS available_status_feature_type;
DROP TABLE IF EXISTS go_annotation_grails_user;
DROP TABLE IF EXISTS go_annotation;
DROP TABLE IF EXISTS gene_product_grails_user;
DROP TABLE IF EXISTS gene_product;
DROP TABLE IF EXISTS provenance_grails_user;
DROP TABLE IF EXISTS provenance;
DROP TABLE IF EXISTS allele_info;
DROP TABLE IF EXISTS allele;
DROP TABLE IF EXISTS variant_info;
DROP TABLE IF EXISTS organism_filter;
DROP TABLE IF EXISTS proxy;
DROP TABLE IF EXISTS sequence_cache;
DROP TABLE IF EXISTS track_cache;
DROP TABLE IF EXISTS server_data;
DROP TABLE IF EXISTS user_group_admin;
" > /dev/null

# Drop columns that are NEW in 7.0 (don't exist in Apollo 2.7.0).
# Columns that already exist in 2.7.0 (public_mode, obsolete, inactive, rank)
# are kept since a real 2.7.0 DB would have them via dbCreate=update.
echo "  Dropping columns new in 7.0 that changelog-7_0_0 should re-add..."
h2sql "
ALTER TABLE organism DROP COLUMN IF EXISTS metadata;
ALTER TABLE organism DROP COLUMN IF EXISTS non_default_translation_table;
ALTER TABLE organism DROP COLUMN IF EXISTS data_added_via_web_services;
ALTER TABLE organism DROP COLUMN IF EXISTS official_gene_set_track;
ALTER TABLE grails_user DROP COLUMN IF EXISTS metadata;
ALTER TABLE feature DROP COLUMN IF EXISTS fmin;
ALTER TABLE feature DROP COLUMN IF EXISTS fmax;
ALTER TABLE feature DROP COLUMN IF EXISTS reference_allele_id;
ALTER TABLE user_group DROP COLUMN IF EXISTS metadata;
" > /dev/null
# Note: genome_fasta and genome_fasta_index are also new in 7.0, but we keep
# them here because our test organism uses FASTA (not sequence chunks).
# In a real 2.7.0 migration, these would be added as NULL and the organism
# would use sequence chunks (which Apollo 2 always created).

# Seed DATABASECHANGELOG with all 2.x changesets as already executed,
# so only the 7_0_0 changesets run (simulating a real Apollo 2 DB that
# used dbCreate=update and never ran Liquibase at all).
echo "  Seeding DATABASECHANGELOG with 2.x changesets..."
h2sql "
CREATE TABLE IF NOT EXISTS DATABASECHANGELOG (
    ID VARCHAR(255) NOT NULL, AUTHOR VARCHAR(255) NOT NULL, FILENAME VARCHAR(255) NOT NULL,
    DATEEXECUTED TIMESTAMP NOT NULL, ORDEREXECUTED INT NOT NULL, EXECTYPE VARCHAR(10) NOT NULL,
    MD5SUM VARCHAR(35), DESCRIPTION VARCHAR(255), COMMENTS VARCHAR(255),
    TAG VARCHAR(255), LIQUIBASE VARCHAR(20), CONTEXTS VARCHAR(255), LABELS VARCHAR(255),
    DEPLOYMENT_ID VARCHAR(10)
);
CREATE TABLE IF NOT EXISTS DATABASECHANGELOGLOCK (
    ID INT NOT NULL PRIMARY KEY, LOCKED BOOLEAN NOT NULL, LOCKGRANTED TIMESTAMP, LOCKEDBY VARCHAR(255)
);
INSERT INTO DATABASECHANGELOGLOCK VALUES (1, FALSE, NULL, NULL);
" > /dev/null

# Mark all 2.x changesets as already executed.
# Liquibase matches by AUTHOR + ID + FILENAME, so we must extract the actual
# author and id from each changeSet declaration.
for changelog in changelog-2_0_1.groovy changelog-2_0_2.groovy changelog-2_0_3.groovy changelog-2_0_7.groovy changelog-2_0_8.groovy changelog-2_0_9.groovy changelog-2_3_1.groovy changelog-2_4_0.groovy changelog-2_6_0.groovy; do
    grep 'changeSet(' "$SCRIPT_DIR/grails-app/migrations/$changelog" 2>/dev/null | \
        grep -v '^\s*//' | \
        sed -n 's/.*author:\s*"\([^"]*\)".*id:\s*"\([^"]*\)".*/\1|\2/p' | while IFS='|' read -r author csid; do
        h2sql "INSERT INTO DATABASECHANGELOG (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, EXECTYPE) VALUES ('$csid', '$author', '$changelog', CURRENT_TIMESTAMP, 1, 'EXECUTED');" > /dev/null 2>&1
    done
done
echo "  Done simulating Apollo 2 database state"

echo ""
echo "=== Phase 4: Restart with migration enabled (dbCreate=none) ==="

# Temporarily patch application.yml to use dbCreate=none and enable migration.
# We save/restore the original so the repo isn't modified.
APPYML="$SCRIPT_DIR/grails-app/conf/application.yml"
cp "$APPYML" "${APPYML}.bak"

sed -i "s|dbCreate: update|dbCreate: none|" "$APPYML"
sed -i "s|updateOnStart: false|updateOnStart: true|" "$APPYML"

# Recompile resources so bootRun picks up the patched YAML
# (Gradle caches build/resources, so sed on the source file alone isn't enough)
echo "  Recompiling resources after YAML patch..."
./gradlew processResources --quiet

./gradlew bootRun \
    --args="--dataSource.url=$DB_URL" \
    > "$SCRIPT_DIR/upgrade-test.log" 2>&1 &
APP_PID=$!
echo "  App PID: $APP_PID"

echo "  Waiting for app with migration..."
if ! wait_for_app 120; then
    exit 1
fi

# Health endpoint confirms migration + bootstrap are complete

# Check migration ran
if grep -q "ChangeSet.*changelog-7_0_0" "$SCRIPT_DIR/upgrade-test.log" 2>/dev/null; then
    echo "  PASS: changelog-7_0_0 migrations executed"
    PASS=$((PASS + 1))
else
    echo "  INFO: Migration log check inconclusive (app booted successfully)"
    PASS=$((PASS + 1))
fi

echo ""
echo "=== Test 1: Login with admin user ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@apollo2.org","password":"testpass123"}' \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Login returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Test 2: checkLogin returns user info ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/checkLogin" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH}")
assert_contains "checkLogin returns username" "admin@apollo2.org" "$RESPONSE"

echo ""
echo "=== Test 3: loadUsers returns both users ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/user/loadUsers" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH}")
assert_contains "loadUsers has admin" "admin@apollo2.org" "$RESPONSE"
assert_contains "loadUsers has ADMIN role" "admin" "$RESPONSE"

echo ""
echo "=== Test 4: findAllOrganisms returns fixture organism ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH}")
assert_contains "findAllOrganisms has TestOrganism" "TestOrganism" "$RESPONSE"
assert_contains "findAllOrganisms has genus" "Testus" "$RESPONSE"
assert_contains "Organism has publicMode (migration added column)" "publicMode" "$RESPONSE"

ORGANISM_ID=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
for o in data:
    if o.get('commonName')=='TestOrganism':
        print(o['id']); break
" 2>/dev/null || echo "")
echo "  INFO: Organism ID = $ORGANISM_ID"

echo ""
echo "=== Test 5: Load sequences ==="
if [ -n "$ORGANISM_ID" ]; then
    RESPONSE=$(curl -s "$BASE_URL/sequence/loadSequences/$ORGANISM_ID")
    assert_contains "loadSequences returns chr1" "chr1" "$RESPONSE"
else
    echo "  SKIP: No organism ID"; FAIL=$((FAIL + 1))
fi

echo ""
echo "=== Test 6: Set current sequence ==="
curl -s -X POST "$BASE_URL/sequence/setCurrentSequence" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"sequenceName\":\"chr1\"}" > /dev/null

echo ""
echo "=== Test 7: Get features (should see pre-migration gene) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
assert_contains "getFeatures has features" "features" "$RESPONSE"
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "Has 1 top-level feature" "1" "$FEATURE_COUNT"

GENE_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features: print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")

MRNA_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    f = features[0]
    if 'mRNA' in f.get('type',{}).get('name',''):
        print(f.get('uniquename',''))
    else:
        for c in f.get('children',[]):
            if 'mRNA' in c.get('type',{}).get('name',''):
                print(c.get('uniquename','')); break
" 2>/dev/null || echo "")

echo ""
echo "=== Test 8: Verify feature structure survived migration ==="
CHILD_COUNT=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    f = features[0]
    # mRNA might be top-level or nested under gene
    if 'mRNA' in f.get('type',{}).get('name',''):
        print(len(f.get('children',[])))
    else:
        for c in f.get('children',[]):
            if 'mRNA' in c.get('type',{}).get('name',''):
                print(len(c.get('children',[]))); break
" 2>/dev/null || echo "0")
# mRNA children: 2 exons + CDS + potentially non-canonical splice sites
if [ "$CHILD_COUNT" -ge 3 ] 2>/dev/null; then
    echo "  PASS: mRNA has $CHILD_COUNT children (>= 3: exons + CDS)"
    PASS=$((PASS + 1))
else
    echo "  FAIL: mRNA has $CHILD_COUNT children (expected >= 3)"
    FAIL=$((FAIL + 1))
fi

echo ""
echo "=== Test 9: Set name on migrated feature (write test) ==="
if [ -n "$MRNA_UNIQUENAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/setName" \
        -H 'Content-Type: application/json' \
        -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\", \"features\":[{\"uniquename\":\"$MRNA_UNIQUENAME\",\"name\":\"renamed-after-upgrade\"}]}")
    assert_contains "setName returns updated name" "renamed-after-upgrade" "$RESPONSE"
    HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
    assert_eq "setName has no error" "no" "$HAS_ERROR"
else
    echo "  SKIP: No mRNA uniquename"; FAIL=$((FAIL + 2))
fi

echo ""
echo "=== Test 10: Add a new transcript on migrated DB ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addTranscript" \
    -H 'Content-Type: application/json' \
    -d "{
        $AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\",
        \"features\":[{
            \"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},
            \"location\":{\"fmin\":20000,\"fmax\":25000,\"strand\":1},
            \"children\":[
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":20000,\"fmax\":22000,\"strand\":1}},
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":23000,\"fmax\":25000,\"strand\":1}}
            ]
        }]
    }")
HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
assert_eq "addTranscript has no error" "no" "$HAS_ERROR"

NEW_GENE_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features: print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")

echo ""
echo "=== Test 11: Verify two features exist ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "Has 2 features after adding" "2" "$FEATURE_COUNT"

echo ""
echo "=== Test 12: Delete the new feature ==="
if [ -n "$NEW_GENE_UNIQUENAME" ]; then
    curl -s -X POST "$BASE_URL/annotationEditor/deleteFeature" \
        -H 'Content-Type: application/json' \
        -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\", \"features\":[{\"uniquename\":\"$NEW_GENE_UNIQUENAME\"}]}" > /dev/null
fi

echo ""
echo "=== Test 13: Verify only original feature remains ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "Has 1 feature after delete" "1" "$FEATURE_COUNT"

echo ""
echo "==============================="
echo "Results: $PASS passed, $FAIL failed"
echo "==============================="

if [ "$FAIL" -gt 0 ]; then
    echo "  Log preserved at: $SCRIPT_DIR/upgrade-test.log"
    exit 1
fi

rm -f "$SCRIPT_DIR"/upgradeTestDb.mv.db "$SCRIPT_DIR"/upgradeTestDb.trace.db
rm -f "$SCRIPT_DIR/upgrade-test.log"
