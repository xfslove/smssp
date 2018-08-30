package com.github.xfslove.util;

import java.nio.charset.Charset;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class StringUtil {

  /**
   * 定长字符串，左对齐存储，空余位置补’\0’
   *
   * @param origin  origin string
   * @param length  bytes size
   * @param charset charset
   * @return 定长字符串
   */
  public static byte[] getOctetStringBytes(String origin, int length, Charset charset) {
    if (origin == null || origin.length() == 0) {
      return new byte[length];
    }

    byte[] bytes = origin.getBytes(charset);
    byte[] octet = new byte[length];

    int rs = bytes.length > length ? length : bytes.length;

    System.arraycopy(bytes, 0, octet, 0, rs);

    return octet;
  }
}
