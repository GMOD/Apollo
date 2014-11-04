package org.bbop.apollo

/**
 * Created by ndunn on 10/28/14.
 */
enum FeatureStringEnum {
     FEATURES,
     PARENT_ID,
     USERNAME,
     TYPE,
     PROPERTIES,
     TIMEACCESSION,
     TIMELASTMODIFIED,
     RESIDUES,
     CHILDREN,
     CDS("CDS"),
     EXON("Exon"),
     GENE("Gene"),
     PSEUDOGENE("Pseudogene"),
     STOP_CODON_READTHROUGH("StopCodonReadThrough"),
     STOP_CODON_READHTHROUGH_SUFFIX("-stop_codon_read_through"),
     TRANSCRIPT("Transcript"),
     NONCANONICALFIVEPRIMESPLICESITE("NonCanonicalFivePrimeSpliceSite"),
     NONCANONICALTHREEPRIMESPLICESITE("NonCanonicalThreePrimeSpliceSite"),
     COMMENT("Comment"),
     LOCATION,
     FMIN,
     FMAX,
     STRAND,
     NAME,
     VALUE,
     CV,
     SEQUENCE,
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
     UNIQUENAME


     String value

     public FeatureStringEnum(String value){
          this.value = value
     }

     public FeatureStringEnum(){
          this.value = name().toLowerCase()
     }

}