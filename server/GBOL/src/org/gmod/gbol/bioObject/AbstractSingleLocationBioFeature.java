package org.gmod.gbol.bioObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.util.SequenceUtil;

/**Abstract wrapper class that inherits from Region for Bio objects that have a single location.
 * 
 * @author elee
 *
 */

public abstract class AbstractSingleLocationBioFeature extends Region {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf -  - Configuration containing mapping information
	 */
	public AbstractSingleLocationBioFeature(Feature feature, BioObjectConfiguration conf) {
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

	/** Convenience method for setting the location.  Assumes that it only contains a single
	 *  location so the previous location will be removed.
	 *  
	 *  @param featureLocation - new FeatureLocation to set this gene to
	 */
	public void setFeatureLocation(FeatureLocation featureLocation) {
		Collection<FeatureLocation> locs = getFeatureLocations();
		if (locs != null) {
			locs.clear();
		}
		feature.addFeatureLocation(featureLocation);
	}
	
	/** Convenience method to set the location for a feature without having to create a
	 *  FeatureLocation object.  This method will take care of the underlying FeatureLocation
	 *  instantiation.  Sets the source for the feature location if it is not null.
	 *  
	 * @param fmin - leftmost/minimal boundary in the linear range represented by the featureloc
	 * @param fmax - rightmost/maximal boundary in the linear range represented by the featureloc
	 * @param strand - orientation/directionality of the location. Should be 0, -1 or +1. 
	 * @param source - source feature which this location is relative to
	 */
	public void setFeatureLocation(int fmin, int fmax, int strand, AbstractBioFeature source) {
		FeatureLocation loc = getFeatureLocation();
		if (loc == null) {
			loc = new FeatureLocation();
			setFeatureLocation(loc);
		}
		if (source != null) {
			loc.setSourceFeature(source.getFeature());
		}
		loc.setFmin(fmin);
		loc.setFmax(fmax);
		loc.setStrand(strand);
	}

	/** Convenience method to get the fmin for the location of this feature.
	 * 
	 * @return fmin of this feature.  Returns null if no FeatureLocation is set for this feature
	 */
	public Integer getFmin() {
		if (getFeatureLocation() == null) {
			return null;
		}
		return getFeatureLocation().getFmin();
	}
	
	/** Convenience method to set the fmin for the location of this feature.  Changes the
	 *  underlying FeatureLocation.fmin.
	 *  
	 * @param fmin - New fmin to set for this feature
	 */
	public void setFmin(Integer fmin) {
		getFeatureLocation().setFmin(fmin);
	}
	
	public void setSourceFeature(AbstractBioFeature sourceFeature) {
		FeatureLocation loc = getFeatureLocation();
		if (loc == null) {
			return;
		}
		loc.setSourceFeature(sourceFeature.getFeature());
	}
	
	/** Convenience method to check whether the fmin is partial for the location of this feature.
	 * 
	 * @return true if the fmin is partial.  Returns null if no FeatureLocation is set for this feature
	 */
	public Boolean isFminPartial() {
		if (getFeatureLocation() == null) {
			return null;
		}
		return getFeatureLocation().isIsFminPartial();
	}
	
	/** Convenience method to set whether the fmin is partial for the location of this feature.
	 * 
	 * @param fminPartial - Whether the fmin is partial
	 */
	public void setFminPartial(Boolean fminPartial) {
		getFeatureLocation().setIsFminPartial(fminPartial);
	}
	
	/** Convenience method to get the fmax for the location of this feature.
	 * 
	 * @return fmax of this feature.  Returns null if no FeatureLocation is set for this feature
	 */
	public Integer getFmax() {
		if (getFeatureLocation() == null) {
			return null;
		}
		return getFeatureLocation().getFmax();
	}

	/** Convenience method to set the fmax for the location of this feature.  Changes the
	 *  underlying FeatureLocation.fmax.
	 *  
	 * @param fmax - New fmax to set for this feature
	 */
	public void setFmax(Integer fmax) {
		getFeatureLocation().setFmax(fmax);
	}
	
	/** Convenience method to check whether the fmax is partial for the location of this feature.
	 * 
	 * @return true if the fmax is partial.  Returns null if no FeatureLocation is set for this feature
	 */
	public Boolean isFmaxPartial() {
		if (getFeatureLocation() == null) {
			return null;
		}
		return getFeatureLocation().isIsFmaxPartial();
	}
	
	/** Convenience method to set whether the fmax is partial for the location of this feature.
	 * 
	 * @param fmaxPartial - Whether the fmax is partial
	 */
	public void setFmaxPartial(Boolean fmaxPartial) {
		getFeatureLocation().setIsFmaxPartial(fmaxPartial);
	}
	
	/** Convenience method to get the strand for the location of this feature.
	 * 
	 * @return Strand of this feature.  Returns null if no FeatureLocation is set for this feature
	 */
	public Integer getStrand() {
		if (getFeatureLocation() == null) {
			return null;
		}
		return getFeatureLocation().getStrand();
	}
	
	/** Convenience method to set the strand for the location of this feature.
	 * 
	 * @param strand - New strand for this feature
	 */
	public void setStrand(Integer strand) {
		getFeatureLocation().setStrand(strand);
	}
	
	/** Checks whether this AbstractSimpleLocationBioFeature overlaps the comparison
	 * AbstractSimpleLocationBioFeature.
	 * 
	 * @param other - AbstractSimpleLocationBioFeature to check overlap against
	 * @return true is there is overlap
	 */
	public boolean overlaps(AbstractSingleLocationBioFeature other) {
		return overlaps(other.getFeatureLocation());
	}
	
	public boolean overlaps(AbstractSingleLocationBioFeature other, boolean compareStrands) {
		return overlaps(other.getFeatureLocation(), compareStrands);
	}

	/** Checks whether this AbstractSimpleLocationBioFeature overlaps the FeatureLocation.
	 * 
	 * @param location - FeatureLocation to check overlap against
	 * @return true is there is overlap
	 */
	public boolean overlaps(FeatureLocation location) {
		return overlaps(location, true);
	}

	public boolean overlaps(FeatureLocation location, boolean compareStrands) {
		if (getFeatureLocation().getSourceFeature() != location.getSourceFeature() &&
				!getFeatureLocation().getSourceFeature().equals(location.getSourceFeature())) {
			return false;
		}
		int thisFmin = getFeatureLocation().getFmin();
		int thisFmax = getFeatureLocation().getFmax();
		int thisStrand = getFeatureLocation().getStrand();
		int otherFmin = location.getFmin();
		int otherFmax = location.getFmax();
		int otherStrand = location.getStrand();
		boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
		if (strandsOverlap &&
				(thisFmin <= otherFmin && thisFmax > otherFmin ||
				thisFmin >= otherFmin && thisFmin < otherFmax)) {
			return true;
		}
		return false;
	}
	
	/** Checks whether this AbstractSimpleLocationBioFeature is adjacent to the comparison
	 * AbstractSimpleLocationBioFeature.
	 * 
	 * @param other - AbstractSimpleLocationBioFeature to check adjacency against
	 * @return true if there is adjacency
	 */
	public boolean isAdjacentTo(AbstractSingleLocationBioFeature other) {
		return isAdjacentTo(other.getFeatureLocation());
	}
	
	public boolean isAdjancentTo(AbstractSingleLocationBioFeature other, boolean compareStrands) {
		return isAdjacentTo(other.getFeatureLocation(), compareStrands);
	}

	/** Checks whether this AbstractSimpleLocationBioFeature is adjacent to the FeatureLocation.
	 * 
	 * @param location - FeatureLocation to check adjacency against
	 * @return true if there is adjacency
	 */
	public boolean isAdjacentTo(FeatureLocation location) {
		return isAdjacentTo(location, true);
	}

	public boolean isAdjacentTo(FeatureLocation location, boolean compareStrands) {
		if (getFeatureLocation().getSourceFeature() != location.getSourceFeature() &&
				!getFeatureLocation().getSourceFeature().equals(location.getSourceFeature())) {
			return false;
		}
		int thisFmin = getFeatureLocation().getFmin();
		int thisFmax = getFeatureLocation().getFmax();
		int thisStrand = getFeatureLocation().getStrand();
		int otherFmin = location.getFmin();
		int otherFmax = location.getFmax();
		int otherStrand = location.getStrand();
		boolean strandsOverlap = compareStrands ? thisStrand == otherStrand : true;
		if (strandsOverlap &&
				(thisFmax == otherFmin ||
				thisFmin == otherFmax)) {
			return true;
		}
		return false;
	}

	
	/** Get the underlying sequence associated with this feature.  Return this sequence's residues
	 *  if it is set, otherwise return the source's sequence given it's feature location.
	 *  Reverse complements the sequence if this feature is on the minus strand.
	 * 
	 * @return String representing sequence associated with this feature.  Returns null if there's no sequence
	 */
	public String getResidues() {
		if (feature.getResidues() != null) {
			return feature.getResidues();
		}
		if (getFeatureLocation() == null || getFeatureLocation().getSourceFeature() == null) {
			return null;
		}
		String sequence = getFeatureLocation().getSourceFeature().getResidues(getFeatureLocation().getFmin(), getFeatureLocation().getFmax());
		if (sequence != null) {
			if (getFeatureLocation().getStrand() == -1) {
				sequence = SequenceUtil.reverseComplementSequence(sequence);
			}
			return sequence;
		}
		return null;
	}
	
	/** Convert local coordinate to source feature coordinate.
	 * 
	 * @param localCoordinate - Coordinate to convert to source coordinate
	 * @return Source feature coordinate, -1 if local coordinate is longer than feature's length or negative
	 */
	public int convertLocalCoordinateToSourceCoordinate(int localCoordinate) {
		if (localCoordinate < 0 || localCoordinate > getLength()) {
			return -1;
		}
		if (getFeatureLocation().getStrand() == -1) {
			return getFeatureLocation().getFmax() - localCoordinate - 1;
		}
		else {
			return getFeatureLocation().getFmin() + localCoordinate;
		}
	}
	
	/** Convert source feature coordinate to local coordinate.
	 * 
	 * @param sourceCoordinate - Coordinate to convert to local coordinate
	 * @return Local coordinate, -1 if source coordinate is <= fmin or >= fmax
	 */
	public int convertSourceCoordinateToLocalCoordinate(int sourceCoordinate) {
		if (sourceCoordinate < getFeatureLocation().getFmin() || sourceCoordinate > getFeatureLocation().getFmax()) {
			return -1;
		}
		if (getFeatureLocation().getStrand() == -1) {
			return getFeatureLocation().getFmax() - 1 - sourceCoordinate;
		}
		else {
			return sourceCoordinate - getFeatureLocation().getFmin();
		}
	}
	
	/** Get the length of this feature.
	 * 
	 * @return Length of feature
	 */
	public int getLength() {
		return getFeatureLocation().getFmax() - getFeatureLocation().getFmin();
	}

	/** Get the children for this feature.  Returns an empty collection if there are no children.
	 *  Wrapper classes for the children are generated on the fly.
	 *  
	 * @return Collection of AbstractSingleLocationBioFeature objects for the children
	 */
	public Collection<? extends AbstractSingleLocationBioFeature> getChildren() {
		Collection<AbstractSingleLocationBioFeature> children = new ArrayList<AbstractSingleLocationBioFeature>();
		for (FeatureRelationship fr : feature.getChildFeatureRelationships()) {
			children.add((AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(fr.getSubjectFeature(), getConfiguration()));
		}
		return children;
	}

	/** Get the parents for this feature.  Returns an empty collection if there are no parents.
	 *  Wrapper classes for the parents are generated on the fly.
	 *  
	 * @return Collection of AbstractSingleLocationBioFeature objects for the children
	 */
	public Collection<? extends AbstractSingleLocationBioFeature> getParents() {
		Collection<AbstractSingleLocationBioFeature> parents = new ArrayList<AbstractSingleLocationBioFeature>();
		for (FeatureRelationship fr : feature.getParentFeatureRelationships()) {
			parents.add((AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(fr.getObjectFeature(), getConfiguration()));
		}
		return parents;
	}

	public String toString() {
		if (getFeatureLocation() == null) {
			return super.toString();
		}
		return super.toString() + String.format(" [%s%d, %d%s, %d, %s]",
				getFeatureLocation().isIsFminPartial() ? "<" : "", getFeatureLocation().getFmin(),
				getFeatureLocation().getFmax(), getFeatureLocation().isIsFmaxPartial() ? ">" : "",
				getFeatureLocation().getStrand(),
				getFeatureLocation().getSourceFeature() != null ? getFeatureLocation().getSourceFeature().getUniqueName() : "not_set");
	}
	
	public AbstractSingleLocationBioFeature cloneFeature(String uniqueName) {
		try {
			return getClass().getConstructor(getClass(), String.class).newInstance(this, uniqueName);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
