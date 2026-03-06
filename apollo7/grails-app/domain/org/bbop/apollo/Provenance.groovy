package org.bbop.apollo

class Provenance {

  static constraints = {
    feature nullable: false
    field nullable: false,blank: false
    reference nullable: false, blank: false
    dateCreated nullable: false
    lastUpdated nullable: false
    evidenceRef nullable: false, blank: false
    evidenceRefLabel nullable: true, blank: true
    withOrFromArray nullable: true, blank: true
    notesArray nullable: true, blank: true

  }

  static hasMany = [
    owners: User
  ]

  Feature feature
  String field // this is new
  String reference
  Date lastUpdated
  Date dateCreated
  String evidenceRef
  String evidenceRefLabel
  String notesArray
  String withOrFromArray


}
