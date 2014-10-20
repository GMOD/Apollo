package org.bbop.apollo

class Publication {

    static constraints = {
    }

    private Integer publicationId;
    private CVTerm type;
    private String title;
    private String volumeTitle;
    private String volume;
    private String seriesName;
    private String issue;
    private String publicationYear;
    private String pages;
    private String miniReference;
    private String uniqueName;
    private Boolean isObsolete;
    private String publisher;
    private String publicationPlace;
//    private Set<PublicationRelationship> childPublicationRelationships = new HashSet<PublicationRelationship>(0);
//    private Set<PublicationAuthor> publicationAuthors = new HashSet<PublicationAuthor>(0);
//    private Set<PublicationDBXref> publicationDBXrefs = new HashSet<PublicationDBXref>(0);
//    private Set<PublicationRelationship> parentPublicationRelationships = new HashSet<PublicationRelationship>(0);

    static hasMany = [
            childPublicationRelationships : PublicationRelationship
            ,parentPublicationRelationships : PublicationRelationship
            ,publicationAuthors: PublicationAuthor
            ,publicationDBXrefs: PublicationDBXref
    ]


}
