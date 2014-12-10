navigation = {
    // Declare the "app" scope, used by default in tags
    app {

        // A nav item pointing to HomeController, using the default action
//        home()
        organism(controller:'organism',action: 'list',titleText:'Organisms')
        sequence(controller:  'sequence',action: 'index',titleText:'Sequences')
        annotator(controller:'annotator',action: 'index',titleText:'Annotate')
        user(controller:'user',action: 'permissions',titleText:'User Permissions')

        // Items pointing to ContentController, using the specific action
//        about(controller:'content')
//        contact(controller:'content')
//        help(controller:'content')
//
//        // Some user interface actions in second-level nav
//        // All in BooksController
//        books {
//            // "list" action in "books" controller
//            list()
//            // "create" action in "books" controller
//            create()
//        }
//
//        // More convoluted stuff split across controllers/locations
//        support(controller:'content', action:'support') {
//            faq(url:'http://faqs.mysite.com') // point to CMS
//            makeRequest(controller:'supportRequest', action:'create')
//        }
    }

    // Some back-end admin scaffolding stuff in a separate scope
//    admin {
//        // Use "list" action as default item, even if its not default action
//        // and create automatic sub-items for the other actions
//        books(controller:'bookAdmin', action:'list, create, search')
//
//        // User admin, with default screen using "search" action
//        users(controller:'userAdmin', action:'search') {
//            // Declare action alias so "create" is active for both "create" and "update" actions
//            create(action:'create', actionAliases:'update')
//        }
//    }
}