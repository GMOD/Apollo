package org.bbop.apollo

class PublicationAuthor {

    static constraints = {
    }

    Publication publication;
    int rank;
    Boolean editor;
    String surname;
    String givenNames;
    String suffix;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        PublicationAuthor castOther = ( PublicationAuthor ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && (this.getRank()==castOther.getRank());
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + this.getRank();




        return result;
    }

    public PublicationAuthor generateClone() {
        PublicationAuthor cloned = new PublicationAuthor();
        cloned.publication = this.publication;
        cloned.rank = this.rank;
        cloned.editor = this.editor;
        cloned.surname = this.surname;
        cloned.givenNames = this.givenNames;
        cloned.suffix = this.suffix;
        return cloned;
    }
}
