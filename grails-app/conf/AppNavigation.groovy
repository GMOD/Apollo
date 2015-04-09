navigation = {
    // Declare the "app" scope, used by default in tags
    app {

        // A nav item pointing to HomeController, using the default action
        //home()
        organism(controller:'organism',action: 'list',titleText:'Organisms')
        sequence(controller:  'sequence',action: 'index',titleText:'Sequences')
        annotator(controller:'annotator',action: 'index',titleText:'Annotate')
        user(controller:'user',action: 'permissions',titleText:'User Permissions')

    }

    // Some back-end admin scaffolding stuff in a separate scope
    //admin {
        //// Use "list" action as default item, even if its not default action
        //// and create automatic sub-items for the other actions
        //books(controller:'bookAdmin', action:'list, create, search')
        //
        //// User admin, with default screen using "search" action
        //users(controller:'userAdmin', action:'search') {
        //    // Declare action alias so "create" is active for both "create" and "update" actions
        //    create(action:'create', actionAliases:'update')
        //}
    //}
}
