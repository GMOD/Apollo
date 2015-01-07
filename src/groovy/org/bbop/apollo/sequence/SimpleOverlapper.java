package org.bbop.apollo.sequence;

import org.bbop.apollo.web.overlap.Overlapper;
import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.Transcript;

public class SimpleOverlapper implements Overlapper {

    @Override
    public boolean overlaps(Transcript transcript, Gene gene) {
        return transcript.overlaps(gene);
    }

    @Override
    public boolean overlaps(Transcript transcript1, Transcript transcript2) {
        return transcript1.overlaps(transcript2);
    }
    
}
