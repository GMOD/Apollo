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
     STOP_CODON_READTHROUGH("StopCodonReadThrough")
     ;


     String value

     public FeatureStringEnum(String value){
          this.value = value
     }

     public FeatureStringEnum(){
          this.value = name().toLowerCase()
     }

}