package org.bbop.apollo.pages;

import geb.Page;

class LoginDialogPage extends ScaffoldPage{

    static url = '/apollo/annotator'

    static at = { title.startsWith 'Annotator' }

    static content = {
        login {  $('#loginDialogId') }
    }

//    static content = {
//        heading { $("h1") }
//        message { $("div.message").text() }
//    }
}
