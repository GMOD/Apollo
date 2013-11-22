package org.gmod.gbol.bioObject;

import org.gmod.gbol.simpleObject.AbstractSimpleObject;
import org.gmod.gbol.simpleObject.SimpleObjectIteratorInterface;

/** Abstract class for iterators used to retrieve AbstractSimpleObjects (mainly used for writing
 *  framework).
 * 
 * @author elee
 *
 */
public abstract class AbstractSimpleObjectIterator implements SimpleObjectIteratorInterface {

	protected AbstractSimpleObject current;

	public void remove() {
	}
}
