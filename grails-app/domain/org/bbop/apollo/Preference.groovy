package org.bbop.apollo

class Preference {

    static constraints = {
        name nullable: true ,blank: false
        preferencesString nullable: true, blank: false
    }

    String name
    String preferencesString // can embed JSONObject
}
