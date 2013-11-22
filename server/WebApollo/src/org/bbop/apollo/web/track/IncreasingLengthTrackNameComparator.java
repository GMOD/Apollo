package org.bbop.apollo.web.track;

import org.bbop.apollo.web.config.ServerConfiguration;

public class IncreasingLengthTrackNameComparator implements TrackNameComparator {

	@Override
	public int compare(ServerConfiguration.TrackConfiguration o1, ServerConfiguration.TrackConfiguration o2) {
		int len1 = o1.getSourceFeature().getEnd() - o1.getSourceFeature().getStart();
		int len2 = o2.getSourceFeature().getEnd() - o2.getSourceFeature().getStart();
		return new Integer(len1).compareTo(len2);
	}

}
