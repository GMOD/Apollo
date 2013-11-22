package org.gmod.gbol.simpleObject.io;


public abstract class AbstractSimpleIO implements SimpleObjectIOInterface {

	/*
	public boolean write(Collection<AbstractSimpleObject> simpleObjects)
	{
		List<AbstractSimpleObject> flatSimpleObjects = new ArrayList<AbstractSimpleObject>();
		for (AbstractSimpleObject o : simpleObjects) {
			flatSimpleObjects.addAll(o.getWriteableObjects());
		}
		return writeFlattenedObjects(flatSimpleObjects);
	}
	*/
	
	//abstract protected boolean writeFlattenedObjects(Collection<AbstractSimpleObject> flatSimpleObjects);
}
