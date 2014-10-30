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
     CV,
     ;


     String value

     public FeatureStringEnum(String value){
          this.value = value
     }

     public FeatureStringEnum(){
          this.value = name().toLowerCase()
     }

}