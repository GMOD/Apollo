package org.bbop.apollo

import grails.transaction.Transactional

@Transactional(readOnly = true)
class ProxyService {

    private final static List<String> defaultProxies = ["http://golr.berkeleybop.org/solr/select"]

    /**
     * Looks through all proxies to return valid proxies
     *
     * @return
     */
    Proxy findProxyForUrl(String referenceUrl){
        Proxy proxy = Proxy.findByReferenceUrl(referenceUrl)
        return proxy
    }

    /**
     *
     * @param urlProxy
     * @return
     */
    @Transactional
    Proxy findDefaultProxy(String urlProxy) {
        if(defaultProxies.contains(urlProxy)){
            Proxy proxy = new Proxy(
                    referenceUrl: urlProxy
                    ,targetUrl: urlProxy
                    ,active: true
            ).save(failOnError: true)
            return proxy
        }
        return null
    }
}
