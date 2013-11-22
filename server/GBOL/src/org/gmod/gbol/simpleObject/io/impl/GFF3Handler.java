package org.gmod.gbol.simpleObject.io.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DB;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.FeatureRelationship;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.gmod.gbol.simpleObject.io.FileHandler;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOInterface;


public class GFF3Handler extends FileHandler implements SimpleObjectIOInterface {

	private Organism organism;
	private String newlineCharacter;
	private String columnDelimiter;
	private String sourceFeature;
	
	private enum ParseMode {FEATURE,FASTA,FEATURE_COMPLETE};
	private ParseMode parseMode;
	
	private Map<String,Feature> features;
	//private Map<String,Feature> sourceFeatures;
	
	// Configuration settings
	private CVTerm sourceFeatureType;
	private CVTerm synonymType;
	private CVTerm featureRelationshipType;
	
	private CV sequenceOntology;
	private CV gffFeaturePropertyOntology;
	private CV synonymOntology;
	private CV relationshipOntology;
	
	private Map<String,CVTerm> featureTypes;
	private Map<String,CVTerm> sources;
	private Map<String,DB> dbs;
	
	private Map<String,CVTerm> featurePropertyTypes;

	public GFF3Handler(String filePath) throws IOException {
		super(filePath);
		this.newlineCharacter = "\n";
		this.columnDelimiter = "\t";
		this.parseMode = ParseMode.FEATURE;
		//this.sourceFeatures =  new HashMap<String,Feature>();
		this.features =  new HashMap<String,Feature>();
		this.featureTypes = new HashMap<String,CVTerm>();
		this.sources = new HashMap<String,CVTerm>();
		this.dbs = new HashMap<String,DB>();
		this.featurePropertyTypes = new HashMap<String,CVTerm>();
		
		this.organism = null;
		this.setSequenceOntologyName("SO");
		this.setGFFFeaturePropertyOntologyName("GFF Feature Property");
		this.setSynonymOntologyName("synonym");
		this.setSynonymTypeName("synonym");
	}

	public Iterator<Feature> getAllFeatures() {
		return this.features.values().iterator();
	}

	// Rename this to something less confusing
	public List<Feature> getTopLevelFeatures() {
		List<Feature> features = new ArrayList<Feature>();
		for (Feature f : this.features.values()){
			if ((f.getParentFeatureRelationships().size()==0) && (!f.getType().equals(this.sourceFeatureType))){
				features.add(f);
			}
		}
		return features;
	}
	
	public List<Feature> getSourceFeatures() {
		return null;
	}
	
	private Organism getOrganism() {
		return organism;
	}

	public void setOrganism(Organism organism) {
		this.organism = organism;
	}

