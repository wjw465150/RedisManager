package org.apache.catalina.session.ext;

import java.io.IOException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.session.StandardSession;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.wjw.kryo.wrapper.KryoSerializer;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.PipelineBlock;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.TransactionBlock;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * 
 * @author Administrator
 */
public class RedisManager extends StandardManager {
	private final Log log = LogFactory.getLog(RedisManager.class);

	static final String TOMCAT_SESSION_PREFIX = "TS:";
	static ShardedJedisPool _shardedPool = null;
	static JedisPool _pool = null;
	//->---------------属性----------------------
	boolean debugEnabled = false;
	private String debug = "false"; //是否打开调试模式

	/**
	 * 是否打开调试模式
	 * 
	 * @return the debug
	 */
	public String getDebug() {
		return debug;
	}

	/**
	 * 是否打开调试模式
	 * 
	 * @param debug
	 *          the debug to set
	 */
	public void setDebug(String debug) {
		this.debug = debug;
	}

	boolean stickySessionEnabled = true;
	private String stickySession = "true"; //是否打开会话粘连模式

	/**
	 * 是否打开会话粘连模式
	 * 
	 * @return the stickySession
	 */
	public String getStickySession() {
		return stickySession;
	}

	/**
	 * 是否打开会话粘连模式
	 * 
	 * @param stickySession
	 *          the stickySession to set
	 */
	public void setStickySession(String stickySession) {
		this.stickySession = stickySession;
	}

	protected String serverlist = "127.0.0.1:6379"; //用逗号(,)分隔的"ip:port"列表

	/**
	 * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
	 * 
	 * @return
	 */
	public String getServerlist() {
		return serverlist;
	}

	/**
	 * the list of all cache servers;用逗号(,)分隔的"ip:port"列表
	 * 
	 * @param serverlist
	 */
	public void setServerlist(String serverlist) {
		this.serverlist = serverlist;
	}

	protected String minConn = "5";

	/**
	 * Returns the minimum number of spare connections in available pool.
	 * 
	 * @return number of connections
	 */
	public String getMinConn() {
		return minConn;
	}

	/**
	 * Sets the minimum number of spare connections to maintain in our available
	 * pool.
	 * 
	 * @param minConn
	 *          - number of connections
	 */
	public void setMinConn(String minConn) {
		this.minConn = minConn;
	}

	protected String maxConn = "100";

	/**
	 * Returns the maximum number of spare connections allowed in available pool.
	 * 
	 * @return number of connections
	 */
	public String getMaxConn() {
		return maxConn;
	}

	/**
	 * Sets the maximum number of spare connections allowed in our available pool.
	 * 
	 * @param maxConn
	 *          - number of connections
	 */
	public void setMaxConn(String maxConn) {
		this.maxConn = maxConn;
	}

	protected String socketTO = "6000";

	/**
	 * Returns the socket timeout for reads.
	 * 
	 * @return the socketTO timeout in ms
	 */
	public String getSocketTO() {
		return socketTO;
	}

	/**
	 * Sets the socket timeout for reads.
	 * 
	 * @param socketTO
	 *          timeout in ms
	 */
	public void setSocketTO(String socketTO) {
		this.socketTO = socketTO;
	}

	//<----------------属性----------------------

	public RedisManager() {
		super();
	}

