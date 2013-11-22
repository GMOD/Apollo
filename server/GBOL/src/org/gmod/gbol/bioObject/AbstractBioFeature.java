package org.gmod.gbol.bioObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.conf.BioObjectConfigurationException;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DB;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureDBXref;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureProperty;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.FeatureSynonym;
import org.gmod.gbol.simpleObject.Organism;

/** Abstract class for all Bio objects that wrap a Feature object.
 * 
 * @author elee
 *
 */
public abstract class AbstractBioFeature extends AbstractBioObject {

//	private static final long serialVersionUID = -1714347943267242986L;
	private static final long serialVersionUID = 1L;
	protected Feature feature;
	
	/** Constructor.
	 * 
	 * @param feature - Feature object that this object will wrap
	 * @param conf - Configuration containing mapping information
	 */
	public AbstractBioFeature(Feature feature, BioObjectConfiguration conf)	{
		super(conf);
		this.feature = feature;
	}
	
	/** Get all feature locations associated with this feature.
	 * 
	 * @return Collection of feature locations associated with this feature
	 */
	public Collection<FeatureLocation> getFeatureLocations() {
		return feature.getFeatureLocations();
	}
	
	/** Get the name of this feature.
	 * 
	 * @return Name of this feature
	 */
	public String getName() {
		return feature.getName();
	}

	/** Set the name of this feature.
	 * 
	 * @param name - Name for this feature
	 */
	public void setName(String name) {
		feature.setName(name);
	}
	
	/** Get the unique name of this feature.
	 * 
	 * @return Uniquename of this feature
	 */
	public String getUniqueName() {
		return feature.getUniqueName();
	}
	
	/** Set the uniquename of this feature.
	 * 
	 * @param uniqueName - Uniquename for this feature
	 */
	public void setUniqueName(String uniqueName) {
		feature.setUniqueName(uniqueName);
	}

	/** Get the synonyms of this feature.
	 * @return 
	 * 
	 * @return synonyms - Synonyms for this feature
	 */
	public Set<FeatureSynonym> getSynonyms() {
		return feature.getFeatureSynonyms();
	}
	
	/** Set the synonyms of this feature.
	 * 
	 * @param synonyms - Synonyms for this feature
	 */
	public void setSynonyms(Set<FeatureSynonym> synonyms) {
		feature.setFeatureSynonyms(synonyms);
	}
	
	/** Get the organism for this feature.
	 * 
	 * @return Organism for this feature
	 */
	public Organism getOrganism() {
		return feature.getOrganism();
	}
	
	/** Set the organism for this feature.
	 * 
	 * @param organism - Organism for this feature
	 */
	public void setOrganism(Organism organism) {
		feature.setOrganism(organism);
	}

	/** Whether this feature is for an analysis.
	 * 
	 * @return true if this feature is from an analysis
	 */
	public boolean isAnalysis() {
		return feature.isIsAnalysis();
	}
	
	/** Set whether this feature is for an analysis.
	 * 
	 * @param isAnalysis - true if this feature is from an analysis
	 */
	public void setIsAnalysis(boolean isAnalysis) {
		feature.setIsAnalysis(isAnalysis);
	}
	
	/** Whether this feature is obsolete.
	 * 
	 * @return true if this feature is obsolete
	 */
	public boolean isObsolete() {
		return feature.isIsObsolete();
	}
	
	/** Set whether this feature is obsolete.
	 * 
	 * @param isObsolete - true if this feature is obsolete
	 */
	public void setIsObsolete(boolean isObsolete) {
		feature.setIsObsolete(isObsolete);
	}
	
	/** Get the residues for this feature.
	 * 
	 * @return Residues for this feature.
	 */
	public String getResidues() {
		return feature.getResidues();
	}
	
	/** Set the residues for this feature.
	 * 
	 * @param residues - Residues for this feature
	 */
	public void setResidues(String residues) {
		feature.setResidues(residues);
	}
	
