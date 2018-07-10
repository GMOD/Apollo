package org.bbop.apollo

class InsertionArtifact extends SequenceAlterationArtifact {

    static constraints = {
    }

    static String cvTerm = "insertion_artifact"
    static String ontologyId = "SO:0002175"
    static String alternateCvTerm = "InsertionArtifact"

    @Override
    int getOffset() {
        return alterationResidue.length()
    }
}
