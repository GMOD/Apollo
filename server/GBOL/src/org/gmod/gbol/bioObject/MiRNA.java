package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

public class MiRNA extends Transcript {

	private static final long serialVersionUID = 1L;

	/** Constructor.
	 * 
	 * @param feature - Feature object that this class wraps
	 * @param conf - Configuration containing mapping information
	 */
	public MiRNA(Feature feature, BioObjectConfiguration conf) {
		super(feature, conf);
	}
	
	/** Copy constructor.
	 * 
	 * @param mirna - mirna to create the copy from
	 * @param uniqueName - String representing the unique name for this mirna
	 */
	public MiRNA(MiRNA mirna, String uniqueName) {
		this(mirna.getFeature().getOrganism(), uniqueName, mirna.getFeature().isIsAnalysis(),
				mirna.getFeature().isIsObsolete(),
				new Timestamp(mirna.getFeature().getTimeAccessioned().getTime()), mirna.getConfiguration());
		feature.addFeatureLocation(new FeatureLocation(mirna.getFeatureLocation()));
	}

	/** Alternate constructor to create a mirna object without having to pre-create the underlying
	 *  Feature object.  The constructor will take care of creating the underlying Feature object.
	 * 
	 * @param organism - Organism that this Gene belongs to
	 * @param uniqueName - String representing the unique name for this mirna
	 * @param analysis - boolean flag for whether this feature is a result of an analysis
	 * @param obsolete - boolean flag for whether this feature is obsolete
	 * @param dateAccessioned - Timestamp for when this feature was first accessioned
	 * @param conf - Configuration containing mapping information
	 */
    public MiRNA(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
    	super(new Feature(
    			conf.getDefaultCVTermForClass("MiRNA"),
    			null,
    			organism,
    			null,
    			uniqueName,
    			null,
    			null,
    			null,
    			analysis,
    			obsolete,
    			dateAccessioned,
    			null),
    			conf);
    }

    @Override
    public boolean isProteinCoding() {
    	return false;
    }
}
