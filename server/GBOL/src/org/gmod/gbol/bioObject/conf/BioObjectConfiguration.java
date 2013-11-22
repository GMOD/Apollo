package org.gmod.gbol.bioObject.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.gmod.gbol.bioObject.AbstractBioFeature;
import org.gmod.gbol.bioObject.AbstractBioObject;
import org.gmod.gbol.bioObject.AbstractBioFeatureProperty;
import org.gmod.gbol.bioObject.SequenceFeature;
import org.gmod.gbol.bioObject.util.BioObjectUtil;
import org.gmod.gbol.simpleObject.CV;
import org.gmod.gbol.simpleObject.CVTerm;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Class that stores the mapping between classes and types.
 * 
 * @author elee
 *
 */

public class BioObjectConfiguration implements Serializable {

	private Map<CVTerm, String> termToClass;
	private Map<String, CVTerm> classToDefault;
	private Map<String, List<CVTerm>> classToTerms;
	private Map<String, Set<CVTerm>> classToDescendantTerms; 

	/** Constructor.
	 * 
	 * @param xmlFileName - Configuration XML file name for mappings
	 */
	public BioObjectConfiguration(String xmlFileName) {
		try {
			FileInputStream fis = new FileInputStream(xmlFileName);
			init(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			throw new BioObjectConfigurationException("Error reading configuration: " + e.getMessage());
		} catch (IOException e) {
		}
	}
	
	/** Constructor.
	 * 
	 * @param xmlInputStream - Configuration XML input stream for mappings
	 */
	public BioObjectConfiguration(InputStream xmlInputStream) {
		init(xmlInputStream);
	}
	
	/** Get the name of the class corresponding to the given CVTerm.
	 * 
	 * @param cvterm - CVTerm of interest
	 * @return Name of the class.  Returns <code>null</code> if CVTerm doesn't exist
	 */
	public String getClassForCVTerm(CVTerm cvterm)
	{
		return termToClass.get(cvterm);
	}
	
	/** Get the default CVTerm for a given class name.
	 * 
	 * @param className - Class name to get the default CVTerm for
	 * @return Default CVTerm for a given class.  Returns <code>null</code> if no default is set for class name
	 */
	public CVTerm getDefaultCVTermForClass(String className)
	{
		return classToDefault.get(className);
	}
	
	/** Get the associated cvterms for a given class
	 * 
	 * @param className - Class name to get the associated cvterms for
	 * @return Collection of CVTerm objects associated with a given class.  Returns an empty container if none is found
	 */
	public Collection<CVTerm> getCVTermsForClass(String className)
	{
		Collection<CVTerm> classNames = classToTerms.get(className);
		if (classNames == null) {
			System.err.println("No CVTerms for class: " + className);
			return new ArrayList<CVTerm>();
		}
		return classNames;
	}
	
	/** Get the associated cvterms and child features for a given class
	 * 
	 * @param className - Class name to get the associated cvterms for
	 * @return Collection of CVTerm objects associated with a given class.  Returns <code>null</code> if none is found
	 */
	public Collection<CVTerm> getDescendantCVTermsForClass(String className)
	{
		Collection<CVTerm> classNames = classToDescendantTerms.get(className);
		if (classNames == null) {
			return new ArrayList<CVTerm>();
		}
		return classNames;
	}
	
	private void init(InputStream xmlInputStream)
	{
		termToClass = new HashMap<CVTerm, String>();
		classToDefault = new HashMap<String, CVTerm>();
		classToTerms = new HashMap<String, List<CVTerm>>();
		try {
			SchemaFactory sf = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			URL xsd = getClass().getResource("/conf/gbol_mappings.xsd");
			Schema schema = sf.newSchema(xsd);
			Validator validator = schema.newValidator();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new BioObjectConfigurationErrorHandler());
			Document doc = db.parse(xmlInputStream);
			DOMSource source = new DOMSource(doc);
			validator.validate(source);
			processMappings(doc, "feature_mappings");
			processMappings(doc, "feature_property_mappings");
			processMappings(doc, "attribute_mappings");
			processMappings(doc, "relationship_mappings");
			
			// Build classToDescendantTerms from classToTerms
			classToDescendantTerms = new HashMap<String, Set<CVTerm>>();
			String pkg = AbstractBioObject.class.getPackage().getName();

			for (String className : classToTerms.keySet()) {
				Class<? extends AbstractBioObject> current;
				try {
					current = (Class<AbstractBioObject>)Class.forName(pkg + "." + className);
				} catch (ClassNotFoundException e) {
					throw new BioObjectConfigurationException("Error parsing configuration: " + e.getMessage());
				}
				if (!AbstractBioFeature.class.isAssignableFrom(current) &&
						!AbstractBioFeatureProperty.class.isAssignableFrom(current)) {
					continue;
				}
				while (current != AbstractBioFeature.class && current != AbstractBioFeatureProperty.class) {
					String shortClassName = BioObjectUtil.stripPackageNameFromClassName(current.getName());
					if (classToDescendantTerms.get(shortClassName) == null) {
						classToDescendantTerms.put(shortClassName, new HashSet<CVTerm>());
					}
					classToDescendantTerms.get(shortClassName).addAll(classToTerms.get(className));
					
					current = (Class<AbstractBioFeature>)current.getSuperclass();
				}
				
				
			}
		}
		catch (ParserConfigurationException e) {
			throw new BioObjectConfigurationException("Error parsing configuration: " + e.getMessage());
		}
		catch (SAXException e) {
			throw new BioObjectConfigurationException("Error parsing configuration: " + e.getMessage());
		}
		catch (IOException e) {
			throw new BioObjectConfigurationException("Error reading configuration: " + e.getMessage());
		}
	}
	
	private void processMappings(Document doc, String root)
	{
		NodeList featureMappings = doc.getElementsByTagName(root);
		if (featureMappings.getLength() == 0) {
			return;
		}
		NodeList types = ((Element)featureMappings.item(0)).getElementsByTagName("type");
		for (int i = 0; i < types.getLength(); ++i) {
			Element type = (Element)types.item(i);
			String cv = type.getAttribute("cv");
			String term = type.getAttribute("term");
			boolean isDefault = type.getAttribute("default").equals("true");
			String readClass = type.getElementsByTagName("read_class").item(0).getTextContent();
			CVTerm cvterm = new CVTerm(term, new CV(cv));
			termToClass.put(cvterm, readClass);
			if (isDefault) {
				classToDefault.put(readClass, cvterm);
			}
			List<CVTerm> cvterms = classToTerms.get(readClass);
			if (cvterms == null) {
				cvterms = new ArrayList<CVTerm>();
				classToTerms.put(readClass, cvterms);
			}
			cvterms.add(cvterm);
		}
	}
	
	private class BioObjectConfigurationErrorHandler implements ErrorHandler
	{
		
		public void error(SAXParseException e)
		{
			throw new BioObjectConfigurationException("Error in configuration XML: " + e.getMessage());
		}

		public void fatalError(SAXParseException e)
		{
			throw new BioObjectConfigurationException("Error in configuration XML: " + e.getMessage());
		}
		
		public void warning(SAXParseException e)
		{
		}
	}
}
