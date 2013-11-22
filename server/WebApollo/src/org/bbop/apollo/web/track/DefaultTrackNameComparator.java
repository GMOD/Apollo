package org.bbop.apollo.web.track;

import org.bbop.apollo.web.config.ServerConfiguration;

public class DefaultTrackNameComparator implements TrackNameComparator {

	@Override
	public int compare(ServerConfiguration.TrackConfiguration o1, ServerConfiguration.TrackConfiguration o2) {
		return o1.getSourceFeature().getUniqueName().compareTo(o2.getSourceFeature().getUniqueName());
	}

}
