package org.bbop.apollo
/**
 * Created by ndunn on 10/28/14.
 */
enum CvTermStringEnum {
     PART_OF("PartOf")

     String value

     private CvTermStringEnum(String value){
          this.value = value
     }

     private CvTermStringEnum(){
          this.value = name().toLowerCase()
     }
}