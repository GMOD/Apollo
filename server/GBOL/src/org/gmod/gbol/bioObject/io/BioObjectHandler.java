package org.gmod.gbol.bioObject.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractBioObject;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;
import org.gmod.gbol.simpleObject.io.SimpleObjectIOInterface;

/** I/O handler for processing Bio objects.
 * 
 * @author elee
 *
 */

public class BioObjectHandler implements BioObjectIOInterface {

	private BioObjectConfiguration conf;
	private SimpleObjectIOInterface handler;
	
	/** Constructor.
	 * 
	 * @param conf - File name of configuration containing mapping information
	 * @param handler - SimpleObjectIOInterface that handles the underlying simple object data source
	 */
	public BioObjectHandler(String conf, SimpleObjectIOInterface handler)
	{
		this(new BioObjectConfiguration(conf), handler);
	}

	/** Constructor.
	 * 
	 * @param conf - Configuration containing mapping information
	 * @param handler - SimpleObjectIOInterface that handles the underlying simple object data source
	 */
	public BioObjectHandler(BioObjectConfiguration conf, SimpleObjectIOInterface handler)
	{
		this.conf = conf;
		this.handler = handler;
	}
	
	private class BioObjectIterator<T extends AbstractBioFeature> implements Iterator<T> {
		protected Stack<Iterator<? extends AbstractSimpleObject>> iterators = new Stack<Iterator<? extends AbstractSimpleObject>>();
		protected Iterator<? extends AbstractSimpleObject> currentIterator;
		public BioObjectIterator(Iterator<? extends AbstractSimpleObject> iterator) {
			this.iterators.add(iterator);
		}
		public BioObjectIterator(Collection<Iterator<? extends AbstractSimpleObject>> iterators) {
			this.iterators.addAll(iterators);
		}
		public boolean hasNext() {
			if (iterators == null)
				return false;
			if (currentIterator == null && iterators.size() == 0)
				return false;
			if (currentIterator == null)
				currentIterator = iterators.pop();
			return currentIterator.hasNext();
		}
		public void remove() {
			if (currentIterator != null)
				currentIterator.remove();
		}
		public T next() {
			if (currentIterator == null && iterators.size() > 0)
				currentIterator = iterators.pop();
			// TODO: Find out what exception we should throw if we're out of iterators
			return ((T) BioObjectUtil.createBioObject(currentIterator.next(), conf));
		}
	}
	
	public Iterator<AbstractBioFeature> getAllFeatures() throws Exception
	{
		return new BioObjectIterator<AbstractBioFeature>(handler.getAllFeatures());
	}

	public Iterator<AbstractBioFeature> getFeaturesByRange(
		String organismGenus, String organismSpecies, String sourceFeatureTypeCVName, String sourceFeatureTypeCVTermName,
		String sourceFeatureUniqueName, int fmin, int fmax, int strand) throws Exception
	{
		Organism o = new Organism();
		o.setGenus(organismGenus);
		o.setSpecies(organismSpecies);
		CVTerm c = new CVTerm(sourceFeatureTypeCVTermName, new CV(sourceFeatureTypeCVName));
		Feature sourceFeature = handler.getFeature(o, c, sourceFeatureUniqueName);
		
		FeatureLocation loc = new FeatureLocation();
		loc.setFmin(fmin);
		loc.setFmax(fmax);
		loc.setSourceFeature(sourceFeature);
		loc.setStrand(strand);
		
		return new BioObjectIterator<AbstractBioFeature>(handler.getAllFeaturesByRange(loc));
	}
	
	public Iterator<Gene> getAllGenes() throws Exception
	{
		Collection<CVTerm> cvterms =
			conf.getDescendantCVTermsForClass(BioObjectUtil.stripPackageNameFromClassName(Gene.class.getName()));
		ArrayList<Iterator<? extends AbstractSimpleObject>> termIterators = new ArrayList<Iterator<? extends AbstractSimpleObject>>();
		for (CVTerm cvterm : cvterms) {
			termIterators.add(handler.getFeaturesByCVTerm(cvterm));
		}
		return new BioObjectIterator<Gene>(termIterators);
	}

	public Iterator<Gene> getGenesByRange(
		String organismGenus, String organismSpecies, String sourceFeatureTypeCVName, String sourceFeatureTypeCVTermName,
		String sourceFeatureUniqueName, int fmin, int fmax, int strand) throws Exception
	{
		Collection<CVTerm> cvterms =
			conf.getDescendantCVTermsForClass(BioObjectUtil.stripPackageNameFromClassName(Gene.class.getName()));
		
		Organism o = new Organism();
		o.setGenus(organismGenus);
		o.setSpecies(organismSpecies);
		CVTerm c = new CVTerm(sourceFeatureTypeCVTermName, new CV(sourceFeatureTypeCVName));
		Feature sourceFeature = handler.getFeature(o, c, sourceFeatureUniqueName);
		
		FeatureLocation loc = new FeatureLocation();
		loc.setFmin(fmin);
		loc.setFmax(fmax);
		loc.setSourceFeature(sourceFeature);
		loc.setStrand(strand);
		
		ArrayList<Iterator<? extends AbstractSimpleObject>> termIterators = new ArrayList<Iterator<? extends AbstractSimpleObject>>();
		for (CVTerm cvterm : cvterms) {
			termIterators.add(handler.getFeaturesByCVTermAndRange(cvterm, loc));
		}
		return new BioObjectIterator<Gene>(termIterators);
	}
	
	public void write(Collection<? extends AbstractBioFeature> features)
	{
		for (AbstractBioFeature f : features) {
			SimpleObjectIteratorInterface i = f.getWriteableSimpleObjects(conf);
			try {
				handler.write(i);
			}
			catch (Exception e) {
				throw new RuntimeException("Error writing object: " + e.getMessage());
			}
			
			/*
			while (i.hasNext()) {
				AbstractSimpleObject o = i.next();
				*/
				/*
				if (o instanceof Feature) {
					System.out.println(toString((Feature)o));
				}
				else {
					System.out.println(toString((FeatureRelationship)o));
				}
				*/
				/*
				System.out.println(o.getClass().getName());
				SimpleObjectIteratorInterface j = o.getWriteableObjects();
				while (j.hasNext()) {
					AbstractSimpleObject o2 = j.next();
					System.out.println("\t" + o2.getClass().getName());
				}
				*/
			/*
			}
			*/
		}
	}
	
	public String toString(Feature f)
	{
		FeatureLocation fl = f.getFeatureLocations().iterator().next();
		String retVal = String.format("%s\t%s\t%d\t%d", f.getName(), f.getType().getName(), fl.getFmin(), fl.getFmax());
		if (fl.getSourceFeature() != null) {
			retVal += "\n\t\t[srcfeature]" + toString(fl.getSourceFeature());
		}
		return retVal;
	}
}
