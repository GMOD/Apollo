package org.bbop.apollo.go

import org.bbop.apollo.Feature
import org.bbop.apollo.User


class GoAnnotation {

  static constraints = {
    aspect nullable: false, blank: false
    feature nullable: false
    goRef nullable: false, blank: false
    goRefLabel nullable: true, blank: true
    evidenceRef nullable: false, blank: false
    evidenceRefLabel nullable: true, blank: true
    geneProductRelationshipRef nullable: true, blank: false
    negate nullable: false
    withOrFromArray nullable: true, blank: true
    notesArray nullable: true, blank: true
    dateCreated nullable: false
    lastUpdated nullable: false
    reference nullable: false, blank: false
  }

  static hasMany = [
    owners: User
  ]

  String aspect
  Feature feature
  String goRef
  String evidenceRef
  String goRefLabel
  String evidenceRefLabel
  String geneProductRelationshipRef
  Boolean negate
  String withOrFromArray
  String notesArray
  String reference
  Date lastUpdated
  Date dateCreated

}
