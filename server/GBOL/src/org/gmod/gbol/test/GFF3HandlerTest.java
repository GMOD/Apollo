package org.gmod.gbol.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.gmod.gbol.simpleObject.io.FileHandler;
import org.gmod.gbol.simpleObject.io.impl.GFF3Handler;
import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.Feature;
import org.gmod.gbol.simpleObject.FeatureRelationship;

import junit.framework.Assert;
import junit.framework.TestCase;

public class GFF3HandlerTest extends TestCase {
	
	FileHandler fileHandler;
	private final String filePath = "testSupport/exampleGFF3.gff"; 	
	@Override
	public void setUp(){
		try {
			this.fileHandler = new GFF3Handler(filePath);
			((GFF3Handler)this.fileHandler).setSourceFeatureType("chromosome");
			((GFF3Handler)this.fileHandler).setSequenceOntologyName("SO");
		} catch (IOException e) {
			System.err.println("Unable to open " + filePath + " for GFF3HandlerTest support.");
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void testGFF3Read(){
		Collection<Feature> features = new ArrayList<Feature>();
		
		try {
			for (Iterator<? extends AbstractSimpleObject> iter = this.fileHandler.readAll(); iter.hasNext();) {
				features.add((Feature)iter.next());
			}
			System.out.println("Read " + features.size() + " features.");
			System.out.println("Found " + ((GFF3Handler)this.fileHandler).getTopLevelFeatures().size() + " top level features.");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.assertTrue("Failed to read " + filePath + ".\nERROR: " + e.getMessage() + "\n",false);
		}
		for (Feature f : ((GFF3Handler)this.fileHandler).getTopLevelFeatures()){
			printFeature(f,0);
		}
	}
	
	private void printFeature(Feature f, int level){
		for (int i=0;i<level;i++){
			System.out.print("\t");
		}
		System.out.println(f.getType().getName() + ":\t" + f.getUniqueName());
		for (FeatureRelationship fr : f.getChildFeatureRelationships()){
			printFeature(fr.getSubjectFeature(),(level+1));
		}
	}
	
	
	
	
	
	
	
}