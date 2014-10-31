package org.bbop.apollo

/**
 * Created by ndunn on 10/28/14.
 */
enum FeatureStringEnum {
     FEATURES,
     PARENT_ID,
     USERNAME,
     TYPE,
     RESIDUES,
     CHILDREN,
     CDS("CDS"),
     EXON("Exon"),
     GENE("Gene"),
     PSEUDOGENE("Pseudogene"),
     STOP_CODON_READTHROUGH("StopCodonReadThrough"),
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
     DB,
     DBXREFS,
     ACCESSION,
     CDS_SUFFIX("-CDS"),
     MINUS1FRAMESHIFT("Minus1Frameshift"),
     MINUS2FRAMESHIFT("Minus2Frameshift"),
     PLUS1FRAMESHIFT("Plus1Frameshift"),
     PLUS2FRAMESHIFT("Plus2Frameshift"),
     ;


     String value

     public FeatureStringEnum(String value){
          this.value = value
     }

     public FeatureStringEnum(){
          this.value = name().toLowerCase()
     }

}