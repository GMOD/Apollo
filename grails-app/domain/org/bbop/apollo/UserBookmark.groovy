package org.bbop.apollo


class UserBookmark extends Bookmark{


    Organism organism
    User user

    static constraints = {
        user nullable: false
    }

}
