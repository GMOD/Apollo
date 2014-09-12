package org.bbop.apollo.unit;

import junit.framework.TestCase;
import org.bbop.apollo.tools.seq.search.blat.BlatCommandLine;
import org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

public class BlatCommandLineTest extends TestCase {

    private BioObjectConfiguration conf;

    public void setUp() {
        conf = new BioObjectConfiguration("src/test/resources/testSupport/mapping.xml");
    }
    
    public void testSearch() throws Exception {

        try {
            BlatCommandLine blat = new BlatCommandLineNucleotideToNucleotide();
            blat.setBioObjectConfiguration(conf);
            blat.parseConfiguration("src/test/resources/testSupport/blat_config.xml");
            for (Match match : blat.search("TCGTTTCGATTAAATGTTCCATTCGTAACATCTCACTGAAAGGGGTTGCCAAGTATTATTGTCTGAAACT", "Group1.33")) {
                assertEquals("Query fmin", new Integer(0), match.getQueryFmin());
                assertEquals("Query fmax", new Integer(69), match.getQueryFmax());
                assertEquals("Query strand", new Integer(1), match.getQueryStrand());
                assertEquals("Subject id", "Group1.33", match.getSubjectUniqueName());
                assertEquals("Subject fmin", new Integer(630), match.getSubjectFmin());
                assertEquals("Subject fmax", new Integer(699), match.getSubjectFmax());
                assertEquals("Subject strand", new Integer(1), match.getSubjectStrand());
                assertEquals("Significance", 5.3e-32, match.getSignificance());
                assertEquals("Raw score", 135.0, match.getRawScore());
            }
        } catch (RuntimeException e) {
            String message = e.getMessage();
            if(message.contains("Cannot run program \"/usr/local/bin/blat\": error=2, No such file or directory")){
                // do nothing
                System.out.println("BLAT not installed, ignoring test.");
            }
            else{
                throw e;
            }
        }
    }

}
