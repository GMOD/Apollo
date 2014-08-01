package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

public class RRNA extends Transcript {

    private static final long serialVersionUID = 1L;

    /** Constructor.
     * 
     * @param feature - Feature object that this class wraps
     * @param conf - Configuration containing mapping information
     */
    public RRNA(Feature feature, BioObjectConfiguration conf) {
        super(feature, conf);
    }
    
    /** Copy constructor.
     * 
     * @param rrna - rRNA to create the copy from
     * @param uniqueName - String representing the unique name for this rRNA
     */
    public RRNA(RRNA rrna, String uniqueName) {
        this(rrna.getFeature().getOrganism(), uniqueName, rrna.getFeature().isIsAnalysis(),
                rrna.getFeature().isIsObsolete(),
                new Timestamp(rrna.getFeature().getTimeAccessioned().getTime()), rrna.getConfiguration());
        feature.addFeatureLocation(new FeatureLocation(rrna.getFeatureLocation()));
    }

    /** Alternate constructor to create a rRNA object without having to pre-create the underlying
     *  Feature object.  The constructor will take care of creating the underlying Feature object.
     * 
     * @param organism - Organism that this Gene belongs to
     * @param uniqueName - String representing the unique name for this rRNA
     * @param analysis - boolean flag for whether this feature is a result of an analysis
     * @param obsolete - boolean flag for whether this feature is obsolete
     * @param dateAccessioned - Timestamp for when this feature was first accessioned
     * @param conf - Configuration containing mapping information
     */
    public RRNA(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
        super(new Feature(
                conf.getDefaultCVTermForClass("RRNA"),
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
