# Apollo Grails Upgrade Plan: 2.5.5 → 7.x

## Current State

- **Grails:** 2.5.5
- **Java:** 1.8
- **Gradle:** 2.11
- **GORM/Hibernate:** 4.3.8.1
- **Groovy:** 2.x (implicit via Grails 2.5.5)
- **Security:** Apache Shiro 1.2.1 plugin + custom DbRealm
- **Codebase size:** 28 controllers, 51 services, 220 domain classes, 44+ test specs
- **Deployment:** WAR on Tomcat 8/9, Docker
- **Database migrations:** Liquibase (database-migration plugin 1.4.1)

## Strategy: Incremental Version Upgrades

A direct Grails 2→7 migration is not feasible. The recommended path is:

```
Grails 2.5.5 → 3.3.x → 5.x → 7.x
```

Grails 4 can be skipped (3→5 is documented), and Grails 6 can be skipped (5→7 is
documented). Each hop involves significant structural changes.

---

## Phase 0: Preparation (Before Any Upgrade)

### 0.1 — Inventory and Risk Assessment
- [ ] Catalog all 16 plugins and check availability for Grails 3+
- [ ] Identify plugins with no Grails 3+ equivalent (these need replacement or removal)
- [ ] Map all `javax.*` imports across the codebase
- [ ] Document all external configuration files (`apollo-config.groovy`, Docker configs)
- [ ] Ensure all tests pass on the current version
- [ ] Set up CI to run the full test suite as a regression baseline

### 0.2 — Plugin Replacement Planning

| Current Plugin | Grails 3+ Status | Action |
|---|---|---|
| shiro 1.2.1 | Archived/unmaintained | Replace with Spring Security Core |
| hibernate4 4.3.8.1 | Replaced by hibernate5 | Upgrade to GORM hibernate5 |
| rest-api-doc 0.6 | Unknown/likely dead | Remove or replace |
| scaffolding 2.1.2 | Available for Grails 3+ | Upgrade version |
| cache / cache-ehcache | Available for Grails 3+ | Upgrade version |
| asset-pipeline 2.1.5 | Available for Grails 3+ | Upgrade version |
| spring-websocket 1.3.1 | Available for Grails 3+ | Upgrade version |
| audit-logging 1.0.3 | Available for Grails 3+ | Upgrade version |
| yammer-metrics 3.0.1-2 | Likely dead | Replace with Micrometer or remove |
| quartz2 2.1.6.2 | quartz plugin available for Grails 3+ | Replace with grails-quartz |
| database-migration 1.4.1 | Available for Grails 3+ | Upgrade version |
| jquery / jquery-ui | Not a plugin in Grails 3+ | Handle via asset-pipeline or npm |
| cors 1.1.8 | Built into Spring Boot | Remove plugin, use Spring Boot CORS |
| twitter-bootstrap 3.3.5 | Not a plugin in Grails 3+ | Handle via asset-pipeline or npm |
| rest-client-builder 2.1.1 | Available but may need replacement | Evaluate |
| code-coverage / coveralls | Different approach in Grails 3+ | Use JaCoCo |

### 0.3 — Version Control
- [ ] Create a long-lived `grails-upgrade` branch
- [ ] Tag current working state as `pre-upgrade-grails-2.5.5`

---

## Phase 1: Grails 2.5.5 → 3.3.x

This is the **largest and most disruptive** hop. Grails 3 fundamentally changed the
project structure, build system, and configuration format.

### 1.1 — Generate a Fresh Grails 3.3.x App
- [ ] Install Grails 3.3.x SDK
- [ ] Run `grails create-app apollo` to get the reference project structure
- [ ] Use this as the target layout to map files into

### 1.2 — Project Structure Migration

| Grails 2 Location | Grails 3 Location |
|---|---|
| `grails-app/conf/BuildConfig.groovy` | `build.gradle` |
| `grails-app/conf/Config.groovy` | `grails-app/conf/application.yml` + `application.groovy` |
| `grails-app/conf/DataSource.groovy` | `grails-app/conf/application.yml` |
| `grails-app/conf/UrlMappings.groovy` | `grails-app/controllers/UrlMappings.groovy` |
| `grails-app/conf/BootStrap.groovy` | `grails-app/init/BootStrap.groovy` |
| `grails-app/conf/SecurityFilters.groovy` | Interceptors (`grails-app/controllers/*Interceptor.groovy`) |
| `test/unit/` | `src/test/groovy/` |
| `test/integration/` | `src/integration-test/groovy/` |
| `src/groovy/` | `src/main/groovy/` |
| `src/java/` | `src/main/java/` |
| `grails-app/conf/spring/resources.groovy` | `grails-app/conf/spring/resources.groovy` (same) |
| `web-app/` | `src/main/webapp/` or asset-pipeline |
| `scripts/` | Gradle tasks |

### 1.3 — Build System Migration
- [ ] Migrate `BuildConfig.groovy` dependencies → `build.gradle`
- [ ] Migrate all repository declarations
- [ ] Update Gradle wrapper to version compatible with Grails 3.3.x (~Gradle 3.5)
- [ ] Convert Ant-based GWT build tasks to Gradle tasks
- [ ] Migrate JBrowse build integration to new Gradle structure

