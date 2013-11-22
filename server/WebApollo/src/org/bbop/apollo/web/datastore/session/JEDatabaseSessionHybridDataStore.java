package org.bbop.apollo.web.datastore.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bbop.apollo.editor.session.DataStore;
import org.bbop.apollo.web.datastore.JEDatabase;
import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;

public class JEDatabaseSessionHybridDataStore implements DataStore {

	private JEDatabase db;
	private BioObjectConfiguration conf;
	private Map<String, String> uniqueNameToStoredUniqueName;
	private List<FeatureData> features;
	private Feature sourceFeature;
	private Map<String, AbstractSingleLocationBioFeature> dirtyFeatures;
	
	public JEDatabaseSessionHybridDataStore(JEDatabase db, BioObjectConfiguration conf, Feature sourceFeature) {
		this.db = db;
		this.conf = conf;
		this.sourceFeature = sourceFeature;
		uniqueNameToStoredUniqueName = new HashMap<String, String>();
		features = new LinkedList<FeatureData>();
		dirtyFeatures = new HashMap<String, AbstractSingleLocationBioFeature>();
		processFeatureIterator(db.getFeatureIterator());
		processFeatureIterator(db.getSequenceAlterationIterator());
	}
	
	private void processFeatureIterator(Iterator<Feature> iter) {
		while (iter.hasNext()) {
			AbstractSingleLocationBioFeature feature = (AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(iter.next(), conf);
			indexFeature(feature, false);
			features.add(new FeatureData(feature));
			Collections.sort(features, new FeatureDataPositionComparator());
		}
	}
	
	@Override
	public List<AbstractSingleLocationBioFeature> getFeatures() {
		List<AbstractSingleLocationBioFeature> features = new ArrayList<AbstractSingleLocationBioFeature>();
		Iterator<Feature> iter = db.getFeatureIterator();
		while (iter.hasNext()) {
			Feature feature = iter.next();
			AbstractSingleLocationBioFeature gbolFeature = (AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(feature, conf);
			addSourceToFeature(gbolFeature, sourceFeature);
			features.add(gbolFeature);
		}
		return features;
	}

	@Override
	public List<SequenceAlteration> getSequenceAlterations() {
		List<SequenceAlteration> sequenceAlterations = new ArrayList<SequenceAlteration>();
		Iterator<Feature> iter = db.getSequenceAlterationIterator();
		while (iter.hasNext()) {
			Feature feature = iter.next();
			SequenceAlteration gbolFeature = (SequenceAlteration)BioObjectUtil.createBioObject(feature, conf);
			addSourceToFeature(gbolFeature, sourceFeature);
			sequenceAlterations.add(gbolFeature);
		}
		return sequenceAlterations;
	}

	@Override
	public AbstractSingleLocationBioFeature getFeatureByUniqueName(String uniqueName) {
		String storedUniqueName = uniqueNameToStoredUniqueName.get(uniqueName);
		if (storedUniqueName == null) {
			return null;
		}
		if (!dirtyFeatures.containsKey(storedUniqueName)) {
			Feature gsolFeature = db.getFeatureByUniqueName(storedUniqueName);
			if (gsolFeature == null) {
				gsolFeature = db.getSequenceAlterationByUniqueName(storedUniqueName);
				if (gsolFeature == null) {
					return null;
				}
			}
			AbstractSingleLocationBioFeature feature = findFeature((AbstractSingleLocationBioFeature)BioObjectUtil.createBioObject(gsolFeature, conf), uniqueName);
			if (feature != null) {
				addSourceToFeature(getTopLevelFeature(feature), sourceFeature);
			}
			beginTransactionForFeature(feature);
			return feature;
		}
		else {
			AbstractSingleLocationBioFeature feature = findFeature(dirtyFeatures.get(storedUniqueName), uniqueName);
			return feature;
		}
	}

	@Override
	public void addSequenceAlteration(SequenceAlteration sequenceAlteration) {
		indexFeature(sequenceAlteration);
	}

	@Override
	public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration) {
		unindexFeature(sequenceAlteration);
	}

	@Override
	public void addFeature(AbstractSingleLocationBioFeature feature) {
		if (uniqueNameToStoredUniqueName.containsKey(feature.getUniqueName())) {
			deleteFeature(feature);
		}
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		features.add(new FeatureData(topLevelFeature));
		if (getFeatureByUniqueName(topLevelFeature.getUniqueName()) == null) {
			beginTransactionForFeature(topLevelFeature);
		}
		indexFeature(topLevelFeature);
//		Collections.sort(features, new FeatureDataPositionComparator());
	}

	@Override
	public void deleteFeature(AbstractSingleLocationBioFeature feature) {
		FeatureDataPositionComparator comparator = new FeatureDataPositionComparator();
		int index = Collections.binarySearch(features, new FeatureData(feature), comparator);
		if (index >= 0) {
			while (index > 0) {
				AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index - 1).getUniqueName());
				if (comparator.compare(indexedFeature, feature) == 0) {
					--index;
				}
				else {
					break;
				}
			}
			while (index < features.size()) {
				AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index).getUniqueName());
				if (comparator.compare(indexedFeature, feature) != 0) {
					break;
				}
				if (indexedFeature.equals(feature)) {
					features.remove(index);
					unindexFeature(feature);
					break;
				}
				++index;
			}
		}
	}

	@Override
	public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(FeatureLocation location, boolean compareStrands) {
		LinkedList<AbstractSingleLocationBioFeature> overlappingFeatures =
				new LinkedList<AbstractSingleLocationBioFeature>();
		int low = 0;
		int high = features.size() - 1;
		int index = -1;
		while (low <= high) {
			int mid = low + ((high - low) / 2);
			AbstractSingleLocationBioFeature feature = getFeatureByUniqueName(features.get(mid).getUniqueName());
			if (feature == null) {
				uniqueNameToStoredUniqueName.remove(features.get(mid).getUniqueName());
				features.remove(mid);
				return getOverlappingFeatures(location, compareStrands);
			}
			if (feature.overlaps(location, compareStrands)) {
				index = mid;
				break;
			}
			else if (feature.getFeatureLocation().getFmin() < location.getFmin()) {
				low = mid + 1;
			}
			else {
				high = mid - 1;
			}
		}
		if (index >= -1) {
			for (int i = index; i >= 0; --i) {
				AbstractSingleLocationBioFeature feature = getFeatureByUniqueName(features.get(i).getUniqueName());
				if (feature == null) {
					uniqueNameToStoredUniqueName.remove(features.get(i).getUniqueName());
					features.remove(i);
					return getOverlappingFeatures(location, compareStrands);
				}
				if (feature.overlaps(location, compareStrands)) {
					overlappingFeatures.addFirst(feature);
				}
				else {
					break;
				}
			}
			for (int i = index + 1; i < features.size(); ++i) {
				AbstractSingleLocationBioFeature feature = getFeatureByUniqueName(features.get(i).getUniqueName());
				if (feature == null) {
					uniqueNameToStoredUniqueName.remove(features.get(i).getUniqueName());
					features.remove(i);
					return getOverlappingFeatures(location, compareStrands);
				}
				if (feature.overlaps(location, compareStrands)) {
					overlappingFeatures.addLast(feature);
				}
				else {
					break;
				}
			}
		}
		return overlappingFeatures;
	}
	
	private int getFeatureIndexByUniqueName(AbstractSingleLocationBioFeature feature) {
		FeatureDataPositionComparator comparator = new FeatureDataPositionComparator();
		int index = Collections.binarySearch(features, new FeatureData(feature), comparator);
		if (index >= 0) {
			while (index > 0) {
				AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index - 1).getUniqueName());
				if (comparator.compare(indexedFeature, feature) == 0) {
					--index;
				}
				else {
					break;
				}
			}
			while (index < features.size()) {
				AbstractSingleLocationBioFeature indexedFeature = getFeatureByUniqueName(features.get(index).getUniqueName());
				if (comparator.compare(indexedFeature, feature) != 0) {
					break;
				}
				if (indexedFeature.getUniqueName().equals(feature.getUniqueName())) {
					return index;
				}
				++index;
			}
		}
		return -1;
	}

	@Override
	public void indexFeature(AbstractSingleLocationBioFeature feature) {
		indexFeature(feature, true);
	}

	private void indexFeature(AbstractSingleLocationBioFeature feature, boolean needUpdate) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		uniqueNameToStoredUniqueName.put(feature.getUniqueName(), topLevelFeature.getUniqueName());

		if (needUpdate) {
			updateTopLevelFeatureBoundaries(feature);
		}
		
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			indexFeature(child, false);
		}
	}
	
	@Override
	public void unindexFeature(AbstractSingleLocationBioFeature feature) {
		uniqueNameToStoredUniqueName.remove(feature.getUniqueName());
		
//		System.out.println("unindexing :" + feature.getUniqueName());
		
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			unindexFeature(child);
		}
		
		/*
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		if (feature.equals(topLevelFeature)) {
			db.deleteFeature(topLevelFeature);
		}
		else {
			db.writeFeature(topLevelFeature);
		}
		*/
	}
	
	private void updateTopLevelFeatureBoundaries(AbstractSingleLocationBioFeature feature) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		int index = getFeatureIndexByUniqueName(topLevelFeature);
		boolean needSort = false;
		if (index == -1) {
			features.add(new FeatureData(getTopLevelFeature(topLevelFeature)));
			needSort = true;
		}
		else {
			FeatureData storedData = features.get(index);
			if (!(topLevelFeature.getFmin().equals(storedData.getFmin())) || !(topLevelFeature.getFmax().equals(storedData.getFmax()))) {
				storedData.setFmin(topLevelFeature.getFmin());
				storedData.setFmax(topLevelFeature.getFmax());
				storedData.setLength(topLevelFeature.getLength());
				needSort = true;
			}
		}
		if (needSort) {
			Collections.sort(features, new FeatureDataPositionComparator());
		}
		
		System.out.println("updateTopLevelFeatureBoundaries");
		for (FeatureData f : features) {
			System.out.println(f);
		}
		System.out.println();
		
	}
	
	public void beginTransactionForFeature(AbstractSingleLocationBioFeature feature) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		if (!dirtyFeatures.containsKey(topLevelFeature.getUniqueName())) {
			dirtyFeatures.put(topLevelFeature.getUniqueName(), topLevelFeature);
		}
