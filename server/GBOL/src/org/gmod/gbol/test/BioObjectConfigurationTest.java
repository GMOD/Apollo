package org.gmod.gbol.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;

import junit.framework.TestCase;

public class BioObjectConfigurationTest extends TestCase {

	private BioObjectConfiguration conf;

	public BioObjectConfigurationTest() {
		conf = new BioObjectConfiguration("testSupport/gbol_mapping_1.xml");
	}
	
	public void testGetClassForId() {
		assertEquals(conf.getClassForCVTerm(new CVTerm("gene", new CV("SO"))), "Gene");
		assertEquals(conf.getClassForCVTerm(new CVTerm("mygene", new CV("mine"))), "Gene");
		assertNull(conf.getClassForCVTerm(new CVTerm("gene", new CV("mine"))));
		assertEquals(conf.getClassForCVTerm(new CVTerm("part_of", new CV("relationship"))), "PartOf");
	}
	
	public void testGetDefaultForClass() {
		CVTerm geneCvterm = conf.getDefaultCVTermForClass("Gene");
		checkCVTerm(geneCvterm, "gene", "SO");
		CVTerm partOfCvterm = conf.getDefaultCVTermForClass("PartOf");
		checkCVTerm(partOfCvterm, "part_of", "relationship");
	}
	
	public void testGetCVTermsForClass() {
		List<CVTerm> cvterms = (List<CVTerm>)conf.getCVTermsForClass("Gene");
		assertEquals(cvterms.size(), 2);
		CVTerm cvterm1 = cvterms.get(0);
		CVTerm cvterm2 = cvterms.get(1);
		checkCVTerm(cvterm1, "gene", "SO");
		checkCVTerm(cvterm2, "mygene", "mine");
		assertEquals("cvterm for class 'foo': ", 0, conf.getCVTermsForClass("foo").size());
		cvterms = (List<CVTerm>)conf.getCVTermsForClass("PartOf");
		assertEquals(cvterms.size(), 1);
		checkCVTerm(cvterms.get(0), "part_of", "relationship");
	}
	
	public void testGetDescendantCVTermsForClass() {
		Collection<CVTerm> cvterms = conf.getDescendantCVTermsForClass("Frameshift");
		assertEquals(cvterms.size(), 3);
	}
	
	private void checkCVTerm(CVTerm cvterm, String term, String cv) {
		assertEquals(cvterm.getName(), term);
		assertEquals(cvterm.getCv().getName(), cv);
	}
	
}
