package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a gene.
 * 
 * @author elee
 *
 */

public class Pseudogene extends Gene {

    private static final long serialVersionUID = 1L;

    /** Constructor.
     * 
     * @param feature - Feature object that this class wraps
     * @param conf - Configuration containing mapping information
     */
    public Pseudogene(Feature feature, BioObjectConfiguration conf) {
        super(feature, conf);
    }
    
    public Pseudogene(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
        super(new Feature(
                conf.getDefaultCVTermForClass("Pseudogene"),
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
    
    /** Copy constructor.
     * 
     * @param gene - Pseudogene to create the copy from
     * @param uniqueName - String representing the unique name for this Gene
     */
    public Pseudogene(Pseudogene gene, String uniqueName) {
        this(gene.getFeature().getOrganism(), uniqueName, gene.getFeature().isIsAnalysis(),
                gene.getFeature().isIsObsolete(),
                new Timestamp(gene.getFeature().getTimeAccessioned().getTime()), gene.getConfiguration());
        feature.addFeatureLocation(new FeatureLocation(gene.getFeatureLocation()));
    }


    @Override
    public boolean isPseudogene() {
        return true;
    }
}
