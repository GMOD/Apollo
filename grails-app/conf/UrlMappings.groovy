class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }


        "/"(redirect: '/annotator/index')
        "500"(view: '/error')
        "/menu"(view: '/menu')
        "/version.jsp"(controller: 'annotator', view: "version")

        // set this routing here
        "/${preference}/jbrowse/"(controller: "jbrowse", action: "indexRouter", params:params)
        "/${preference}/jbrowse/index.html"(controller: "jbrowse", action: "indexRouter", params:params)
        "/${preference}/jbrowse/css/**"(controller:"jbrowse", action: "indexRouter",params:params)
        "/${preference}/jbrowse/src/**"(controller:"jbrowse", action: "indexRouter",params:params)

        "/${preference}/jbrowse/data/${path}"(controller: "jbrowse", action: "data")
        "/${preference}/jbrowse/data/${path}**"(controller: "jbrowse", action: "data")
        "/${preference}/jbrowse/data/trackList.json"(controller:"jbrowse", action: "trackList")
        "/proxy/request/${url}"(controller:"proxy", action: "request")


        "/AnnotationEditorService"(controller:"annotationEditor",action: "handleOperation",params:params)
        "/Login"(controller:"login",action: "handleOperation",params:params)
        "/ProxyService"(controller:"ncbiProxyService",action: "index",params:params)
        "/IOService"(controller:"IOService",action: "handleOperation",params:params)
        "/IOService/download"(controller:"IOService",action: "download", params:params)
        "/jbrowse/web_services/api"(controller:"annotationEditor",action: "web_services", params:params)
        "/jbrowse/web_services/api"(controller:"webServices",action: "index", params:params)
    }
}
