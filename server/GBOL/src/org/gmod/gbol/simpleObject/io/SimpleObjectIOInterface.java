package org.gmod.gbol.simpleObject.io;

import java.util.Iterator;

import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

public interface SimpleObjectIOInterface {

	/** Write simple objects to the underlying data source.
	 * 
	 * @param iter - SimpleObjectIteratorInterface for the objects to write
	 * @throws SimpleObjectIOException - Error in processing write request
	 */
	public void write(SimpleObjectIteratorInterface iter) throws SimpleObjectIOException;
	
	/** Read all simple objects in the underlying data source.
	 * 
	 * @return Iterator for the AbstractSimpleObject objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends AbstractSimpleObject> readAll() throws SimpleObjectIOException;
	
	/** Get all feature objects in the underlying data source.
	 * 
	 * @return Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getAllFeatures() throws SimpleObjectIOException;
	
	/** Get all feature objects within a specified range in the underlying data source.
	 * 
	 * @param loc - FeatureLocation defining the range to retrieve the features from
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getAllFeaturesByRange(FeatureLocation loc) throws SimpleObjectIOException;

	/** Get all feature objects overlapping a specified range in the underlying data source.
	 * 
	 * @param loc - FeatureLocation defining the range to retrieve the features from
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getAllFeaturesByOverlappingRange(FeatureLocation loc) throws SimpleObjectIOException;
	
	/** Get all feature objects contained in the source feature in the underlying data source.
	 * 
	 * @param sourceFeature - Feature for the source feature
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getAllFeaturesBySourceFeature(Feature sourceFeature) throws SimpleObjectIOException;
	
	/** Get all feature objects for a given type (cvterm) in the underlying data source.
	 * 
	 * @param cvterm - CVTerm defining the type of the features to retrieve
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getFeaturesByCVTerm(CVTerm cvterm) throws SimpleObjectIOException;
	
	/** Get all feature objects for a given type (cvterm) and within a specified range in the underlying
	 *  data source.
	 * 
	 * @param cvterm - CVTerm defining the type of the features to retrieve
	 * @param loc - FeatureLocation defining the range to retrieve the features from
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getFeaturesByCVTermAndRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException;
	
	/** Get all feature objects for a given type (cvterm) and overlapping a specified range in the underlying
	 *  data source.
	 * 
	 * @param cvterm - CVTerm defining the type of features to retrieve
	 * @param loc - FeatureLocation defining the range to retrieve the feature from
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getFeaturesByCVTermAndOverlappingRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException;
	
	/** Get a specific feature by organism, type (cvterm), and uniquename.
	 * 
	 * @param organism - Organism that this feature belongs to
	 * @param type - CVTerm defining the type of feature to retrieve
	 * @param uniquename - String for the uniquename associated with the feature to retrieve
	 * @return - Feature that fits all requested criteria
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Feature getFeature(Organism organism, CVTerm type, String uniquename) throws SimpleObjectIOException;
	
}