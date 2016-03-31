package org.bbop.apollo
class CV {

    static constraints = {
    }

     String name;
     String definition;

     static hasMany = [
             cvterms: CVTerm
     ]
}
