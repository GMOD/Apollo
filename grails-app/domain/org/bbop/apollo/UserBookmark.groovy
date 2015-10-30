package org.bbop.apollo


class UserBookmark {

    User user
    Bookmark bookmark

    static constraints = {
        user nullable: false
        bookmark nullable: false
    }

}
