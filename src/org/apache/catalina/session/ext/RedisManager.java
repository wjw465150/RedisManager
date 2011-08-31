package org.apache.catalina.session.ext;

import internal.com.thoughtworks.xstream.XStream;
import internal.com.thoughtworks.xstream.io.xml.XppDriver;
import java.io.*;
import org.apache.catalina.session.*;

import redis.clients.jedis.*;
import org.apache.catalina.*;
import internal.org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 *
 * @author Administrator
 */
public class RedisManager extends StandardManager {

  static final int LIFE_TIME = 86400;  //属性存活期多少秒(24 * 60 *60 =24小时)
  static final String SNA_ID_VALUE = "R";
  static final XStream _xstream = new XStream(new XppDriver());
  static ShardedJedisPool _shardedPool = null;
  static JedisPool _pool = null;
//->---------------属性----------------------
  boolean debugEnabled = false;
  private String debug = "false";  //是否打开调试模式

  /**
   * 是否打开调试模式
   * @return the debug
   */
  public String getDebug() {
    return debug;
  }

  /**
   * 是否打开调试模式
   * @param debug the debug to set
   */
  public void setDebug(String debug) {
    this.debug = debug;
  }
  protected String serverlist = "127.0.0.1:6379";  //用逗号(,)分隔的"ip:port"列表

  /**
   * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
   * @return
   */
  public String getServerlist() {
    return serverlist;
  }

  /**
   * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
   * @param serverlist
   */
  public void setServerlist(String serverlist) {
    this.serverlist = serverlist;
  }
  protected String minConn = "5";

  /**
   * Returns the minimum number of spare connections in available pool.
   * @return number of connections
   */
  public String getMinConn() {
    return minConn;
  }

  /**
   * Sets the minimum number of spare connections to maintain in our available pool.
   * @param minConn - number of connections
   */
  public void setMinConn(String minConn) {
    this.minConn = minConn;
  }
  protected String maxConn = "100";

  /**
   * Returns the maximum number of spare connections allowed in available pool.
   * @return number of connections
   */
  public String getMaxConn() {
    return maxConn;
  }

  /**
   * Sets the maximum number of spare connections allowed in our available pool.
   * @param maxConn - number of connections
   */
  public void setMaxConn(String maxConn) {
    this.maxConn = maxConn;
  }
  protected String socketTO = "6000";

  /**
   * Returns the socket timeout for reads.
   * @return the socketTO timeout in ms
   */
  public String getSocketTO() {
    return socketTO;
  }

  /**
   * Sets the socket timeout for reads.
   * @param socketTO timeout in ms
   */
  public void setSocketTO(String socketTO) {
    this.socketTO = socketTO;
  }
//<----------------属性----------------------

  public RedisManager() {
    super();
  }

  /**
   * Return the active Session, associated with this Manager, with the
   * specified session id (if any); otherwise return <code>null</code>.
   *
   * @param id The session id for the session to be returned
   *
   * @exception IllegalStateException if a new session cannot be
   *  instantiated for any reason
   * @exception IOException if an input/output error occurs while
   *  processing this request
   */
  @Override
  public Session findSession(String id) throws IOException {
    Session session = super.findSession(id);
    if (session == null && id != null) {  //说明session有可能在另一个节点上
      try {
        String snaIdValue = jedisGet(id);
        if (snaIdValue != null && snaIdValue.equals(SNA_ID_VALUE)) {  //Redis里有Session ID
          if (this.debugEnabled) {
            log.info("cached found and local not! id=" + id);
          }

          //->
          RedisSession redisSession = new RedisSession(this);
          redisSession.setNew(false);
          redisSession.setValid(true);
          redisSession.setCreationTime(System.currentTimeMillis());
          redisSession.setMaxInactiveInterval(this.maxInactiveInterval);
          redisSession.setCachedId(id);

          this.add(redisSession);
          sessionCounter++;

          return redisSession;
          //<-
        }
      } catch (Exception ex) {
        log.error("error:", ex);
      }
    }

    return session;
  }

  /**
   * Construct and return a new session object, based on the default
   * settings specified by this Manager's properties.  The session
   * id will be assigned by this method, and available via the getId()
   * method of the returned session.  If a new session cannot be created
   * for any reason, return <code>null</code>.
   *
   * @exception IllegalStateException if a new session cannot be
   *  instantiated for any reason
   */
  @Override
  public Session createSession(String sessionId) {
    Session session = super.createSession(sessionId);
    sessionId = session.getId();
    if (this.debugEnabled) {
      log.info("id=" + sessionId);
    }

    try {
      jedisSetex(sessionId, session.getMaxInactiveInterval(), SNA_ID_VALUE);
    } catch (Exception ex) {
      log.error("error:", ex);
    }
    return session;
  }

  /**
   * 间接被createSession()调用.
   */
  @Override
  protected StandardSession getNewSession() {
    RedisSession session = new RedisSession(this);
//    if (this.debugEnabled) {
//      log.info("id=" + session.getId());
//    }

    return session;
  }

