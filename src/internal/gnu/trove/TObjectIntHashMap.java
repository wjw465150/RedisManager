///////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2001, Eric D. Friedman All Rights Reserved.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
///////////////////////////////////////////////////////////////////////////////

package internal.gnu.trove;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * An open addressed Map implementation for Object keys and int values.
 * 
 * Created: Sun Nov 4 08:52:45 2001
 * 
 * @author Eric D. Friedman
 * @version $Id: TObjectIntHashMap.java,v 1.13 2003/03/23 04:06:59 ericdf Exp $
 */
public class TObjectIntHashMap extends TObjectHash implements Serializable {

	/** the values of the map */
	protected transient int[] _values;

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance with the default
	 * capacity and load factor.
	 */
	public TObjectIntHashMap() {
		super();
	}

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance with a prime capacity
	 * equal to or greater than <tt>initialCapacity</tt> and with the default load
	 * factor.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 */
	public TObjectIntHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance with a prime capacity
	 * equal to or greater than <tt>initialCapacity</tt> and with the specified
	 * load factor.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 * @param loadFactor
	 *          a <code>float</code> value
	 */
	public TObjectIntHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance with the default
	 * capacity and load factor.
	 * 
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TObjectIntHashMap(TObjectHashingStrategy strategy) {
		super(strategy);
	}

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance whose capacity is the
	 * next highest prime above <tt>initialCapacity + 1</tt> unless that value is
	 * already prime.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TObjectIntHashMap(int initialCapacity, TObjectHashingStrategy strategy) {
		super(initialCapacity, strategy);
	}

	/**
	 * Creates a new <code>TObjectIntHashMap</code> instance with a prime value at
	 * or near the specified capacity and load factor.
	 * 
	 * @param initialCapacity
	 *          used to find a prime capacity for the table.
	 * @param loadFactor
	 *          used to calculate the threshold over which rehashing takes place.
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TObjectIntHashMap(int initialCapacity, float loadFactor, TObjectHashingStrategy strategy) {
		super(initialCapacity, loadFactor, strategy);
	}

	/**
	 * @return an iterator over the entries in this map
	 */
	public TObjectIntIterator iterator() {
		return new TObjectIntIterator(this);
	}

	/**
	 * initializes the hashtable to a prime capacity which is at least
	 * <tt>initialCapacity + 1</tt>.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 * @return the actual capacity chosen
	 */
	protected int setUp(int initialCapacity) {
		int capacity;

		capacity = super.setUp(initialCapacity);
		_values = new int[capacity];
		return capacity;
	}

	/**
	 * Inserts a key/value pair into the map.
	 * 
	 * @param key
	 *          an <code>Object</code> value
	 * @param value
	 *          an <code>int</code> value
	 * @return the previous value associated with <tt>key</tt>, or null if none
	 *         was found.
	 */
	public int put(Object key, int value) {
		int previous = (int) 0;
		int index = insertionIndex(key);
		boolean isNewMapping = true;
		if (index < 0) {
			index = -index - 1;
			previous = _values[index];
			isNewMapping = false;
		}
		Object oldKey = _set[index];
		_set[index] = key;
		_values[index] = value;

		if (isNewMapping) {
			postInsertHook(oldKey == null);
		}
		return previous;
	}

	/**
	 * rehashes the map to the new capacity.
	 * 
	 * @param newCapacity
	 *          an <code>int</code> value
	 */
	protected void rehash(int newCapacity) {
		int oldCapacity = _set.length;
		Object oldKeys[] = _set;
		int oldVals[] = _values;

		_set = new Object[newCapacity];
		_values = new int[newCapacity];

		for (int i = oldCapacity; i-- > 0;) {
			if (oldKeys[i] != null && oldKeys[i] != REMOVED) {
				Object o = oldKeys[i];
				int index = insertionIndex(o);
				if (index < 0) {
					throwObjectContractViolation(_set[(-index - 1)], o);
				}
				_set[index] = o;
				_values[index] = oldVals[i];
			}
		}
	}

	/**
	 * retrieves the value for <tt>key</tt>
	 * 
	 * @param key
	 *          an <code>Object</code> value
	 * @return the value of <tt>key</tt> or null if no such mapping exists.
	 */
	public int get(Object key) {
		int index = index(key);
		return index < 0 ? (int) 0 : _values[index];
	}

	/**
	 * Empties the map.
	 * 
	 */
	public void clear() {
		super.clear();
		Object[] keys = _set;
		int[] vals = _values;

		for (int i = keys.length; i-- > 0;) {
			keys[i] = null;
			vals[i] = (int) 0;
		}
	}

	/**
	 * Deletes a key/value pair from the map.
	 * 
	 * @param key
	 *          an <code>Object</code> value
	 * @return an <code>int</code> value
	 */
	public int remove(Object key) {
		int prev = (int) 0;
		int index = index(key);
		if (index >= 0) {
			prev = _values[index];
			removeAt(index); // clear key,state; adjust size
		}
		return prev;
	}

