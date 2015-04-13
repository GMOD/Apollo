package org.bbop.apollo.gwt.shared;

/**
 * Created by ndunn on 4/2/15.
 */
public enum FeatureStringEnum {
        FEATURES,
        FEATURE_PROPERTY,
        PARENT_ID,
        USERNAME,
        TYPE,
        PARENT_TYPE,
        PROPERTIES,
        TIMEACCESSION,
        DEFAULT,
        TIMELASTMODIFIED,
        RESIDUES,
        CHILDREN,
        CDS("CDS"),
        EXON("Exon"),
        GENE("Gene"),
        PSEUDOGENE("Pseudogene"),
        STOP_CODON_READTHROUGH("StopCodonReadThrough"),
        STOP_CODON_READHTHROUGH_SUFFIX("-stop_codon_read_through"),
        READTHROUGH_STOP_CODON,
        TRANSCRIPT("Transcript"),
        NONCANONICALFIVEPRIMESPLICESITE("NonCanonicalFivePrimeSpliceSite"),
        NONCANONICALTHREEPRIMESPLICESITE("NonCanonicalThreePrimeSpliceSite"),
        DATE_LAST_MODIFIED,
        DATE_CREATION,
        COMMENT("Comment"),
        TAG_VALUE_DELIMITER("="),
        COMMENTS,
        CANNED_COMMENTS,
        STATUS,
        NOTES,
        TAG,
        NON_RESERVED_PROPERTIES,
        LOCATION,
        FMIN,
        FMAX,
        IS_FMIN_PARTIAL,
        IS_FMAX_PARTIAL,
        STRAND,
        NAME,
        VALUE,
        CV,
        SEQUENCE,
        TRACK,
        DB,
        DBXREFS,
        ACCESSION,
        CDS_SUFFIX("-CDS"),
        MINUS1FRAMESHIFT("Minus1Frameshift"),
        MINUS2FRAMESHIFT("Minus2Frameshift"),
        PLUS1FRAMESHIFT("Plus1Frameshift"),
        PLUS2FRAMESHIFT("Plus2Frameshift"),
        DELETION_PREFIX("Deletion-"),
        INSERTION_PREFIX("Insertion-"),
        OWNER("Owner"),
        ORGANISM,
        SYMBOL,
        ALTERNATECVTERM("alternateCvTerm"),
        DESCRIPTION,
        ANNOTATION_INFO_EDITOR_CONFIGS,
        HASDBXREFS("hasDbxrefs"),
        HASATTRIBUTES("hasAttributes"),
        HASPUBMEDIDS("hasPubmedIds"),
        HASGOIDS("hasGoIds"),
        HASCOMMENTS("hasComments"),
        SUPPORTED_TYPES,
        OLD_DBXREFS,
        NEW_DBXREFS,
        ATTRIBUTES,
        PUBMEDIDS("pubmed_ids"),
        GOIDS("go_ids"),
        SYNONYMS,
        UNIQUENAME,
        // TODO: move these to a SequenceTypeEnum
        TYPE_PEPTIDE("peptide"),
        TYPE_CDS("cds"),
        TYPE_CDNA("cdna"),
        TYPE_GENOMIC("genomic"),
        EXPORT_ID("ID"),
        EXPORT_DBXREF("Dbxref"),
        EXPORT_NAME("Name"),
        EXPORT_ALIAS("Alias"),
        EXPORT_NOTE("Note"),
        EXPORT_PARENT("Parent"),
        ORGANISM_JBROWSE_DIRECTORY("organismJBrowseDirectory"),
        ORGANISM_ID("organismId"),
        SEQUENCE_NAME("sequenceName"),
        DEFAULT_SEQUENCE_NAME("defaultSequenceName"),
        PERMISSIONS,
        ERROR,
        ERROR_MESSAGE,
        REQUEST_INDEX,
        HAS_USERS,
        USER_ID("userId"),
        LOCKED,
        ;


        private String value;

        FeatureStringEnum(String value) {
            this.value = value;
        }

        FeatureStringEnum() {
            this.value = name().toLowerCase();
        }

        @Override
        public String toString() {
            return value;
        }

        public String getValue() {
                return value;
        }

}
