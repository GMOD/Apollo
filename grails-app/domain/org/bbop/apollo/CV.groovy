package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class CV {

    static constraints = {
    }

     String name;
     String definition;

     static hasMany = [
             cvterms: CVTerm
     ]
}
