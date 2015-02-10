class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }


//        "/"(view:"/index")
        "/"(action: "index",controller: "organism")
//        "/home"(action: "index", controller: "genome")
        "500"(view: '/error')
        "/menu"(view: '/menu')

        // set this routing here
//        "/jbrowse"(controller: "jbrowse", action: "index")
        "/jbrowse/"(uri: "/jbrowse/index.html")
        "/jbrowse/data/${fileName}"(controller: "jbrowse", action: "data")
        "/jbrowse/data/seq/refSeqs.json"(controller: "jbrowse", action: "seq")
        "/jbrowse/data/seq/${a}/${b}/${c}/${group}"(controller: "jbrowse", action: "seqMapper",params:params)
        "/jbrowse/data/names/${directory}/${jsonFile}.json"(controller: "jbrowse", action: "namesFiles")
        "/jbrowse/data/names/${fileName}.json"(controller: "jbrowse", action: "names")
        "/jbrowse/data/names/meta.json"(controller: "jbrowse", action: "meta")
//        "/jbrowse/data/tracks/**"(controller: "jbrowse", action: "tracks")
        "/jbrowse/data/tracks/$trackName/$groupName/${jsonFile}.json" {
            controller = 'jbrowse'
            action = 'tracks'
        }
        "/AnnotationEditorService"(controller:"annotationEditor",action: "handleOperation",params:params){

        }
        // In UrlMappings.groovy
    }
}