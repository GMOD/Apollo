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
        "/${clientToken}/version.jsp"(controller: 'annotator', view: "version")

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
