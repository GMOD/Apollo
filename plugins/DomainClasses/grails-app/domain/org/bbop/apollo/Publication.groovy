package org.bbop.apollo

class Publication {

    static constraints = {
    }

    CVTerm type;
    String title;
    String volumeTitle;
    String volume;
    String seriesName;
    String issue;
    String publicationYear;
    String pages;
    String miniReference;
    String uniqueName;
    Boolean isObsolete;
    String publisher;
    String publicationPlace;


    static hasMany = [
            childPublicationRelationships : PublicationRelationship
            ,parentPublicationRelationships : PublicationRelationship
            ,publicationAuthors: PublicationAuthor
            ,publicationDBXrefs: PublicationDBXref
    ]

    static mappedBy = [
            childPublicationRelationships :"subjectPublication"
            ,parentPublicationRelationships : "objectPublication"
    ]


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        Publication castOther = ( Publication ) other;

        return ( (this.getUniqueName()==castOther.getUniqueName()) || ( this.getUniqueName()!=null && castOther.getUniqueName()!=null && this.getUniqueName().equals(castOther.getUniqueName()) ) );
    }

    public int hashCode() {
        int result = 17;











        result = 37 * result + ( getUniqueName() == null ? 0 : this.getUniqueName().hashCode() );







        return result;
    }

    public Publication generateClone() {
        Publication cloned = new Publication();
        cloned.type = this.type;
        cloned.title = this.title;
        cloned.volumeTitle = this.volumeTitle;
        cloned.volume = this.volume;
        cloned.seriesName = this.seriesName;
        cloned.issue = this.issue;
        cloned.publicationYear = this.publicationYear;
        cloned.pages = this.pages;
        cloned.miniReference = this.miniReference;
        cloned.uniqueName = this.uniqueName;
        cloned.isObsolete = this.isObsolete;
        cloned.publisher = this.publisher;
        cloned.publicationPlace = this.publicationPlace;
        cloned.childPublicationRelationships = this.childPublicationRelationships;
        cloned.publicationAuthors = this.publicationAuthors;
        cloned.publicationDBXrefs = this.publicationDBXrefs;
        cloned.parentPublicationRelationships = this.parentPublicationRelationships;
        return cloned;
    }

}