	/** Add a property to this feature.
	 * 
	 * @param property - AbstractBioFeatureProperty to be added to this feature
	 */
	public void addProperty(AbstractBioFeatureProperty property) {
		int rank = 0;
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (fp.getType().equals(property.getFeatureProperty().getType())) {
				if (fp.getRank() > rank) {
					rank = fp.getRank();
				}
			}
		}
		property.getFeatureProperty().setRank(rank + 1);
		boolean ok = feature.getFeatureProperties().add(property.getFeatureProperty());
	}
	
	public void deleteProperty(AbstractBioFeatureProperty property) {
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (fp.getType().equals(property.getFeatureProperty().getType()) && fp.getValue().equals(property.getFeatureProperty().getValue())) {
				feature.getFeatureProperties().remove(fp);
				return;
			}
		}
	}

	
	/** Get the owner of this feature.
	 * 
	 * @return Owner of this feature
	 */
	public Owner getOwner() {
		Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("Owner");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (ownerCvterms.contains(fp.getType())) {
				return new Owner(fp, conf);
			}
		}
		return null;
	}
	
	/** Set the owner of this feature.
	 * 
	 * @param owner - Owner of this feature
	 */
	public void setOwner(Owner owner) {
		Collection<CVTerm> ownerCvterms = conf.getCVTermsForClass("Owner");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (ownerCvterms.contains(fp.getType())) {
				feature.getFeatureProperties().remove(fp);
				break;
			}
		}
		addProperty(owner);
	}

	/** Set the owner of this feature.
	 * 
	 * @param owner - Owner of this feature
	 */
	public void setOwner(String owner) {
		setOwner(new Owner(this, owner, conf));
	}
	
	/** Get comments for this feature.
	 * 
	 * @return Comments for this feature
	 */
	public Collection<Comment> getComments() {
		Collection<CVTerm> commentCvterms = conf.getCVTermsForClass("Comment");
		List<Comment> comments = new ArrayList<Comment>();
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (commentCvterms.contains(fp.getType())) {
				comments.add(new Comment(fp, conf));
			}
		}
		Collections.sort(comments, new Comparator<Comment>() {
			@Override
			public int compare(Comment comment1, Comment comment2) {
				if (comment1.getFeatureProperty().getType().equals(comment2.getFeatureProperty().getType())) {
					return new Integer(comment1.getFeatureProperty().getRank()).compareTo(comment2.getFeatureProperty().getRank());
				}
				return new Integer(comment1.getFeatureProperty().hashCode()).compareTo(comment2.getFeatureProperty().hashCode());
			}
		});
		return comments;
	}
	
	/** Add a comment to this feature.
	 * 
	 * @param comment - Comment to be added
	 */
	public void addComment(Comment comment) {
		addProperty(comment);
	}
	
	/** Add a comment to this feature.
	 * 
	 * @param comment - Comment to be added
	 */
	public void addComment(String comment) {
		addComment(new Comment(this, comment, conf));
	}

	/** Delete a comment from this feature.
	 * 
	 * @param comment - Comment to be deleted
	 */
	public void deleteComment(Comment comment) {
		Collection<CVTerm> commentCvterms = conf.getCVTermsForClass("Comment");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (commentCvterms.contains(fp.getType()) && fp.getValue().equals(comment.getComment())) {
				feature.getFeatureProperties().remove(fp);
				return;
			}
		}

	}
	
	/** Delete a comment from this feature.
	 * 
	 * @param comment - Comment to be deleted
	 */
	public void deleteComment(String comment) {
		deleteComment(new Comment(this, comment, conf));
	}
	
	/** Get the description for this feature.
	 * 
	 * @return Description for this feature
	 */
	public Description getDescription() {
		Collection<CVTerm> descriptionCvterms = conf.getCVTermsForClass("Description");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (descriptionCvterms.contains(fp.getType())) {
				return new Description(fp, conf);
			}
		}
		return null;
	}	

	/** Set the description for this feature.
	 * 
	 * @param description - Description to set for the feature
	 */
	public void setDescription(String description) {
		Description desc = getDescription();
		if (desc == null) {
			addProperty(new Description(this, description, conf));
		}
		else {
			desc.setDescription(description);
		}
	}

	/** Get the symbol for this feature.
	 * 
	 * @return Symbol for this feature
	 */
	public Symbol getSymbol() {
		Collection<CVTerm> symbolCvterms = conf.getCVTermsForClass("Symbol");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (symbolCvterms.contains(fp.getType())) {
				return new Symbol(fp, conf);
			}
		}
		return null;
	}	

	/** Set the symbol for this feature.
	 * 
	 * @param symbol - Symbol to set for the feature
	 */
	public void setSymbol(String symbol) {
		Symbol sym = getSymbol();
		if (sym == null) {
			addProperty(new Symbol(this, symbol, conf));
		}
		else {
			sym.setSymbol(symbol);
		}
	}
	
	/** Get the status for this feature.
	 * 
	 * @return Status for this feature
	 */
	public Status getStatus() {
		Collection<CVTerm> statusCvterms = conf.getCVTermsForClass("Status");
		
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (statusCvterms.contains(fp.getType())) {
				return new Status(fp, conf);
			}
		}
		return null;
	}	

	/** Set the status for this feature.
	 * 
	 * @param status - Status to set for the feature
	 */
	public void setStatus(String status) {
		Status desc = getStatus();
		if (desc == null) {
			addProperty(new Status(this, status, conf));
		}
		else {
			desc.setStatus(status);
		}
	}

	/** Delete the status for this feature.
	 * 
	 */
	public void deleteStatus() {
		Status desc = getStatus();
		if (desc != null) {
			deleteProperty(desc);
		}
	}
	
	/** Get all non primary dbxrefs for this feature.
	 * 
	 * @return - Collection of DBXrefs for this feature
	 */
	public Collection<DBXref> getNonPrimaryDBXrefs() {
		List<DBXref> dbxrefs = new ArrayList<DBXref>();
		for (FeatureDBXref dbxref : feature.getFeatureDBXrefs()) {
			dbxrefs.add(dbxref.getDbxref());
		}
		return dbxrefs;
	}

	/** Add a non primary dbxref to this feature.
	 * 
	 * @param dbxref - DBXref to be added
	 */
	public void addNonPrimaryDBXref(DBXref dbxref) {
		feature.addDBXref(dbxref);
	}
	
	/** Add a non primary dbxref to this feature.
	 * 
	 * @param db - String for the database for the dbxref
	 * @param accession - String for the accession for the dbxref
	 */
	public void addNonPrimaryDBXref(String db, String accession) {
		addNonPrimaryDBXref(new DBXref(new DB(db), accession));
	}
	
	public void deleteNonPrimaryDBXref(String db, String accession) {
		deleteNonPrimaryDBXref(new DBXref(new DB(db), accession));
	}
	
	public void deleteNonPrimaryDBXref(DBXref dbxref) {
		for (FeatureDBXref featureDbxref : feature.getFeatureDBXrefs()) {
			if (dbxref.equals(featureDbxref.getDbxref())) {
				boolean ok = feature.getFeatureDBXrefs().remove(featureDbxref);
				break;
			}
		}
		/*
		Iterator<FeatureDBXref> iter = feature.getFeatureDBXrefs().iterator();
		while (iter.hasNext()) {
			FeatureDBXref featureDbxref = iter.next();
			if (dbxref.equals(featureDbxref.getDbxref())) {
				iter.remove();
				break;
			}
		}
		*/
	}
	
	public Collection<GenericFeatureProperty> getNonReservedProperties() {
		Collection<CVTerm> excludeCvterms = new ArrayList<CVTerm>();
		List<GenericFeatureProperty> props = new ArrayList<GenericFeatureProperty>();
		excludeCvterms.addAll(conf.getCVTermsForClass("Comment"));
		excludeCvterms.addAll(conf.getCVTermsForClass("Owner"));
		excludeCvterms.addAll(conf.getCVTermsForClass("Description"));
		excludeCvterms.addAll(conf.getCVTermsForClass("Symbol"));
		excludeCvterms.addAll(conf.getCVTermsForClass("Status"));
		excludeCvterms.addAll(conf.getCVTermsForClass("ReadthroughStopCodon"));
		for (FeatureProperty fp : feature.getFeatureProperties()) {
			if (!excludeCvterms.contains(fp.getType())) {
				props.add(new GenericFeatureProperty(fp, conf));
			}
		}
		return props;
	}
	
	public void addNonReservedProperty(String tag, String value) {
		addProperty(new GenericFeatureProperty(this, tag, value, conf));
	}
	
	public void deleteNonReservedProperty(String tag, String value) {
		deleteProperty(new GenericFeatureProperty(this, tag, value, conf));
	}
	
	/** Get the time this feature was created.
	 * 
	 * @return Date when this feature was created
	 */
	public Date getTimeAccessioned() {
		return feature.getTimeAccessioned();
	}
	
	/** Set the time when this feature was created.
	 * 
	 * @param date - Date when this feature was created
	 */
	public void setTimeAccessioned(Date date) {
		feature.setTimeAccessioned(date);
	}
	
	/** Get the time this feature was last modified.
	 * 
	 * @return Date when this feature was last modified
	 */
	public Date getTimeLastModified() {
		return feature.getTimeLastModified();
	}
	
	/** Set the time when this feature was last modified.
	 * 
	 * @param date - Date when this feature was last modified
	 */
	public void setTimeLastModified(Date date) {
		feature.setTimeLastModified(date);
	}

	/** Get the type for the feature in the form of "CV:CVTERM".
	 * 
	 * @return String representation of the feature type
	 */
	public String getType() {
		return feature.getType().getCv().getName() + ":" + feature.getType().getName();
	}
	
	/** Get a string representation of this feature.  It returns a string of the following format:
	 *  uniqueName(type).
	 *  
	 *  @return String representation of this feature
	 */
	@Override
	public String toString() {
		return String.format("%s (%s)", getUniqueName(), feature.getType());
	}

	@Override
	public boolean equals(Object comparisonObject) {
		if (this == comparisonObject) {
			return true;
		}
		if (!this.getClass().equals(comparisonObject.getClass())) {
			return false;
		}
		return getFeature().equals(((AbstractBioFeature)comparisonObject).getFeature());
	}
	
	public SimpleObjectIterator getWriteableSimpleObjects(BioObjectConfiguration c)	{
		return new SimpleObjectIterator(this, c);
	}

	/** Friendly method for getting the GSOL feature.
	 * 
	 * @return Wrapped feature object
	 */
	Feature getFeature() {
		return feature;
	}
	
	protected Iterator<AbstractBioFeatureRelationship> getBioFeatureRelationships() {
		return new BioFeatureRelationshipIterator(this, conf);
	}

	protected Feature translateSimpleObjectType(BioObjectConfiguration c) {
		Feature clone = new Feature(feature);
		// no cvterm found for this type, use default
		if (c.getClassForCVTerm(feature.getType()) == null) {
			String className =
				BioObjectUtil.stripPackageNameFromClassName(getClass().getName());
			CVTerm defaultCvTerm = c.getDefaultCVTermForClass(className);
			if (defaultCvTerm == null) {
				throw new BioObjectConfigurationException("No default set for " + className);
			}
			clone.setType(defaultCvTerm);
		}
		//clone and translate FeatureLocations
		clone.setFeatureLocations(new HashSet<FeatureLocation>());
		for (FeatureLocation loc : feature.getFeatureLocations()) {
			FeatureLocation cloneLoc = new FeatureLocation(loc);
			AbstractBioFeature srcFeat =
				(AbstractBioFeature)BioObjectUtil.createBioObject(loc.getSourceFeature(), conf);
			if (srcFeat != null) {
				cloneLoc.setSourceFeature(srcFeat.translateSimpleObjectType(c));
			}
			clone.getFeatureLocations().add(cloneLoc);
		}
		return clone;
	}
	
	private static class SimpleObjectIterator extends AbstractSimpleObjectIterator {		
		private AbstractBioFeature feature;
		private BioObjectConfiguration conf;
		private Iterator<AbstractBioFeatureRelationship> frIter;
		private AbstractSimpleObjectIterator soIter;
		private boolean returnedFeature;
		
		public SimpleObjectIterator(AbstractBioFeature feature, BioObjectConfiguration conf) {
			this.feature = feature;
			this.conf = conf;
			this.returnedFeature = false;
		}
		
		public AbstractSimpleObject peek() {
			if (!returnedFeature) {
				current = feature.translateSimpleObjectType(conf);
				return current;
			}
			if (soIter == null) {
				return null;
			}
			return current;
		}

		public boolean hasNext() {
			if (!returnedFeature) {
				return true;
			}
			if (soIter == null) {
				return false;
			}
			return soIter.hasNext();
		}

		public AbstractSimpleObject next() {
			AbstractSimpleObject retVal = null;
			if (!returnedFeature) {
				retVal = peek();
				returnedFeature = true;
				frIter = feature.getBioFeatureRelationships();
				if (frIter.hasNext()) {
					soIter = frIter.next().getWriteableSimpleObjects(conf);
				}
				return retVal;
			}
			retVal = soIter.next();
			current = retVal;
			if (!soIter.hasNext() && frIter.hasNext()) {
				soIter = frIter.next().getWriteableSimpleObjects(conf);
			}
			return retVal;
		}
	}
	
	private class BioFeatureRelationshipIterator implements Iterator<AbstractBioFeatureRelationship> {
		private Iterator<FeatureRelationship> frIter;
		private BioObjectConfiguration conf;
		
		public BioFeatureRelationshipIterator(AbstractBioFeature feature, BioObjectConfiguration conf)
		{
			this.conf = conf;
			frIter = feature.feature.getChildFeatureRelationships().iterator();
		}

		public AbstractBioFeatureRelationship next()
		{
			return (AbstractBioFeatureRelationship)BioObjectUtil.createBioObject(frIter.next(), conf);
		}
		
		public boolean hasNext()
		{
			return frIter.hasNext();
		}
		
		public void remove()
		{
		}
	}
	
}
