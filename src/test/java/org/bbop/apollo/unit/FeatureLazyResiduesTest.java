package org.bbop.apollo.unit;

import junit.framework.TestCase;
import org.bbop.apollo.web.data.FeatureLazyResidues;
import org.bbop.apollo.web.data.FeatureSequenceChunkManager;
import org.junit.Ignore;

/**
 * No seq_data directory in old or new, so just ignoring
 */
@Ignore
public class FeatureLazyResiduesTest extends TestCase {

    private FeatureSequenceChunkManager chunkManager;
    private String track;
    
    public FeatureLazyResiduesTest() {
        track = "test";
        chunkManager = FeatureSequenceChunkManager.getInstance(track);
        chunkManager.setChunkSize(20000);
        chunkManager.setSequenceDirectory("src/test/resources/testSupport/seq_data");
    }

    public void testGetResidues() {
        FeatureLazyResidues feature = new FeatureLazyResidues(track);
        assertEquals("getResidues(0, 10): ", "CGACAATGCA", feature.getResidues(0, 10));
        assertEquals("getResidues(19990, 20000): ", "TATATTAATT", feature.getResidues(19990, 20000));
        assertEquals("getResidues(19990, 20010): ", "TATATTAATTGTGGCCGAAT", feature.getResidues(19990, 20010));
        assertEquals("getResidues(19990, 20001): ", "TATATTAATTG", feature.getResidues(19990, 20001));
        feature.getResidues(0, 40001);
    }

}
