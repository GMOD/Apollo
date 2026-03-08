package org.bbop.apollo

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.boot.logging.DeferredLogFactory
import org.springframework.boot.logging.DeferredLog
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources

/**
 * Loads apollo-config.groovy using Groovy's ConfigSlurper, providing backward
 * compatibility with Apollo 2.x external configuration.
 *
 * Search order:
 *   1. System property: -Dapollo.config.location=/path/to/apollo-config.groovy
 *   2. Environment variable: APOLLO_CONFIG_LOCATION=/path/to/apollo-config.groovy
 *   3. ./apollo-config.groovy (current working directory)
 *   4. Classpath: apollo-config.groovy
 *
 * The Groovy config is parsed with the active Spring profile (e.g. "production")
 * so environment-specific blocks like environments { production { ... } } work
 * exactly as they did in Grails 2.
 */
class ApolloConfigLoader implements EnvironmentPostProcessor {

    private final DeferredLog log = new DeferredLog()

    @Override
    void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        File configFile = findConfigFile(environment)
        if (!configFile) {
            URL classpathConfig = getClass().getClassLoader().getResource("apollo-config.groovy")
            if (classpathConfig) {
                log.info "Loading apollo-config.groovy from classpath: ${classpathConfig}"
                loadConfig(classpathConfig, environment)
            }
            application.addInitializers({ ctx -> log.replayTo(ApolloConfigLoader) })
            return
        }

        log.info "Loading apollo-config.groovy from: ${configFile.absolutePath}"
        loadConfig(configFile.toURI().toURL(), environment)
        application.addInitializers({ ctx -> log.replayTo(ApolloConfigLoader) })
    }

    private File findConfigFile(ConfigurableEnvironment environment) {
        String explicitPath = System.getProperty("apollo.config.location")
                ?: environment.getProperty("APOLLO_CONFIG_LOCATION")

        if (explicitPath) {
            File f = new File(explicitPath)
            if (f.exists()) {
                return f
            }
            log.warn "apollo-config.groovy not found at specified location: ${explicitPath}"
            return null
        }

        File cwd = new File("apollo-config.groovy")
        if (cwd.exists()) {
            return cwd
        }

        return null
    }

    private void loadConfig(URL configUrl, ConfigurableEnvironment environment) {
        try {
            ConfigSlurper slurper = new ConfigSlurper(getActiveProfile(environment))
            ConfigObject configObject = slurper.parse(configUrl)
            Map<String, Object> flatMap = flattenConfig(configObject, "")

            if (flatMap) {
                log.info "Loaded ${flatMap.size()} properties from apollo-config.groovy"
                MutablePropertySources sources = environment.getPropertySources()
                sources.addFirst(new MapPropertySource("apollo-config-groovy", flatMap))
            }
        } catch (Exception e) {
            log.error "Failed to load apollo-config.groovy: ${e.message}", e
        }
    }

    private String getActiveProfile(ConfigurableEnvironment environment) {
        String[] profiles = environment.getActiveProfiles()
        if (profiles.length > 0) {
            return profiles[0]
        }
        String grailsEnv = System.getProperty("grails.env")
        if (grailsEnv) {
            return grailsEnv
        }
        return "production"
    }

    private Map<String, Object> flattenConfig(ConfigObject config, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>()
        config.each { key, value ->
            String fullKey = prefix ? "${prefix}.${key}" : key.toString()
            if (value instanceof ConfigObject && !value.isEmpty()) {
                result.putAll(flattenConfig(value, fullKey))
            } else if (value instanceof Map && !(value instanceof ConfigObject)) {
                result.putAll(flattenMap(value, fullKey))
            } else {
                result.put(fullKey, value)
            }
        }
        return result
    }

    private Map<String, Object> flattenMap(Map map, String prefix) {
        Map<String, Object> result = new LinkedHashMap<>()
        map.each { key, value ->
            String fullKey = "${prefix}.${key}"
            if (value instanceof Map) {
                result.putAll(flattenMap(value, fullKey))
            } else {
                result.put(fullKey, value)
            }
        }
        return result
    }
}
