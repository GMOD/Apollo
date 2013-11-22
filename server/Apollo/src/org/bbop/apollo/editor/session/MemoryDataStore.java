package org.bbop.apollo.editor.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;
import org.gmod.gbol.bioObject.SequenceAlteration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.FeatureLocation;

public class MemoryDataStore implements DataStore {

	private List<SequenceAlteration> sequenceAlterations;
	private List<AbstractSingleLocationBioFeature> features;
	private Map<String, AbstractSingleLocationBioFeature> uniqueNameToFeature;
	
	public MemoryDataStore() {
		sequenceAlterations = Collections.synchronizedList(new ArrayList<SequenceAlteration>());
		features = Collections.synchronizedList(new LinkedList<AbstractSingleLocationBioFeature>());
		uniqueNameToFeature = new ConcurrentHashMap<String, AbstractSingleLocationBioFeature>();
	}
	
	public List<AbstractSingleLocationBioFeature> getFeatures() {
		return features;
	}

	public List<SequenceAlteration> getSequenceAlterations() {
		return sequenceAlterations;
	}
	
	public void addSequenceAlteration(SequenceAlteration sequenceAlteration) {
		indexFeature(sequenceAlteration);
		sequenceAlterations.add(sequenceAlteration);
	}
	
	public void deleteSequenceAlteration(SequenceAlteration sequenceAlteration) {
		unindexFeature(sequenceAlteration);
		sequenceAlterations.remove(sequenceAlteration);
	}

	public void addFeature(AbstractSingleLocationBioFeature feature) {
		if (uniqueNameToFeature.containsKey(feature.getUniqueName())) {
			deleteFeature(uniqueNameToFeature.get(feature.getUniqueName()));
		}
		features.add(feature);
		Collections.sort(features, new BioObjectUtil.FeaturePositionComparator<AbstractSingleLocationBioFeature>(false));
		indexFeature(feature);
	}
	
	public void deleteFeature(AbstractSingleLocationBioFeature feature) {
		BioObjectUtil.FeaturePositionComparator<AbstractSingleLocationBioFeature> comparator = new BioObjectUtil.FeaturePositionComparator<AbstractSingleLocationBioFeature>(false);
		int index = Collections.binarySearch(features, feature, comparator);
		if (index >= 0) {
			while (index > 0) {
				AbstractSingleLocationBioFeature indexedFeature = features.get(index - 1);
				if (comparator.compare(indexedFeature, feature) == 0) {
					--index;
				}
				else {
					break;
				}
			}
			while (index < features.size()) {
				AbstractSingleLocationBioFeature indexedFeature = features.get(index);
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

	public AbstractSingleLocationBioFeature getFeatureByUniqueName(String uniqueName) {
		return uniqueNameToFeature.get(uniqueName);
	}
	
	public Collection<AbstractSingleLocationBioFeature> getOverlappingFeatures(FeatureLocation location, boolean compareStrands) {
		LinkedList<AbstractSingleLocationBioFeature> overlappingFeatures =
			new LinkedList<AbstractSingleLocationBioFeature>();
		int low = 0;
		int high = features.size() - 1;
		int index = -1;
		while (low <= high) {
			int mid = low + ((high - low) / 2);
			AbstractSingleLocationBioFeature feature = features.get(mid);
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
				AbstractSingleLocationBioFeature feature = features.get(i);
				if (feature.overlaps(location, compareStrands)) {
					overlappingFeatures.addFirst(feature);
				}
				else {
					break;
				}
			}
			for (int i = index + 1; i < features.size(); ++i) {
				AbstractSingleLocationBioFeature feature = features.get(i);
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
	
	public void indexFeature(AbstractSingleLocationBioFeature feature) {
		uniqueNameToFeature.put(feature.getUniqueName(), feature);
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			indexFeature(child);
		}
	}

	public void unindexFeature(AbstractSingleLocationBioFeature feature) {
		uniqueNameToFeature.remove(feature.getUniqueName());
		for (AbstractSingleLocationBioFeature child : feature.getChildren()) {
			unindexFeature(child);
		}
	}
	
	public void beginTransactionForFeature(AbstractSingleLocationBioFeature feature) {
	}
	
	public void endTransactionForFeature(AbstractSingleLocationBioFeature feature) {
	}

	public void addToStore(AbstractSingleLocationBioFeature feature) {
		// TODO Auto-generated method stub
		
	}

	public void removeFromStore(AbstractSingleLocationBioFeature feature) {
		// TODO Auto-generated method stub
		
	}

	public void endTransactionForAllFeatures() {
		// TODO Auto-generated method stub
		
	}

}
