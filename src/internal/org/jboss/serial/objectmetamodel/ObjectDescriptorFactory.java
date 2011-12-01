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

import internal.org.jboss.serial.classmetamodel.ClassMetaData;
import internal.org.jboss.serial.classmetamodel.ClassMetamodelFactory;
import internal.org.jboss.serial.classmetamodel.StreamingClass;
import internal.org.jboss.serial.exception.SerializationException;
import internal.org.jboss.serial.persister.ClassReferencePersister;
import internal.org.jboss.serial.persister.PersistResolver;
import internal.org.jboss.serial.persister.Persister;
import internal.org.jboss.serial.util.ClassMetaConsts;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author clebert suconic
 */
public class ObjectDescriptorFactory implements ClassMetaConsts {

	static Object objectFromDescription(final ObjectsCache cache, ObjectsCache.JBossSeralizationInputInterface input)
	    throws IOException {
		Object description = null;

		byte byteIdentify = (byte) cache.getInput().readByte();

		if (byteIdentify == DataContainerConstants.RESET) {
			cache.reset();
			return objectFromDescription(cache, input);
		}
		if (byteIdentify == DataContainerConstants.NULLREF) {
			return null;
		} else if (byteIdentify == DataContainerConstants.NEWDEF) {
			return readObjectDescriptionFromStreaming(cache, input.readObjectReference(), input);
		} else if (byteIdentify == DataContainerConstants.SMARTCLONE_DEF) {
			int reference = input.readObjectReference();
			if (cache.getSafeToReuse() == null) {
				throw new IOException("SafeClone repository mismatch");
			}
			description = cache.getSafeToReuse().findReference(reference);
			if (description == null) {
				throw new IOException("SafeClone repository mismatch - didn't find reference " + reference);
			}
			return description;

		} else if (byteIdentify == DataContainerConstants.OBJECTREF) {
			int reference = input.readObjectReference();

			if (description == null) {
				description = cache.findObjectInCacheRead(reference);
			}

			if (description == null) {
				throw new SerializationException("Object reference " + reference + " was not found");
			}

			return description;
		} else {
			return input.readImmutable(byteIdentify, cache);
		}

	}

	/**
	 * First level of a describe object, will look if it's a newDef, or if it's
	 * already loaded. If the object was never loaded before, it will call
	 * readObjectDescriptionFromStreaming. If it was already loaded, it will just
	 * return from objectCache
	 */
	static void describeObject(final ObjectsCache cache, Object obj) throws IOException {

		ObjectsCache.JBossSeralizationOutputInterface outputParent = cache.getOutput();

		if (obj == null) {
			outputParent.writeByte(DataContainerConstants.NULLREF);
			return;
		}
		if (obj != null && ClassMetamodelFactory.isImmutable(obj.getClass())) {
			outputParent.saveImmutable(cache, obj);
			return;
		}

		ClassMetaData metaData = null;
		if (obj instanceof Class) {
			metaData = ClassMetamodelFactory.getClassMetaData((Class) obj, cache.isCheckSerializableClass());
		} else {
			metaData = ClassMetamodelFactory.getClassMetaData(obj.getClass(), cache.isCheckSerializableClass());
			if (metaData.getWriteReplaceMethod() != null) {
				try {
					Object orig = obj;
					obj = metaData.getWriteReplaceMethod().invoke(obj, EMPTY_OBJECT_ARRAY);
					if (obj != null && obj != orig && obj.getClass() != metaData.getClazz()) {
						describeObject(cache, obj);
						return;
					}
					metaData = ClassMetamodelFactory.getClassMetaData(obj.getClass(), cache.isCheckSerializableClass());
				} catch (Exception e) {
					IOException io = new IOException("Metadata Serialization Error");
					io.initCause(e);
					throw io;
				}
			}
		}

		if (cache.getSubstitution() != null) {
			Object orig = obj;
			obj = cache.getSubstitution().replaceObject(obj);
			if (obj != null && obj != orig && obj.getClass() != metaData.getClazz()) {
				describeObject(cache, obj);
				return;
			}
		}

		int description = 0;

		if (cache.getSafeToReuse() != null) {
			description = cache.getSafeToReuse().storeSafe(obj);
			if (description != 0) {
				outputParent.writeByte(DataContainerConstants.SMARTCLONE_DEF);
				cache.getOutput().addObjectReference(description);
				return;
			}

		}

		description = cache.findIdInCacheWrite(obj);

		if (description != 0) {
			outputParent.writeByte(DataContainerConstants.OBJECTREF);
			cache.getOutput().addObjectReference(description);
			return;
		} else {
			description = cache.putObjectInCacheWrite(obj);
			outputParent.writeByte(DataContainerConstants.NEWDEF);
			cache.getOutput().addObjectReference(description);

			int cacheId = cache.findIdInCacheWrite(metaData);
			if (cacheId == 0) {
				cacheId = cache.putObjectInCacheWrite(metaData);
				outputParent.writeByte(DataContainerConstants.NEWDEF);
				outputParent.addObjectReference(cacheId);
				StreamingClass.saveStream(metaData, outputParent);
			} else {
				outputParent.writeByte(DataContainerConstants.OBJECTREF);
				outputParent.addObjectReference(cacheId);
			}

			Persister persister = PersistResolver.resolvePersister(obj, metaData);

			outputParent.writeByte(persister.getId());
			persister.writeData(metaData, cache.getOutput(), obj, cache.getSubstitution());

			return;
		}
	}

