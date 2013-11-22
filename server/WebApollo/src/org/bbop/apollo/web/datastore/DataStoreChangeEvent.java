package org.bbop.apollo.web.datastore;

import java.util.EventObject;

import org.json.JSONObject;

public class DataStoreChangeEvent extends EventObject {

	private static final long serialVersionUID = 1L;

	private JSONObject features;
	private String track;
	private Operation operation;
	private boolean sequenceAlterationEvent;
	
	public DataStoreChangeEvent(Object source, JSONObject features, String track, Operation operation) {
		this(source, features, track, operation, false);
	}

	public DataStoreChangeEvent(Object source, JSONObject features, String track, Operation operation, boolean sequenceAlterationEvent) {
		super(source);
		this.features = features;
		this.track = track;
		this.operation = operation;
		this.sequenceAlterationEvent = sequenceAlterationEvent;
	}
	
	public enum Operation {
		ADD,
		DELETE,
		UPDATE
	}
	
	public JSONObject getFeatures() {
		return features;
	}
	
	public String getTrack() {
		return track;
	}

	public Operation getOperation() {
		return operation;
	}
	
	public boolean isSequenceAlterationEvent() {
		return sequenceAlterationEvent;
	}

}
