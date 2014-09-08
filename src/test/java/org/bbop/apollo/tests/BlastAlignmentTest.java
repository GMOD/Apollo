package org.bbop.apollo.tests;

import org.bbop.apollo.tools.seq.search.blast.BlastAlignment;
import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

import junit.framework.TestCase;

public class BlastAlignmentTest extends TestCase {

    private BioObjectConfiguration conf;

    public void setUp() {
        conf = new BioObjectConfiguration("testSupport/mapping.xml");
    }
    
    public void testConvertToMatch() {
        String queryId = "query";
        String subjectId = "subject";
        double percentId = 100;
        int alignmentLength = 75;
        int numMismatches = 0;
        int numGaps = 0;
        int queryStart = 1;
        int queryEnd = 75;
        int subjectStart = 81;
        int subjectEnd = 155;
        double eValue = 2.3e-34;
        double bitscore = 143;
        BlastAlignment alignment = new BlastAlignment(queryId, subjectId, percentId, alignmentLength, numMismatches, numGaps, queryStart, queryEnd, subjectStart, subjectEnd,
                eValue, bitscore);
        Match match = alignment.convertToMatch(conf);
        assertEquals("Query id", queryId, match.getQueryUniqueName());
        assertEquals("Query fmin", new Integer(queryStart - 1), match.getQueryFmin());
        assertEquals("Query fmax", new Integer(queryEnd - 1), match.getQueryFmax());
        assertEquals("Query strand", new Integer(1), match.getQueryStrand());
        assertEquals("Subject id", subjectId, match.getSubjectUniqueName());
        assertEquals("Subject fmin", new Integer(subjectStart - 1), match.getSubjectFmin());
        assertEquals("Subject fmax", new Integer(subjectEnd - 1), match.getSubjectFmax());
        assertEquals("Subject strand", new Integer(1), match.getSubjectStrand());
        assertEquals("Significance", eValue, match.getSignificance());
        assertEquals("Raw score", bitscore, match.getRawScore());
        alignment = new BlastAlignment(queryId, subjectId, percentId, alignmentLength, numMismatches, numGaps, queryEnd, queryStart, subjectStart, subjectEnd,
                eValue, bitscore);
        match = alignment.convertToMatch(conf);
        assertEquals("Query fmin", new Integer(queryStart - 1), match.getQueryFmin());
        assertEquals("Query fmax", new Integer(queryEnd - 1), match.getQueryFmax());
        assertEquals("Query strand", new Integer(-1), match.getQueryStrand());
        alignment = new BlastAlignment(queryId, subjectId, percentId, alignmentLength, numMismatches, numGaps, queryStart, queryEnd, subjectEnd, subjectStart,
                eValue, bitscore);
        match = alignment.convertToMatch(conf);
        assertEquals("Subject fmin", new Integer(subjectStart - 1), match.getSubjectFmin());
        assertEquals("Subject fmax", new Integer(subjectEnd - 1), match.getSubjectFmax());
        assertEquals("Subject strand", new Integer(-1), match.getSubjectStrand());
    }

}