	/**
	 * Compares this map with another map for equality of their stored entries.
	 * 
	 * @param other
	 *          an <code>Object</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean equals(Object other) {
		if (!(other instanceof TObjectIntHashMap)) {
			return false;
		}
		TObjectIntHashMap that = (TObjectIntHashMap) other;
		if (that.size() != this.size()) {
			return false;
		}
		return forEachEntry(new EqProcedure(that));
	}

	private static final class EqProcedure implements TObjectIntProcedure {
		private final TObjectIntHashMap _otherMap;

		EqProcedure(TObjectIntHashMap otherMap) {
			_otherMap = otherMap;
		}

		public final boolean execute(Object key, int value) {
			int index = _otherMap.index(key);
			if (index >= 0 && eq(value, _otherMap.get(key))) {
				return true;
			}
			return false;
		}

		/**
		 * Compare two ints for equality.
		 */
		private final boolean eq(int v1, int v2) {
			return v1 == v2;
		}

	}

	/**
	 * removes the mapping at <tt>index</tt> from the map.
	 * 
	 * @param index
	 *          an <code>int</code> value
	 */
	protected void removeAt(int index) {
		super.removeAt(index); // clear key, state; adjust size
		_values[index] = (int) 0;
	}

	/**
	 * Returns the values of the map.
	 * 
	 * @return a <code>Collection</code> value
	 */
	public int[] getValues() {
		int[] vals = new int[size()];
		int[] v = _values;
		Object[] keys = _set;

		for (int i = v.length, j = 0; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED) {
				vals[j++] = v[i];
			}
		}
		return vals;
	}

	/**
	 * returns the keys of the map.
	 * 
	 * @return a <code>Set</code> value
	 */
	public Object[] keys() {
		Object[] keys = new Object[size()];
		Object[] k = _set;

		for (int i = k.length, j = 0; i-- > 0;) {
			if (k[i] != null && k[i] != REMOVED) {
				keys[j++] = k[i];
			}
		}
		return keys;
	}

	/**
	 * checks for the presence of <tt>val</tt> in the values of the map.
	 * 
	 * @param val
	 *          an <code>int</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean containsValue(int val) {
		Object[] keys = _set;
		int[] vals = _values;

		for (int i = vals.length; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED && val == vals[i]) {
				return true;
			}
		}
		return false;
	}

	/**
	 * checks for the present of <tt>key</tt> in the keys of the map.
	 * 
	 * @param key
	 *          an <code>Object</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean containsKey(Object key) {
		return contains(key);
	}

	/**
	 * Executes <tt>procedure</tt> for each key in the map.
	 * 
	 * @param procedure
	 *          a <code>TObjectProcedure</code> value
	 * @return false if the loop over the keys terminated because the procedure
	 *         returned false for some key.
	 */
	public boolean forEachKey(TObjectProcedure procedure) {
		return forEach(procedure);
	}

	/**
	 * Executes <tt>procedure</tt> for each value in the map.
	 * 
	 * @param procedure
	 *          a <code>TIntProcedure</code> value
	 * @return false if the loop over the values terminated because the procedure
	 *         returned false for some value.
	 */
	public boolean forEachValue(TIntProcedure procedure) {
		Object[] keys = _set;
		int[] values = _values;
		for (int i = values.length; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED && !procedure.execute(values[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Executes <tt>procedure</tt> for each key/value entry in the map.
	 * 
	 * @param procedure
	 *          a <code>TOObjectIntProcedure</code> value
	 * @return false if the loop over the entries terminated because the procedure
	 *         returned false for some entry.
	 */
	public boolean forEachEntry(TObjectIntProcedure procedure) {
		Object[] keys = _set;
		int[] values = _values;
		for (int i = keys.length; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED && !procedure.execute(keys[i], values[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Retains only those entries in the map for which the procedure returns a
	 * true value.
	 * 
	 * @param procedure
	 *          determines which entries to keep
	 * @return true if the map was modified.
	 */
	public boolean retainEntries(TObjectIntProcedure procedure) {
		boolean modified = false;
		Object[] keys = _set;
		int[] values = _values;
		for (int i = keys.length; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED && !procedure.execute(keys[i], values[i])) {
				removeAt(i);
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Transform the values in this map using <tt>function</tt>.
	 * 
	 * @param function
	 *          a <code>TIntFunction</code> value
	 */
	public void transformValues(TIntFunction function) {
		Object[] keys = _set;
		int[] values = _values;
		for (int i = values.length; i-- > 0;) {
			if (keys[i] != null && keys[i] != REMOVED) {
				values[i] = function.execute(values[i]);
			}
		}
	}

	/**
	 * Increments the primitive value mapped to key by 1
	 * 
	 * @param key
	 *          the key of the value to increment
	 * @return true if a mapping was found and modified.
	 */
	public boolean increment(Object key) {
		return adjustValue(key, (int) 1);
	}

	/**
	 * Adjusts the primitive value mapped to key.
	 * 
	 * @param key
	 *          the key of the value to increment
	 * @param amount
	 *          the amount to adjust the value by.
	 * @return true if a mapping was found and modified.
	 */
	public boolean adjustValue(Object key, int amount) {
		int index = index(key);
		if (index < 0) {
			return false;
		} else {
			_values[index] += amount;
			return true;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();

		// number of entries
		stream.writeInt(_size);

		SerializationProcedure writeProcedure = new SerializationProcedure(stream);
		if (!forEachEntry(writeProcedure)) {
			throw writeProcedure.exception;
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();

		int size = stream.readInt();
		setUp(size);
		while (size-- > 0) {
			Object key = stream.readObject();
			int val = stream.readInt();
			put(key, val);
		}
	}
} // TObjectIntHashMap
