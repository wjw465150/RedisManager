// Copyright (c) 2002, Eric D. Friedman All Rights Reserved.

package internal.gnu.trove;

/**
 * This object hashing strategy uses the System.identityHashCode method to
 * provide identity hash codes. These are identical to the value produced by
 * Object.hashCode(), even when the type of the object being hashed overrides
 * that method.
 * 
 * Created: Sat Aug 17 11:13:15 2002
 * 
 * @author Eric Friedman
 * @version $Id: TObjectIdentityHashingStrategy.java,v 1.2 2002/08/18 19:14:28
 *          ericdf Exp $
 */

public final class TObjectIdentityHashingStrategy implements TObjectHashingStrategy {
	/**
	 * Delegates hash code computation to the System.identityHashCode(Object)
	 * method.
	 * 
	 * @param object
	 *          for which the hashcode is to be computed
	 * @return the hashCode
	 */
	public final int computeHashCode(Object object) {
		return System.identityHashCode(object);
	}

	/**
	 * Compares object references for equality.
	 * 
	 * @param o1
	 *          an <code>Object</code> value
	 * @param o2
	 *          an <code>Object</code> value
	 * @return true if o1 == o2
	 */
	public final boolean equals(Object o1, Object o2) {
		return o1 == o2;
	}
} // TObjectIdentityHashingStrategy