	/*
	 * private static Object readImmutable(byte byteDescription,ObjectsCache
	 * cache, JBossSeralizationInputInterface input) throws IOException {
	 * 
	 * Object retObject=null; switch (byteDescription) { case
	 * DataContainerConstants.STRING: retObject =
	 * StringUtil.readString(input,cache.getStringBuffer());break; case
	 * DataContainerConstants.BYTE: retObject = new Byte(input.readByte());break;
	 * case DataContainerConstants.CHARACTER: retObject = new
	 * Character(input.readChar());break; case DataContainerConstants.SHORT:
	 * retObject = new Short(input.readShort());break; case
	 * DataContainerConstants.INTEGER: retObject = new
	 * Integer(input.readInt());break; case DataContainerConstants.LONG: retObject
	 * = new Long(input.readLong());break; case DataContainerConstants.DOUBLE:
	 * retObject = new Double(input.readDouble());break; case
	 * DataContainerConstants.FLOAT: retObject = new
	 * Float(input.readFloat());break; case DataContainerConstants.BOOLEAN:
	 * retObject = new Boolean(input.readBoolean());break; }
	 * 
	 * if (isDebug) { log.debug("byteDescription=" + byteDescription);
	 * log.debug("readImmutable return=" + retObject + " class=" +
	 * retObject.getClass().getName()); }
	 * 
	 * return retObject; }
	 */

	/*
	 * private static void saveImmutable(ObjectsCache cache, Object obj,
	 * JBossSeralizationOutputInterface output) throws IOException { if (isDebug)
	 * { log.debug("saveImmutable::obj=" +obj + " class=" +
	 * obj.getClass().getName()); } if (obj instanceof String) {
	 * output.writeByte(DataContainerConstants.STRING);
	 * StringUtil.saveString(output,(String)obj,cache.getStringBuffer()); } else
	 * if (obj instanceof Byte) { output.writeByte(DataContainerConstants.BYTE);
	 * output.writeByte(((Byte)obj).byteValue()); } else if (obj instanceof
	 * Character) { output.writeByte(DataContainerConstants.CHARACTER);
	 * output.writeChar(((Character)obj).charValue()); } else if (obj instanceof
	 * Short) { output.writeByte(DataContainerConstants.SHORT);
	 * output.writeShort(((Short)obj).shortValue()); } else if (obj instanceof
	 * Integer) { output.writeByte(DataContainerConstants.INTEGER);
	 * output.writeInt(((Integer)obj).intValue()); } else if (obj instanceof Long)
	 * { output.writeByte(DataContainerConstants.LONG);
	 * output.writeLong(((Long)obj).longValue()); } else if (obj instanceof
	 * Double) { output.writeByte(DataContainerConstants.DOUBLE);
	 * output.writeDouble(((Double)obj).doubleValue()); } else if (obj instanceof
	 * Float) { output.writeByte(DataContainerConstants.FLOAT);
	 * output.writeFloat(((Float)obj).floatValue()); } else if (obj instanceof
	 * BooleanContainer || obj instanceof Boolean) {
	 * output.writeByte(DataContainerConstants.BOOLEAN);
	 * output.writeBoolean(((Boolean)obj).booleanValue()); } else { throw new
	 * SerializationException("I don't know how to write type " +
	 * obj.getClass().getName() + " yet"); }
	 * 
	 * }
	 */

	private static Object readObjectDescriptionFromStreaming(final ObjectsCache cache, final int reference,
	    ObjectsCache.JBossSeralizationInputInterface input) throws IOException {
		byte defClass = input.readByte();
		StreamingClass streamingClass = null;
		if (defClass == DataContainerConstants.NEWDEF) {
			int referenceId = input.readObjectReference();
			streamingClass = StreamingClass.readStream(input, cache.getClassResolver(), cache.getLoader());
			cache.putObjectInCacheRead(referenceId, streamingClass);
		} else {
			int referenceId = input.readObjectReference();
			streamingClass = (StreamingClass) cache.findObjectInCacheRead(referenceId);
			if (streamingClass == null) {
				throw new IOException("Didn't find StreamingClass circular refernce id=" + referenceId);
			}

		}

		ClassMetaData metaData = streamingClass.getMetadata();

		byte persisterId = input.readByte();
		Persister persister = PersistResolver.resolvePersister(persisterId);
		//Persister persister = PersistResolver.resolvePersister(description.getMetaData().getClazz(),
		//        description.getMetaData(),description.getMetaData().isArray());

		/*
		 * ObjectDescription description = new ObjectDescription();
		 * description.setMetaData
		 * (ClassMetamodelFactory.getClassMetaData(reference.getClassName
		 * (),cache.getLoader(),false));
		 * cache.putObjectInCache(reference,description);
		 */

		Object value = persister.readData(cache.getLoader(), streamingClass, metaData, reference, cache, cache.getInput(),
		    cache.getSubstitution());

		if (!(persister instanceof ClassReferencePersister)) //JBSER-83
		{
			if (cache.getSubstitution() != null) {
				value = cache.getSubstitution().replaceObject(value);
			}

			try {
				if (metaData.getReadResolveMethod() != null) {
					value = metaData.getReadResolveMethod().invoke(value, new Object[] {});
					cache.reassignObjectInCacheRead(reference, value);
				}
			} catch (IllegalAccessException e) {
				throw new SerializationException(e);
			} catch (InvocationTargetException e) {
				throw new SerializationException(e);
			}
		}

		return value;
	}

}
