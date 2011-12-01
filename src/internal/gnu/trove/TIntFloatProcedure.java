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
 * Interface for procedures that take two parameters of type int and float.
 * 
 * Created: Mon Nov 5 22:03:30 2001
 * 
 * @author Eric D. Friedman
 * @version $Id: TIntFloatProcedure.java,v 1.7 2002/09/22 21:53:41 ericdf Exp $
 */

public interface TIntFloatProcedure {

	/**
	 * Executes this procedure. A false return value indicates that the
	 * application executing this procedure should not invoke this procedure
	 * again.
	 * 
	 * @param a
	 *          an <code>int</code> value
	 * @param b
	 *          an <code>float</code> value
	 * @return true if additional invocations of the procedure are allowed.
	 */
	public boolean execute(int a, float b);
}// TIntFloatProcedure
