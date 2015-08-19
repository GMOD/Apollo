package org.bbop.apollo.sequence.search;


import java.util.Collection;
import org.bbop.apollo.sequence.search.blast.BlastAlignment;
import org.codehaus.groovy.grails.web.json.JSONObject;

import org.bbop.apollo.Match;

public abstract class SequenceSearchTool {

    public abstract void parseConfiguration(JSONObject config) throws SequenceSearchToolException;

    public abstract Collection<BlastAlignment> search(String uniqueToken, String query, String databaseId) throws SequenceSearchToolException;
    
    public Collection<BlastAlignment> search(String uniqueToken, String query) throws SequenceSearchToolException {
        return search(uniqueToken, query, null);
    }


    
}
