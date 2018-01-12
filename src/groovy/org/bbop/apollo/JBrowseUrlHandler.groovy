package org.bbop.apollo

import java.util.regex.Pattern

class JBrowseUrlHandler {

    static Pattern startsWithAlpha = Pattern.compile("^\\p{Alpha}.*")

    static boolean hasProtocol(String paramString ) {
        paramString.contains("http://") || paramString.contains("https://") || paramString.contains("ftp://")
    }

    static String fixUrlTemplate(String entryValue,String contextPath) {
        if(!hasProtocol(entryValue) && startsWithAlpha.matcher(entryValue).matches() && !entryValue.startsWith(contextPath) && !entryValue.startsWith(contextPath.substring(1))){
            entryValue = (contextPath.startsWith("/") ? contextPath.substring(1) : contextPath) + "/JBrowse/" + entryValue
        }

        return entryValue
    }
}
