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

import internal.org.jboss.serial.classmetamodel.ClassMetaData;
import internal.org.jboss.serial.classmetamodel.ClassMetaDataSlot;
import internal.org.jboss.serial.classmetamodel.ClassMetadataField;
import internal.org.jboss.serial.classmetamodel.FieldsManager;
import internal.org.jboss.serial.classmetamodel.StreamingClass;
import internal.org.jboss.serial.exception.SerializationException;
import internal.org.jboss.serial.objectmetamodel.ObjectSubstitutionInterface;
import internal.org.jboss.serial.objectmetamodel.ObjectsCache;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Field;

/**
 * This is the persister of a regular object.
 * 
 * @author clebert suconic
 */
public class RegularObjectPersister implements Persister {

	byte id;

	public byte getId() {
		return id;
	}

	public void setId(byte id) {
		this.id = id;
	}

	public void writeData(ClassMetaData metaData, ObjectOutput out, Object obj, ObjectSubstitutionInterface substitution)
	    throws IOException {
		defaultWrite(out, obj, metaData, substitution);
	}

	public static void defaultWrite(ObjectOutput output, Object obj, ClassMetaData metaClass,
	    ObjectSubstitutionInterface substitution) throws IOException {
		//data.getCache().putClassMetaData(metaClass);

		ClassMetaDataSlot slots[] = metaClass.getSlots();

		//output.writeInt(slots.length);

		for (int slotNr = 0; slotNr < slots.length; slotNr++) {
			if (slots[slotNr].getPrivateMethodWrite() != null) {
				writeSlotWithMethod(slots[slotNr], output, obj, substitution);
			} else {
				writeSlotWithFields(slots[slotNr], output, obj, substitution);
			}
		}

	}

	private static void readSlotWithMethod(ClassMetaDataSlot slot, short[] fieldsKey, ObjectInput input, Object obj,
	    ObjectSubstitutionInterface substitution) throws IOException {
		try {
			slot.getPrivateMethodRead().invoke(obj,
			    new Object[] { new ObjectInputStreamProxy(input, fieldsKey, obj, slot, substitution) });
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			IOException io = new IOException(e.getMessage());
			io.initCause(e);
			throw io;
		}
	}

	private static void writeSlotWithMethod(ClassMetaDataSlot slot, ObjectOutput out, Object obj,
	    ObjectSubstitutionInterface substitution) throws IOException {
		try {
			slot.getPrivateMethodWrite().invoke(obj,
			    new Object[] { new ObjectOutputStreamProxy(out, obj, slot, substitution) });
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			IOException io = new IOException(e.getMessage());
			io.initCause(e);
			throw io;
		}
	}

	static void writeSlotWithFields(ClassMetaDataSlot slot, ObjectOutput output, Object obj,
	    ObjectSubstitutionInterface substitution) throws IOException {
		ClassMetadataField[] fields = slot.getFields();
		//output.writeShort(fields.length);

		for (int fieldNR = 0; fieldNR < fields.length; fieldNR++) {
			ClassMetadataField field = fields[fieldNR];

			if (field.getField().getType().isPrimitive() && !field.getField().getType().isArray()) {
				writeOnPrimitive(output, obj, field);
			} else {
				//value = field.getField().get(obj);
				Object value = null;
				value = FieldsManager.getFieldsManager().getObject(obj, field);

				//output.writeUTF(field.getFieldName());
				//output.writeByte(DataContainerConstants.OBJECTREF);
				output.writeObject(value);
			}
		}
	}

