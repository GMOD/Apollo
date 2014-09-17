class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?" {
            constraints {
                // apply constraints here
            }
        }


//        "/"(view:"/index")
//        "/"(action: "index",controller: "genome")
        "/home"(action: "index", controller: "genome")
        "500"(view: '/error')

        // set this routing here
//        "/jbrowse"(controller: "jbrowse", action: "index")
        "/jbrowse/"(uri: "/jbrowse/index.html")
        "/jbrowse/data/**"(controller: "jbrowse", action: "data")
        "/jbrowse/data/seq/refSeqs.json"(controller: "jbrowse", action: "seq")
//        "/jbrowse/data/tracks/**"(controller: "jbrowse", action: "tracks")
        "/jbrowse/data/tracks/$trackName/$groupName/${jsonFile}.json" {
            controller = 'jbrowse'
            action = 'tracks'
        }
        // In UrlMappings.groovy
    }
}
