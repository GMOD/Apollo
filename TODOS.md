# Apollo TODOs (Grails 7 Migration)

## High Priority

- [ ] Verify all controller methods have `return` after error renders — grep for `render.*error` patterns without a following `return` to catch any we missed
- [ ] Convert ChadoHandlerService closures (25+) to for-loops, or gate Chado export entry points with a "not yet supported in v7" error — currently every closure will throw `ClassNotFoundException` if triggered

## Medium Priority

- [ ] MySQL end-to-end testing — driver updated but full test suite not yet run against MySQL
- [ ] Port OpenID Connect auth to Spring Security 6's `oauth2Login()` — blocks upgrade for OIDC sites
- [ ] Run test suite against a real production database dump to validate edge cases
- [ ] Fill in annotation editing test count (shows "—" in UPGRADE_SUMMARY.md)

## Low Priority

- [ ] Chado export end-to-end testing — requires external Chado-schema PostgreSQL DB
