package org.bbop.apollo

class ServerData {

    static constraints = {
        name unique: true
    }

    String name
    Date dateCreated
    Date lastUpdated

    String getName(){
        if(!name){
            name = "ApolloSever-${org.bbop.apollo.gwt.shared.ClientTokenGenerator.generateRandomString()}"
        }
        return name
    }


}
