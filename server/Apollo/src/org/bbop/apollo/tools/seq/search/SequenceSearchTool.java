package org.bbop.apollo.tools.seq.search;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

public abstract class SequenceSearchTool {

	protected BioObjectConfiguration conf;
	
	public void setBioObjectConfiguration(BioObjectConfiguration conf) {
		this.conf = conf;
	}
	
	public void parseConfiguration(String configFileName) throws SequenceSearchToolException {
		try {
			parseConfiguration(new FileInputStream(configFileName));
		} catch (FileNotFoundException e) {
			throw new SequenceSearchToolException("Error reading config: " + e.getMessage());
		}
	}
	
	public abstract void parseConfiguration(InputStream config) throws SequenceSearchToolException;
	
	public abstract Collection<Match> search(String uniqueToken, String query, String databaseId) throws SequenceSearchToolException;
	
	public Collection<Match> search(String uniqueToken, String query) throws SequenceSearchToolException {
		return search(uniqueToken, query, null);
	}
	
}
