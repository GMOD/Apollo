package org.bbop.apollo.web.track;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbop.apollo.web.config.ServerConfiguration;

public class BtauTrackNameComparator implements TrackNameComparator {

	private Pattern pattern;
	
	public BtauTrackNameComparator() {
		pattern = Pattern.compile("(\\d+)");
	}
	
	@Override
	public int compare(ServerConfiguration.TrackConfiguration o1, ServerConfiguration.TrackConfiguration o2) {
		Matcher matcher1 = pattern.matcher(o1.getSourceFeature().getUniqueName());
		Matcher matcher2 = pattern.matcher(o2.getSourceFeature().getUniqueName());
		
		matcher1.find();
		matcher2.find();
		
		Integer group1 = new Integer(matcher1.group(1));
		Integer group2 = new Integer(matcher2.group(1));

		return group1.compareTo(group2);
	}

}
