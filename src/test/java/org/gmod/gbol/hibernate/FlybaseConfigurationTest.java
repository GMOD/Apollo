package org.gmod.gbol.hibernate;

import org.gmod.gbol.simpleObject.CVTerm;
import org.gmod.gbol.simpleObject.CVTermRelationship;
import org.junit.Ignore;

@Ignore
public class FlybaseConfigurationTest extends AbstractGBOLHibernateTest{

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    public FlybaseConfigurationTest(String name) {
        super(name);
        try {

            if (this.sf == null){
                this.configureConnection("src/test/resources/testSupport/flybaseConfig.cfg.xml");
            }
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
            e.printStackTrace();
        }
    }
    
    public void testConfigure(){
        
        CVTerm cvterm = (CVTerm) this.sf.getCurrentSession().get(CVTerm.class, 30);
        logger.info("Parent: " + cvterm.getName());
        for (CVTermRelationship cvtr : cvterm.getChildCVTermRelationships()){
            logger.info(cvtr.getSubjectCVTerm().getName() + " " + cvtr.getType().getName() + " " + cvtr.getObjectCVTerm().getName());
        }
        
        
        
        
    }
    
}
