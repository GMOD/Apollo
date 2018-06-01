class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }

        //known bug for export plugin because grails is convention over configuration
        // https://stackoverflow.com/questions/22882691/grails-2-3-x-get-the-value-of-url-parameters
        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
                action(validator: { return it == 'export'})
            }
        }
        

        "/"(redirect: '/annotator/index')
        "500"(view: '/error')
        "/menu"(view: '/menu')
        "/version.jsp"(controller: 'annotator', view: "version")
        "/${clientToken}/version.jsp"(controller: 'annotator', view: "version")

        "/track/nclist/${organismString}/${trackName}/${sequence}:${fmin}..${fmax}.json"(controller: "track", action: "nclist")
        "/track/nclist/${organismString}/${trackName}/?loc=${sequence}:${fmin}..${fmax}"(controller: "track", action: "nclist")

//        "/trackForName/${organismString}/${trackName}/${sequence}/${featureName}.json"(controller: "track", action: "jsonName")
        "/track/${organismString}/${trackName}/${sequence}/${featureName}.${type}"(controller: "track", action: "featuresByName",[params:params])
        "/track/${organismString}/${trackName}/${sequence}:${fmin}..${fmax}.${type}"(controller: "track", action: "featuresByLocation",[params:params])
        "/track/${organismString}/${trackName}/?loc=${sequence}:${fmin}..${fmax}.${type}"(controller: "track", action: "featuresByLocation",[params:params])

        "/track/list/${organismName}"(controller: "track", action: "getTracks",[params:params])
        "/track/cache/clear/${organismName}/${trackName}"(controller: "track", action: "clearTrackCache")
        "/track/cache/clear/${organismName}"(controller: "track", action: "clearOrganismCache")

        "/vcf/${organismString}/${trackName}/${sequence}:${fmin}..${fmax}.${type}"(controller: "vcf", action: "featuresByLocation",[params:params])

        "/sequence/${organismString}/?loc=${sequenceName}:${fmin}..${fmax}"(controller: "sequence", action: "sequenceByLocation",[params:params])
        "/sequence/${organismString}/${sequenceName}:${fmin}..${fmax}"(controller: "sequence", action: "sequenceByLocation",[params:params])
        "/sequence/${organismString}/${sequenceName}/${featureName}.${type}"(controller: "sequence", action: "sequenceByName",[params:params])
        "/sequence/cache/clear/${organismName}/${sequenceName}"(controller: "sequence", action: "clearSequenceCache")
        "/sequence/cache/clear/${organismName}"(controller: "sequence", action: "clearOrganismCache")

        // TODO: remove if we merge with the JSON
        "/track/biolink/${organismString}/${trackName}/${sequence}:${fmin}..${fmax}.biolink"(controller: "track", action: "biolink")
        "/track/biolink/${organismString}/${trackName}/?loc=${sequence}:${fmin}..${fmax}"(controller: "track", action: "biolink")

        // set this routing here
        "/jbrowse/"(controller: "jbrowse", action: "indexRouter", params:params)
        "/${clientToken}/jbrowse/"(controller: "jbrowse", action: "indexRouter", params:params)
        "/${clientToken}/jbrowse/index.html"(controller: "jbrowse", action: "indexRouter", params:params)
        "/${clientToken}/jbrowse/${path}**" {
            controller= "jbrowse"
            action= "passthrough"
            prefix= "jbrowse"
//            permanent = true
        }
        "/${clientToken}/stomp/${path}**" {
            controller= "jbrowse"
            action= "passthrough"
            prefix= "stomp"
//            permanent = true
        }

        "/${clientToken}/jbrowse/data/${path}"(controller: "jbrowse", action: "data")
        "/${clientToken}/jbrowse/data/${path}**"(controller: "jbrowse", action: "data")
        "/${clientToken}/jbrowse/data/trackList.json"(controller:"jbrowse", action: "trackList")
        "/jbrowse/data/${path}"(controller: "jbrowse", action: "data")
        "/jbrowse/data/${path}**"(controller: "jbrowse", action: "data")
        "/jbrowse/data/trackList.json"(controller:"jbrowse", action: "trackList")



        "/${clientToken}/AnnotationEditorService"(controller:"annotationEditor",action: "handleOperation",params:params)
        "/Login"(controller:"login",action: "handleOperation",params:params)
        "/${clientToken}/Login"(controller:"login",action: "handleOperation",params:params)
        "/${clientToken}/sequence/lookupSequenceByNameAndOrganism"(controller:"sequence",action: "lookupSequenceByNameAndOrganism", params:params)

        "/proxy/request/${url}"(controller:"proxy", action: "request")
        "/${clientToken}/proxy/request/${url}"(controller:"proxy", action: "request")

        "/ProxyService"(controller:"ncbiProxyService",action: "index",params:params)
        "/${clientToken}/ProxyService"(controller:"ncbiProxyService",action: "index",params:params)

        "/IOService"(controller:"IOService",action: "handleOperation",params:params)
        "/${clientToken}/IOService"(controller:"IOService",action: "handleOperation",params:params)

        "/IOService/download"(controller:"IOService",action: "download", params:params)
        "/${clientToken}/IOService/download"(controller:"IOService",action: "download", params:params)

        "/jbrowse/web_services/api"(controller:"annotationEditor",action: "web_services", params:params)
        "/jbrowse/web_services/api"(controller:"webServices",action: "index", params:params)
        "/${clientToken}/jbrowse/web_services/api"(controller:"webServices",action: "index", params:params)

        // if all else fails
        // TODO: pass all of these into the same function and remap from there
        "/${clientToken}/${path}**.bw" {
            controller= "jbrowse"
            action= "data"
            fileType = "bw"
        }
        "/${clientToken}/${path}**.bai" {
            controller= "jbrowse"
            action= "data"
            fileType = "bai"
        }
        "/${clientToken}/${path}**.bam" {
            controller= "jbrowse"
            action= "data"
            fileType = "bam"
        }
//        "/${clientToken}/${path}**.conf" {
//            controller= "jbrowse"
//            action= "data"
//            fileType = "conf"
//        }
        "/${clientToken}/${path}**.gtf" {
            controller= "jbrowse"
            action= "data"
            fileType = "gtf"
        }
        "/${clientToken}/${path}**.vcf.gz.tbi" {
            controller= "jbrowse"
            action= "data"
            fileType = "vcf.gz.tbi"
        }
        "/${clientToken}/${path}**.vcf.gz" {
            controller= "jbrowse"
            action= "data"
            fileType = "vcf.gz"
        }
        "/${clientToken}/${path}**.gff3" {
            controller= "jbrowse"
            action= "data"
            fileType = "gff3"
        }
        "/${clientToken}/${path}**.gff" {
            controller= "jbrowse"
            action= "data"
            fileType = "gff"
        }
    }
}
