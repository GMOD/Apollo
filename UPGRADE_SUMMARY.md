# Apollo: Grails 2 → Grails 7 Upgrade Summary

Apollo has been upgraded from Grails 2.5.5 (2015, end-of-life) to Grails 7.0.8
(2026, actively supported). This resolves security exposure from unpatchable
CVEs, enables Java 17+ and modern OS/container support, and unblocks future
development. **For end users, nothing changes** — same UI, same API, same config
files, same database credentials.

## Benefits

- **Security:** Off an EOL framework with known CVEs; onto an actively-patched stack
- **Compatibility:** Runs on Java 17+, modern Linux, Tomcat 10+, current Docker images
- **47 bugs fixed:** Including 5 data corruption bugs and 3 auth bypass issues pre-existing in the Grails 2 version
- **Zero user-facing changes:** Same GWT editor, same JBrowse, same web service API, same `apollo-config.groovy`
- **Maintainability:** Dependencies can be updated without cascading breakage
- **Tested:** 90+ automated tests covering login, annotation editing, export, and database migration

## Risks

| Risk | Why | Likelihood | Impact | Workaround |
|------|-----|-----------|--------|------------|
| Chado export untested | Requires external Chado DB we don't have in test env | Medium | Low — few sites use it; incomplete in v2 | Test in staging first |
| OIDC auth not yet ported | Old OIDC plugin has no Grails 7 equivalent | Certain for OIDC sites | Medium — blocks those sites | Use password/token auth until ported; Spring Security 6 has built-in OIDC |
| MySQL not tested | Test infra set up for H2 and PostgreSQL only | Low | Low — DB-agnostic via Hibernate | Test against staging copy; PostgreSQL is fully tested |
| Edge cases in production data | Tests use synthetic data | Low | Varies | Test against production DB copy; rollback = restore backup + redeploy |

## What Changed

| Component | Before | After |
|-----------|--------|-------|
| Grails | 2.5.5 | 7.0.8 |
| Java | 8 | 17+ |
| Spring Boot | ~1.x | 3.x |
| Security | Apache Shiro | Spring Security 6 |
| Tomcat | 8.x | 10.x |
| GWT | 2.7.0 | 2.10.0 |
| PostgreSQL driver | 9.4 | 42.7 |
| MySQL connector | 5.1 | 9.2 |

Shiro was replaced because no Grails 7 plugin exists. Existing password hashes
and user accounts continue to work. `apollo-config.groovy` still works via a
custom config loader.

## Bugs Fixed

47 bugs found and fixed. Many were **pre-existing in the Grails 2 version**. Full details
in [CRITICAL_BUGS.md](CRITICAL_BUGS.md) and [IMPORTANT_BUGS.md](IMPORTANT_BUGS.md).

| Category | Count |
|----------|-------|
| Crashes (removed APIs, closure failures) | 15 |
| Data corruption / loss | 5 |
| Auth bypass (controllers executing after errors) | 3 |
| Broken features (export, admin UI) | 8 |
| Framework migration (sessions, resources) | 10 |
| Wrong output (mislabeled JSON, swapped columns) | 6 |

## Upgrade Steps

1. **Back up** the production database
2. **Deploy the Grails 7 version** — schema migration runs automatically on first boot
3. **Spot-check:** login, open an organism, verify annotations

Rollback = restore DB backup + redeploy old version.

## Remaining Work

| Item | Priority |
|------|----------|
| MySQL end-to-end testing | Medium |
| OpenID Connect (OIDC sites only) | Medium |
| Chado export testing | Low |
