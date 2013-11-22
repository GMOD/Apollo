package org.bbop.apollo.web.track;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bbop.apollo.web.config.ServerConfiguration;

public class AmelTrackNameComparator implements TrackNameComparator {

	private Pattern pattern;
	
	public AmelTrackNameComparator() {
		pattern = Pattern.compile("(\\d+)\\.?(\\d*)");
	}
	
	@Override
	public int compare(ServerConfiguration.TrackConfiguration o1, ServerConfiguration.TrackConfiguration o2) {
		Matcher matcher1 = pattern.matcher(o1.getSourceFeature().getUniqueName());
		Matcher matcher2 = pattern.matcher(o2.getSourceFeature().getUniqueName());
		
		matcher1.find();
		matcher2.find();
		
		Integer group1 = new Integer(matcher1.group(1));
		Integer group2 = new Integer(matcher2.group(1));

		boolean hasSubgroup1 = matcher1.group(2).length() > 0;
		boolean hasSubgroup2 = matcher2.group(2).length() > 0;
		
		// both GroupUn
		if (!hasSubgroup1 && !hasSubgroup2) {
			return group1.compareTo(group2);
		}
		
		// o1 is GroupUn
		if (!hasSubgroup1) {
			return 1;
		}
		
		// o2 is GroupUn
		if (!hasSubgroup2) {
			return -1;
		}
		
		// placed scaffolds
		if (!group1.equals(group2)) {
			return group1.compareTo(group2);
		}
		
		return new Integer(matcher1.group(2)).compareTo(new Integer(matcher2.group(2)));
	}

}
