package org.gmod.gbol.hibernate;

public class FlybaseGenotypeTest extends AbstractGBOLHibernateTest{

    public FlybaseGenotypeTest(String name) {
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
    
    public void testAllelQuery(){
        System.out.println("YO!");
        
    }
    
}
