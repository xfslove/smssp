package com.github.xfslove.sgip12.message;

import java.io.Serializable;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public interface SgipMessage extends Serializable {

  MessageHead getHead();
}
