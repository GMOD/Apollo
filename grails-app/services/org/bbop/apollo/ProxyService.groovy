package org.bbop.apollo

import grails.transaction.Transactional

@Transactional
class ProxyService {

    /**
     * Stubs default proxies
     */
    Boolean stubDefaultProxies() {
//        Proxy.findOrSaveByActive()
    }

    /**
     * Looks through all proxies to return valid proxies
     * @return
     */
    Proxy findProxyForUrl(String inputUrl){

    }
}
