# Apollo 2 → Apollo 7 Database Upgrade Plan

## Overview

This plan addresses enabling smooth database migration from Apollo 2 (Grails 2.5.5)
production databases to Apollo 7 (Grails 7.0.7). The domain models are identical
between versions, so the core schema is compatible. The work focuses on migration
tooling, schema validation, configuration migration, and automated testing.

## Phase 1: Re-enable Database Migration Plugin

### 1.1 Add database-migration plugin to build.gradle

Uncomment and update the database-migration plugin dependency to a Grails 7
compatible version.

**Files:** `build.gradle`

**Tasks:**
- Research the latest `database-migration` plugin version compatible with Grails 7
  (likely 4.2.1+ or a newer release)
- Add `implementation "org.grails.plugins:database-migration:<version>"` to build.gradle
- Add Liquibase core dependency if not pulled transitively
- Verify the app compiles and boots with the plugin enabled
- Run existing unit and integration tests to confirm no regressions

### 1.2 Configure migration settings in application.yml

**Files:** `grails-app/conf/application.yml`

**Tasks:**
- Add `grails.plugin.databasemigration.updateOnStart: true` for development
- Add `grails.plugin.databasemigration.updateOnStartFileName: changelog.groovy`
- Set production `dbCreate` to `validate` (instead of `none`) so Hibernate checks
  schema compatibility on startup and fails fast with a clear error
- Keep development `dbCreate` as `update` for local work

### 1.3 Verify existing changelogs execute against a fresh database

**Tasks:**
- Boot the app with `dbCreate: none` and migration plugin enabled against a fresh
  H2 database to verify all changelogs from 2.0.1 through 2.6.0 execute cleanly
- Fix any Liquibase DSL incompatibilities with the Grails 7 version of the plugin

---

## Phase 2: Create Grails 7 Migration Changelog

### 2.1 Generate schema diff

**Tasks:**
- Create a fresh H2 database using `dbCreate: create` (what GORM 7 generates)
- Create a second H2 database by running all existing Liquibase changelogs
  (what an Apollo 2 production DB looks like)
- Compare the two schemas to identify differences:
  - New columns added by GORM 7 that don't exist in old schema
  - Column type differences (varchar vs text, tinyint vs boolean)
  - Index name differences
  - Missing or renamed join tables
  - The `PartOf.groovy` domain class was removed — may need a `dropTable` if
    the table exists

### 2.2 Write changelog-7_0_0.groovy

**Files:** `grails-app/migrations/changelog-7_0_0.groovy`

**Tasks:**
- Add changesets for each schema difference found in 2.1
- Use `preConditions` on every changeset so migrations are idempotent
  (safe to run on databases at any intermediate state)
- Include changes for:
  - Any column type migrations needed
  - Any new indexes GORM 7 expects
  - Dropping the `part_of` table if it exists (PartOf.groovy was removed)
  - Any default value changes
- Add to the main `changelog.groovy` include list

### 2.3 Test the changelog against both fresh and migrated databases

**Tasks:**
- Run changelog-7_0_0.groovy against a database that has run all 2.x changelogs
- Run the full changelog suite against a completely fresh database
- Boot Apollo 7 with `dbCreate: validate` against both and confirm startup succeeds

---

## Phase 3: Configuration Migration Guide

### 3.1 Write a config conversion utility script

**Files:** `scripts/convert-apollo-config.sh` (new)

**Tasks:**
- Parse common `apollo-config.groovy` settings and output equivalent YAML
- Handle at minimum:
  - `dataSource.url`, `dataSource.username`, `dataSource.password`,
    `dataSource.driverClassName`
  - `apollo.common_data_directory`
  - `apollo.authentications` list
  - `apollo.admin` bootstrap admin config
- Output a valid `application-apollo.yml` or a list of environment variables

### 3.2 Document environment variable mapping

**Files:** `BACKWARDS_INCOMPATIBILITIES.md` (update existing)

**Tasks:**
- Add a section mapping every `apollo-config.groovy` key to its Apollo 7 equivalent
- Document the `APOLLO_DB_URL`, `APOLLO_DB_DRIVER`, `APOLLO_DB_USERNAME`,
  `APOLLO_DB_PASSWORD` environment variables
- Document `SPRING_CONFIG_ADDITIONAL_LOCATION` for YAML config files

---

## Phase 4: Database Upgrade E2E Test

### 4.1 Create an Apollo 2 database fixture

**Files:** `src/test/resources/apollo2-schema.sql` (new)

**Tasks:**
- Export the DDL that `changelog-2_0_0.groovy` through `changelog-2_6_0.groovy`
  would produce (the schema of a fully-migrated Apollo 2.6 database)
- Include sample data:
  - An admin user with a known password hash
  - A non-admin user with organism permissions
  - An organism with sequences
  - Features (gene, mRNA, exons) with feature locations and relationships
  - Feature events (history)
  - Preferences
- This fixture represents a realistic Apollo 2 production database

### 4.2 Write an upgrade integration test

**Files:** `src/integration-test/groovy/org/bbop/apollo/DatabaseUpgradeIntegrationSpec.groovy` (new)

