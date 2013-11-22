package org.gmod.gbol.util;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.w3c.dom.Document;

/**
 * HibernateUtil is a class that creates a new Hibernate {@link SessionFactory}
 * based on the users configuration file.
 * 
 * @see SessionFactory
 * @see HibernateException
 * @see Configuration
 * @author Robert Bruggner
 * 
 */
public class HibernateUtil {
       
    static Map name2factory = new ConcurrentHashMap();

    public synchronized static SessionFactory getSessionFactory(String filename) throws Exception  {
	SessionFactory sfact = (SessionFactory)name2factory.get(filename);
	if (sfact == null)  {
	    sfact = HibernateUtil.buildSessionFactory(filename);
	    name2factory.put(filename, sfact);
	}
	return sfact;
    }

	/**
	 * Returns a new Hibernate {@link SessionFactory} given a hibernate configuration file.
	 * 
	 * @param filename
	 *            Hibernate configuration filename
	 * @return a {@link SessionFactory}
	 * @throws Exception
	 *             when configuration of the Hibernate {@link SessionFactory} fails.  
	 */
    public static SessionFactory buildSessionFactory(String filename) throws Exception{
    	try {
    		 
    		 System.out.println("Trying to configure new SessionFactory using " + filename);
    		 File f = new File(filename);
    		 
    		 Configuration c = new Configuration().configure(f);
    		 SessionFactory sessionFactory = c.buildSessionFactory();
    		 return sessionFactory;
    	} catch (HibernateException he){
    		System.out.println("Couldn't build session!");
    		System.out.println(he.getMessage());
    		System.err.println("Couldn't build session. " + he.getMessage());
    		he.printStackTrace();
    		throw he;
    	}
    }
    
	/**
	 * Returns a new Hibernate {@link SessionFactory} given an InputStream for a hibernate configuration file.
	 * 
	 * @param config
	 *            InputStream for a Hibernate configuration filename
	 * @return a {@link SessionFactory}
	 * @throws Exception
	 *             when configuration of the Hibernate {@link SessionFactory} fails.  
	 */
    public static SessionFactory buildSessionFactory(InputStream config) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(config);
		return buildSessionFactory(doc);
    }
    
	/**
	 * Returns a new Hibernate {@link SessionFactory} given an Document for a hibernate configuration file.
	 * 
	 * @param config
	 *            Document for a Hibernate configuration filename
	 * @return a {@link SessionFactory}
	 * @throws Exception
	 *             when configuration of the Hibernate {@link SessionFactory} fails.  
	 */
    public static SessionFactory buildSessionFactory(Document config) throws Exception {
    	try {
    		Configuration c = new Configuration().configure(config);
    		SessionFactory sessionFactory = c.buildSessionFactory();
    		return sessionFactory;
    	}
    	catch (HibernateException he) {
    		System.out.println("Couldn't build session!");
    		System.out.println(he.getMessage());
    		System.err.println("Couldn't build session. " + he.getMessage());
    		he.printStackTrace();
    		throw he;
    	}
    }
}