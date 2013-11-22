package org.gmod.gbol.bioObject;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.conf.BioObjectConfigurationException;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;

/** Abstract class for all Bio objects that wrap a FeatureRelationship object.
 * 
 * @author elee
 *
 */
public abstract class AbstractBioFeatureRelationship extends AbstractBioObject {

	private static final long serialVersionUID = 1L;
	protected FeatureRelationship featureRelationship;
	
	/** Constructor.
	 * 
	 * @param fr - Feature object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public AbstractBioFeatureRelationship(FeatureRelationship fr, BioObjectConfiguration conf) {
		super(conf);
		this.featureRelationship = fr;
	}

	/** Get the AbstractBioFeature object for the subject in the relationship.  The AbstractBioFeature
	 *  object is generated on the fly every time this method is called.
	 * 
	 * @return AbstractBioFeature for the subject feature
	 */
	public AbstractBioFeature getSubjectFeature() {
		return (AbstractBioFeature)BioObjectUtil.createBioObject(featureRelationship.getSubjectFeature(), conf);
	}
	
	/** Get the AbstractBioFeature object for the object in the relationship.  The AbstractBioFeature
	 *  object is generated on the fly every time this method is called.
	 * 
	 * @return AbstractBioFeature for the object feature
	 */
	public AbstractBioFeature getObjectFeature() {
		return (AbstractBioFeature)BioObjectUtil.createBioObject(featureRelationship.getObjectFeature(), conf);
	}

	public SimpleObjectIterator getWriteableSimpleObjects(BioObjectConfiguration c) {
		return new SimpleObjectIterator(this, c);
	}
	
	protected FeatureRelationship translateSimpleObjectType(BioObjectConfiguration c) {
		FeatureRelationship clone = new FeatureRelationship(featureRelationship);
		String frClass = c.getClassForCVTerm(featureRelationship.getType());
		if (frClass == null) {
			String className =
				BioObjectUtil.stripPackageNameFromClassName(getClass().getName());
			CVTerm defaultCvTerm = c.getDefaultCVTermForClass(className);
			if (defaultCvTerm == null) {
				throw new BioObjectConfigurationException("No default set for " + className);
			}
			clone.setType(defaultCvTerm);
		}
		return clone;
	}
	
	private static class SimpleObjectIterator extends AbstractSimpleObjectIterator {
		private enum Status {
			featureRelationship,
			subject
		}

		private AbstractBioFeatureRelationship featureRelationship;
		private BioObjectConfiguration conf;
		private Status status;
		private AbstractSimpleObjectIterator subjectIter;
		private AbstractSimpleObjectIterator objectIter;
		
		public SimpleObjectIterator(AbstractBioFeatureRelationship featureRelationship,
				BioObjectConfiguration conf) {
			this.featureRelationship = featureRelationship;
			this.conf = conf;
			status = Status.featureRelationship;
		}
		
		public AbstractSimpleObject peek() {
			if (status == Status.featureRelationship) {
				subjectIter = featureRelationship.getSubjectFeature().getWriteableSimpleObjects(conf);
				objectIter = featureRelationship.getObjectFeature().getWriteableSimpleObjects(conf);
				FeatureRelationship fr = featureRelationship.translateSimpleObjectType(conf);
				fr.setSubjectFeature((Feature)subjectIter.peek());
				fr.setObjectFeature((Feature)objectIter.peek());
				current = fr;
				return fr;
			}
			return current;
		}

		public boolean hasNext() {
			if (status == Status.featureRelationship) {
				return true;
			}
			else if (status == Status.subject) {
				return subjectIter.hasNext();
			}
			return false;
		}

		public AbstractSimpleObject next() {
			AbstractSimpleObject retVal = null;
			if (status == Status.featureRelationship) {
				retVal = peek();
				status = Status.subject;
			}
			else if (status == Status.subject) {
				retVal = subjectIter.next();
			}
			current = retVal;
			return retVal;
		}
	}
}
