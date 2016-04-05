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
        "/jbrowse/"(controller: "jbrowse", action: "indexRouter", params:params)
        "/jbrowse/index.html"(controller: "jbrowse", action: "indexRouter", params:params)
        "/jbrowse/data/${path}"(controller: "jbrowse", action: "data")
        "/jbrowse/data/${path}**"(controller: "jbrowse", action: "data")
        "/jbrowse/data/trackList.json"(controller:"jbrowse", action: "trackList")
        "/proxy/request/${url}"(controller:"proxy", action: "request")


        "/AnnotationEditorService"(controller:"annotationEditor",action: "handleOperation",params:params)
        "/Login"(controller:"login",action: "handleOperation",params:params)
        "/ProxyService"(controller:"ncbiProxyService",action: "index",params:params)
        "/IOService"(controller:"IOService",action: "handleOperation",params:params)
        "/IOService/download"(controller:"IOService",action: "download", params:params)
        "/jbrowse/web_services/api"(controller:"annotationEditor",action: "web_services", params:params)
        "/jbrowse/web_services/api"(controller:"webServices",action: "index", params:params)

        // add other types
        "/bigwig/stats/global"(controller: "bigwig",action: "global")
        "/bigwig/stats/region"(controller: "bigwig",action: "region")
        "/bigwig/stats/regionFeatureDensities"(controller: "bigwig",action: "regionFeatureDensities")
        "/bigwig/features/${sequenceName}"(controller: "bigwig",action: "features",params:params,sequenceName:sequenceName)
        "/sequence/stats/global"(controller: "sequence",action: "statsGlobal",params:params)
        "/sequence/stats/region"(controller: "sequence",action: "statsRegion",params:params)
        "/sequence/stats/regionFeatureDensities"(controller: "sequence",action: "regionFeatureDensities",params:params)
        "/sequence/features/${sequenceName}"(controller: "sequence",action: "features",params:params,sequenceName:sequenceName)

        "/projectionTrack/stats/global"(controller: "projectionTrack",action: "statsGlobal",params:params)
        "/projectionTrack/stats/region"(controller: "projectionTrack",action: "statsRegion",params:params)
        "/projectionTrack/stats/regionFeatureDensities"(controller: "projectionTrack",action: "regionFeatureDensities",params:params)
        "/projectionTrack/features/${sequenceName}"(controller: "projectionTrack",action: "features",params:params,sequenceName:sequenceName)

//        "/web_services/api"(controller:"annotationEditor",action: "web_services", params:params)
    }
}