**Tasks:**
- Load the Apollo 2 fixture SQL into the test H2 database
- Run the Grails 7 migration changelog against it
- Boot the GORM session and verify:
  - Users can be loaded and passwords validate
  - Organisms and sequences are intact
  - Features with relationships load correctly
  - Feature events (history) are accessible
  - Permissions are correctly associated
- This test runs as part of `./gradlew integrationTest`

### 4.3 Write an upgrade e2e test script

**Files:** `test-database-upgrade.sh` (new)

**Tasks:**
- Start Apollo 7 pointing at the Apollo 2 fixture database
- Verify via HTTP API:
  - Login with the fixture admin user works
  - `findAllOrganisms` returns the fixture organism
  - `getFeatures` returns the fixture features
  - `addTranscript` works (write path on migrated data)
  - `setName` works
  - `deleteFeature` works
  - Feature history is accessible
- Stop app and report results
- This tests the full stack including the migration plugin running on startup

---

## Phase 5: Production Database Testing

### 5.1 Test against PostgreSQL

**Tasks:**
- Set up a local PostgreSQL instance
- Load the Apollo 2 fixture SQL (PostgreSQL dialect)
- Run Apollo 7 with migration plugin against it
- Verify all e2e tests pass
- Document any PostgreSQL-specific migration issues

### 5.2 Test against MySQL

**Tasks:**
- Set up a local MySQL instance
- Load the Apollo 2 fixture SQL (MySQL dialect)
- Run Apollo 7 with migration plugin against it
- Verify all e2e tests pass
- Document any MySQL-specific migration issues (InnoDB dialect changes, etc.)

### 5.3 Verify JDBC driver compatibility

**Tasks:**
- Confirm `com.mysql:mysql-connector-j:9.2.0` works with MySQL 5.7+ and 8.0+
- Confirm `org.postgresql:postgresql:42.7.4` works with PostgreSQL 10+
- Document minimum supported database server versions

---

## Phase 6: Docker Upgrade Path

### 6.1 Update Dockerfile

**Tasks:**
- Update base image to Java 17+ (e.g., Eclipse Temurin 17)
- Update Tomcat to 10+ for Jakarta Servlet API
- Add environment variable documentation for database configuration
- Include the migration plugin so `updateOnStart` runs automatically

### 6.2 Write docker-compose upgrade test

**Tasks:**
- Create a `docker-compose.test.yml` that:
  - Starts a PostgreSQL container with Apollo 2 fixture data
  - Starts Apollo 7 container pointing at it
  - Runs the e2e test suite against the migrated database
- This simulates the real-world upgrade path for Docker deployments

---

## Implementation Order

| Step | Phase | Status | Notes |
|------|-------|--------|-------|
| 1 | 1.1 - Add migration plugin | DONE | `org.apache.grails:grails-data-hibernate5-dbmigration` |
| 2 | 1.2 - Configure migration settings | DONE | `updateOnStart: false` default, `dbCreate: validate` for prod |
| 3 | 1.3 - Verify existing changelogs | DONE | Changelogs 2_0_1-2_6_0 run against GORM schema, 2_0_0 excluded |
| 4 | 2.1 - Generate schema diff | DONE | 25 tables + 13 columns missing from Liquibase-only schema |
| 5 | 2.2 - Write changelog-7_0_0 | DONE | All changesets use preConditions for idempotency |
| 6 | 2.3 - Test changelog | DONE | Tested: GORM schema + migrations + validate boot |
| 7 | 4.1 - Create Apollo 2 fixture | TODO | Need SQL fixture with sample data |
| 8 | 4.2 - Upgrade integration test | TODO | Spock test loading fixture + verifying GORM |
| 9 | 4.3 - Upgrade e2e test | TODO | Shell script testing full API against migrated DB |
| 10 | 3.1 - Config conversion script | TODO | apollo-config.groovy → YAML converter |
| 11 | 3.2 - Document env vars | TODO | Update BACKWARDS_INCOMPATIBILITIES.md |
| 12 | 5.1 - PostgreSQL testing | TODO | Test against real PostgreSQL |
| 13 | 5.2 - MySQL testing | TODO | Test against real MySQL |
| 14 | 5.3 - Driver compatibility | TODO | Verify JDBC driver versions |
| 15 | 6.1 - Update Dockerfile | TODO | Java 17+, Tomcat 10+ |
| 16 | 6.2 - Docker compose test | TODO | End-to-end Docker upgrade test |

Steps 1-6 are complete. Next priority is steps 7-9 (automated upgrade testing).

## Key Risks

1. **database-migration plugin may not have a Grails 7 compatible release** —
   If so, we need to use raw Liquibase with Spring Boot's built-in support
   (`org.liquibase:liquibase-core` + `spring.liquibase.changelog`) instead of the
   Grails plugin.

2. **GORM 7 may generate subtly different DDL** — Column types, index names,
   or join table structures may differ. The schema diff (step 2.1) will reveal these.

3. **Hibernate proxy issues with existing data** — We fixed some proxy unwrapping
   issues. There may be more that only surface with complex production data
   (deeply nested feature hierarchies, many-to-many relationships).

4. **H2 vs PostgreSQL/MySQL differences** — Migrations tested on H2 may behave
   differently on production databases. Phase 5 addresses this.
