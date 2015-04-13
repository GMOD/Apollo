class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }


        //"/"(view:"/index")
        //"/home"(action: "index", controller: "genome")
        "/"(redirect: '/annotator/index')
        "500"(view: '/error')
        "/menu"(view: '/menu')
        "/version.jsp"(controller: 'annotator', view: "version")

        // set this routing here
        //"/jbrowse"(controller: "jbrowse", action: "index")
        "/jbrowse/"(uri: "/jbrowse/index.html")
        "/jbrowse/data/${fileName}"(controller: "jbrowse", action: "data")
        "/jbrowse/data/bigwig/${fileName}"(controller: "jbrowse", action: "bigwig")
        "/jbrowse/data/bam/${fileName}"(controller: "jbrowse", action: "bam")
        "/jbrowse/data/seq/refSeqs.json"(controller: "jbrowse", action: "seq")
        "/jbrowse/data/seq/$a/$b/$c/$group"(controller: "jbrowse", action: "seqMapper",params:params)
        "/jbrowse/data/names/${directory}/${jsonFile}.json"(controller: "jbrowse", action: "namesFiles")
        "/jbrowse/data/names/${fileName}.json"(controller: "jbrowse", action: "names")
        "/jbrowse/data/names/meta.json"(controller: "jbrowse", action: "meta")
        //"/jbrowse/data/tracks/**"(controller: "jbrowse", action: "tracks")
        "/jbrowse/data/tracks/$trackName/$groupName/${jsonFile}.json" {
            controller = 'jbrowse'
            action = 'tracks'
        }
        "/AnnotationEditorService"(controller:"annotationEditor",action: "handleOperation",params:params){

        }

        "/Login"(controller:"login",action: "handleOperation",params:params){
        }
    }
}