	/**
	 * Return the active Session, associated with this Manager, with the specified
	 * session id (if any); otherwise return <code>null</code>.
	 * 
	 * @param id
	 *          The session id for the session to be returned
	 * 
	 * @exception IllegalStateException
	 *              if a new session cannot be instantiated for any reason
	 * @exception IOException
	 *              if an input/output error occurs while processing this request
	 */
	@Override
	public Session findSession(String id) throws IOException {
		Session session = super.findSession(id);
		if (!this.isStarted()) {
			return session;
		}
		if (session == null && id != null) { //说明session有可能在另一个节点上
			try {
				boolean idExists = jedisExists(TOMCAT_SESSION_PREFIX + id);
				if (idExists) { //Redis里有Session ID
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
	 * Construct and return a new session object, based on the default settings
	 * specified by this Manager's properties. The session id will be assigned by
	 * this method, and available via the getId() method of the returned session.
	 * If a new session cannot be created for any reason, return <code>null</code>
	 * .
	 * 
	 * @exception IllegalStateException
	 *              if a new session cannot be instantiated for any reason
	 */
	@Override
	public Session createSession(String sessionId) {
		Session session = super.createSession(sessionId);
		if (!this.isStarted()) {
			return session;
		}

		sessionId = session.getId();
		if (this.debugEnabled) {
			log.info("id=" + sessionId);
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
	 * Remove this Session from the active Sessions for this Manager, and from the
	 * Redis.
	 * 
	 * @param session
	 *          Session to be removed
	 */
	@Override
	public void remove(Session session) {
		if (this.debugEnabled) {
			log.info("id=" + session.getId());
		}
		super.remove(session);
		if (!this.isStarted()) {
			return;
		}

		try {
			jedisDel(TOMCAT_SESSION_PREFIX + session.getId());
		} catch (Exception ex) {
			log.error("error:", ex);
		}
	}

	public boolean isStarted() {
		return (super.getState() == LifecycleState.STARTED);
	}

	@Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();

		debugEnabled = Boolean.parseBoolean(debug);
		stickySessionEnabled = Boolean.parseBoolean(stickySession);

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
						poolConfig.setTimeBetweenEvictionRunsMillis(1000L * 30L); //驱逐线程30秒执行一次
						poolConfig.setNumTestsPerEvictionRun(-1); //-1,表示在驱逐线程执行时,测试所有的空闲对象

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
	protected void stopInternal() throws LifecycleException {
		try {
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
		} finally {
			super.stopInternal();
		}
	}

	@Override
	public String toString() {
		return "RedisManager{" + "stickySession=" + stickySession + ",debug=" + debug + ",serverlist=" + serverlist
		    + ",minConn=" + minConn + ",maxConn=" + maxConn + ",socketTO=" + socketTO + '}';
	}

	byte[] serialize(Object obj) throws IOException {
		return KryoSerializer.write(obj);
	}

	Object deserialize(byte[] bb) throws IOException, ClassNotFoundException {
		return KryoSerializer.read(bb);
	}

	//TODO@Redis方法
	public Boolean jedisExists(String key) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.exists(key);
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
				return jedis.exists(key);
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

	Long jedisExpire(String key, int seconds) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.expire(key, seconds);
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
				return jedis.expire(key, seconds);
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

	java.util.List<Object> jedisMulti(String key, TransactionBlock jedisTransaction) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.multi(jedisTransaction);
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
				return jedisA.multi(jedisTransaction);
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

	java.util.List<Object> jedisPipelined(String key, PipelineBlock pipelineBlock) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.pipelined(pipelineBlock);
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
				return jedisA.pipelined(pipelineBlock);
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

	byte[] jedisHget(String hkey, String field) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.hget(hkey.getBytes(Protocol.CHARSET), field.getBytes(Protocol.CHARSET));
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
				return jedis.hget(hkey.getBytes(Protocol.CHARSET), field.getBytes(Protocol.CHARSET));
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

	public Long jedisHset(String hkey, String field, byte[] value) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.hset(hkey.getBytes(Protocol.CHARSET), field.getBytes(Protocol.CHARSET), value);
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
				return jedis.hset(hkey.getBytes(Protocol.CHARSET), field.getBytes(Protocol.CHARSET), value);
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

	public Long jedisHdel(String hkey, String field) {
		if (_pool != null) {
			Jedis jedis = null;
			try {
				jedis = _pool.getResource();
				return jedis.hdel(hkey, field);
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
				return jedis.hdel(hkey, field);
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

	//	String jedisGet(String key) {
	//		if (_pool != null) {
	//			Jedis jedis = null;
	//			try {
	//				jedis = _pool.getResource();
	//				byte[] byteValue = jedis.get(key.getBytes(Protocol.CHARSET));
	//				if (byteValue == null) {
	//					return null;
	//				}
	//				return new String(byteValue, Protocol.CHARSET);
	//			} catch (IOException e) {
	//				throw new JedisConnectionException(e);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_pool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		} else {
	//			ShardedJedis jedis = null;
	//			try {
	//				jedis = _shardedPool.getResource();
	//				byte[] byteValue = jedis.get(key.getBytes(Protocol.CHARSET));
	//				if (byteValue == null) {
	//					return null;
	//				}
	//				return new String(byteValue, Protocol.CHARSET);
	//			} catch (IOException e) {
	//				throw new JedisConnectionException(e);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_shardedPool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		}
	//	}
	//
	//	String jedisSetex(String key, int seconds, String value) {
	//		if (_pool != null) {
	//			Jedis jedis = null;
	//			try {
	//				jedis = _pool.getResource();
	//				return jedis.setex(key.getBytes(Protocol.CHARSET), seconds, value.getBytes(Protocol.CHARSET));
	//			} catch (IOException e) {
	//				throw new JedisConnectionException(e);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_pool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		} else {
	//			ShardedJedis jedis = null;
	//			try {
	//				jedis = _shardedPool.getResource();
	//				return jedis.setex(key.getBytes(Protocol.CHARSET), seconds, value.getBytes(Protocol.CHARSET));
	//			} catch (IOException e) {
	//				throw new JedisConnectionException(e);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_shardedPool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		}
	//	}
	//
	//	Long jedisTtl(String key) {
	//		if (_pool != null) {
	//			Jedis jedis = null;
	//			try {
	//				jedis = _pool.getResource();
	//				return jedis.ttl(key);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_pool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		} else {
	//			ShardedJedis jedis = null;
	//			try {
	//				jedis = _shardedPool.getResource();
	//				return jedis.ttl(key);
	//			} finally {
	//				if (jedis != null) {
	//					try {
	//						_shardedPool.returnResource(jedis);
	//					} catch (Throwable thex) {
	//					}
	//				}
	//			}
	//		}
	//	}

}
