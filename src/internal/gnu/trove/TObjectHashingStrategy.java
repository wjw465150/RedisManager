// Copyright (c) 2002, Eric D. Friedman All Rights Reserved.

package internal.gnu.trove;

import java.io.Serializable;

/**
 * Interface to support pluggable hashing strategies in maps and sets.
 * Implementors can use this interface to make the trove hashing algorithms use
 * object values, values provided by the java runtime, or a custom strategy when
 * computing hashcodes.
 * 
 * Created: Sat Aug 17 10:52:32 2002
 * 
 * @author Eric Friedman
 * @version $Id: TObjectHashingStrategy.java,v 1.1 2002/08/18 16:43:15 ericdf
 *          Exp $
 */

public interface TObjectHashingStrategy extends Serializable {

	/**
	 * Computes a hash code for the specified object. Implementors can use the
	 * object's own <tt>hashCode</tt> method, the Java runtime's
	 * <tt>identityHashCode</tt>, or a custom scheme.
	 * 
	 * @param object
	 *          for which the hashcode is to be computed
	 * @return the hashCode
	 */
	public int computeHashCode(Object o);

	/**
	 * Compares o1 and o2 for equality. Strategy implementors may use the objects'
	 * own equals() methods, compare object references, or implement some custom
	 * scheme.
	 * 
	 * @param o1
	 *          an <code>Object</code> value
	 * @param o2
	 *          an <code>Object</code> value
	 * @return true if the objects are equal according to this strategy.
	 */
	public boolean equals(Object o1, Object o2);
} // TObjectHashingStrategy
