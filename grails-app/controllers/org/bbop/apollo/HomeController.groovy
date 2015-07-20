package org.bbop.apollo

import org.grails.plugins.metrics.groovy.Timed

class HomeController {

    @Timed(name = "SystemInfo")
    def systemInfo() {

        Map<String,String> runtimeMapInstance = new HashMap<>()
        Map<String,String> servletMapInstance = new HashMap<>()
        Map<String,String> javaMapInstance = new HashMap<>()

        javaMapInstance.putAll(System.getenv())

        servletContext.getAttributeNames().each {
            servletMapInstance.put(it,servletContext.getAttribute(it))
        }

        runtimeMapInstance.put("Available processors",""+Runtime.getRuntime().availableProcessors())
        runtimeMapInstance.put("Free memory",Runtime.getRuntime().freeMemory()/1E6+" MB")
        runtimeMapInstance.put("Max memory",""+Runtime.getRuntime().maxMemory()/1E6 +" MB")
        runtimeMapInstance.put("Total memory",""+Runtime.getRuntime().totalMemory()/1E6 +" MB")


//        servletContext
        render view: "systemInfo", model:[javaMapInstance:javaMapInstance,servletMapInstance:servletMapInstance,runtimeMapInstance:runtimeMapInstance]
    }
}
