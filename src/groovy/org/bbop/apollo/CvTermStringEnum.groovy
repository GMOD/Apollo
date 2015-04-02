package org.bbop.apollo
/**
 * Created by ndunn on 10/28/14.
 */
enum CvTermStringEnum {
     PART_OF("PartOf")

     String value

     public CvTermStringEnum(String value){
          this.value = value
     }

     public CvTermStringEnum(){
          this.value = name().toLowerCase()
     }
}