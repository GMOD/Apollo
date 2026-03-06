package org.bbop.apollo.sequence

import org.bbop.apollo.Gene
import org.bbop.apollo.Transcript

/**
 * Created by ndunn on 10/29/14.
 */
interface Overlapper {
    public boolean overlaps(Transcript transcript, Gene gene);

    public boolean overlaps(Transcript transcript1, Transcript transcript2);
}
