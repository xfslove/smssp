package com.github.xfslove.sgip12.util;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class StringUtil {

  /**
   * ensure a string with a specified size
   *
   * @param origin origin string
   * @param size   ensure size
   * @return ensured string
   */
  public static byte[] ensure(String origin, int size) {
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