	private static void writeOnPrimitive(final ObjectOutput out, final Object obj, final ClassMetadataField metaField)
	    throws IOException {

		try {
			//out.writeUTF(metaField.getFieldName());
			final Field field = metaField.getField();
			final Class clazz = field.getType();
			if (clazz == Integer.TYPE) {
				//out.writeByte(DataContainerConstants.INTEGER);
				out.writeInt(FieldsManager.getFieldsManager().getInt(obj, metaField));
				//out.writeInt(field.getInt(obj));
			} else if (clazz == Byte.TYPE) {
				//out.writeByte(DataContainerConstants.BYTE);
				out.writeByte(FieldsManager.getFieldsManager().getByte(obj, metaField));
				//out.writeByte(field.getByte(obj));
			} else if (clazz == Long.TYPE) {
				//out.writeByte(DataContainerConstants.LONG);
				out.writeLong(FieldsManager.getFieldsManager().getLong(obj, metaField));
				//out.writeLong(field.getLong(obj));
			} else if (clazz == Float.TYPE) {
				//out.writeByte(DataContainerConstants.FLOAT);
				out.writeFloat(FieldsManager.getFieldsManager().getFloat(obj, metaField));
				//out.writeFloat(field.getFloat(obj));
			} else if (clazz == Double.TYPE) {
				//out.writeByte(DataContainerConstants.DOUBLE);
				out.writeDouble(FieldsManager.getFieldsManager().getDouble(obj, metaField));
				//out.writeDouble(field.getDouble(obj));
			} else if (clazz == Short.TYPE) {
				//out.writeByte(DataContainerConstants.SHORT);
				out.writeShort(FieldsManager.getFieldsManager().getShort(obj, metaField));
				//out.writeShort(field.getShort(obj));
			} else if (clazz == Character.TYPE) {
				//out.writeByte(DataContainerConstants.CHARACTER);
				out.writeChar(field.getChar(obj));
			} else if (clazz == Boolean.TYPE) {
				//out.writeByte(DataContainerConstants.BOOLEAN);
				out.writeBoolean(field.getBoolean(obj));
			} else {
				throw new RuntimeException("Unexpected datatype " + clazz.getName());
			}
		} catch (IllegalAccessException access) {
			IOException io = new IOException(access.getMessage());
			io.initCause(access);
			throw io;
		}
	}

	public Object readData(ClassLoader loader, StreamingClass streaming, ClassMetaData metaData, int referenceId,
	    ObjectsCache cache, ObjectInput input, ObjectSubstitutionInterface substitution) throws IOException {
		Object obj = metaData.newInstance();
		cache.putObjectInCacheRead(referenceId, obj);
		return defaultRead(input, obj, streaming, metaData, substitution);

	}

	public static Object defaultRead(ObjectInput input, Object obj, StreamingClass streaming, ClassMetaData metaData,
	    ObjectSubstitutionInterface substitution) throws IOException {

		try {

			//final int numberOfSlots = input.readInt();

			ClassMetaDataSlot[] slots = metaData.getSlots();
			for (int slotNR = 0; slotNR < slots.length; slotNR++) {
				ClassMetaDataSlot slot = metaData.getSlots()[slotNR];

				if (slot.getPrivateMethodRead() != null) {
					readSlotWithMethod(slot, streaming.getKeyFields()[slotNR], input, obj, substitution);
				} else {
					readSlotWithFields(streaming.getKeyFields()[slotNR], slot, input, obj);
				}
			}

			return obj;
		} catch (ClassNotFoundException e) {
			throw new SerializationException("Error reading " + obj.getClass().getName(), e);
		}
		/*
		 * catch (IllegalAccessException e) { throw new
		 * SerializationException("Error reading " +
		 * field.getField().getDeclaringClass().getName() + " field=" +
		 * field.getFieldName(),e); }
		 */
	}

	static void readSlotWithFields(short fieldsKey[], ClassMetaDataSlot slot, ObjectInput input, Object obj)
	    throws IOException, ClassNotFoundException {
		//final int numberOfFields = input.readShort();
		short numberOfFields = (short) fieldsKey.length;
		for (short i = 0; i < numberOfFields; i++) {
			//final String fieldName = input.readUTF();
			ClassMetadataField field = slot.getFields()[fieldsKey[i]];
			//byte dataType = input.readByte();

			if (field.getField().getType() == Integer.TYPE) {
				FieldsManager.getFieldsManager().setInt(obj, field, input.readInt());
			} else if (field.getField().getType() == Byte.TYPE) {
				FieldsManager.getFieldsManager().setByte(obj, field, input.readByte());
			} else if (field.getField().getType() == Long.TYPE) {
				FieldsManager.getFieldsManager().setLong(obj, field, input.readLong());
			} else if (field.getField().getType() == Float.TYPE) {
				FieldsManager.getFieldsManager().setFloat(obj, field, input.readFloat());
			} else if (field.getField().getType() == Double.TYPE) {
				FieldsManager.getFieldsManager().setDouble(obj, field, input.readDouble());
			} else if (field.getField().getType() == Short.TYPE) {
				FieldsManager.getFieldsManager().setShort(obj, field, input.readShort());
			} else if (field.getField().getType() == Character.TYPE) {
				FieldsManager.getFieldsManager().setCharacter(obj, field, input.readChar());
			} else if (field.getField().getType() == Boolean.TYPE) {
				FieldsManager.getFieldsManager().setBoolean(obj, field, input.readBoolean());
			} else {
				Object objTmp = input.readObject();
				FieldsManager.getFieldsManager().setObject(obj, field, objTmp);
			}
		}
	}

	public boolean canPersist(Object obj) {
		// not implemented
		return false;
	}

}
