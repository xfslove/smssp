package com.github.xfslove.cmpp20.message;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface CmppMessage extends Serializable {

  MessageHead getHead();
}
