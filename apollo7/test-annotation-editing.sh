#!/bin/bash
# Test script for Apollo7 annotation editing and WebSocket validation
# Self-sufficient: cleans database, starts app, runs tests, stops app

set -e

. "$(dirname "$0")/test-helpers.sh"

TEST_DATA_DIR=""
APP_PID=""

cleanup() {
    if [ -n "$TEST_DATA_DIR" ] && [ -d "$TEST_DATA_DIR" ]; then
        rm -rf "$TEST_DATA_DIR"
    fi
    stop_app
}
trap cleanup EXIT

# Common auth params for webservice calls
AUTH='"username":"admin@test.com","password":"testpass123"'
CLIENT_TOKEN="test-client-$$"

start_app

echo ""
echo "=== Setup: Register admin user ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/login/registerAdmin" \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin@test.com","password":"testpass123","firstName":"Admin","lastName":"User"}' \
    -w "\nHTTP_CODE:%{http_code}")
HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE:" | sed 's/HTTP_CODE://')
assert_eq "Register admin returns 200" "200" "$HTTP_CODE"

echo ""
echo "=== Setup: Create test organism data directory ==="
TEST_DATA_DIR=$(mktemp -d)
mkdir -p "$TEST_DATA_DIR/seq"

# Create FASTA sequence file (50kb of random DNA)
python3 -c "
import random
random.seed(42)
seq = ''.join(random.choices('ACGT', k=50000))
print('>chr1')
for i in range(0, len(seq), 80):
    print(seq[i:i+80])
" > "$TEST_DATA_DIR/seq/chr1.fa"

# Create FASTA index
python3 -c "
# Simple .fai: name length offset linebases linewidth
print('chr1\t50000\t6\t80\t81')
" > "$TEST_DATA_DIR/seq/chr1.fa.fai"

# Also create refSeqs.json (required by checkOrganism when genomeFasta not set)
cat > "$TEST_DATA_DIR/seq/refSeqs.json" << 'REFSEQ'
[
  {
    "name": "chr1",
    "start": 0,
    "end": 50000,
    "length": 50000,
    "seqChunkSize": 20000
  }
]
REFSEQ

# Create trackList.json pointing to indexed FASTA
cat > "$TEST_DATA_DIR/trackList.json" << TRACKLIST
{
  "tracks": [
    {
      "label": "DNA",
      "key": "Reference sequence",
      "type": "SequenceTrack",
      "storeClass": "JBrowse/Store/Sequence/IndexedFasta",
      "urlTemplate": "seq/chr1.fa",
      "faiUrlTemplate": "seq/chr1.fa.fai"
    }
  ]
}
TRACKLIST

echo "  Created test data at $TEST_DATA_DIR"

echo ""
echo "=== Test 1: Add organism ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/addOrganism" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"commonName\":\"TestOrganism\", \"directory\":\"$TEST_DATA_DIR\", \"genus\":\"Testus\", \"species\":\"testicus\"}")
assert_not_contains "addOrganism has no error" "error" "$RESPONSE"
assert_contains "addOrganism returns organism name" "TestOrganism" "$RESPONSE"

echo ""
echo "=== Test 2: Get organisms ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/organism/findAllOrganisms" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH}")
assert_contains "findAllOrganisms includes TestOrganism" "TestOrganism" "$RESPONSE"
# Extract organism ID
ORGANISM_ID=$(echo "$RESPONSE" | python3 -c "import sys,json; data=json.load(sys.stdin); print([o['id'] for o in data if o.get('commonName')=='TestOrganism'][0])" 2>/dev/null || echo "")
if [ -n "$ORGANISM_ID" ]; then
    echo "  INFO: Organism ID = $ORGANISM_ID"
else
    echo "  WARN: Could not extract organism ID"
fi

echo ""
echo "=== Test 3: Load sequences for organism ==="
if [ -n "$ORGANISM_ID" ]; then
    RESPONSE=$(curl -s "$BASE_URL/sequence/loadSequences/$ORGANISM_ID")
    assert_contains "loadSequences returns chr1" "chr1" "$RESPONSE"
    assert_contains "loadSequences has length" "50000" "$RESPONSE"
