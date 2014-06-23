package org.bbop.apollo.tools.seq.search;

import org.gmod.gbol.bioObject.Match;
import org.gmod.gbol.bioObject.conf.BioObjectConfiguration;

public interface Alignment {

    public Match convertToMatch(BioObjectConfiguration conf);

}
