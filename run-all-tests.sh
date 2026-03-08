#!/bin/bash
# Consolidated test runner for Apollo7
# Runs all test suites sequentially, each with a fresh database and app instance.
# Usage: ./run-all-tests.sh [suite...]
#   No args: runs all suites
#   With args: runs only named suites (login, annotation, upgrade)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

SUITES_AVAILABLE="login annotation upgrade postgresql"
SUITES_TO_RUN="${*:-$SUITES_AVAILABLE}"

TOTAL_PASS=0
TOTAL_FAIL=0
SUITE_RESULTS=""

run_suite() {
    local name="$1" script="$2"
    echo ""
    echo "############################################"
    echo "# Suite: $name"
    echo "############################################"
    echo ""

    if bash "$script"; then
        local result="PASS"
    else
        local result="FAIL"
    fi

    # Extract pass/fail counts from script output
    local counts
    counts=$(bash "$script" 2>&1 | grep "^Results:" || true)

    SUITE_RESULTS="${SUITE_RESULTS}  ${name}: ${result}\n"
}

for suite in $SUITES_TO_RUN; do
    case "$suite" in
        login)
            if bash "$SCRIPT_DIR/test-login-flow.sh"; then
                SUITE_RESULTS="${SUITE_RESULTS}  login: PASS\n"
            else
                SUITE_RESULTS="${SUITE_RESULTS}  login: FAIL\n"
                TOTAL_FAIL=$((TOTAL_FAIL + 1))
            fi
            ;;
        annotation)
            if bash "$SCRIPT_DIR/test-annotation-editing.sh"; then
                SUITE_RESULTS="${SUITE_RESULTS}  annotation: PASS\n"
            else
                SUITE_RESULTS="${SUITE_RESULTS}  annotation: FAIL\n"
                TOTAL_FAIL=$((TOTAL_FAIL + 1))
            fi
            ;;
        upgrade)
            if bash "$SCRIPT_DIR/test-database-upgrade.sh"; then
                SUITE_RESULTS="${SUITE_RESULTS}  upgrade: PASS\n"
            else
                SUITE_RESULTS="${SUITE_RESULTS}  upgrade: FAIL\n"
                TOTAL_FAIL=$((TOTAL_FAIL + 1))
            fi
            ;;
        postgresql)
            if bash "$SCRIPT_DIR/test-postgresql.sh"; then
                SUITE_RESULTS="${SUITE_RESULTS}  postgresql: PASS\n"
            else
                SUITE_RESULTS="${SUITE_RESULTS}  postgresql: FAIL\n"
                TOTAL_FAIL=$((TOTAL_FAIL + 1))
            fi
            ;;
        *)
            echo "Unknown suite: $suite (available: $SUITES_AVAILABLE)"
            exit 1
            ;;
    esac
done

echo ""
echo "############################################"
echo "# All Suites Complete"
echo "############################################"
echo -e "$SUITE_RESULTS"

if [ "$TOTAL_FAIL" -gt 0 ]; then
    echo "OVERALL: FAIL ($TOTAL_FAIL suite(s) failed)"
    exit 1
else
    echo "OVERALL: PASS"
fi
