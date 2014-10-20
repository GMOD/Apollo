package org.bbop.apollo

/**
 * Converted
 * Chado?
 */
class CV {

    static constraints = {
    }

//     Integer cvId;
     String name;
     String definition;
//     Set<CVTerm> cvterms = new HashSet<CVTerm>(0);
     static hasMany = [
             cvterms: CVTerm
     ]
}