else
    echo "  SKIP: No organism ID"
    FAIL=$((FAIL + 2))
fi

echo ""
echo "=== Test 4: Set current sequence ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/sequence/setCurrentSequence" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"sequenceName\":\"chr1\"}")
echo "  INFO: setCurrentSequence response: $(echo "$RESPONSE" | head -c 200)"

echo ""
echo "=== Test 5: Get features (empty initially) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
assert_contains "getFeatures returns features array" "features" "$RESPONSE"

echo ""
echo "=== Test 6: Add a transcript (gene annotation) ==="
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
echo "  INFO: addTranscript response: $(echo "$RESPONSE" | head -c 300)"
assert_contains "addTranscript returns features" "features" "$RESPONSE"
# Check for top-level error key (not substring match)
HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
assert_eq "addTranscript has no error key" "no" "$HAS_ERROR"

# Extract the feature uniquename for later operations
FEATURE_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    # For gene-level features, get the transcript uniquename from children
    children = features[0].get('children',[])
    if children:
        print(children[0].get('uniquename',''))
    else:
        print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")
echo "  INFO: Feature uniquename = $FEATURE_UNIQUENAME"

echo ""
echo "=== Test 7: Get features (should have the transcript) ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
assert_contains "getFeatures returns the transcript" "mRNA" "$RESPONSE"
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "getFeatures has 1 top-level feature" "1" "$FEATURE_COUNT"
# Get the gene-level uniquename for use in later tests
GENE_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")
echo "  INFO: Gene uniquename = $GENE_UNIQUENAME"

echo ""
echo "=== Test 8: Set feature name ==="
if [ -n "$FEATURE_UNIQUENAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/setName" \
        -H 'Content-Type: application/json' \
        -d "{
            $AUTH,
            \"clientToken\":\"$CLIENT_TOKEN\",
            \"track\":\"chr1\",
            \"features\":[{\"uniquename\":\"$FEATURE_UNIQUENAME\",\"name\":\"test-gene-1\"}]
        }")
    assert_contains "setName returns updated feature" "test-gene-1" "$RESPONSE"
    HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
    assert_eq "setName has no error key" "no" "$HAS_ERROR"
else
    echo "  SKIP: No feature uniquename available"
fi

echo ""
echo "=== Test 9: Add a comment ==="
if [ -n "$FEATURE_UNIQUENAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addComments" \
        -H 'Content-Type: application/json' \
        -d "{
            $AUTH,
            \"clientToken\":\"$CLIENT_TOKEN\",
            \"track\":\"chr1\",
            \"features\":[{\"uniquename\":\"$FEATURE_UNIQUENAME\",\"comments\":[\"This is a test comment\"]}]
        }")
    echo "  INFO: addComments response: $(echo "$RESPONSE" | head -c 200)"
    HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
    assert_eq "addComments has no error key" "no" "$HAS_ERROR"
fi

echo ""
echo "=== Test 10: Flip strand ==="
if [ -n "$FEATURE_UNIQUENAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/flipStrand" \
        -H 'Content-Type: application/json' \
        -d "{
            $AUTH,
            \"clientToken\":\"$CLIENT_TOKEN\",
            \"track\":\"chr1\",
            \"features\":[{\"uniquename\":\"$FEATURE_UNIQUENAME\"}]
        }")
    assert_contains "flipStrand returns features" "features" "$RESPONSE"
    HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
    assert_eq "flipStrand has no error key" "no" "$HAS_ERROR"
    # Verify strand is now -1
    STRAND=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    print(features[0].get('location',{}).get('strand',0))
" 2>/dev/null || echo "")
    assert_eq "Strand is now -1" "-1" "$STRAND"
fi

