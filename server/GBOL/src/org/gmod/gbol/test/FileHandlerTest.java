package org.gmod.gbol.test;

import java.io.IOException;

import org.gmod.gbol.simpleObject.io.FileHandler;
import org.gmod.gbol.simpleObject.io.impl.GFF3Handler;

import junit.framework.Assert;
import junit.framework.TestCase;

public class FileHandlerTest extends TestCase {
	
	FileHandler fileHandler;
	private final String filePath = "testSupport/exampleGFF3.gff"; 
	
	public void testConstructor(){
		try {
			this.fileHandler = new GFF3Handler(filePath);
			Assert.assertTrue("Opened file.",true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Assert.assertTrue("Could not open file: " + e.getMessage(),false);
			e.printStackTrace();
		}
	}
}