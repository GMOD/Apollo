package org.bbop.apollo.web.datastore.history;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gmod.gbol.bioObject.AbstractSingleLocationBioFeature;

public class Transaction implements Serializable {

	private static final long serialVersionUID = 1L;

	public enum Operation {
		ADD_FEATURE,
		DELETE_FEATURE,
		ADD_TRANSCRIPT,
		DELETE_TRANSCRIPT,
		ADD_EXON,
		DELETE_EXON,
		MERGE_EXONS,
		SPLIT_EXON,
		SET_EXON_BOUNDARIES,
		MERGE_TRANSCRIPTS,
		SPLIT_TRANSCRIPT,
		SET_TRANSLATION_START,
		UNSET_TRANSLATION_START,
		SET_TRANSLATION_END,
		UNSET_TRANSLATION_END,
		SET_TRANSLATION_ENDS,
		SET_LONGEST_ORF,
		FLIP_STRAND,
		SET_READTHROUGH_STOP_CODON,
		UNSET_READTHROUGH_STOP_CODON,
		SET_BOUNDARIES
	}
	
	private Map<String, Object> attributes;
	private Operation operation;
	private String featureUniqueName;
	private List<AbstractSingleLocationBioFeature> oldFeatures;
	private List<AbstractSingleLocationBioFeature> newFeatures;
	private String editor;
	private Date date;
	
	public Transaction(Operation operation, String featureUniqueName, String editor) {
		this.operation = operation;
		this.featureUniqueName = featureUniqueName;
		this.editor = editor;
		attributes = new HashMap<String, Object>();
		oldFeatures = new ArrayList<AbstractSingleLocationBioFeature>();
		newFeatures = new ArrayList<AbstractSingleLocationBioFeature>();
		date = new Date();
	}
	
	public Transaction(Transaction transaction) {
		this.operation = transaction.operation;
		this.featureUniqueName = transaction.featureUniqueName;
		this.editor = transaction.editor;
		this.attributes = transaction.attributes;
		this.oldFeatures = transaction.oldFeatures;
		this.newFeatures = transaction.newFeatures;
		this.date = transaction.date;
	}
	
	public List<AbstractSingleLocationBioFeature> getOldFeatures() {
		return oldFeatures;
	}
	
	public void addOldFeature(AbstractSingleLocationBioFeature feature) {
		oldFeatures.add(feature);
	}
	
	public List<AbstractSingleLocationBioFeature> getNewFeatures() {
		return newFeatures;
	}
	
	public void addNewFeature(AbstractSingleLocationBioFeature feature) {
		newFeatures.add(feature);
	}
	
	public Map<String, Object> getAttributes() {
		return attributes;
	}
	
	public Object getAttribute(String key) {
		return attributes.get(key);
	}
	
	public void setAttribute(String key, Object value) {
		attributes.put(key, value);
	}
	
	public Operation getOperation() {
		return operation;
	}
	
	public String getFeatureUniqueName() {
		return featureUniqueName;
	}
	
	public void setFeatureUniqueName(String featureUniqueName) {
		this.featureUniqueName = featureUniqueName;
	}
	
	public String getEditor() {
		return editor;
	}
	
	public Date getDate() {
		return date;
	}
	
}
