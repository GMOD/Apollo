package org.bbop.apollo.sequence;


import org.bbop.apollo.Gene;
import org.bbop.apollo.Transcript;

public class NoOverlapper implements Overlapper {

    @Override
    public boolean overlaps(Transcript transcript, Gene gene) {
        return false;
    }

    @Override
    public boolean overlaps(Transcript transcript1, Transcript transcript2) {
        return false;
    }
}
