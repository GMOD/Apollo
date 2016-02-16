package org.bbop.apollo

class PublicationDBXref {

    static constraints = {
    }


    Publication publication;
    DBXref dbxref;
    boolean isCurrent;


    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        PublicationDBXref castOther = ( PublicationDBXref ) other;

        return ( (this.getPublication()==castOther.getPublication()) || ( this.getPublication()!=null && castOther.getPublication()!=null && this.getPublication().equals(castOther.getPublication()) ) ) && ( (this.getDbxref()==castOther.getDbxref()) || ( this.getDbxref()!=null && castOther.getDbxref()!=null && this.getDbxref().equals(castOther.getDbxref()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getPublication() == null ? 0 : this.getPublication().hashCode() );
        result = 37 * result + ( getDbxref() == null ? 0 : this.getDbxref().hashCode() );

        return result;
    }

    public PublicationDBXref generateClone() {
        PublicationDBXref cloned = new PublicationDBXref();
        cloned.publication = this.publication;
        cloned.dbxref = this.dbxref;
        cloned.isCurrent = this.isCurrent;
        return cloned;
    }

}
