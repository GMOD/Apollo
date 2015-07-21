package org.bbop.apollo.report

import org.bbop.apollo.Sequence

/**
 * Created by nathandunn on 7/17/15.
 */
class SequenceSummary extends OrganismSummary{

    Sequence sequence
    String getName(){sequence.name}
    Long getId(){sequence.id}
    Integer getLength(){sequence.length}

}