	public Iterator<? extends Feature> getAllFeaturesByRange(FeatureLocation loc) throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
	}

	public Iterator<? extends Feature> getAllFeaturesByOverlappingRange(FeatureLocation loc) throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
	}
	
	public Iterator<? extends Feature> getFeaturesByCVTermAndRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
	}

	public Iterator<? extends Feature> getFeaturesByCVTermAndOverlappingRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
	}

	public Iterator<? extends Feature> getAllFeaturesBySourceFeature(Feature sourceFeature) throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
	}

	@SuppressWarnings("unchecked")
	public Iterator<?extends AbstractSimpleObject> readAll() throws SimpleObjectIOException {
		
		this.openHandle();
		StringBuilder contents = this.readFileContents();
		
		String[] lines = contents.toString().split(this.newlineCharacter);
		for (int i=0;i<lines.length;i++){
			String line = lines[i].trim();
			if ((!line.equals("")) && (line != null)){

				if (line.charAt(0) == '#'){
					if (line.charAt(1) == '#'){
						this.processDirective(lines[i].substring(2).trim());
					}
				} else {
					if (this.parseMode.equals(ParseMode.FEATURE)){
						try {
							this.constructFeature(line);
						} catch (Exception e){
							System.err.println("Error parsing line " + i + " of GFF3 file " + this.getFilePath());
							System.err.println("Error message: " + e.getMessage());
							System.exit(-1);
						}
					} else if (this.parseMode.equals(ParseMode.FASTA)){
						this.parseFASTA(line.trim());
					} else {
						System.err.println("Don't know how to handle line " + line + " in parse mode " + this.parseMode.toString() + ".");
					}
				}
			}
		}
		return (Iterator<? extends AbstractSimpleObject>) this.features.values().iterator();
	}

	public Iterator<? extends Feature> getFeaturesByCVTerm(CVTerm cvterm) throws SimpleObjectIOException {
		throw new RuntimeException("Not implemented yet");
	}

	public void write(SimpleObjectIteratorInterface simpleObjects) {
		System.err.println("Not implemented yet.");
	}
	
	private void parseFASTA(String line) {
		if (line.charAt(0) == '>'){
			this.sourceFeature = line.substring(1);
			if (!this.features.containsKey(this.sourceFeature)){
				this.constructSourceFeature(this.sourceFeature);
			}
		} else {
			this.features.get(this.sourceFeature).getResidues().concat(line);
		}
	}
	
	private void processDirective(String directive) {
		if (directive.equals("#")){
			System.out.println("Feature Parsing Complete.");
			this.parseMode = ParseMode.FEATURE_COMPLETE;
		} else if (directive.equals("FASTA")){
			this.parseMode = ParseMode.FASTA;
		} else {
			System.err.println("Can't handle directive '" + directive + "' yet.");
		}
	}
	
	private void constructFeature(String line) throws Exception {
		
		Feature feature = new Feature();
		feature.setOrganism(this.getOrganism());
		
		String[] parts = line.split(this.columnDelimiter);
		if (parts.length != 9){
			throw new Exception("Unexpected number of columns (" + parts.length + "). Expecting 9.");
		}
		
		if (!this.features.containsKey(parts[0])){
			this.constructSourceFeature(parts[0]);
		}
		
		if (!this.sources.containsKey(parts[1])){
			this.sources.put(parts[1], new CVTerm("source",this.gffFeaturePropertyOntology));
		}
		feature.addFeatureProperty(this.sources.get(parts[1]), parts[1]);

		if (!this.featureTypes.containsKey(parts[2])){
			this.featureTypes.put(parts[2], new CVTerm(parts[2],this.sequenceOntology));
		}
		feature.setType(this.featureTypes.get(parts[2]));
		
		FeatureLocation fl = new FeatureLocation();
	
		// Subtract 1 to convert to interbase.
		fl.setFmin((Integer.parseInt(parts[3])-1));
		fl.setFmax(Integer.parseInt(parts[4]));
		if (fl.getFmin()>=fl.getFmax()){
			throw new Exception("Feature fmax (" + fl.getFmax() + ") cannot be <= feature fmin (" + fl.getFmin() + ").");
		}
		if (parts[6].equals("+")){
			fl.setStrand(1);
		} else if (parts[6].equals("-")){
			fl.setStrand(-1);
		} else {
			fl.setStrand(0);
		}
		
		try {
			fl.setPhase(Integer.parseInt(parts[7]));
		} catch (Exception e){
			// No Phase.
		}
		
		feature.addFeatureLocation(fl);
		
		// This needs to be altered to handle escaped semicolons. 
		for (String attribute : parts[8].split(";")){
			String[] keyval = attribute.split("=");
			this.processAttribute(feature, keyval[0], keyval[1]);
		}
		
		if (feature.getUniqueName() == null) {
			throw new Exception("Feature is missing required attribute 'ID'.");
		}
		
	}
	
	private void processAttribute(Feature feature, String key, String value) throws Exception {
		if (key.equals("ID")){
			feature.setUniqueName(value);
			if (this.features.containsKey(feature.getUniqueName())){
				throw new Exception("Feature ID " + feature.getUniqueName() + " is not unique.");
			} else {
				this.features.put(feature.getUniqueName(), feature);
			}
		} else if (key.equals("Name")){
			feature.setName(value);
		} else if (key.equals("Alias")){
			feature.addSynonym(this.synonymType, value);
		} else if (key.equals("Parent")){
			for (String parentId : value.split(",")){
				if (!this.features.containsKey(parentId)){
					throw new Exception("Can't find parent feature " + parentId + ". Maybe it hasn't been parsed yet?");
				}
				FeatureRelationship fr = new FeatureRelationship();
				fr.setSubjectFeature(feature);
				fr.setObjectFeature(this.features.get(parentId));
				fr.setType(this.featureRelationshipType);
				this.features.get(parentId).getChildFeatureRelationships().add(fr);
				feature.getParentFeatureRelationships().add(fr);
			}
		} else if (key.equals("Target")){
			System.err.println("Can't handle 'Target' attribute yet.");
		} else if (key.equals("Gap")){
			System.err.println("Can't handle 'Gap' attribute yet.");
		} else if (key.equals("Derives_from")){
			System.err.println("Can't handle 'Derives_from' attribute yet.");
		} else if (key.equals("Dbxref")){
			if (!value.contains(":")){
				throw new Exception("Dbxref must be of the form DBTAG:ID");
			}
			String db = value.substring(0, value.indexOf(':'));
			String accession = value.substring(value.indexOf(':')+1);
			if (!this.dbs.containsKey(db)){
				this.dbs.put(db, new DB(db));
			}
			DBXref dbxref = new DBXref();
			dbxref.setDb(this.dbs.get(db));
			dbxref.setAccession(accession);
			feature.addDBXref(dbxref);
		} else if (key.equals("Ontology_term")){
			System.err.println("Can't handle 'Ontology_term' attribute yet.");
		} else {
			if (!this.featurePropertyTypes.containsKey(key)){
				this.featurePropertyTypes.put(key, new CVTerm(key,this.gffFeaturePropertyOntology));
			}
			feature.addFeatureProperty(this.featurePropertyTypes.get(key), value);
		}
	}
	
	private void constructSourceFeature(String uniqueName) {
		Feature sourceFeature = new Feature();
		sourceFeature.setUniqueName(uniqueName);
		sourceFeature.setIsObsolete(false);
		sourceFeature.setType(this.getSourceFeatureType());
		sourceFeature.setResidues("");
		this.features.put(uniqueName, sourceFeature);
	}

	public String getNewlineCharacter() {
		return newlineCharacter;
	}

	public void setNewlineCharacter(String newlineCharacter) {
		this.newlineCharacter = newlineCharacter;
	}

	public String getColumnDelimiter() {
		return columnDelimiter;
	}

	public void setColumnDelimiter(String columnDelimiter) {
		this.columnDelimiter = columnDelimiter;
	}


	public void setSequenceOntologyName(String sequenceOntologyName) {
		this.sequenceOntology = new CV(sequenceOntologyName);
	}
	
	public CVTerm getSourceFeatureType() {
		return this.sourceFeatureType;
	}

	public void setSourceFeatureType(String sourceFeatureType) {
		this.sourceFeatureType = new CVTerm(sourceFeatureType,this.sequenceOntology);
	}
	
	public void setGFFFeaturePropertyOntologyName(String gffFeaturePropertyOntologyName) {
		this.gffFeaturePropertyOntology = new CV(gffFeaturePropertyOntologyName);
	}
	
	public void setSynonymOntologyName(String synonymOntologyName) {
		this.synonymOntology = new CV(synonymOntologyName);
	}
	
	public void setSynonymTypeName(String synonymTypeName) {
		this.synonymType = new CVTerm(synonymTypeName,this.synonymOntology);
	}

	public void setRelationshipOntologyName(String relationshipOntologyName) {
		this.relationshipOntology = new CV(relationshipOntologyName);
	}
	
	public void setFeatureRelationshipTypeName(String featureRelationshipTypeName) {
		this.featureRelationshipType = new CVTerm(featureRelationshipTypeName,this.relationshipOntology);
	}

	public Feature getFeature(Organism organism, CVTerm type, String uniquename)
			throws SimpleObjectIOException {
		// TODO Auto-generated method stub
		return null;
	}
	
}