package org.bbop.apollo

class DB {

    static constraints = {
        name nullable: false, unique: true
        urlPrefix nullable: true
        url nullable: true
        description nullable: true
    }

     String name;
     String description;
     String urlPrefix;
     String url;

    public boolean equals(Object other) {
        if (this.is(other)) return true
        if (getClass() != other.class) return false
        DB castOther = ( DB ) other;

        return ( (this.getName()==castOther.getName()) || ( this.getName()!=null && castOther.getName()!=null && this.getName().equals(castOther.getName()) ) );
    }

    public int hashCode() {
        int result = 17;


        result = 37 * result + ( getName() == null ? 0 : this.getName().hashCode() );



        return result;
    }

    public DB generateClone() {
        DB cloned = new DB();
        cloned.name = this.name;
        cloned.description = this.description;
        cloned.urlPrefix = this.urlPrefix;
        cloned.url = this.url;
        return cloned;
    }
}
