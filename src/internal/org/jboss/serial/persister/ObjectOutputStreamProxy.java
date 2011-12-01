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

package internal.org.jboss.serial.persister;

import internal.org.jboss.serial.classmetamodel.ClassMetaDataSlot;
import internal.org.jboss.serial.exception.SerializationException;
import internal.org.jboss.serial.objectmetamodel.FieldsContainer;
import internal.org.jboss.serial.objectmetamodel.ObjectSubstitutionInterface;

import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

/**
 * $Id: ObjectOutputStreamProxy.java,v 1.7 2006/04/11 23:13:09 csuconic Exp $
 * 
 * @author Clebert Suconic
 */
public class ObjectOutputStreamProxy extends ObjectOutputStream {

	Object currentObj;
	ClassMetaDataSlot currentMetaClass;
	ObjectSubstitutionInterface currentSubstitution;
	FieldsContainer currentContainer = null;

	ObjectOutput bout;

	public ObjectOutputStreamProxy(ObjectOutput output, Object currentObj, ClassMetaDataSlot currentMetaClass,
	    ObjectSubstitutionInterface currentSubstitution) throws IOException {
		super();
		this.bout = output;
		this.currentObj = currentObj;
		this.currentMetaClass = currentMetaClass;
		this.currentSubstitution = currentSubstitution;
	}

	protected void writeObjectOverride(Object obj) throws IOException {
		bout.writeObject(obj);
	}

	public void writeUnshared(Object obj) throws IOException {
		writeObjectOverride(obj);
	}

	public void defaultWriteObject() throws IOException {
		writeFields();
	}

	public void writeFields() throws IOException {
		if (currentContainer != null) {
			currentContainer.writeMyself(this);
			currentContainer = null;
		} else {
			RegularObjectPersister.writeSlotWithFields(currentMetaClass, bout, currentObj, currentSubstitution);
		}
	}

	public void reset() throws IOException {
	}

	protected void writeStreamHeader() throws IOException {
	}

	protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
	}

	/**
	 * Writes a byte. This method will block until the byte is actually written.
	 * 
	 * @param val
	 *          the byte to be written to the stream
	 * @throws IOException
	 *           If an I/O error has occurred.
	 */
	public void write(int val) throws IOException {
		bout.write(val);
	}

	/**
	 * Writes an array of bytes. This method will block until the bytes are
	 * actually written.
	 * 
	 * @param buf
	 *          the data to be written
	 * @throws IOException
	 *           If an I/O error has occurred.
	 */
	public void write(byte[] buf) throws IOException {
		bout.write(buf);
	}

	public void write(byte[] buf, int off, int len) throws IOException {
		if (buf == null) {
			throw new SerializationException("buf can't be null");
		}
		bout.write(buf, off, len);
	}

	/**
	 * Flushes the stream. This will write any buffered output bytes and flush
	 * through to the underlying stream.
	 * 
	 * @throws IOException
	 *           If an I/O error has occurred.
	 */
	public void flush() throws IOException {
		bout.flush();
	}

	protected void drain() throws IOException {
		//bout.drain();
	}

	public void close() throws IOException {
		flush();
		bout.close();
	}

	public void writeBoolean(boolean val) throws IOException {
		bout.writeBoolean(val);
	}

	public void writeByte(int val) throws IOException {
		bout.writeByte(val);
	}

	public void writeShort(int val) throws IOException {
		bout.writeShort(val);
	}

	public void writeChar(int val) throws IOException {
		bout.writeChar(val);
	}

	public void writeInt(int val) throws IOException {
		bout.writeInt(val);
	}

	public void writeLong(long val) throws IOException {
		bout.writeLong(val);
	}

	public void writeFloat(float val) throws IOException {
		bout.writeFloat(val);
	}

	public void writeDouble(double val) throws IOException {
		bout.writeDouble(val);
	}

	public void writeBytes(String str) throws IOException {
		bout.writeBytes(str);
	}

	public void writeChars(String str) throws IOException {
		bout.writeChars(str);
	}

	public void writeUTF(String str) throws IOException {
		bout.writeUTF(str);
	}

	public ObjectOutputStream.PutField putFields() throws IOException {
		currentContainer = new FieldsContainer(currentMetaClass);
		return currentContainer.createPut();
	}

}
