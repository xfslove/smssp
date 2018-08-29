package com.github.xfslove.sgip12.util;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class StringUtil {

  /**
   * 定长字符串，左对齐存储，空余位置补’\0’
   *
   * @param origin origin string
   * @param size   bytes size
   * @return 定长字符串
   */
  public static byte[] toOctetStringBytes(String origin, int size) {
    if (origin == null || origin.length() == 0) {
      return new byte[size];
    }

    byte[] bytes = origin.getBytes();

    int rs = Math.min(bytes.length, size);

    byte[] ensured = new byte[rs];
    System.arraycopy(bytes, 0, ensured, 0, rs);

    return ensured;
  }
}
