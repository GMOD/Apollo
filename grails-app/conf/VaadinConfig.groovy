vaadin {

    // Your Vaadin UI classes that extends com.vaadin.ui.UI.
    // The application will be available at e.g. http://localhost:8080/grails-vaadin7-demo/
    mapping = [
            "/*": "app.MyUI"
    ]

    // Extra mapping in case you need 'reserve' a URL and it shouldn't be mapped to e.g. /* by Vaadin
    // mappingExtras = [
    //         '/console/*'
    // ]

    // This is optional because the servlet is provided by default.
    // servletClass = "com.mycompany.MyGrailsAwareApplicationServlet"

    productionMode = false

    // Uncomment this to enable asynchronous communication, useful for vaadin-push
    // asyncSupported = true

    // Uncomment and provide name of the theme (a directory name in web-app/VAADIN/themes folder)
    // themes = ['sample']
    // You can specify exact version of Vaadin for SASS compilation
    // sassCompile = '7.1.9'
}

environments {
    production {
        vaadin {
            productionMode = true
        }
    }
}
