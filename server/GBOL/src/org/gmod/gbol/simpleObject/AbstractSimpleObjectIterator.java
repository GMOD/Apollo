package org.gmod.gbol.simpleObject;

import java.util.Collection;
import java.util.Iterator;

public abstract class AbstractSimpleObjectIterator implements SimpleObjectIteratorInterface {

	protected static class Status
	{
		public final static int done = -1;
		public final static int self = 0;
		public final static int notSet = Integer.MAX_VALUE;
	}
	
	protected AbstractSimpleObject object;
	protected AbstractSimpleObject current;
	protected AbstractSimpleObject clone;
	protected int status;
	protected AbstractSimpleObjectIterator soIter;
	protected Iterator<? extends AbstractSimpleObject> containerIter;
	
	public AbstractSimpleObjectIterator(AbstractSimpleObject object)
	{
		this.object = object;
		status = Status.notSet;
		clone = object.generateClone();
	}
	
	public AbstractSimpleObject peek()
	{
		if (status == Status.self) {
			current = clone;
		}
		return current;
	}

	public boolean hasNext() {
		/*
		if (status == Status.self) {
			return true;
		}
		if (status == Status.notSet) {
			return true;
		}
		if (status == Status.done) {
			return false;
		}
		if (soIter == null) {
			return false;
		}
		return soIter.hasNext();
		*/
		if (status == Status.done) {
			return false;
		}
		return true;
	}

	public abstract AbstractSimpleObject next();

	public void remove()
	{
	}
	
	protected AbstractSimpleObject processCollectionIterators(int newStatus,
			Collection<? extends AbstractSimpleObject> container)
	{
		AbstractSimpleObject retVal = null;
		if (soIter == null || !soIter.hasNext()) {
			if (containerIter != null && containerIter.hasNext()) {
				soIter = containerIter.next().getWriteableObjects();
			}
			else {
				status = newStatus;
				retVal = current;
				if (container != null) {
					containerIter = container.iterator();
					if (containerIter.hasNext()) {
						soIter = containerIter.next().getWriteableObjects();
					}
				}
			}
		}
		return retVal;
	}
	
	protected AbstractSimpleObject processSingletonIterator(int newStatus, AbstractSimpleObject obj)
	{
		AbstractSimpleObject retVal = null;
		if (soIter == null || !soIter.hasNext()) {
			retVal = current;
			current = null;
			status = newStatus;
			if (obj != null) {
				soIter = obj.getWriteableObjects();
			}
		}
		return retVal;
	}
	
	protected AbstractSimpleObject processLastCollectionIterator()
	{
		AbstractSimpleObject retVal = null;
		if (!soIter.hasNext()) {
			if (containerIter != null && containerIter.hasNext()) {
				soIter = containerIter.next().getWriteableObjects();
			}
			else {
				retVal = current;
				status = Status.self;
			}
		}
		return retVal;
	}
	
	protected AbstractSimpleObject processLastSingletonIterator()
	{
		AbstractSimpleObject retVal = null;
		if (soIter == null || !soIter.hasNext()) {
			retVal = current;
			status = Status.self;
		}
		return retVal;
	}
	
}
