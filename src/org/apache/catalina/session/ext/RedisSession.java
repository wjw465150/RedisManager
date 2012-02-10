package org.apache.catalina.session.ext;

import org.apache.catalina.session.StandardSession;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

import redis.clients.jedis.Protocol;

//imp tomcat StandardSession
public class RedisSession extends StandardSession {

	protected transient Log log = LogFactory.getLog(RedisSession.class);
	protected transient RedisManager _manager;

	public RedisSession(RedisManager manager) {
		super(manager);

		this._manager = manager;
	}

	/**
	 * Update the accessed time information for this session. This method should
	 * be called by the context when a request comes in for a particular session,
	 * even if the application does not reference it.
	 */
	@Override
	public void access() {
		if (_manager.debugEnabled) {
			log.info("id=" + this.id);
		}
		super.access();

		if (!_manager.isInitialized()) {
			return;
		}
		try {
			_manager.jedisExpire(RedisManager.TOMCAT_SESSION_PREFIX + this.id, this.maxInactiveInterval);
		} catch (Exception ex) {
			log.error("error:", ex);
		}
	}

	@Override
	public Object getAttribute(String name) {
		Object value = super.getAttribute(name);

		if (name.startsWith("javax.zkoss.zk.ui.Session")) {
			return value;
		}

		if (!_manager.isInitialized()) {
			return value;
		}
		try {
			byte[] bytesValue = _manager.jedisHget(RedisManager.TOMCAT_SESSION_PREFIX + this.id, name);
			_manager.jedisExpire(RedisManager.TOMCAT_SESSION_PREFIX + this.id, this.maxInactiveInterval);
			if (bytesValue == null) {
				return value;
			}

			if (_manager.debugEnabled) {
				log.info("id=" + this.id + ",name=" + name + ",strValue=" + new String(bytesValue, Protocol.CHARSET));
			}

			return _manager.deserialize(bytesValue);
		} catch (Exception ex) {
			log.error("error:name=" + name + ";value=" + value, ex);
			return value;
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		super.setAttribute(name, value);

		if (value == null) {
			return;
		}

		if (name.startsWith("javax.zkoss.zk.ui.Session")) {
			return;
		}

		if (!_manager.isInitialized()) {
			return;
		}
		try {
			byte[] bytesValue = _manager.serialize(value);
			if (_manager.debugEnabled) {
				log.info("id=" + this.id + ",name=" + name + ",strValue=" + new String(bytesValue, Protocol.CHARSET));
			}
			_manager.jedisHset(RedisManager.TOMCAT_SESSION_PREFIX + this.id, name, bytesValue);
			_manager.jedisExpire(RedisManager.TOMCAT_SESSION_PREFIX + this.id, this.maxInactiveInterval);
		} catch (Exception ex) {
			log.error("error:name=" + name + ";value=" + value, ex);
		}
	}

	@Override
	protected void removeAttributeInternal(String name, boolean notify) {
		if (_manager.debugEnabled) {
			log.info("id=" + this.id + ",name=" + name + ",notify=" + notify);
		}
		super.removeAttributeInternal(name, notify);

		if (!_manager.isInitialized()) {
			return;
		}
		try {
			_manager.jedisHdel(RedisManager.TOMCAT_SESSION_PREFIX + this.id, name);
		} catch (Exception ex) {
			log.error("error:", ex);
		}
	}

	@Override
	public void expire(boolean notify) {
		if (_manager.debugEnabled) {
			log.info("id=" + this.id + ",notify=" + notify);
		}
		super.expire(notify); //在expire里就会清空当前session的所有属性

		if (!_manager.isInitialized()) {
			return;
		}
		try {
			_manager.jedisDel(RedisManager.TOMCAT_SESSION_PREFIX + this.id);
		} catch (Exception ex) {
			log.error("error:", ex);
		}
	}

	void setCachedId(String id) {
		this.id = id;
	}
}
