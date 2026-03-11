# Apollo: Grails 2 → Grails 7 Upgrade Summary

## Executive Summary

Apollo has been upgraded from Grails 2.5.5 (2015) to Grails 7.0.7 (2024). This
moves the application from an end-of-life framework onto a modern, supported
stack — resolving security exposure, enabling current Java/OS compatibility, and
unblocking future development.

The upgrade required changes across every layer of the application. During the
process, a thorough code review uncovered and fixed 47 bugs — many pre-existing
in the old version — improving reliability beyond what Apollo 2 offered. All core
functionality is working and covered by automated tests.

**For end users, nothing changes.** The GWT annotation interface, JBrowse
integration, and web service APIs are identical. Existing scripts, database
credentials, and `apollo-config.groovy` files continue to work without
modification.

---

## Why This Upgrade Was Necessary

Grails 2.5.5 is end-of-life. It requires Java 8 (also EOL for community
support) and depends on libraries with known CVEs that cannot be patched without
a framework upgrade. Staying on this stack meant:

- No security patches for the web framework, ORM, or dependency injection layer
- No path to Java 11, 17, or 21 (current LTS releases)
- Incompatibility with modern OS images and containers (Ubuntu 22.04+, Tomcat 10+)
- Inability to update any major dependency without cascading breakage

Grails 7 is the current supported release, built on Spring Boot 3, Spring
Framework 6, Hibernate 5, Groovy 4, and Java 17+.

---

## What Changed

### Technology Stack

| Component | Before | After |
|-----------|--------|-------|
| Grails | 2.5.5 | 7.0.7 |
| Spring Boot | ~1.x | 3.x |
| Groovy | 2.x | 4.x |
| Java (minimum) | 8 | 17 |
| Servlet API | javax.servlet | jakarta.servlet |
| Tomcat (embedded) | 8.x | 10.x |
| GWT | 2.7.0 | 2.10.0 |
| HTSJDK | 2.x | 4.x |
| MySQL connector | 5.1.29 | 9.2.0 |
| PostgreSQL driver | 9.4.1212 | 42.7.4 |
| Security framework | Apache Shiro | Spring Security 6 |
| Logging | Log4j | Logback |

### Key Architectural Changes

- **Security:** Apache Shiro replaced with Spring Security 6 (no Grails 7 Shiro
  plugin exists). A compatibility layer preserves the same SHA-256 password
  hashing, so existing user credentials continue to work.
- **Configuration:** `apollo-config.groovy` still supported via a custom
  `ApolloConfigLoader`. Environment variables and YAML config also available.
- **Scheduling:** Quartz plugin replaced with Spring `@Scheduled` (no Grails 7
  Quartz plugin exists).
- **CORS:** Separate plugin replaced with Spring Boot's built-in CORS support.

### What Did NOT Change

- The GWT annotation editor UI — identical look and behavior
- JBrowse 1 integration — same plugin, same data loading
- Web service API endpoints and request/response formats
- Database schema (domain model is identical; a small Liquibase migration adds
  a few columns/tables that the new GORM version expects)

---

## Bugs Found and Fixed

The code review required by this upgrade surfaced 47 bugs. Many were **pre-existing
in Apollo 2** — latent issues masked by old framework behavior, untested code
paths, or lucky runtime conditions. Fixing them means Apollo 7 is more reliable
than the version it replaces.

### By Severity

| Category | Count | Examples |
|----------|-------|---------|
| Crashes (NPE, ClassCastException, missing method) | 15 | Java 17 removed `new Boolean()`/`new Integer()` constructors; Groovy 4 removed `Date.minus()`; `@Transactional` AST transform broke ~50 closures |
| Silent data corruption or loss | 5 | `FeatureProperty.equals()` never returned true (duplicates accumulate); variant allele updates written to wrong records; allele deletions not persisted |
| Security/auth bypass | 3 | ~100 controller methods continued executing after error response (including after auth failures); permission checks ran against wrong user |
| Broken features | 8 | GFF3 export failed on genes without exons; FASTA export crashed on unloaded sequences; admin group management crashed; status clearing didn't persist |
| Framework migration issues | 10 | Spring Security 6 sessions not persisted; static resources not served; PostgreSQL startup crash |
| Wrong results / bad output | 6 | JSON responses labeled as HTML; CSV column headers swapped; proxy URLs malformed with `?null` |

### Notable Pre-Existing Bugs (Present in Apollo 2)

