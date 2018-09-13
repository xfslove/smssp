package com.github.xfslove.smssp.util;

import java.nio.charset.Charset;

/**
 * @author hanwen
 * created at 2018/8/28
 */
public class ByteUtil {

  /**
   * 定长字符串，左对齐存储，空余位置补’\0’
   *
   * @param origin  origin string
   * @param length  bytes size
   * @param charset charset
   * @return 定长字符串
   */
  public static byte[] getStringOctetBytes(String origin, int length, Charset charset) {
    return getOctetBytes(origin == null ? null : origin.getBytes(charset), length);
  }

  public static byte[] getOctetBytes(byte[] origin, int length) {
    if (origin == null || origin.length == 0) {
      return new byte[length];
    }

    byte[] octet = new byte[length];

    int rs = origin.length > length ? length : origin.length;

    System.arraycopy(origin, 0, octet, 0, rs);

    return octet;
  }

  public static byte[] concat(byte[]... arrays) {
    int length = 0;
    for (byte[] array : arrays) {
      length += array.length;
    }
    byte[] result = new byte[length];
    int pos = 0;
    for (byte[] array : arrays) {
      System.arraycopy(array, 0, result, pos, array.length);
      pos += array.length;
    }
    return result;
  }
}