  /**
   * Remove this Session from the active Sessions for this Manager,
   * and from the Redis.
   *
   * @param session Session to be removed
   */
  @Override
  public void remove(Session session) {
    if (this.debugEnabled) {
      log.info("id=" + session.getId());
    }
    super.remove(session);

    try {
      jedisDel(session.getId());
    } catch (Exception ex) {
      log.error("error:", ex);
    }
  }

  @Override
  public void init() {
    super.init();

    debugEnabled = Boolean.parseBoolean(debug);

    synchronized (RedisManager.class) {
      try {
        if (_shardedPool == null && _pool == null) {
          try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxActive(Integer.valueOf(maxConn));
            poolConfig.setMinIdle(Integer.valueOf(minConn));
            int maxIdle = poolConfig.minIdle + 5;
            if (maxIdle > poolConfig.maxActive) {
              maxIdle = poolConfig.maxActive;
            }
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMaxWait(1000L);
            poolConfig.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK);
            poolConfig.setTestOnBorrow(false);
            poolConfig.setTestOnReturn(false);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 10L); //空闲对象,空闲多长时间会被驱逐出池里
            poolConfig.setTimeBetweenEvictionRunsMillis(1000L * 30L);  //驱逐线程30秒执行一次
            poolConfig.setNumTestsPerEvictionRun(-1);   //-1,表示在驱逐线程执行时,测试所有的空闲对象

            String[] servers = serverlist.split(",");
            java.util.List<JedisShardInfo> shards = new java.util.ArrayList<JedisShardInfo>(servers.length);
            for (int i = 0; i < servers.length; i++) {
              String[] hostAndPort = servers[i].split(":");
              JedisShardInfo shardInfo = new JedisShardInfo(hostAndPort[0], Integer.parseInt(hostAndPort[1]), Integer.valueOf(socketTO));
              if (hostAndPort.length == 3) {
                shardInfo.setPassword(hostAndPort[2]);
              }
              shards.add(shardInfo);
            }

            if (shards.size() == 1) {
              _pool = new JedisPool(poolConfig, shards.get(0).getHost(), shards.get(0).getPort(), shards.get(0).getTimeout(), shards.get(0).getPassword());
              log.info("使用:JedisPool");
            } else {
              _shardedPool = new ShardedJedisPool(poolConfig, shards);
              log.info("使用:ShardedJedisPool");
            }

            log.info("RedisShards:" + shards.toString());
            log.info("初始化RedisManager:" + this.toString());
          } catch (Exception ex) {
            log.error("error:", ex);
          }
        }

      } catch (Exception ex) {
        log.error("error:", ex);
      }
    }
  }

  @Override
  public void destroy() {
    synchronized (RedisManager.class) {
      if (_shardedPool != null) {
        ShardedJedisPool myPool = _shardedPool;
        _shardedPool = null;
        try {
          myPool.destroy();
          log.info("销毁RedisManager:" + this.toString());
        } catch (Exception ex) {
          log.error("error:", ex);
        }

      }

      if (_pool != null) {
        JedisPool myPool = _pool;
        _pool = null;
        try {
          myPool.destroy();
          log.info("销毁RedisManager:" + this.toString());
        } catch (Exception ex) {
          log.error("error:", ex);
        }

      }
    }

    super.destroy();
  }

  @Override
  public String toString() {
    return "RedisManager{" + "debug=" + debug
            + ",serverlist=" + serverlist
            + ",minConn=" + minConn + ",maxConn=" + maxConn
            + ",socketTO=" + socketTO + '}';
  }

  String jedisGet(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        byte[] byteValue = jedis.get(key.getBytes(Protocol.CHARSET));
        if (byteValue == null) {
          return null;
        }
        return new String(byteValue, Protocol.CHARSET);
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        byte[] byteValue = jedis.get(key.getBytes(Protocol.CHARSET));
        if (byteValue == null) {
          return null;
        }
        return new String(byteValue, Protocol.CHARSET);
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  String jedisSetex(String key, int seconds, String value) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.setex(key.getBytes(Protocol.CHARSET), seconds, value.getBytes(Protocol.CHARSET));
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        return jedis.setex(key.getBytes(Protocol.CHARSET), seconds, value.getBytes(Protocol.CHARSET));
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }

  Long jedisDel(String key) {
    if (_pool != null) {
      Jedis jedis = null;
      try {
        jedis = _pool.getResource();
        return jedis.del(key.getBytes(Protocol.CHARSET));
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _pool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    } else {
      ShardedJedis jedis = null;
      try {
        jedis = _shardedPool.getResource();
        byte[] bytesKey = key.getBytes(Protocol.CHARSET);
        Jedis jedisA = jedis.getShard(bytesKey);
        return jedisA.del(bytesKey);
      } catch (IOException e) {
        throw new JedisConnectionException(e);
      } finally {
        if (jedis != null) {
          try {
            _shardedPool.returnResource(jedis);
          } catch (Throwable thex) {
          }
        }
      }
    }
  }
}