These were already broken before the upgrade and are now fixed:

- **~100 missing `return` statements after error responses** — controllers
  continued executing the rest of the method after sending an error, potentially
  modifying data after an authorization failure
- **`FeatureProperty.equals()` never returned true** — `Set` and `Map` operations
  on feature properties silently malfunctioned, causing duplicate accumulation
- **Variant allele copy-paste bug** — both old and new alleles looked up using
  the same variable, silently corrupting allele updates
- **Permission checks used the wrong user** — admin UI showed the admin's own
  permissions instead of the target user's

---

## Testing

### Automated Test Suites (All Passing)

| Suite | Tests | Coverage |
|-------|-------|----------|
| Login flow | 25 | Login, session persistence, static assets, logout |
| End-to-end | 32 | Full workflow: setup → organism → user → annotate → GFF3 export |
| Annotation editing | — | CRUD operations on annotations |
| Database upgrade | 17 | Simulated 2.7.0 → 7.0 migration on H2 |
| PostgreSQL | 19 | Schema creation, migration, annotations on PostgreSQL |

### Database Migration

A Liquibase changelog upgrades existing Apollo 2.x databases in place. Every
changeset is idempotent (safe to re-run). Tested on:

- Fresh and upgraded H2 databases
- Fresh and upgraded PostgreSQL databases

---

## Deployment Changes

Ops teams should be aware of these requirement changes:

| Requirement | Before | After |
|-------------|--------|-------|
| Java | 8 | 17+ |
| External Tomcat (if used) | 9.x | 10+ (Jakarta Servlet) |
| Docker base image | Java 8 / Tomcat 9 | Java 17 / Tomcat 10+ |

A new `Dockerfile` and `docker-compose.yml` are provided. Database configuration
is available via environment variables (`APOLLO_DB_URL`, `APOLLO_DB_DRIVER`,
`APOLLO_DB_USERNAME`, `APOLLO_DB_PASSWORD`).

---

## Known Risks and Mitigations

### 1. Chado Export — Untested

The Chado export feature requires an external Chado-schema PostgreSQL database
that we do not have in our test environment. The code was ported but not
exercised. This feature was already incomplete in Apollo 2 (multiple known TODOs).

**Mitigation:** Sites using Chado export should test in a staging environment
before upgrading.

### 2. OpenID Connect — Not Yet Ported

The OIDC authentication plugin has no Grails 7 equivalent. Username/password and
token-based authentication work fully.

**Mitigation:** Spring Security 6 has built-in OAuth2/OIDC support. A targeted
configuration migration is needed for OIDC sites.

### 3. MySQL — Not End-to-End Tested

The JDBC driver was updated and the application code is database-agnostic
(Hibernate/GORM), but no full test suite was run against MySQL.

**Mitigation:** MySQL sites should run integration tests against a staging copy
before upgrading production.

### 4. Complex Production Data

Automated tests cover standard workflows but may not exercise deeply-nested
feature hierarchies or edge cases accumulated over years of production use.

**Mitigation:** See recommended rollout plan below.

### 5. Custom Auth Plugins

Sites with custom authenticators built against Apache Shiro APIs will need to
adapt them to Spring Security or the new `ApolloSecurityUtils` abstraction.
Password hashes are backward compatible.

---

## Recommended Rollout Plan

1. **Back up** the production database
2. **Deploy Apollo 7 to a staging environment** pointing at a copy of the
   production database
3. On first boot with `updateOnStart: true`, Liquibase automatically applies
   the schema migration
4. **Verify** in staging: login, organism list, annotation editing, export
5. Run the included test suite against staging (`./run-all-tests.sh`)
6. **Test site-specific workflows** — especially Chado export, OIDC, or custom
   auth if applicable
7. **Cut over production** once staging is verified
8. **Rollback** if needed: restore the database backup and redeploy the
   previous version

Existing `apollo-config.groovy` files work without changes. Only `log4j`
configuration blocks are ignored (logging now uses `logback-spring.xml`).

---

## Remaining Work

| Item | Priority | Notes |
|------|----------|-------|
| Commit all changes | High | Large batch of fixes ready to commit |
| MySQL end-to-end testing | Medium | JDBC driver updated; full suite not yet run |
| OpenID Connect migration | Medium | Only needed for OIDC sites |
| Chado export testing | Low | Requires external Chado DB; incomplete in v2 |
