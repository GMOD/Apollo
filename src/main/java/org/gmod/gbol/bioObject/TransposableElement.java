package org.gmod.gbol.bioObject;

import java.sql.Timestamp;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;

/** Wrapper class representing a TransposableElement. 
 * 
 * @author elee
 *
 */

public class TransposableElement extends AbstractSingleLocationBioFeature {

    private static final long serialVersionUID = 1L;

    /** Constructor.
     * 
     * @param feature - Feature object that this class wraps
     * @param conf - Configuration containing mapping information
     */
    public TransposableElement(Feature feature, BioObjectConfiguration conf) {
        super(feature, conf);
    }

    /** Copy constructor.
     * 
     * @param transposableElement - TransposableElement to create the copy from
     * @param uniqueName - String representing the unique name for this transposableElement
     */
    public TransposableElement(TransposableElement transposableElement, String uniqueName) {
        this(transposableElement.getFeature().getOrganism(), uniqueName, transposableElement.getFeature().isIsAnalysis(),
                transposableElement.getFeature().isIsObsolete(),
                new Timestamp(transposableElement.getFeature().getTimeAccessioned().getTime()), transposableElement.getConfiguration());
        feature.addFeatureLocation(new FeatureLocation(transposableElement.getFeatureLocation()));
    }
    
    /** Alternate constructor to create a TransposableElement object without having to pre-create the underlying
     *  Feature object.  The constructor will take care of creating the underlying Feature object.
     * 
     * @param organism - Organism that this Gene belongs to
     * @param uniqueName - String representing the unique name for this TransposableElement
     * @param analysis - boolean flag for whether this feature is a result of an analysis
     * @param obsolete - boolean flag for whether this feature is obsolete
     * @param dateAccessioned - Timestamp for when this feature was first accessioned
     * @param conf - Configuration containing mapping information
     */
    public TransposableElement(Organism organism, String uniqueName, boolean analysis,
            boolean obsolete, Timestamp dateAccessioned, BioObjectConfiguration conf) {
        super(new Feature(
                conf.getDefaultCVTermForClass("TransposableElement"),
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
}
