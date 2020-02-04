package org.bbop.apollo.go

import org.bbop.apollo.Feature
import org.bbop.apollo.Transcript
import org.bbop.apollo.User


class GeneProduct {

  static constraints = {
    feature nullable: false
    productName nullable: false,blank: false
    reference nullable: false, blank: false
    dateCreated nullable: false
    lastUpdated nullable: false
    evidenceRef nullable: false, blank: false
    evidenceRefLabel nullable: true, blank: true
    withOrFromArray nullable: true, blank: true


    goRef nullable: false, blank: false
    goRefLabel nullable: true, blank: true
    geneProductRelationshipRef nullable: true, blank: false
  }

  static hasMany = [
    owners: User
  ]

  Transcript feature
  String productName // this is new
  String reference
  Date lastUpdated
  Date dateCreated
  String evidenceRef
  String evidenceRefLabel
  String withOrFromArray


  String goRef
  String goRefLabel
  String geneProductRelationshipRef

}
