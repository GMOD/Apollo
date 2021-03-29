package org.bbop.apollo.websocket


import java.security.Principal

class StompPrincipal implements Principal {
    String userName

    StompPrincipal(userName) {
        this.userName = userName
    }

    @Override
    public String getName() {
        return this.userName
    }
}
