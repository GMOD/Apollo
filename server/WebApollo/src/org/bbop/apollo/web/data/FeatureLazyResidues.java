package org.bbop.apollo.web.data;

import java.io.IOException;

import org.gmod.gbol.simpleObject.Feature;

public class FeatureLazyResidues extends Feature {

	private static final long serialVersionUID = 1L;

	private String track;
	private int fmin;
	private int fmax;
	
	public FeatureLazyResidues(String track) {
		super();
		this.track = track;
	}

	@Override
	public String getResidues(int fmin, int fmax) {
		FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track);
		int chunkSize = chunkManager.getChunkSize();
		int startChunkNumber = fmin / chunkSize;
		int endChunkNumber = (fmax - 1) / chunkSize;
		StringBuilder sequence = new StringBuilder();
		try {
			for (int i = startChunkNumber; i <= endChunkNumber; ++i) {
				sequence.append(chunkManager.getSequenceForChunk(i));
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		int startPosition = fmin - (startChunkNumber * chunkSize);
		return sequence.substring(startPosition, startPosition + (fmax - fmin));
	}
	
	@Override
	public Integer getSequenceLength() {
		FeatureSequenceChunkManager chunkManager = FeatureSequenceChunkManager.getInstance(track);
		return chunkManager.getSequenceLength();
	}
	
	public int getFmin() {
		return fmin;
	}
	
	public void setFmin(int fmin) {
		this.fmin = fmin;
	}
	
	public int getFmax() {
		return fmax;
	}
	
	public void setFmax(int fmax) {
		this.fmax = fmax;
	}
}
