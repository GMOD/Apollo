# Backwards Incompatibilities: Apollo Grails 2.5.5 → Grails 7

This document catalogs all known backwards incompatibilities introduced by the
upgrade from Grails 2.5.5 to Grails 7.0.7.

## Critical: Security Framework

### Shiro → Spring Security (NOT YET MIGRATED)

The Apache Shiro plugin for Grails does not exist for Grails 7. The codebase
currently retains Shiro library dependencies as a transitional measure, but
the full security stack needs migration to Spring Security Core.

**What breaks:**
- `DbRealm.groovy` (Shiro realm) — has no Grails 7 equivalent
- `SecurityFilters.groovy` → converted to `SecurityInterceptor.groovy` but
  still references `SecurityUtils.getSubject()` (Shiro API)
- `PermissionService` uses `SecurityUtils.subject.principal` throughout
- `LoginController` uses `SecurityUtils.getSubject()`, `subject.login()`,
  `subject.logout()`, Shiro `SavedRequest`, etc.
- `AuthenticatingHandshakeHandler` uses Shiro `UsernamePasswordToken`
- All controllers/services that call `permissionService.isAdmin()` or
  `permissionService.authenticateWithToken()` go through Shiro

**Migration path:**
1. Add `spring-security-core` plugin
2. Adapt `User`/`Role` domain classes (currently custom, need Spring Security
   conventions or a custom `UserDetailsService`)
3. Replace `SecurityUtils.subject.principal` with Spring Security's
   `SecurityContextHolder.getContext().authentication.name`
4. Replace `SecurityFilters`/`SecurityInterceptor` with `@Secured` annotations
   or Spring Security filter chain
5. Replace `DbRealm` with a `UserDetailsService` implementation

**User impact:** Any custom authenticator plugins or external auth integrations
will need to be rewritten against Spring Security APIs.

## Critical: External Configuration

### apollo-config.groovy no longer auto-loaded

**Old behavior:** `grails.config.locations` in `Config.groovy` loaded
`apollo-config.groovy` from the classpath or current directory.

**New behavior:** Spring Boot 3 does not support `grails.config.locations`.
Configuration must be provided via:
- `application-{profile}.yml` files
- Environment variables (e.g., `APOLLO_DB_URL`, `APOLLO_DB_DRIVER`)
- `spring.config.additional-location` JVM property
- `SPRING_CONFIG_ADDITIONAL_LOCATION` env var

**User impact:** All existing deployment configurations in `apollo-config.groovy`
must be converted to YAML or properties format. Docker deployments need updated
environment variable mappings.

## Critical: Database

### MySQL connector artifact changed

**Old:** `mysql:mysql-connector-java:5.1.29`
**New:** `com.mysql:mysql-connector-j:9.2.0`

The Maven coordinates changed. The driver class name may also need updating
in production configurations.

### PostgreSQL driver updated

**Old:** `org.postgresql:postgresql:9.4.1212`
**New:** `org.postgresql:postgresql:42.7.4`

Should be backwards compatible, but test against your specific PostgreSQL version.

### H2 database MVCC option removed

**Old:** `jdbc:h2:...;MVCC=TRUE;...`
**New:** `jdbc:h2:...;LOCK_TIMEOUT=10000;...`

H2 v2 removed the `MVCC` parameter. Existing dev databases will need to be
recreated.

### Hibernate flush mode changed

**Old:** `flush.mode = 'manual'` (OSIV mode)
**New:** Default is `COMMIT`

This can cause queries to return stale data if code relied on manual flush
timing. Monitor for data consistency issues.

### Database migration plugin

The `database-migration` plugin needs verification for Grails 7 compatibility.
Existing Liquibase changelogs should be compatible, but the plugin wiring may
need updates.

## High: Import Changes

### org.codehaus.groovy.grails → org.grails

All internal Grails API imports changed package:

