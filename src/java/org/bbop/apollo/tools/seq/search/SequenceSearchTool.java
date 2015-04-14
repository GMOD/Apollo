package org.bbop.apollo.tools.seq.search;

import java.util.Collection;
import org.codehaus.groovy.grails.web.json.JSONObject;

import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

public abstract class SequenceSearchTool {

    protected BioObjectConfiguration conf;
    
    public void setBioObjectConfiguration(BioObjectConfiguration conf) {
        this.conf = conf;
    }
    
    public abstract void parseConfiguration(JSONObject config) throws SequenceSearchToolException;

    public abstract Collection<Match> search(String uniqueToken, String query, String databaseId) throws SequenceSearchToolException;
    
    public Collection<Match> search(String uniqueToken, String query) throws SequenceSearchToolException {
        return search(uniqueToken, query, null);
    }
    
}