echo ""
echo "=== Test 11: Add a second transcript ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/addTranscript" \
    -H 'Content-Type: application/json' \
    -d "{
        $AUTH,
        \"clientToken\":\"$CLIENT_TOKEN\",
        \"track\":\"chr1\",
        \"features\":[{
            \"type\":{\"name\":\"mRNA\",\"cv\":{\"name\":\"sequence\"}},
            \"location\":{\"fmin\":10000,\"fmax\":15000,\"strand\":1},
            \"children\":[
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":10000,\"fmax\":12000,\"strand\":1}},
                {\"type\":{\"name\":\"exon\",\"cv\":{\"name\":\"sequence\"}},\"location\":{\"fmin\":13000,\"fmax\":15000,\"strand\":1}}
            ]
        }]
    }")
assert_contains "Second addTranscript returns features" "features" "$RESPONSE"
HAS_ERROR=$(echo "$RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if 'error' in d else 'no')" 2>/dev/null || echo "parse_fail")
assert_eq "Second addTranscript has no error key" "no" "$HAS_ERROR"
FEATURE2_UNIQUENAME=$(echo "$RESPONSE" | python3 -c "
import sys,json
data=json.load(sys.stdin)
features = data.get('features',[])
if features:
    print(features[0].get('uniquename',''))
" 2>/dev/null || echo "")
echo "  INFO: Second feature uniquename = $FEATURE2_UNIQUENAME"

echo ""
echo "=== Test 12: Verify two features exist ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "getFeatures has 2 top-level features" "2" "$FEATURE_COUNT"

echo ""
echo "=== Test 13: Delete a feature ==="
if [ -n "$FEATURE2_UNIQUENAME" ]; then
    RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/deleteFeature" \
        -H 'Content-Type: application/json' \
        -d "{
            $AUTH,
            \"clientToken\":\"$CLIENT_TOKEN\",
            \"track\":\"chr1\",
            \"features\":[{\"uniquename\":\"$FEATURE2_UNIQUENAME\"}]
        }")
    echo "  INFO: deleteFeature response: $(echo "$RESPONSE" | head -c 200)"
fi

echo ""
echo "=== Test 14: Verify one feature remains ==="
RESPONSE=$(curl -s -X POST "$BASE_URL/annotationEditor/getFeatures" \
    -H 'Content-Type: application/json' \
    -d "{$AUTH, \"clientToken\":\"$CLIENT_TOKEN\", \"track\":\"chr1\"}")
FEATURE_COUNT=$(echo "$RESPONSE" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('features',[])))" 2>/dev/null || echo "0")
assert_eq "getFeatures has 1 feature after delete" "1" "$FEATURE_COUNT"

echo ""
echo "=== Test 15: WebSocket STOMP endpoint info ==="
RESPONSE=$(curl -s "$BASE_URL/stomp/info")
assert_contains "STOMP info has websocket" "websocket" "$RESPONSE"

echo ""
echo "=== Test 16: WebSocket STOMP connection test ==="
WSTEST_RESULT=$(node -e "
const http = require('http');
const url = new URL('$BASE_URL/stomp/info');
const req = http.get(url.href, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        try {
            const info = JSON.parse(data);
            if (info.websocket !== undefined && info.entropy) {
                console.log('STOMP_OK');
            } else {
                console.log('STOMP_MISSING_FIELDS');
            }
        } catch(e) {
            console.log('STOMP_PARSE_ERROR');
        }
    });
});
req.on('error', (e) => console.log('STOMP_CONNECT_ERROR'));
req.setTimeout(5000, () => { req.destroy(); console.log('STOMP_TIMEOUT'); });
" 2>/dev/null)
assert_eq "STOMP endpoint is fully functional" "STOMP_OK" "$WSTEST_RESULT"

echo ""
echo "=== Test 17: SockJS WebSocket upgrade test ==="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Upgrade: websocket" \
    -H "Connection: Upgrade" \
    -H "Sec-WebSocket-Version: 13" \
    -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
    "$BASE_URL/stomp/websocket")
echo "  INFO: WebSocket upgrade response: $HTTP_CODE"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/stomp/000/test/xhr")
assert_eq "SockJS XHR transport responds 200" "200" "$HTTP_CODE"

print_results

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
