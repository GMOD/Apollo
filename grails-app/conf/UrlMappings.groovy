class UrlMappings {

	static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

//        "/"(view:"/index")
//        "/"(action: "index",controller: "genome")
        "/home"(action: "index",controller: "genome")
        "500"(view:'/error')
	}
}
