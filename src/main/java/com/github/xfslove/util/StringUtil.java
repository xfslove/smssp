package com.github.xfslove.util;

import java.nio.charset.StandardCharsets;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class StringUtil {

  /**
   * 定长字符串，左对齐存储，空余位置补’\0’
   *
   * @param origin origin string
   * @param length bytes size
   * @return 定长字符串
   */
  public static byte[] getOctetStringBytes(String origin, int length) {
    if (origin == null || origin.length() == 0) {
      return new byte[length];
    }

    byte[] bytes = origin.getBytes(StandardCharsets.ISO_8859_1);

    int rs = bytes.length > length ? length : bytes.length;

    byte[] octet = new byte[rs];
    System.arraycopy(bytes, 0, octet, 0, rs);

    return octet;
  }
}
