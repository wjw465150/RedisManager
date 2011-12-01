/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package internal.org.jboss.serial.objectmetamodel;

import java.io.DataOutput;

/**
 * DataExport is class which <b>is not part of the public API</b> used only
 * during the persistence of the meta-model into bytes which happens at
 * {@link org.jboss.serial.objectmetamodel.DataContainer#saveData(DataOutput)}
 * and
 * {@link org.jboss.serial.objectmetamodel.DataContainer#loadData(DataInput))}
 * 
 * So... <b> Don't use this class </b>
 * 
 * $Id: DataExport.java,v 1.7 2006/02/24 20:33:10 csuconic Exp $
 * 
 * @author Clebert Suconic
 */
public abstract class DataExport {
	//public abstract void writeMyself(DataOutput output) throws IOException;
	//public abstract void readMyself(DataInput input) throws IOException;

}
