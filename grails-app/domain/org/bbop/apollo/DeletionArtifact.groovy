package org.bbop.apollo

class DeletionArtifact extends SequenceAlterationArtifact {

    static constraints = {
    }

    Integer deletionLength

    static String cvTerm = "deletion_artifact"
    static String ontologyId = "SO:0002174"
    static String alternateCvTerm = "DeletionArtifact"

    @Override
    int getOffset() {
        return deletionLength
    }
}
