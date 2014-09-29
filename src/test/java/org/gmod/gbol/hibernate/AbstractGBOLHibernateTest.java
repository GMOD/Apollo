package org.gmod.gbol.hibernate;

import junit.framework.TestCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gmod.gbol.util.HibernateUtil;
import org.hibernate.SessionFactory;
import org.junit.Ignore;

@Ignore
public class AbstractGBOLHibernateTest extends TestCase {

    private final Logger logger = LogManager.getLogger(LogManager.ROOT_LOGGER_NAME);

    SessionFactory sf;
//    private final String log4jPropFile = "src/test/resources/testSupport/log4j.properties";
    
    public AbstractGBOLHibernateTest(String name) {
        super(name);
//        PropertyConfigurator.configure(this.log4jPropFile);
    }
    
    public void configureConnection(String filename) throws Exception{
        try {
            this.sf = HibernateUtil.buildSessionFactory(filename);
            
        } catch (Exception e) {
            logger.error("Unable configure session factory for GBOL Test: " + e.getMessage());
            e.printStackTrace();
            throw (e);
        }
    }

    protected void setUp() throws Exception {
        this.sf.getCurrentSession().getTransaction().begin();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        this.sf.getCurrentSession().getTransaction().rollback();
        this.sf.getCurrentSession().close();
        super.tearDown();
    }

}
