package org.gmod.gbol.bioObject.io;

import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.Gene;

/** I/O interface for all Bio object layer I/O handlers.
 * 
 * @author elee
 *
 */
public interface BioObjectIOInterface {

	/**	Get all features from data source.  Returns all features mapped to their corresponding
	 *  data types.
	 *  
	 * @return Iterator of AbstractBioFeature objects - empty if no objects are found
	 * @throws Exception if an error has occurred fetching features
	 */
	public Iterator<AbstractBioFeature> getAllFeatures() throws Exception;

	/**	Get all features from data source within a specific range.  Returns all features 
	 * mapped to their corresponding  data types.
	 *  
	 * @return Iterator of AbstractBioFeature objects - empty if no objects are found
	 * @throws Exception if an error has occurred fetching features
	 */
	public Iterator<AbstractBioFeature> getFeaturesByRange(
			String organismGenus, String organismSpecies, String sourceFeatureTypeCVName, String sourceFeatureTypeCVTermName,
			String sourceFeatureUniqueName, int fmin, int fmax, int strand) throws Exception;
	
	
	/** Get all genes from data source.  Returns any feature that is configured to be
	 *  mapped to Gene.
	 *  
	 * @return Iterator of Gene objects - empty if no objects are found
	 * @throws Exception if an error has occurred fetching genes
	 */
	public Iterator<Gene> getAllGenes() throws Exception;
	
	/** Get all genes from data source within a specific range.  Returns any feature 
	 * that is configured to be  mapped to Gene.
	 *  
	 * @return Iterator of Gene objects - empty if no objects are found
	 * @throws Exception if an error has occurred fetching genes
	 */
	public Iterator<Gene> getGenesByRange(
			String organismGenus, String organismSpecies, String sourceFeatureTypeCVName, String sourceFeatureTypeCVTermName,
			String sourceFeatureUniqueName, int fmin, int fmax, int strand) throws Exception;
	
	/** Write the passed features to the data source.
	 * 
	 * @param features - Collection of AbstractBioFeature objects to be written
	 */
	public void write(Collection<? extends AbstractBioFeature> features);
}
