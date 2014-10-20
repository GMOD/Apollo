package org.bbop.apollo

class DBXref {

    static constraints = {
    }

    private Integer dbxrefId;
    private String version;
    private DB db;
    private String accession;
    private String description;
//    private Set<DBXrefProperty> dbxrefProperties = new HashSet<DBXrefProperty>(0);

    static hasMany = [
            dbxrefProperties : DBXrefProperty
    ]
}
