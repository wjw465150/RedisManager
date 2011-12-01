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

/**
 * 
 * Created: Wed Nov 28 21:30:53 2001
 * 
 * @author Eric D. Friedman
 * @version $Id: TObjectHashIterator.java,v 1.1 2001/12/28 20:04:39 ericdf Exp $
 */

class TObjectHashIterator extends THashIterator {
	protected final TObjectHash _objectHash;

	public TObjectHashIterator(TObjectHash hash) {
		super(hash);
		_objectHash = hash;
	}

	protected Object objectAtIndex(int index) {
		return _objectHash._set[index];
	}

} // TObjectHashIterator