| Old | New |
|-----|-----|
| `org.codehaus.groovy.grails.web.json.JSONObject` | `org.grails.web.json.JSONObject` |
| `org.codehaus.groovy.grails.web.json.JSONArray` | `org.grails.web.json.JSONArray` |
| `org.codehaus.groovy.grails.web.json.JSONException` | (use standard exception handling) |
| `org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap` | `org.grails.web.servlet.mvc.GrailsParameterMap` |
| `org.codehaus.groovy.grails.commons.*` | `grails.core.*` or `org.grails.commons.*` |

### javax → jakarta

All Java EE / Servlet API imports changed namespace:

| Old | New |
|-----|-----|
| `javax.servlet.*` | `jakarta.servlet.*` |
| `javax.servlet.http.*` | `jakarta.servlet.http.*` |

### Grails transaction annotations

| Old | New |
|-----|-----|
| `grails.transaction.Transactional` | `grails.gorm.transactions.Transactional` |
| `grails.transaction.NotTransactional` | (removed, just don't annotate) |

## High: Removed Plugins

### jQuery / jQuery UI / Bootstrap plugins

These are no longer Grails plugins. Frontend assets are now managed via:
- WebJars (`org.webjars.npm:bootstrap`, `org.webjars.npm:jquery`)
- Asset pipeline
- Or direct inclusion in `grails-app/assets/`

**User impact:** GSP pages that used `<r:require module="jquery"/>` or similar
resource tags need updating to asset-pipeline tags.

### CORS plugin

**Old:** `org.grails.plugins:cors:1.1.8` with `cors.url.pattern = '*'`
**New:** Built into Spring Boot via `WebMvcConfigurer.addCorsMappings()`

Configured in `Application.groovy`.

### rest-api-doc plugin

This plugin does not exist for Grails 7. Replace with:
- Springdoc OpenAPI (`org.springdoc:springdoc-openapi-starter-webmvc-ui`)
- Or remove API documentation generation

### yammer-metrics plugin

Replace with Spring Boot Actuator (already included) and Micrometer metrics.

### quartz2 plugin

Replace with `org.grails.plugins:quartz` (Grails 7 compatible version).
The `CleanupPreferencesJob` needs migration to the new Quartz plugin format.

### audit-logging plugin

Needs verification for Grails 7 compatibility. The `static auditable = true`
markers on domain classes may need a compatible plugin version.

## Medium: Structural Changes

### Filters → Interceptors

`SecurityFilters.groovy` has been converted to `SecurityInterceptor.groovy`.
The interceptor API is different from the old filter API:

**Old (filters):**
```groovy
def filters = {
    all(controller: '*', action: '*') {
        before = { ... }
    }
}
```

**New (interceptors):**
```groovy
class SecurityInterceptor {
    SecurityInterceptor() {
        matchAll()
    }
    boolean before() { ... }
}
```

### BootStrap location

**Old:** `grails-app/conf/BootStrap.groovy`
**New:** `grails-app/init/org/bbop/apollo/BootStrap.groovy`

### UrlMappings location

**Old:** `grails-app/conf/UrlMappings.groovy`
**New:** `grails-app/controllers/org/bbop/apollo/UrlMappings.groovy`

### Test location

**Old:** `test/integration/` and `test/unit/`
**New:** `src/integration-test/groovy/` and `src/test/groovy/`

### Source code location

**Old:** `src/groovy/` and `src/java/`
**New:** `src/main/groovy/` and `src/main/java/`

### GWT shared code

The GWT shared enums (`FeatureStringEnum`, `GlobalPermissionEnum`,
`PermissionEnum`, etc.) have been moved from `src/gwt/` to
`src/main/java/org/bbop/apollo/gwt/shared/`. No code changes needed
but build scripts referencing the old path need updating.

## Medium: WebSocket Changes

### AbstractWebSocketMessageBrokerConfigurer removed

**Old:** `extends AbstractWebSocketMessageBrokerConfigurer`
**New:** `implements WebSocketMessageBrokerConfigurer`

Spring 6 removed the abstract class. The interface is used directly.

### setAllowedOrigins("*") with credentials

**Old:** `setAllowedOrigins("*")`
**New:** `setAllowedOriginPatterns("*")`

Spring 6 no longer allows `*` as an allowed origin when credentials are
enabled. Use `setAllowedOriginPatterns("*")` instead.

## Medium: Config Access

### grailsApplication.config access

**Old:** `grailsApplication.config?.apollo?.admin`
**New:** `grailsApplication.config.getProperty('apollo.admin', Map)`

The old `ConfigObject` dot-notation access no longer works. Use
`getProperty()` or `@Value` annotations.

## Medium: Dependency Version Bumps

### htsjdk 2.x → 4.x

HTSJDK 4.x removed deprecated APIs and changed some class hierarchies.
Code that reads/writes BAM, VCF, or other genomic formats may need updates.
Key areas to check:
- `VcfHandlerService`
- `Gff3HandlerService`
- `SequenceService`

### Batik 1.7/1.9 → 1.18

SVG generation APIs may have minor changes. Check `SvgService`.

### Log4j → Logback

**Old:** Log4j 2.19 with log4j-1.2-api bridge, configured in `Config.groovy`
**New:** Logback, configured in `logback-spring.xml`

All logging configuration must be migrated.

## Low: Groovy 2 → Groovy 4

### Enum constructors must be private

Groovy 4 enforces that enum constructors are `private`. Public enum constructors
that worked in Groovy 2 will fail to compile.

**Files affected:** `CvTermStringEnum`, `OperationEnum`, `Strand`

### @Validateable is now a trait, not an annotation

**Old:** `@Validateable class Foo { ... }`
**New:** `class Foo implements Validateable { ... }`

### Default parameter values are checked more strictly

Default parameter expressions that reference injected services fail static type
checking in Groovy 4. These need to be moved into the method body:

**Old:** `def foo(boolean useCDS = configWrapperService.useCDS())`
**New:** `def foo(Boolean useCDS = null) { if (useCDS == null) { useCDS = configWrapperService.useCDS() } ... }`

### Controller `def grailsApplication` / `def servletContext` conflicts

Grails 7 controllers automatically inherit `grailsApplication` via
`WebAttributes` trait and `servletContext` via `ServletAttributes` trait.
Explicit `def grailsApplication` or `def servletContext` declarations must
be removed or they'll conflict with the trait methods.

### Closure coercion changes

Some implicit closure-to-SAM-type coercions may behave differently in Groovy 4.
Watch for compilation errors in:
- Domain class constraint closures
- Service method callbacks
- GORM criteria builders

### GString behavior

Minor GString interpolation edge cases changed. Should not affect most code.

## Low: Java 8 → Java 17+

### Removed Java APIs

Some Java APIs removed between 8 and 17:
- `java.security.acl` package removed
- Some `javax.xml` classes moved to modules
- `sun.*` internal APIs no longer accessible

### Module system

Java 17 enforces the module system more strictly. Reflection-heavy code
(which GORM uses) should work via Grails' built-in module opens, but
custom reflection may need `--add-opens` JVM flags.

## Deployment Changes

### Tomcat version

**Old:** Embedded Tomcat 8.0.33 / External Tomcat 9
**New:** Embedded Tomcat 10+ (Jakarta Servlet API)

Docker images need updating from Tomcat 9 to Tomcat 10+ for Jakarta
compatibility. The `catalina.home` / `catalina.base` paths may change.

### WAR deployment

WAR deployment still works but the target container must support
Jakarta Servlet API (Tomcat 10+, Jetty 12+, WildFly 27+).

### Docker

The Dockerfile needs updating:
- Base image: Ubuntu 22.04+ with Java 17+
- Tomcat: Version 10+ (for Jakarta EE)
- Config: Environment variables instead of `apollo-config.groovy`
