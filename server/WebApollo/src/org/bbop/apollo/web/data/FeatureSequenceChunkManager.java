package org.bbop.apollo.web.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bbop.apollo.web.util.Pair;

public class FeatureSequenceChunkManager {

	private String sequenceDirectory;
	private int chunkSize;
	private String chunkPrefix;
	private int sequenceLength;
	private Queue<Pair<Integer, String>> cache;
	private int cacheSize;
	private static Map<String, FeatureSequenceChunkManager> trackToInstance;

	static {
		trackToInstance = new HashMap<String, FeatureSequenceChunkManager>();
	}
	
	public static Map<String, FeatureSequenceChunkManager> getTrackToInstance() {
		return trackToInstance;
	}
	
	public static FeatureSequenceChunkManager getInstance(String track) {
		FeatureSequenceChunkManager instance = trackToInstance.get(track);
		if (instance == null) {
			instance = new FeatureSequenceChunkManager();
			getTrackToInstance().put(track, instance);
		}
		return instance;
	}
	
	private FeatureSequenceChunkManager() {
		cache = new ConcurrentLinkedQueue<Pair<Integer, String>>();
		cacheSize = 50;
		chunkPrefix = "";
	}
	
	public void setSequenceDirectory(String sequenceDirectory) {
		this.sequenceDirectory = sequenceDirectory;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public String getChunkPrefix() {
		return chunkPrefix;
	}
	
	public void setChunkPrefix(String chunkPrefix) {
		this.chunkPrefix = chunkPrefix;
	}
	
	public int getSequenceLength() {
		return sequenceLength;
	}
	
	public void setSequenceLength(int sequenceLength) {
		this.sequenceLength = sequenceLength;
	}
	
	public String getSequenceForChunk(int chunkNumber) throws IOException {
		for (Pair<Integer, String> data : cache) {
			if (data.getFirst().equals(chunkNumber)) {
				return data.getSecond();
			}
		}
		BufferedReader br = new BufferedReader(new FileReader(sequenceDirectory + "/" + chunkPrefix + chunkNumber + ".txt"));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {
			sb.append(line.toUpperCase());
		}
		String sequence = sb.toString();
		if (cache.size() >= cacheSize) {
			cache.remove();
		}
		cache.add(new Pair<Integer, String>(chunkNumber, sequence));
		br.close();
		return sequence;
	}
	
}
