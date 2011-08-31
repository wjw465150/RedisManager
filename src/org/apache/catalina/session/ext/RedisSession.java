package org.apache.catalina.session.ext;

import org.apache.catalina.session.*;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class RedisSession extends StandardSession {

  protected transient Log log = LogFactory.getLog(RedisSession.class);
  protected transient RedisManager _manager;

  public RedisSession(RedisManager manager) {
    super(manager);

    this._manager = manager;
  }

  /**
   * Update the accessed time information for this session.  This method
   * should be called by the context when a request comes in for a particular
   * session, even if the application does not reference it.
   */
  @Override
  public void access() {
    if (_manager.debugEnabled) {
      log.info("id=" + this.id);
    }
    super.access();

    try {
      _manager.jedisSetex(this.id, this.maxInactiveInterval, RedisManager.SNA_ID_VALUE);
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

    try {
      String key = this.id + ":" + name;

      String strValue = _manager.jedisGet(key);
      if (strValue == null) {
        return value;
      }
      if (_manager.debugEnabled) {
        log.info("id=" + this.id + ",name=" + name + ",strValue=" + strValue);
      }
      return RedisManager._xstream.fromXML(strValue);
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

    try {
      String key = this.id + ":" + name;
      String strValue = RedisManager._xstream.toXML(value);
      if (_manager.debugEnabled) {
        log.info("id=" + this.id + ",name=" + name + ",strValue=" + strValue);
      }
      _manager.jedisSetex(key, RedisManager.LIFE_TIME, strValue);
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

    try {
      String key = this.id + ":" + name;
      _manager.jedisDel(key);
    } catch (Exception ex) {
      log.error("error:", ex);
    }
  }

  @Override
  public void expire(boolean notify) {
    if (_manager.debugEnabled) {
      log.info("id=" + this.id + ",notify=" + notify);
    }
    super.expire(notify);  //在expire里就会清空当前session的所有属性

    try {
      _manager.jedisDel(this.id);
    } catch (Exception ex) {
      log.error("error:", ex);
    }
  }

  void setCachedId(String id) {
    this.id = id;
  }
}