### 1.4 — Configuration Migration
- [ ] Convert `Config.groovy` → `application.yml` / `application.groovy`
- [ ] Convert `DataSource.groovy` → `application.yml` datasource section
- [ ] Convert Log4j config → Logback (`logback.groovy`)
- [ ] Migrate external config loading (`apollo-config.groovy`) to Spring Boot externalized config
- [ ] Update all Docker config files

### 1.5 — Security Migration (Shiro → Spring Security Core)
This is the **highest-risk change** in the entire upgrade.

- [ ] Add `spring-security-core` plugin dependency
- [ ] Create `User`, `Role`, `UserRole` domain classes (or adapt existing ones)
- [ ] Migrate `DbRealm` authentication logic to Spring Security `UserDetailsService`
- [ ] Convert `SecurityFilters.groovy` to Spring Security `@Secured` annotations or interceptors
- [ ] Migrate `PermissionTagLib` to use Spring Security tags
- [ ] Migrate all permission checks in services (50+ services use permission logic)
- [ ] Update API authentication (token-based access)
- [ ] Write comprehensive security integration tests before and after

### 1.6 — GORM / Hibernate Migration
- [ ] Upgrade from Hibernate 4 → Hibernate 5 (via GORM hibernate5 plugin)
- [ ] Review all 220 domain classes for GORM 5 compatibility
- [ ] Update HQL queries for Hibernate 5 syntax changes
- [ ] Test flush mode change (AUTO → COMMIT is now default)
- [ ] Verify database migration changelogs still work
- [ ] Test with H2, MySQL, and PostgreSQL

### 1.7 — Filters → Interceptors
- [ ] Convert `SecurityFilters` to Grails interceptors
- [ ] Ensure URL pattern matching is preserved

### 1.8 — Plugin Updates
- [ ] Replace jQuery/jQuery-UI/Bootstrap plugins with asset-pipeline or npm
- [ ] Remove `cors` plugin (use Spring Boot CORS)
- [ ] Replace `yammer-metrics` with Micrometer or remove
- [ ] Replace `quartz2` with `grails-quartz` for Grails 3
- [ ] Update `asset-pipeline`, `cache`, `database-migration` to Grails 3 versions
- [ ] Replace `rest-api-doc` with Swagger/OpenAPI or remove

### 1.9 — Testing Migration
- [ ] Move test specs to `src/test/groovy/` and `src/integration-test/groovy/`
- [ ] Update test superclasses/annotations to Grails 3 testing framework
- [ ] Replace `@TestFor` with Grails 3 testing traits
- [ ] Ensure all 44+ integration specs pass

### 1.10 — Milestone Validation
- [ ] Application starts without errors
- [ ] All controllers respond correctly
- [ ] Authentication and authorization work
- [ ] JBrowse integration works
- [ ] Genome annotation CRUD operations work
- [ ] WebSocket functionality works
- [ ] Database migrations run successfully
- [ ] Docker build works
- [ ] All tests pass

---

## Phase 2: Grails 3.3.x → 5.x

This hop skips Grails 4 and moves to Grails 5, which is built on Spring Boot 2.5+
and uses Groovy 3.

### 2.1 — Generate Reference Grails 5 App
- [ ] Generate stock Grails 5 app from `start.grails.org` for comparison

### 2.2 — Build Updates
- [ ] Upgrade Gradle to 7.x (required for Grails 5)
- [ ] Update `build.gradle` plugin versions and dependency declarations
- [ ] Update GORM plugin versions for Grails 5

### 2.3 — Groovy 2 → Groovy 3 Migration
- [ ] Fix Groovy 3 breaking changes (closure coercion, GString behavior, etc.)
- [ ] Review domain classes, services, controllers for Groovy 3 incompatibilities
- [ ] Update any metaclass usage or Groovy-specific patterns

### 2.4 — Spring Boot 2.x Updates
- [ ] Update Spring Boot configuration properties
- [ ] Review auto-configuration changes
- [ ] Update Spring Security configuration if needed

### 2.5 — Testing Framework Updates
- [ ] Update to Grails 5 testing framework
- [ ] Replace deprecated testing annotations/traits

### 2.6 — Milestone Validation
- [ ] Same validation checklist as Phase 1.10

---

## Phase 3: Grails 5.x → 7.x

This is the **second most disruptive** hop due to the javax → jakarta namespace
migration and the move to Spring Boot 3 / Spring Framework 6.

### 3.1 — Generate Reference Grails 7 App
- [ ] Generate stock Grails 7 app from `start.grails.org` for comparison

### 3.2 — Java Version Upgrade
- [ ] Upgrade from Java 8 → Java 17 (minimum for Grails 7)
- [ ] Update Docker base images to Java 17+
- [ ] Fix any Java 17 incompatibilities (removed APIs, module system)

### 3.3 — javax → jakarta Namespace Migration
This affects every servlet-related import, filter, and annotation.

