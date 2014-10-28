package org.bbop.apollo

/**
 * Created by ndunn on 10/28/14.
 */
enum CvTermStringEnum {
     PART_OF("PartOf"),
     ;


     String value

     public Features(String value){
          this.value = value
     }

     public Features(){
          this.value = name().toLowerCase()
     }
}