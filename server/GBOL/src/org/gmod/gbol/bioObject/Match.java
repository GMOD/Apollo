package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.AnalysisFeature;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a match. 
 * 
 * @author elee
 *
 */
public class Match extends Region {

	private static final long serialVersionUID = 1L;
	
	private AnalysisFeature analysisFeature;
	
	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param analysisFeature - AnalysisFeature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Match(Feature feature, AnalysisFeature analysisFeature, BioObjectConfiguration conf) {
		super(feature, conf);
		this.analysisFeature = analysisFeature;
	}

	/** Alternate constructor to create a Match object without having to pre-create the
	 *  underlying Feature object.  The constructor will take care of creating the underlying Feature
	 *  object.  Match is considered to be an analysis.
	 * 
	 * @param organism - Organism that this Match belongs to
	 * @param uniqueName - String representing the unique name for this Match
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param analysisFeature - AnalysisFeature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public Match(Organism organism, String uniqueName,
			boolean obsolete, Timestamp dateAccessioned, AnalysisFeature analysisFeature, BioObjectConfiguration conf) {
		super(new Feature(
				conf.getDefaultCVTermForClass("Match"),
				null,
				organism,
				null,
				uniqueName,
				null,
				null,
				null,
				true,
				obsolete,
				dateAccessioned,
				null),
				conf);
		this.analysisFeature = analysisFeature;
	}
	
	public Double getRawScore() {
		if (analysisFeature == null) {
			return null;
		}
		return analysisFeature.getRawScore();
	}

	public void setRawScore(double rawScore) {
		if (analysisFeature == null) {
			return;
		}
		analysisFeature.setRawScore(rawScore);
	}
	
	public Double getNormalizedScore() {
		if (analysisFeature == null) {
			return null;
		}
		return analysisFeature.getNormalizedScore();
	}
	
	public void setNormalizedScore(double normalizedScore) {
		if (analysisFeature == null) {
			return;
		}
		analysisFeature.setRawScore(normalizedScore);
	}
	
	public Double getSignificance() {
		if (analysisFeature == null) {
			return null;
		}
		return analysisFeature.getSignificance();
	}
	
	public void setSignificance(double significance) {
		if (analysisFeature == null) {
			return;
		}
		analysisFeature.setRawScore(significance);
	}
	
	public Double getIdentity() {
		if (analysisFeature == null) {
			return null;
		}
		return analysisFeature.getIdentity();
	}
	
	public void setIdentity(double identity) {
		if (analysisFeature == null) {
			return;
		}
		analysisFeature.setIdentity(identity);
	}
	
	public String getQueryUniqueName() {
		return getQueryFeatureLocation().getSourceFeature().getUniqueName();
	}

	public String getSubjectUniqueName() {
		return getSubjectFeatureLocation().getSourceFeature().getUniqueName();
	}
	
	public FeatureLocation getQueryFeatureLocation() {
		for (FeatureLocation loc : getFeatureLocations()) {
			if (loc.getRank() == 0) {
				return loc;
			}
		}
		return null;
	}
	
	public void setQueryFeatureLocation(int fmin, int fmax, int strand, AbstractBioFeature source) {
		FeatureLocation loc = getQueryFeatureLocation();
		boolean needToAdd = false;
		if (loc == null) {
			loc = new FeatureLocation();
			needToAdd = true;
		}
		if (source != null) {
			loc.setSourceFeature(source.getFeature());
		}
		loc.setRank(0);
		loc.setFmin(fmin);
		loc.setFmax(fmax);
		loc.setStrand(strand);
		if (needToAdd) {
			getFeatureLocations().add(loc);
		}
	}

	public Integer getQueryFmin() {
		FeatureLocation loc = getQueryFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getFmin();
	}
	
	public Integer getQueryFmax() {
		FeatureLocation loc = getQueryFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getFmax();
	}
	
	public Integer getQueryStrand() {
		FeatureLocation loc = getQueryFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getStrand();
	}

	public FeatureLocation getSubjectFeatureLocation() {
		for (FeatureLocation loc : getFeatureLocations()) {
			if (loc.getRank() == 1) {
				return loc;
			}
		}
		return null;
	}
	
	public Integer getSubjectFmin() {
		FeatureLocation loc = getSubjectFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getFmin();
	}
	
	public Integer getSubjectFmax() {
		FeatureLocation loc = getSubjectFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getFmax();
	}
	
	public Integer getSubjectStrand() {
		FeatureLocation loc = getSubjectFeatureLocation();
		if (loc == null) {
			return null;
		}
		return loc.getStrand();
	}
	
	public void setSubjectFeatureLocation(int fmin, int fmax, int strand, AbstractBioFeature source) {
		FeatureLocation loc = getSubjectFeatureLocation();
		boolean needToAdd = false;
		if (loc == null) {
			loc = new FeatureLocation();
			needToAdd = true;
		}
		if (source != null) {
			loc.setSourceFeature(source.getFeature());
		}
		loc.setRank(1);
		loc.setFmin(fmin);
		loc.setFmax(fmax);
		loc.setStrand(strand);
		if (needToAdd) {
			getFeatureLocations().add(loc);
		}
	}
	
}
