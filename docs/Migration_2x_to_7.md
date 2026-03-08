# Migrating from Apollo 2.x to Apollo 7

This guide covers the changes between Apollo 2.x (Grails 2) and Apollo 7 (Grails 7).

## Requirements

| | Apollo 2.x | Apollo 7 |
|---|---|---|
| Java | JDK 8 | JDK 17+ |
| Grails | 2.5.5 | 7.0.7 |
| Gradle | 2.x | 8.x |
| Node.js | 6–13 | 18+ |
| GWT | 2.7 | 2.10 |

## Build commands

The `./apollo` wrapper script preserves the same commands:

| Old command | New command | Notes |
|---|---|---|
| `apollo run-local [port]` | `./apollo run-local [port]` | Same behavior |
| `apollo run-app [port]` | `./apollo run-app [port]` | Same behavior |
| `apollo deploy` | `./apollo deploy` | Builds bootWar instead of Grails WAR |
| `apollo test` | `./apollo test` | Same |
| `apollo compile` | `./apollo compile` | Same |
| `apollo clean` | `./apollo clean` | Same |
| `apollo clean-all` | `./apollo clean-all` | Same |
| `apollo debug` | `./apollo debug` | Uses `--debug-jvm` instead of MAVEN_OPTS |
| `apollo jbrowse` | `./apollo jbrowse` | Same — installs JBrowse + WebApollo plugin |

You can also use Gradle directly:

```
./gradlew bootRun                # Run the application
./gradlew compileGwt             # Compile GWT annotator panel
./gradlew installJBrowseWebOnly  # Install JBrowse 1
./gradlew bootWar                # Build WAR for deployment
./gradlew test                   # Run tests
```

## Configuration

### apollo-config.groovy (still supported)

The `apollo-config.groovy` file works the same way as in Apollo 2. The file is loaded
automatically at startup via `ApolloConfigLoader`.

Search order:
1. `-Dapollo.config.location=/path/to/apollo-config.groovy`
2. `APOLLO_CONFIG_LOCATION` environment variable
3. `./apollo-config.groovy` in the current working directory
4. `apollo-config.groovy` on the classpath

Environment-specific blocks work the same:

```groovy
environments {
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://localhost/apollo"
            driverClassName = "org.postgresql.Driver"
            username = "apollo"
            password = "secret"
        }
    }
}
```

### JBrowse configuration

JBrowse settings in `apollo-config.groovy` are read at build time by the Gradle tasks.
The format is the same as Apollo 2:

```groovy
jbrowse {
    git {
        url = "https://github.com/gmod/jbrowse"
        branch = "master"
    }
    plugins {
        WebApollo {
            included = true
        }
        NeatHTMLFeatures {
            included = true
        }
        NeatCanvasFeatures {
            included = true
        }
        RegexSequenceSearch {
            included = true
        }
        HideTrackLabels {
            included = true
        }
        // External plugins cloned from git:
        MyVariantInfo {
            git = 'https://github.com/GMOD/myvariantviewer'
            branch = 'master'
        }
    }
}
```

Plugin types:
- `included = true` — plugin is bundled with JBrowse (e.g. NeatHTMLFeatures). The build
  verifies it exists but does not install it.
- `git = "url"` — plugin is cloned from a git repository into `jbrowse/plugins/<name>`.

After changing JBrowse plugins, run `./apollo clean-all` and then `./apollo jbrowse` to
rebuild.

### Apollo application settings

All `apollo { ... }` settings from `apollo-config.groovy` are still supported and work
the same way. These include:

- `common_data_directory`
- `sequence_search_tools`
- `data_adapters`
- `splice_donor_sites` / `splice_acceptor_sites`
- `proxies`
- `extraTabs`
- `administrativePanel`
- `authentications`
- `native_track_selector_default_on`
- Feature editor toggles (`feature_has_dbxrefs`, `feature_has_go_ids`, etc.)

Defaults for all of these are in `grails-app/conf/application.yml` under the `apollo:` key.

### Environment variables (new in Apollo 7)

For Docker and containerized deployments, database settings can be configured via
environment variables instead of (or in addition to) `apollo-config.groovy`:

| Variable | Description | Default |
|---|---|---|
| `APOLLO_DB_URL` | JDBC database URL | (none) |
| `APOLLO_DB_DRIVER` | JDBC driver class | `org.postgresql.Driver` |
| `APOLLO_DB_USERNAME` | Database username | (none) |
| `APOLLO_DB_PASSWORD` | Database password | (none) |
| `APOLLO_DB_CREATE` | Hibernate ddl-auto | `update` |
| `APOLLO_MIGRATION_ON_START` | Run Liquibase migrations on boot | `false` |

## Database migration

Apollo 7 includes Liquibase-based database migrations for upgrading from Apollo 2.7.0+.

To upgrade an existing database:

1. Set `APOLLO_MIGRATION_ON_START=true` (or `grails.plugin.databasemigration.updateOnStart: true`)
2. Start the application — migrations run automatically
3. After successful migration, you can set it back to `false`

Only upgrading from Apollo 2.7.0 or later is supported.

## Authentication

Apollo 7 uses **Spring Security** instead of Apache Shiro. The authentication system is
otherwise the same — username/password authentication works as before. The `authentications`
config block in `apollo-config.groovy` is still supported for configuring alternative
authenticators (e.g. `remoteUserAuthenticatorService`).

## Deployment

### Embedded server (recommended)

Apollo 7 uses Spring Boot's embedded Tomcat. No external Tomcat installation is needed:

```
./apollo run-local 8080
```

Or for production:

```
./apollo deploy
java -jar build/libs/apollo-*.war
```

### Docker

```
docker compose up
```

See `docker-compose.yml` for a PostgreSQL + Apollo setup.

### WAR to external Tomcat

The WAR file generated by `./apollo deploy` can still be deployed to an external
Tomcat 10+ (Jakarta EE) server, but the embedded server is recommended for simplicity.

Note: Apollo 7 uses Jakarta Servlet API (not javax.servlet), so it requires Tomcat 10+.

## Not yet supported

The following features from Apollo 2.x are not yet available in Apollo 7:

- **Quartz scheduled tasks** — the Quartz plugin is not yet ported. Scheduled jobs
  (e.g. periodic cleanup) will not run.
- **Chado export** — untested. The domain classes and export code are present but have
  not been verified with Grails 7's GORM.
- **OpenID Connect authentication** — not yet ported to Spring Security.

## Removed features

- **GWT DevMode** — use `./apollo debug` for remote debugging instead.
- **`apollo devmode`** — the separate GWT dev mode command is removed. Use `./apollo run-local`
  which compiles GWT automatically.
- **`apollo watchman`** — the watchman-based file watcher for client development is removed.
- **`apollo create-rest-doc`** — REST documentation generation is removed.
- **`apollo jbrowse-tools`** — use `./apollo deploy` which includes Perl tools, or install
  JBrowse tools manually.
