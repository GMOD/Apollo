package org.bbop.apollo.unit;

import org.bbop.apollo.tools.seq.search.blat.BlatCommandLine;
import org.bbop.apollo.tools.seq.search.blat.BlatCommandLineNucleotideToNucleotide;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

import junit.framework.TestCase;

public class BlatCommandLineTest extends TestCase {

    private BioObjectConfiguration conf;

    public void setUp() {
        conf = new BioObjectConfiguration("src/test/resources/testSupport/mapping.xml");
    }
    
    public void testSearch() throws Exception {
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
    }

}
