package org.bbop.apollo

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.transform.CompileStatic
import org.bbop.apollo.security.SecurityConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@CompileStatic
@Import(SecurityConfig)
@EnableScheduling
class Application extends GrailsAutoConfiguration {
    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    @Bean
    WebMvcConfigurer webConfigurer() {
        new WebMvcConfigurer() {
            @Override
            void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
            }

            @Override
            void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/annotator/**")
                    .addResourceLocations("/annotator/")
                registry.addResourceHandler("/jbrowse/**")
                    .addResourceLocations("/jbrowse/")
                registry.addResourceHandler("/css/**")
                    .addResourceLocations("/css/")
                registry.addResourceHandler("/js/**")
                    .addResourceLocations("/js/")
                registry.addResourceHandler("/images/**")
                    .addResourceLocations("/images/")
                registry.addResourceHandler("/translation_tables/**")
                    .addResourceLocations("/translation_tables/")
            }
        }
    }
}
