package org.bbop.apollo.geneProduct

import org.bbop.apollo.Feature
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
    alternate nullable: false
    notesArray nullable: true, blank: true

  }

  static hasMany = [
    owners: User
  ]

  Feature feature
  String productName // this is new
  String reference
  Date lastUpdated
  Date dateCreated
  String evidenceRef
  String evidenceRefLabel
  String notesArray
  String withOrFromArray
  Boolean alternate = false


}
