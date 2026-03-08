# Apollo
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.3555454.svg)](https://doi.org/10.5281/zenodo.3555454)
![Lint](https://github.com/GMOD/Apollo/workflows/Lint/badge.svg)
![Java CI with Gradle](https://github.com/GMOD/Apollo/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![Documentation](https://readthedocs.org/projects/genomearchitect/badge/?version=latest)](https://genomearchitect.readthedocs.org/en/latest/)
[![Chat at Gitter](https://badges.gitter.im/GMOD/Apollo.svg)](https://gitter.im/GMOD/Apollo?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![License](https://img.shields.io/badge/License-BSD%202--Clause-orange.svg)](https://opensource.org/licenses/BSD-2-Clause)

A collaborative, real-time, genome annotation editor. The stack is a Java web application / database backend and a
Javascript client that runs in a web browser as a JBrowse plugin.

Cite Apollo using `Dunn NA, Unni DR, Diesh C, Munoz-Torres M, Harris NL, Yao E, et al. (2019) Apollo: Democratizing genome annotation. PLoS Comput Biol 15(2): e1006790.` <https://doi.org/10.1371/journal.pcbi.1006790>


Questions / Comments / Community contact can be sent to our [Apollo user mailing list](mailto:apollo@lbl.gov) or posted to our [google group](https://groups.google.com/a/lbl.gov/forum/#!forum/apollo).

Complete Apollo installation and configuration instructions are available from the [Apollo documentation pages](http://genomearchitect.readthedocs.io/en/latest/).

The [User's Guide](docs/UsersGuide.md) provides guidance on how to use it.

## Requirements

- Java 17+
- Node.js and yarn (for JBrowse 1 build)
- Git

## Quick start

Launch Apollo locally with an H2 (zero-configuration) database:

    ./apollo run-local 8080

Or with Docker:

    docker compose up

## Setup guide

See the [Setup guide](docs/Setup.md) for deploying and [configuring](docs/Configure.md) a production instance.

Apollo may be launched from [Docker](docs/Docker.md) as well.

## Available commands

| Command | Description |
|---------|-------------|
| `./apollo run-local [port]` | Install JBrowse, compile GWT UI, and run the app |
| `./apollo run-app [port]` | Alias for `run-local` |
| `./apollo debug [port]` | Run with remote debugging on port 5005 |
| `./apollo test` | Run the test suite |
| `./apollo deploy` | Build a WAR file with JBrowse for deployment |
| `./apollo jbrowse` | Install/update JBrowse and the WebApollo plugin |
| `./apollo compile` | Compile the application and GWT UI |
| `./apollo clean` | Remove build artifacts |
| `./apollo clean-all` | Remove build artifacts, JBrowse, and database files |

You can also use Gradle directly:

    ./gradlew bootRun              # Run the application
    ./gradlew compileGwt           # Compile GWT annotator panel
    ./gradlew installJBrowseWebOnly # Install JBrowse 1
    ./gradlew bootWar              # Build WAR for deployment
    ./gradlew test                 # Run tests

## Generate a WAR file

For production deployments:

    ./apollo deploy

Note: create an `apollo-config.groovy` file for your preferred database settings (PostgreSQL recommended for production).

## Docker

    docker compose up

See [docker-compose.yml](docker-compose.yml) for configuration options. Environment variables:

- `APOLLO_DB_URL` - JDBC database URL
- `APOLLO_DB_DRIVER` - Database driver class
- `APOLLO_DB_USERNAME` / `APOLLO_DB_PASSWORD` - Database credentials

## Web Services

[Python library over web services](https://pypi.org/project/apollo/) and other [web services examples](docs/web_services/examples).

## Migrating data from older versions

Follow the steps in our [migration guide](docs/Migration.md) to move annotations and data from older versions.

## Changes from Apollo 2.x

- **Grails 7 / Spring Boot** — replaces Grails 2. Uses `./gradlew` instead of `./grailsw`.
- **Java 17+** required (was Java 8).
- **Spring Security** replaces Apache Shiro for authentication.
- **GWT 2.10** — upgraded from GWT 2.7 for Java 17 compatibility.
- **No embedded Tomcat WAR deployment** — uses Spring Boot's embedded server. WAR files can still be generated with `./apollo deploy`.
- **No Quartz scheduler** — scheduled tasks are not yet ported.
- **Chado export** — untested in this version.

