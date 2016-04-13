package org.bbop.apollo

class Preference {

    static constraints = {
        name nullable: true ,blank: false
        domain nullable: true ,blank: false
        preferencesString nullable: true, blank: false
        clientToken nullable: false, blank: false
        token nullable: true, blank: false
    }

    String name
    String domain  // if we want to filter for a user / group domain
    String preferencesString // can embed JSONObject
    String clientToken // this is a token from the browser
    String token   // this is a general purpose token or JSON string
    Date dateCreated
    Date lastUpdated
}
