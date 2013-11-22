package org.gmod.gbol.util;

import java.io.Serializable;
import java.util.Comparator;

/** Implementation of a hash comparator -- mainly used for TreeSet since it appears that HashMap (and thus HashSet) is broken
 *  in Java for serialization...
 *  
 * @author elee
 */

public class HashComparator<T> implements Comparator<T>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(T o1, T o2) {
		int hash1 = o1.hashCode();
		int hash2 = o2.hashCode();
		if (hash1 < hash2) {
			return -1;
		}
		else if (hash1 > hash2) {
			return 1;
		}
		return 0;
	}
	
}
