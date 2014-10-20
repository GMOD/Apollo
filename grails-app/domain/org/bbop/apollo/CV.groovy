package org.bbop.apollo

class CV {

    static constraints = {
    }

     Integer cvId;
     String name;
     String definition;
//     Set<CVTerm> cvterms = new HashSet<CVTerm>(0);
     static hasMany = [
             cvterms: CVTerm
     ]
}
