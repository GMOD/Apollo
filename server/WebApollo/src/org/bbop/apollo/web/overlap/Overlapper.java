package org.bbop.apollo.web.overlap;

import org.gmod.gbol.bioObject.Gene;
import org.gmod.gbol.bioObject.Transcript;

public interface Overlapper {

	public boolean overlaps(Transcript transcript, Gene gene);
	
	public boolean overlaps(Transcript transcript1, Transcript transcript2);
}
