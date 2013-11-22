package org.gmod.gbol.bioObject;

import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;

/**Abstract wrapper class that inherits from Region for Bio objects that have a single location.
 * 
 * @author elee
 *
 */

public abstract class AbstractSingleLocationBioObject extends Region {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf -  - Configuration containing mapping information
	 */
	public AbstractSingleLocationBioObject(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}

	/** Convenience method for retrieving the location.  Assumes that it only contains a single
	 *  location so it returns the first (and hopefully only) location from the collection of
	 *  locations.  Returns <code>null</code> if none are found.
	 *  
	 * @return FeatureLocation of this object
	 */
	public FeatureLocation getFeatureLocation() {
		Collection<FeatureLocation> locs = getFeatureLocations();
		if (locs != null) {
			Iterator<FeatureLocation> i = locs.iterator();
			if (i.hasNext()) {
				return i.next();
			}
		}
		return null;
	}
	
}
