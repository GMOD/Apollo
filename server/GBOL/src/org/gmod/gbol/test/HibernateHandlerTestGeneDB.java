package org.gmod.gbol.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureLocation;
import org.gmod.gbol.simpleObject.Organism;
import org.gmod.gbol.simpleObject.io.impl.HibernateHandler;

public class HibernateHandlerTestGeneDB extends TestCase {

	private HibernateHandler handler;
	
	public HibernateHandlerTestGeneDB() throws Exception
	{
		PropertyConfigurator.configure("testSupport/log4j.properties");
		try {
			handler = new HibernateHandler("testSupport/genedb.cfg.xml");
		}
		catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
			throw e;
		}
	}
	
	public void testGetFeaturesByCVTerm() throws Exception
	{
		Collection<Feature> genes = new ArrayList<Feature>();
		for (Iterator<? extends Feature> iter = handler.getFeaturesByCVTerm(new CVTerm("snoRNA", new CV("sequence"))); iter.hasNext();) {
			genes.add(iter.next());
		}
		assertEquals("Number of snoRNAs", 3743, genes.size());
		for (Feature f : genes) {
			System.out.println(f.getName());
		}
	}
	
	public void testGetAllFeaturesByRangeWithSrcfeature() throws Exception 
	{
		Organism o = new Organism();
		o.setGenus("Leishmania");
		o.setSpecies("major strain Friedlin");
		CVTerm c = new CVTerm("chromosome", new CV("sequence"));
		Feature chr = handler.getFeature(o, c, "Lmjchr1");

		FeatureLocation loc = new FeatureLocation();
		loc.setFmin(0);
		loc.setFmax(1000);
		loc.setSourceFeature(chr);
		loc.setStrand(0);
		Collection<Feature> features = new ArrayList<Feature>();
		for (Iterator<? extends Feature> iter = handler.getAllFeaturesByRange(loc); iter.hasNext(); ) {
			features.add(iter.next());
		}
		
		assertEquals("Number of features on Lmjchr1 from 1-1000", 2, features.size());
	}
	
	public void testGetChromosomeByOrganismAndTypeAndName() throws Exception
	{
		Organism o = new Organism();
		o.setGenus("Leishmania");
		o.setSpecies("major strain Friedlin");
		CVTerm c = new CVTerm("chromosome", new CV("sequence"));
		Feature chr = handler.getFeature(o, c, "Lmjchr1");
		
		assertEquals("Chromosome feature is named Lmjchr1", "Lmjchr1", chr.getUniqueName());
	}

	public void testGetFeaturesByCVTermAndRange() throws Exception
	{
		Organism o = new Organism();
		o.setGenus("Leishmania");
		o.setSpecies("major strain Friedlin");
		CVTerm c = new CVTerm("chromosome", new CV("sequence"));
		Feature chr = handler.getFeature(o, c, "Lmjchr1");
		assertNotNull(chr);

		FeatureLocation loc = new FeatureLocation();
		loc.setFmin(0);
		loc.setFmax(1000);
		loc.setStrand(0);
		loc.setSourceFeature(chr);

		Collection<Feature> features = new ArrayList<Feature>();
		for (Iterator<? extends Feature> iter = handler.getFeaturesByCVTermAndRange(new CVTerm("direct_repeat", new CV("sequence")), loc); iter.hasNext(); ) {
			features.add(iter.next());
		}
		assertEquals("Number of direct_repeats (1-1000)", 1, features.size());
	}
	
	protected void setUp()
	{
		//handler.beginTransaction();
	}
	
	protected void tearDown()
	{
		//handler.rollbackTransaction();
		//handler.closeSession();
	}
}
