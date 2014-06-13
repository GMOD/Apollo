package org.bbop.apollo.web.dataadapter.chado;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DB;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureDBXref;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureProperty;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;

public class ChadoIO {

	private HibernateHandler handler;
	private Organism organism;
	private Set<String> cvtermExcludeList;
	
	public ChadoIO(String hibernateConfig) throws Exception {
		handler = new HibernateHandler(hibernateConfig);
		cvtermExcludeList = new HashSet<String>();
	}

	public Feature getFeature(CVTerm type, String uniquename) {
		try {
			return handler.getFeature(organism, type, uniquename);
		} catch (SimpleObjectIOException e) {
			return null;
		}
	}
	
	public Iterator<? extends Feature> getFeatures(Feature sourceFeature, boolean nonAnalysisOnly) {
		try {
			return handler.getAllFeaturesBySourceFeature(sourceFeature, nonAnalysisOnly);
		} catch (SimpleObjectIOException e) {
			return null;
		}
	}
	
	public void setOrganism(String genus, String species) throws SimpleObjectIOException {
		organism = handler.getOrganism(genus, species);
	}
	
	public void setOrganism(Organism organism) {
		this.organism = organism;
	}
	
	public Organism getOrganism() {
		return organism;
	}
	
	public void addToCvtermExcludeList(String cvterm) {
		cvtermExcludeList.add(cvterm);
	}
	
	public void writeFeatures(Collection<Feature> features, Feature sourceFeature) throws SimpleObjectIOException {
		deleteFeatures(sourceFeature);
		handler.beginTransaction();
		for (Feature feature : features) {
			try {
				writeFeature(feature, sourceFeature);
			}
			catch (SimpleObjectIOException e) {
				handler.rollbackTransaction();
				throw e;
			}
		}
		handler.commitTransaction();
	}
	
	public void close() {
		handler.closeSession();
	}
	
	private Feature writeFeature(Feature feature, Feature sourceFeature) throws SimpleObjectIOException {
		if (feature.getType().getName().equals("CDS")) {
			feature.getType().setName("polypeptide");
		}
		CVTerm cvterm = handler.getCVTerm(feature.getType().getName(), feature.getType().getCv().getName());
		Feature cloneFeature = new Feature(feature);
		cloneFeature.setType(cvterm);
		cloneFeature.setOrganism(organism);
		cloneFeature.setTimeAccessioned(new Date(feature.getTimeAccessioned().getTime()));
		cloneFeature.setTimeLastModified(new Date(feature.getTimeLastModified().getTime()));
		handler.write(cloneFeature);
		for (FeatureProperty property : cloneFeature.getFeatureProperties()) {
			int idx = property.getValue().indexOf('=');
			if (idx != -1) {
				property.getType().setName(property.getValue().substring(0, idx));
				property.setValue(property.getValue().substring(idx + 1));
			}
			CVTerm propCvterm = null;
			try {
				propCvterm = handler.getCVTerm(property.getType().getName(), property.getType().getCv().getName());
			}
			catch (SimpleObjectIOException e) {
			}
			if (propCvterm == null) {
				DBXref dbxref = new DBXref(handler.getDB("SOFP"), property.getType().getName());
				dbxref.setVersion("");
				property.getType().setDbxref(dbxref);
				property.getType().setCv(handler.getCV(property.getType().getCv().getName()));
				handler.write(property.getType().getDbxref());
				handler.write(property.getType());
				propCvterm = handler.getCVTerm(property.getType().getName(), property.getType().getCv().getName());
			}
			FeatureProperty cloneProperty = new FeatureProperty(property);
			cloneProperty.setType(propCvterm);
			cloneProperty.setFeature(cloneFeature);
			handler.write(cloneProperty);
		}
		for (FeatureDBXref featureDbxref : cloneFeature.getFeatureDBXrefs()) {
			FeatureDBXref cloneFeatureDbxref = new FeatureDBXref(featureDbxref);
			DBXref cloneDbxref = handler.getDBXref(cloneFeatureDbxref.getDbxref().getDb().getName(), cloneFeatureDbxref.getDbxref().getAccession());
			if (cloneDbxref == null) {
				cloneDbxref = new DBXref(cloneFeatureDbxref.getDbxref());
				DB cloneDb = handler.getDB(cloneDbxref.getDb().getName());
				if (cloneDb == null) {
					cloneDb = new DB(cloneDbxref.getDb().getName());
					handler.write(cloneDb);
				}
				cloneDbxref.setDb(cloneDb);
				if (cloneDbxref.getVersion() == null) {
					cloneDbxref.setVersion("");
				}
				handler.write(cloneDbxref);
			}
			cloneFeatureDbxref.setDbxref(cloneDbxref);
			cloneFeatureDbxref.setFeature(cloneFeature);
			handler.write(cloneFeatureDbxref);
		}
		for (FeatureLocation location : cloneFeature.getFeatureLocations()) {
			FeatureLocation cloneLocation = new FeatureLocation(location);
			cloneLocation.setFeature(cloneFeature);
			cloneLocation.setSourceFeature(sourceFeature);
			handler.write(cloneLocation);
		}
		for (FeatureRelationship featureRelationship : cloneFeature.getChildFeatureRelationships()) {
			if (featureRelationship.getSubjectFeature().getType().getName().equals("CDS")) {
				featureRelationship.getType().setName("derives_from");
			}
			Feature subjectFeature = writeFeature(featureRelationship.getSubjectFeature(), sourceFeature);
			FeatureRelationship cloneFeatureRelationship = new FeatureRelationship(featureRelationship);
			CVTerm featureRelationshipCvTerm = handler.getCVTerm(featureRelationship.getType().getName(), featureRelationship.getType().getCv().getName());
			cloneFeatureRelationship.setType(featureRelationshipCvTerm);
			cloneFeatureRelationship.setSubjectFeature(subjectFeature);
			cloneFeatureRelationship.setObjectFeature(cloneFeature);
			handler.write(cloneFeatureRelationship);
		}
		return cloneFeature;
	}
	
	private void deleteFeatures(Feature sourceFeature) throws SimpleObjectIOException {
		handler.beginTransaction();
		for (Iterator<? extends Feature> featureIterator = handler.getAllFeaturesBySourceFeature(sourceFeature, true); featureIterator.hasNext();) {
			Feature feature = featureIterator.next();
			if (cvtermExcludeList.contains(feature.getType().toString())) {
				continue;
			}
			deleteFeature(feature);
		}
		handler.commitTransaction();
	}
	
	private void deleteFeature(Feature feature) throws SimpleObjectIOException {
		CVTerm cvterm = handler.getCVTerm(feature.getType().getName(), feature.getType().getCv().getName());
		Feature existingFeature = handler.getFeature(organism, cvterm, feature.getUniqueName());
		if (existingFeature != null) {
			handler.delete(existingFeature);
		}
	}

}