package org.gmod.gbol.simpleObject;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractSimpleObject {

	public Collection<AbstractSimpleObject> getWriteObjects() {
		ArrayList<AbstractSimpleObject> objects = new ArrayList<AbstractSimpleObject>();
		for (AbstractSimpleObjectIterator iter = this.getWriteableObjects(); iter.hasNext(); ) {
			objects.add(iter.next());
		}
		return objects;
	}

	abstract public AbstractSimpleObjectIterator getWriteableObjects();

	abstract public AbstractSimpleObject generateClone();
}