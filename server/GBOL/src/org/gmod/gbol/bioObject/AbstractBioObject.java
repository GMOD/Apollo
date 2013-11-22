package org.gmod.gbol.bioObject;

import java.io.Serializable;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

/** Abstract wrapper class from which all Bio objects inherit from.
 * 
 * @author elee
 *
 */

public abstract class AbstractBioObject implements Serializable {

	private static final long serialVersionUID = 1L;
	protected BioObjectConfiguration conf;
	
	/** Constructor.
	 * 
	 * @param conf - Configuration containing mapping information
	 */
	public AbstractBioObject(BioObjectConfiguration conf) {
		this.conf = conf;
	}
	
	/** Get the configuration used for this object.
	 * 
	 * @return Configuration used for this object
	 */
	public BioObjectConfiguration getConfiguration() {
		return conf;
	}
	
	/** Returns an iterator that contains the high level AbstractSimpleObjects corresponding to the
	 *  underlying AbstractSimpleObject.
	 *  
	 * @param conf - Configuration containing mapping information
	 * @return SimpleObjectIteratorInterface for iterating through high level AbstractSimpleObjects to be written
	 */
	public abstract AbstractSimpleObjectIterator getWriteableSimpleObjects(BioObjectConfiguration conf);
	
}