- [ ] Replace all `javax.servlet.*` → `jakarta.servlet.*`
- [ ] Replace all `javax.persistence.*` → `jakarta.persistence.*`
- [ ] Replace all `javax.validation.*` → `jakarta.validation.*`
- [ ] Replace all `javax.inject.*` → `jakarta.inject.*`
- [ ] Verify all third-party libraries are jakarta-compatible
- [ ] Update HTSJDK and other Java dependencies for Jakarta compatibility

### 3.4 — Groovy 3 → Groovy 4 Migration
- [ ] Fix Groovy 4 breaking changes
- [ ] Update any Groovy DSL usage

### 3.5 — Spring Boot 3 / Spring Framework 6
- [ ] Update Spring Boot configuration
- [ ] Update Spring Security to Spring Security 6
- [ ] Review all `@Secured` annotations and security config
- [ ] Update WebSocket configuration for Spring 6

### 3.6 — GORM Updates for Grails 7
- [ ] Update GORM hibernate5 plugin to jakarta-compatible version
- [ ] Review domain class mappings
- [ ] Test all database operations

### 3.7 — Build Updates
- [ ] Upgrade Gradle to 8.x
- [ ] Update all plugin versions to Grails 7 compatible
- [ ] Update `database-migration` plugin to latest
- [ ] Update `quartz` plugin to latest
- [ ] Update `spring-security-core` to latest Jakarta-compatible version

### 3.8 — UI Updates
- [ ] Bootstrap 3 → Bootstrap 5.3.3 (Grails 7 default)
- [ ] Update GSP templates for Bootstrap 5 classes
- [ ] Update scaffolding templates

### 3.9 — Deployment Updates
- [ ] Update Docker images (Java 17+, Tomcat 10+ for Jakarta)
- [ ] Update deployment scripts
- [ ] Update external configuration loading for Spring Boot 3

### 3.10 — Final Validation
- [ ] Full regression test suite passes
- [ ] Performance benchmarking vs old version
- [ ] Security audit of new Spring Security setup
- [ ] Docker build and deployment works
- [ ] All genome annotation workflows verified
- [ ] JBrowse integration verified
- [ ] WebSocket functionality verified
- [ ] Multi-database (H2, MySQL, PostgreSQL) testing
- [ ] Load testing with production-scale data

---

## Risk Assessment

| Risk | Severity | Mitigation |
|---|---|---|
| Shiro → Spring Security migration breaks auth | **Critical** | Extensive security test coverage before starting; parallel auth during transition |
| 220 domain classes have GORM incompatibilities | **High** | Automated GORM compatibility scanning; incremental domain class testing |
| Plugin unavailability in newer Grails | **High** | Early plugin audit; replacement planning in Phase 0 |
| javax → jakarta breaks third-party libs (HTSJDK, etc.) | **High** | Check all dependency versions for Jakarta support early |
| JBrowse build integration breaks | **Medium** | JBrowse build is mostly Gradle/npm, relatively independent |
| Database migration compatibility | **Medium** | Test migrations against all three DB engines at each phase |
| External apollo-config.groovy loading changes | **Medium** | Document all config changes for deployers |
| Groovy language changes break domain/service logic | **Medium** | Comprehensive test suite; Groovy migration guides |

## Estimated Effort

| Phase | Relative Effort |
|---|---|
| Phase 0: Preparation | 10% |
| Phase 1: Grails 2 → 3 | 50% |
| Phase 2: Grails 3 → 5 | 15% |
| Phase 3: Grails 5 → 7 | 25% |

Phase 1 dominates because it involves fundamental restructuring of the project
layout, build system, configuration format, and security framework.

## Alternative: Fresh Rewrite

Given the scale of changes (220 domain classes, security framework replacement,
3 major structural overhauls), an alternative worth considering:

1. Generate a fresh Grails 7 app
2. Port domain classes first (they carry the data model)
3. Port services (business logic, largely framework-independent)
4. Port controllers (adapt to new conventions)
5. Implement security fresh with Spring Security Core
6. Port/rebuild views

This may actually be **faster** than incremental upgrades for a codebase this size,
since many of the intermediate compatibility issues are bypassed entirely. The
tradeoff is higher risk of introducing subtle behavioral changes.

## Sources

- [Grails Official Upgrade Guide](https://grails.apache.org/docs/latest/guide/upgrading.html)
- [Upgrading Grails Applications (multi-version)](https://grails.github.io/grails-upgrade/latest/guide/index.html)
- [Grails 2→3 Migration Guide](https://grails.apache.org/docs/3.0.x/guide/upgrading.html)
- [Grails 7 UPGRADE.md](https://github.com/apache/grails-core/blob/7.0.x/UPGRADE7.md)
- [Apache Grails 7.0.0 Release](https://grails.apache.org/blog/2025-10-18-introducing-grails-7.html)
- [HeroDevs: Before You Migrate to Grails 7](https://www.herodevs.com/blog-posts/before-you-migrate-to-grails-7-you-need-to-answer-these-questions)
- [Grails 7 InfoQ Coverage](https://www.infoq.com/news/2025/11/grails-7-released/)
- [Opensource.com: Upgrading Grails 2→3](https://opensource.com/article/18/5/upgrading-grails-applications)
