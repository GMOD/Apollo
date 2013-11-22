package org.gmod.gbol.simpleObject.io.impl;

import java.util.Iterator;

import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.Analysis;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.DB;
import org.gmod.gbol.simpleObject.DBXref;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOException;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOInterface;
import org.gmod.gbol.util.HibernateUtil;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HibernateHandler implements SimpleObjectIOInterface {

	private SessionFactory sf;
    //	private Session session;
	
	/** Constructor
	 * 
	 * @param hibernateConfig - File location of Hibernate XML configuration
	 * @throws Exception - An error has occurred while initializing Hibernate or opening the connection to the database
	 */
	public HibernateHandler(String hibernateConfig) throws Exception {

	    // sf = HibernateUtil.buildSessionFactory(hibernateConfig);
	    // using getSessionFactory() instead of buildSessionFactory(), because may be sharing SessionFactory with servlet filter 
	    //    for "Open Session In View" pattern, so SessionFactory may already have been created
	    //   
	    sf = HibernateUtil.getSessionFactory(hibernateConfig);
		//	session = sf.openSession();
	    //		beginTransaction();
	    System.out.println("in HibernateHandler constructor, SessionFactory: " + sf);
	}

	@Override
	public Iterator<? extends AbstractSimpleObject> readAll() throws SimpleObjectIOException {
		throw new RuntimeException("Not yet implemented");
		//return null;
	}

	@Override
	public Iterator<? extends Feature> getFeaturesByCVTerm(CVTerm cvterm) {
		return getFeaturesByCVTermAndOrganism(cvterm, null);
	}

	/** Get all feature objects for a given type (cvterm) and organism in the underlying database.
	 * 
	 * @param cvterm - CVTerm defining the type of the features to retrieve
	 * @param organism - Organism that this feature belongs to
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException
	 */
	public Iterator<? extends Feature> getFeaturesByCVTermAndOrganism(CVTerm type, Organism organism) {
		String hql = "from Feature where type.name=? and type.cv.name=?";
		if (organism != null) {
			hql += " and organism.genus=? and organism.species=?";
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, type.getName());
		query.setString(1, type.getCv().getName());
		if (organism != null) {
			query.setString(2, organism.getGenus());
			query.setString(3, organism.getSpecies());
		}
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Feature>(i);
	}

	@Override
	public Iterator<? extends Feature> getAllFeatures() {
		return getAllFeatures(false);
	}

	/** Get all feature objects in the underlying database.
	 * 
	 * @param nonAnalysisOnly true if only features that are not analyses should be returned
	 * @return Iterator for the Feature objects
	 */
	public Iterator<? extends Feature> getAllFeatures(boolean nonAnalysisOnly) {
		String hql = "from Feature";
		if (nonAnalysisOnly) {
			hql += " where is_analysis=?";
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		if (nonAnalysisOnly) {
			query.setBoolean(0, false);
		}
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Feature>(i);
	}

	@Override
	public Iterator<? extends Feature> getAllFeaturesByRange(FeatureLocation loc) throws SimpleObjectIOException {
		if (loc.getSourceFeature() == null) {
			throw new SimpleObjectIOException("Missing source feature for FeatureLocation");
		}
		
		String hql = "from Feature as f join f.featureLocations as fl where fl.fmin>=? and fl.fmax<=? and fl.sourceFeature=?";
		int strandParam = 0;
		if (loc.getStrand() != null) {
			hql += " and fl.strand=?";
			strandParam = 3;
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setInteger(0, loc.getFmin());
		query.setInteger(1, loc.getFmax());
		query.setEntity(2, loc.getSourceFeature());
		if (loc.getStrand() != null) {
			query.setInteger(strandParam, loc.getStrand());
		}
		Iterator<?> i = query.iterate();

		return new HibernateIterator<Feature>(i);
	}

	@Override
	public Iterator<? extends Feature> getAllFeaturesByOverlappingRange(FeatureLocation loc) throws SimpleObjectIOException {
		if (loc.getSourceFeature() == null) {
			throw new SimpleObjectIOException("Missing source feature for FeatureLocation");
		}
		
		String hql = "from Feature as f join f.featureLocations as fl where fl.fmin<? and fl.fmax>? and fl.sourceFeature=?";
		int strandParam = 0;
		if (loc.getStrand() != null) {
			hql += " and fl.strand=?";
			strandParam = 3;
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setInteger(0, loc.getFmax());
		query.setInteger(1, loc.getFmin());
		query.setEntity(2, loc.getSourceFeature());
		if (loc.getStrand() != null) {
			query.setInteger(strandParam, loc.getStrand());
		}
		Iterator<?> i = query.iterate();

		return new HibernateIterator<Feature>(i);
	}
	
	@Override
	public Iterator<? extends Feature> getAllFeaturesBySourceFeature(Feature sourceFeature) throws SimpleObjectIOException {
		return getAllFeaturesBySourceFeature(sourceFeature, false);
	}

	/** Get all feature objects contained in the source feature in the underlying database.
	 * 
	 * @param sourceFeature - Feature for the source feature
	 * @param nonAnalysisOnly true if only features that are not analyses should be returned
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getAllFeaturesBySourceFeature(Feature sourceFeature, boolean nonAnalysisOnly) throws SimpleObjectIOException {
		String hql = "from Feature as f join f.featureLocations as fl where fl.sourceFeature=?";
		if (nonAnalysisOnly) {
			hql += " and is_analysis=?";
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setEntity(0, sourceFeature);
		if (nonAnalysisOnly) {
			query.setBoolean(1, false);
		}
		Iterator<?> i = query.iterate();

		return new HibernateIterator<Feature>(i);
	}

	@Override
	public Iterator<? extends Feature> getFeaturesByCVTermAndRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException {
		if (loc.getSourceFeature() == null) {
			throw new SimpleObjectIOException("Missing source feature for FeatureLocation");
		}
		String hql = "from Feature as f join f.featureLocations as fl where fl.fmin>=? and fl.fmax<=? and fl.sourceFeature=? and fl.strand=? and f.type.name=? and f.type.cv.name=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setInteger(0, loc.getFmin());
		query.setInteger(1, loc.getFmax());
		query.setEntity(2, loc.getSourceFeature());
		query.setInteger(3, loc.getStrand());
		query.setString(4, cvterm.getName());
		query.setString(5, cvterm.getCv().getName());
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Feature>(i);
	}

	@Override
	public Iterator<? extends Feature> getFeaturesByCVTermAndOverlappingRange(CVTerm cvterm, FeatureLocation loc) throws SimpleObjectIOException {
		if (loc.getSourceFeature() == null) {
			throw new SimpleObjectIOException("Missing source feature for FeatureLocation");
		}
		String hql = "from Feature as f join f.featureLocations as fl where fl.fmin<? and fl.fmax>? and fl.sourceFeature=? and fl.strand=? and f.type.name=? and f.type.cv.name=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setInteger(0, loc.getFmax());
		query.setInteger(1, loc.getFmin());
		query.setEntity(2, loc.getSourceFeature());
		query.setInteger(3, loc.getStrand());
		query.setString(4, cvterm.getName());
		query.setString(5, cvterm.getCv().getName());
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Feature>(i);
	}
	
	/** Write simple objects to the underlying data source.
	 * 
	 * @param simpleObject - AbstractSimpleObject to write
	 * @throws SimpleObjectIOException - Error in processing write request
	 */
	public void write(AbstractSimpleObject simpleObject) throws SimpleObjectIOException {
		Session session = getCurrentSession();
		try {
			session.saveOrUpdate(simpleObject);
		}
		catch (HibernateException e) {
			try {
				session.replicate(simpleObject, ReplicationMode.OVERWRITE);
			}
			catch (HibernateException e2) {
				throw new SimpleObjectIOException("Error writing feature to database: " + e.getMessage());
			}
			catch (Exception e2) {
				System.out.println(e2);
			}
		}
	}

	@Override
	public void write(SimpleObjectIteratorInterface iter) throws SimpleObjectIOException {
		while (iter.hasNext()) {
			write(iter.next());
		}
	}

	/** Delete entry from the underlying database.
	 * 
	 * @param simpleObject - AbstractSimpleObject to be deleted
	 */
	public void delete(AbstractSimpleObject simpleObject) {
		Session session = getCurrentSession();
		session.delete(simpleObject);
	}
	
	/** Begin database transaction.
	 * 
	 */
	public void beginTransaction() {
		Session session = sf.getCurrentSession();
		session.getTransaction().begin();
	}
	
	/** Commit database transaction.
	 * 
	 */
	public void commitTransaction() {
		Session session = sf.getCurrentSession();
		try {
			session.getTransaction().commit();
		}
		catch (Exception e) {
			System.out.println(e);
		}
	}
	
	/** Rollback database transaction.
	 * 
	 */
	public void rollbackTransaction() {
		Session session = sf.getCurrentSession();
		session.getTransaction().rollback();
	}
	
	/** Close database connection.
	 * 
	 */
	public void closeSession() {
		Session session = sf.getCurrentSession();
		session.close();
	}

    public Session getCurrentSession() {
    	beginTransaction();
    	return sf.getCurrentSession();
    }

	@Override
	public Feature getFeature(Organism organism, CVTerm type, String uniquename) throws SimpleObjectIOException {
		String hql = "FROM Feature AS f WHERE f.organism.genus = ? AND f.organism.species = ? AND f.type.name = ? AND f.type.cv.name = ? AND f.uniqueName = ?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, organism.getGenus());
		query.setString(1, organism.getSpecies());
		query.setString(2, type.getName());
		query.setString(3, type.getCv().getName());
		query.setString(4, uniquename);
		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (Feature)i.next();
		}
		else {
			return null;
		}
	}

	/** Get a CV term from the underlying database.
	 * 
	 * @param cvtermName - String for the name of the cvterm
	 * @param cvName - String for the name of the cv
	 * @return CVTerm corresponding the cvterm and cv names
	 */
	public CVTerm getCVTerm(String cvtermName, String cvName) {
		String hql = "from CVTerm where name=? and cv.name=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, cvtermName);
		query.setString(1, cvName);
		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (CVTerm)i.next();
		}
		else {
			return null;
		}
	}

	/** Get a CV from the underlying database.
	 * 
	 * @param cvName - String for the name of the cv
	 * @return CV corresponding the cv name
	 */
	public CV getCV(String cvName) {
		String hql = "from CV where name=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, cvName);
		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (CV)i.next();
		}
		else {
			return null;
		}
	}

	/** Get a organism from the underlying database.
	 * 
	 * @param genus - String for the organism's genus
	 * @param species - String for the oragnism's species
	 * @return Organism corresponding to the genus and species
	 */
	public Organism getOrganism(String genus, String species) {
		String hql = "from Organism where genus=? and species=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, genus);
		query.setString(1, species);
		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (Organism)i.next();
		}
		else {
			return null;
		}
	}

	/** Get organisms that have features associated with them from the underlying database.
	 * 
	 * @return - Iterator for the Organism objects
	 */
	public Iterator<? extends Organism> getOrganismsWithFeatures() {
		String hql = "select distinct organism from Feature as feature join feature.organism as organism";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Organism>(i);
	}
	
	/** Get analyses for a given organism.
	 * 
	 * @param organism - Organism to retrieve analyses from
	 * @return - Iterator for the Analysis objects
	 */
	public Iterator<? extends Analysis> getAnalysesForOrganism(Organism organism) {
		String hql = "select distinct analysis from AnalysisFeature as analysisfeature join analysisfeature.analysis as analysis join analysisfeature.feature as feature join feature.organism as organism where organism.genus=? and organism.species=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, organism.getGenus());
		query.setString(1, organism.getSpecies());
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Analysis>(i);
	}

	/** Get all top level feature objects (features with no parents) overlapping (including partially) a
	 *  specified range and being of a specified analysis type.
	 * 
	 * @param loc - FeatureLocation defining the range to retrieve the features from
	 * @param analysis - Analysis type to retrieve top level features
	 * @return - Iterator for the Feature objects
	 * @throws SimpleObjectIOException - Error in processing read request
	 */
	public Iterator<? extends Feature> getTopLevelFeaturesByOverlappingRangeAndAnalysis(FeatureLocation loc, Analysis analysis) throws SimpleObjectIOException {
		if (loc.getSourceFeature() == null) {
			throw new SimpleObjectIOException("Missing source feature for FeatureLocation");
		}
		
		String hql = "select f from AnalysisFeature as af join af.analysis as a join af.feature as f join f.featureLocations as fl where fl.fmin<? and fl.fmax>? and fl.sourceFeature=? and a=? and size(f.parentFeatureRelationships)=0";
		int strandParam = 0;
		if (loc.getStrand() != null) {
			hql += " and fl.strand=?";
			strandParam = 4;
		}
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setInteger(0, loc.getFmax());
		query.setInteger(1, loc.getFmin());
		query.setEntity(2, loc.getSourceFeature());
		query.setEntity(3, analysis);
		if (loc.getStrand() != null) {
			query.setInteger(strandParam, loc.getStrand());
		}
		Iterator<?> i = query.iterate();

		return new HibernateIterator<Feature>(i);
	}

	/** Get analysis items that are present in the database that also have features in the feature table.
	 * 
	 * @return Iterator for Analysis objects
	 */
	public Iterator<? extends Analysis> getAnalysesWithFeaturesForOrganism(Organism organism) {
		String hql = "select distinct analysis from AnalysisFeature as analysisfeature inner join analysisfeature.analysis as analysis inner join analysisfeature.feature as feature inner join feature.organism as organism where organism.genus=? and organism.species=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(1, organism.getGenus());
		query.setString(2, organism.getSpecies());
		Iterator<?> i = query.iterate();
		return new HibernateIterator<Analysis>(i);
	}
	
	/** Get an organism by its abbreviation.
	 * 
	 * @param abbreviation - Organism abbreviation
	 * @return Organism corresponding to the abbreviation
	 */
	public Organism getOrganismByAbbreviation(String abbreviation){
		String hql = "from Organism where abbreviation=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, abbreviation);

		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (Organism)i.next();
		}
		else {
			return null;
		}
	}
	
	public DBXref getDBXref(String dbName, String accession) {
		String hql = "from DBXref where db.name=? and accession=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, dbName);
		query.setString(1, accession);

		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (DBXref)i.next();
		}
		else {
			return null;
		}
	}

	public DB getDB(String dbName) {
		String hql = "from DB where name=?";
		Session session = getCurrentSession();
		Query query = session.createQuery(hql);
		query.setString(0, dbName);

		Iterator<?> i = query.iterate();
		if (i.hasNext()) {
			return (DB)i.next();
		}
		else {
			return null;
		}
	}
	
	private class HibernateIterator<T> implements Iterator<T> {
		private Iterator<?> hibernateIterator;
		public HibernateIterator(Iterator<?> hibernateIterator) {
			this.hibernateIterator = hibernateIterator;
		}

		public T next() {
			Object o = hibernateIterator.next();
			if (o instanceof Object[]) {
				Object[] objs = (Object [])o;
				return (T)objs[0];
			} else {
				return (T)o;
			}
		}

		public boolean hasNext() {
			return hibernateIterator.hasNext();
		}
		public void remove() {
			hibernateIterator.remove();
		}
	}
	
}