//		System.out.println("beginTransactionForFeature: " + dirtyFeatures.size() + " [" + topLevelFeature.getUniqueName() + "]");
	}
	
	public void endTransactionForFeature(AbstractSingleLocationBioFeature feature) {
		AbstractSingleLocationBioFeature topLevelFeature = getTopLevelFeature(feature);
		dirtyFeatures.remove(topLevelFeature.getUniqueName());
//		System.out.println("endTransactionForFeature: " + dirtyFeatures.size() + " [" + topLevelFeature.getUniqueName() + "]");
	}

	public void endTransactionForAllFeatures() {
		dirtyFeatures.clear();
//		System.out.println("endTransactionForAllFeatures: " + dirtyFeatures.size());
	}
	
	private AbstractSingleLocationBioFeature getTopLevelFeature(AbstractSingleLocationBioFeature feature) {
		Collection<? extends AbstractSingleLocationBioFeature> parents = feature.getParents();
		if (parents.size() > 0) {
			return getTopLevelFeature(parents.iterator().next());
		}
		else {
			return feature;
		}
	}
	
	private AbstractSingleLocationBioFeature findFeature(AbstractSingleLocationBioFeature feature, String uniqueName) {
		if (feature.getUniqueName().equals(uniqueName)) {
			return feature;
		}
		else {
			Collection<? extends AbstractSingleLocationBioFeature> children = feature.getChildren();
			if (children.size() > 0) {
				for (AbstractSingleLocationBioFeature child : children) {
					AbstractSingleLocationBioFeature c = findFeature(child, uniqueName);
					if (c != null) {
						return c;
					}
				}
			}
		}
		return null;
	}
	
	private void addSourceToFeature(AbstractSingleLocationBioFeature feature, Feature sourceFeature) {
		if (feature.getFeatureLocations().size() > 0) {
			feature.getFeatureLocations().iterator().next().setSourceFeature(sourceFeature);
		}
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			addSourceToFeature(child, sourceFeature);
		}
	}
	
	private class FeatureDataPositionComparator implements Comparator<FeatureData> {

		public FeatureDataPositionComparator() {
		}
		
		public int compare(FeatureData feature1, FeatureData feature2) {
			if (feature1.getUniqueName().equals(feature2.getUniqueName())) {
				return 0;
			}
			int retVal = 0;
			if (feature1.getFmin() < feature2.getFmin()) {
				retVal = -1;
			}
			else if (feature1.getFmin() > feature2.getFmin()) {
				retVal = 1;
			}
			else if (feature1.getFmax() < feature2.getFmax()) {
				retVal = -1;
			}
			else if (feature1.getFmax() > feature2.getFmax()) {
				retVal = 1;
			}
			else if (feature1.getLength() != feature2.getLength()) {
				retVal = feature1.getLength() < feature2.getLength() ? -1 : 1;
			}
			return retVal;
		}
		
		public int compare(AbstractSingleLocationBioFeature feature1, AbstractSingleLocationBioFeature feature2) {
			return compare(new FeatureData(feature1), new FeatureData(feature2));
		}
	}
	
	private class FeatureData {

		private String uniqueName;
		private int fmin;
		private int fmax;
		private int length;
		
		public FeatureData(AbstractSingleLocationBioFeature feature) {
			this(feature.getUniqueName(), feature.getFmin(), feature.getFmax(), feature.getLength());
		}
		
		public FeatureData(String uniqueName, int fmin, int fmax, int length) {
			this.uniqueName = uniqueName;
			this.fmin = fmin;
			this.fmax = fmax;
			this.length = length;
		}

		public String getUniqueName() {
			return uniqueName;
		}

		public int getFmin() {
			return fmin;
		}

		public int getFmax() {
			return fmax;
		}

		public int getLength() {
			return length;
		}
		
		public void setFmin(int fmin) {
			this.fmin = fmin;
		}

		public void setFmax(int fmax) {
			this.fmax = fmax;
		}

		public void setLength(int length) {
			this.length = length;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof FeatureData)) {
				return false;
			}
			FeatureData other = (FeatureData)o;
			return getUniqueName().equals(other.getUniqueName()) && getFmin() == other.getFmin() && getFmax() == other.getFmax() && getLength() == other.getLength();
		}
		
		@Override
		public String toString() {
			return String.format("%s [%d %d]", getUniqueName(), getFmin(), getFmax());
		}
	}

	@Override
	public void addToStore(AbstractSingleLocationBioFeature feature) {
		db.writeFeature(feature);		
	}

	@Override
	public void removeFromStore(AbstractSingleLocationBioFeature feature) {
		db.deleteFeature(feature);
	}
	
}
