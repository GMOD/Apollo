package org.bbop.apollo

class SubstitutionArtifact extends SequenceAlterationArtifact {

    static constraints = {
    }

    static String cvTerm  = "substitution_artifact"
    static String ontologyId = "SO:0002176"
    static String alternateCvTerm = "SubstitutionArtifact"

    @Override
    int getOffset() {
        return alterationResidue.length()
    }
}
