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
 * An open addressed Map implementation for float keys and Object values.
 * 
 * Created: Sun Nov 4 08:52:45 2001
 * 
 * @author Eric D. Friedman
 * @version $Id: TFloatObjectHashMap.java,v 1.14 2003/03/19 04:17:03 ericdf Exp
 *          $
 */
public class TFloatObjectHashMap extends TFloatHash implements Serializable {

	/** the values of the map */
	protected transient Object[] _values;

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance with the default
	 * capacity and load factor.
	 */
	public TFloatObjectHashMap() {
		super();
	}

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance with a prime
	 * capacity equal to or greater than <tt>initialCapacity</tt> and with the
	 * default load factor.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 */
	public TFloatObjectHashMap(int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance with a prime
	 * capacity equal to or greater than <tt>initialCapacity</tt> and with the
	 * specified load factor.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 * @param loadFactor
	 *          a <code>float</code> value
	 */
	public TFloatObjectHashMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance with the default
	 * capacity and load factor.
	 * 
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TFloatObjectHashMap(TFloatHashingStrategy strategy) {
		super(strategy);
	}

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance whose capacity is
	 * the next highest prime above <tt>initialCapacity + 1</tt> unless that value
	 * is already prime.
	 * 
	 * @param initialCapacity
	 *          an <code>int</code> value
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TFloatObjectHashMap(int initialCapacity, TFloatHashingStrategy strategy) {
		super(initialCapacity, strategy);
	}

	/**
	 * Creates a new <code>TFloatObjectHashMap</code> instance with a prime value
	 * at or near the specified capacity and load factor.
	 * 
	 * @param initialCapacity
	 *          used to find a prime capacity for the table.
	 * @param loadFactor
	 *          used to calculate the threshold over which rehashing takes place.
	 * @param strategy
	 *          used to compute hash codes and to compare keys.
	 */
	public TFloatObjectHashMap(int initialCapacity, float loadFactor, TFloatHashingStrategy strategy) {
		super(initialCapacity, loadFactor, strategy);
	}

	/**
	 * @return a deep clone of this collection
	 */
	public Object clone() {
		TFloatObjectHashMap m = (TFloatObjectHashMap) super.clone();
		m._values = (Object[]) this._values.clone();
		return m;
	}

	/**
	 * @return a TFloatObjectIterator with access to this map's keys and values
	 */
	public TFloatObjectIterator iterator() {
		return new TFloatObjectIterator(this);
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
		_values = new Object[capacity];
		return capacity;
	}

	/**
	 * Inserts a key/value pair into the map.
	 * 
	 * @param key
	 *          an <code>float</code> value
	 * @param value
	 *          an <code>Object</code> value
	 * @return the previous value associated with <tt>key</tt>, or null if none
	 *         was found.
	 */
	public Object put(float key, Object value) {
		byte previousState;
		Object previous = null;
		int index = insertionIndex(key);
		boolean isNewMapping = true;
		if (index < 0) {
			index = -index - 1;
			previous = _values[index];
			isNewMapping = false;
		}
		previousState = _states[index];
		_set[index] = key;
		_states[index] = FULL;
		_values[index] = value;
		if (isNewMapping) {
			postInsertHook(previousState == FREE);
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
		float oldKeys[] = _set;
		Object oldVals[] = _values;
		byte oldStates[] = _states;

		_set = new float[newCapacity];
		_values = new Object[newCapacity];
		_states = new byte[newCapacity];

		for (int i = oldCapacity; i-- > 0;) {
			if (oldStates[i] == FULL) {
				float o = oldKeys[i];
				int index = insertionIndex(o);
				_set[index] = o;
				_values[index] = oldVals[i];
				_states[index] = FULL;
			}
		}
	}

	/**
	 * retrieves the value for <tt>key</tt>
	 * 
	 * @param key
	 *          an <code>float</code> value
	 * @return the value of <tt>key</tt> or null if no such mapping exists.
	 */
	public Object get(float key) {
		int index = index(key);
		return index < 0 ? null : _values[index];
	}

	/**
	 * Empties the map.
	 * 
	 */
	public void clear() {
		super.clear();
		float[] keys = _set;
		Object[] vals = _values;
		byte[] states = _states;

		for (int i = keys.length; i-- > 0;) {
			keys[i] = (float) 0;
			vals[i] = null;
			states[i] = FREE;
		}
	}

	/**
	 * Deletes a key/value pair from the map.
	 * 
	 * @param key
	 *          an <code>float</code> value
	 * @return an <code>Object</code> value
	 */
	public Object remove(float key) {
		Object prev = null;
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
		if (!(other instanceof TFloatObjectHashMap)) {
			return false;
		}
		TFloatObjectHashMap that = (TFloatObjectHashMap) other;
		if (that.size() != this.size()) {
			return false;
		}
		return forEachEntry(new EqProcedure(that));
	}

	public int hashCode() {
		HashProcedure p = new HashProcedure();
		forEachEntry(p);
		return p.getHashCode();
	}

	private final class HashProcedure implements TFloatObjectProcedure {
		private int h = 0;

		public int getHashCode() {
			return h;
		}

		public final boolean execute(float key, Object value) {
			h += (_hashingStrategy.computeHashCode(key) ^ HashFunctions.hash(value));
			return true;
		}
	}

	private static final class EqProcedure implements TFloatObjectProcedure {
		private final TFloatObjectHashMap _otherMap;

		EqProcedure(TFloatObjectHashMap otherMap) {
			_otherMap = otherMap;
		}

		public final boolean execute(float key, Object value) {
			int index = _otherMap.index(key);
			if (index >= 0 && eq(value, _otherMap.get(key))) {
				return true;
			}
			return false;
		}

		/**
		 * Compare two objects for equality.
		 */
		private final boolean eq(Object o1, Object o2) {
			return o1 == o2 || ((o1 != null) && o1.equals(o2));
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
		_values[index] = null;
	}

	/**
	 * Returns the values of the map.
	 * 
	 * @return a <code>Collection</code> value
	 */
	public Object[] getValues() {
		Object[] vals = new Object[size()];
		Object[] v = _values;
		byte[] states = _states;

		for (int i = v.length, j = 0; i-- > 0;) {
			if (states[i] == FULL) {
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
	public float[] keys() {
		float[] keys = new float[size()];
		float[] k = _set;
		byte[] states = _states;

		for (int i = k.length, j = 0; i-- > 0;) {
			if (states[i] == FULL) {
				keys[j++] = k[i];
			}
		}
		return keys;
	}

	/**
	 * checks for the presence of <tt>val</tt> in the values of the map.
	 * 
	 * @param val
	 *          an <code>Object</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean containsValue(Object val) {
		byte[] states = _states;
		Object[] vals = _values;

		// special case null values so that we don't have to
		// perform null checks before every call to equals()
		if (null == val) {
			for (int i = vals.length; i-- > 0;) {
				if (states[i] == FULL && val == vals[i]) {
					return true;
				}
			}
		} else {
			for (int i = vals.length; i-- > 0;) {
				if (states[i] == FULL && (val == vals[i] || val.equals(vals[i]))) {
					return true;
				}
			}
		} // end of else
		return false;
	}

	/**
	 * checks for the present of <tt>key</tt> in the keys of the map.
	 * 
	 * @param key
	 *          an <code>float</code> value
	 * @return a <code>boolean</code> value
	 */
	public boolean containsKey(float key) {
		return contains(key);
	}

	/**
	 * Executes <tt>procedure</tt> for each key in the map.
	 * 
	 * @param procedure
	 *          a <code>TFloatProcedure</code> value
	 * @return false if the loop over the keys terminated because the procedure
	 *         returned false for some key.
	 */
	public boolean forEachKey(TFloatProcedure procedure) {
		return forEach(procedure);
	}

	/**
	 * Executes <tt>procedure</tt> for each value in the map.
	 * 
	 * @param procedure
	 *          a <code>TObjectProcedure</code> value
	 * @return false if the loop over the values terminated because the procedure
	 *         returned false for some value.
	 */
	public boolean forEachValue(TObjectProcedure procedure) {
		byte[] states = _states;
		Object[] values = _values;
		for (int i = values.length; i-- > 0;) {
			if (states[i] == FULL && !procedure.execute(values[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Executes <tt>procedure</tt> for each key/value entry in the map.
	 * 
	 * @param procedure
	 *          a <code>TOFloatObjectProcedure</code> value
	 * @return false if the loop over the entries terminated because the procedure
	 *         returned false for some entry.
	 */
	public boolean forEachEntry(TFloatObjectProcedure procedure) {
		byte[] states = _states;
		float[] keys = _set;
		Object[] values = _values;
		for (int i = keys.length; i-- > 0;) {
			if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
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
	public boolean retainEntries(TFloatObjectProcedure procedure) {
		boolean modified = false;
		byte[] states = _states;
		float[] keys = _set;
		Object[] values = _values;
		for (int i = keys.length; i-- > 0;) {
			if (states[i] == FULL && !procedure.execute(keys[i], values[i])) {
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
	 *          a <code>TObjectFunction</code> value
	 */
	public void transformValues(TObjectFunction function) {
		byte[] states = _states;
		Object[] values = _values;
		for (int i = values.length; i-- > 0;) {
			if (states[i] == FULL) {
				values[i] = function.execute(values[i]);
			}
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
			float key = stream.readFloat();
			Object val = stream.readObject();
			put(key, val);
		}
	}
} // TFloatObjectHashMap
