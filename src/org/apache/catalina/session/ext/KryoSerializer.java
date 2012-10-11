package org.apache.catalina.session.ext;

import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Map;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.CopyForIterateCollectionSerializer;
import de.javakaffee.kryoserializers.CopyForIterateMapSerializer;
import de.javakaffee.kryoserializers.DateSerializer;
import de.javakaffee.kryoserializers.EnumMapSerializer;
import de.javakaffee.kryoserializers.EnumSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * 基于Kryo 2.x的(序列化/反序列化)类.可以处理任何类(包括没有实现Serializable接口的类).
 * 
 * @author wjw65150@gmail.com
 * 
 */

public final class KryoSerializer {
  private static final ThreadLocal<Kryo> _threadLocalKryo = new ThreadLocal<Kryo>() {
    protected Kryo initialValue() {
      Kryo kryo = new KryoReflectionFactorySupport() {

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Serializer<?> getDefaultSerializer(final Class type) {
          if (EnumSet.class.isAssignableFrom(type)) {
            return new EnumSetSerializer();
          }
          if (EnumMap.class.isAssignableFrom(type)) {
            return new EnumMapSerializer();
          }
          if (Collection.class.isAssignableFrom(type)) {
            return new CopyForIterateCollectionSerializer();
          }
          if (Map.class.isAssignableFrom(type)) {
            return new CopyForIterateMapSerializer();
          }
          if (Date.class.isAssignableFrom(type)) {
            return new DateSerializer(type);
          }
          return super.getDefaultSerializer(type);
        }
      };

      kryo.setRegistrationRequired(false);
      
      kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
      kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer());
      kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer());
      kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer());
      kryo.register(Collections.singletonList("").getClass(), new CollectionsSingletonListSerializer());
      kryo.register(Collections.singleton("").getClass(), new CollectionsSingletonSetSerializer());
      kryo.register(Collections.singletonMap("", "").getClass(), new CollectionsSingletonMapSerializer());
      kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
      kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
      kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
      kryo.register(InvocationHandler.class, new JdkProxySerializer());
      UnmodifiableCollectionsSerializer.registerSerializers(kryo);
      SynchronizedCollectionsSerializer.registerSerializers(kryo);

      return kryo;
    }
  };

  private KryoSerializer() {
  }

  public static byte[] write(Object obj) {
    return write(obj, -1);
  }

  public static byte[] write(Object obj, int maxBufferSize) {
    Kryo kryo = _threadLocalKryo.get();
    Output output = new Output(1024, maxBufferSize);
    kryo.writeClassAndObject(output, obj);
    return output.toBytes();
  }

  public static Object read(byte[] bytes) {
    Kryo kryo = _threadLocalKryo.get();
    Input input = new Input(bytes);
    return kryo.readClassAndObject(input);
  }

}
