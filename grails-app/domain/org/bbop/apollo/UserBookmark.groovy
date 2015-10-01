package org.bbop.apollo


class UserBookmark extends Bookmark{


    User user

    static constraints = {
        user nullable: false
    }

}
