package com.github.xfslove.smssp.server;

import java.util.Objects;

/**
 * @author hanwen
 * created at 2018/9/8
 */
public interface Notification {

  String getId();

  /**
   * @return 该通知的部分信息
   */
  Partition getPartition();

  /**
   * 把下一个通知合并进来
   *
   * @param next next notification
   * @return true if success
   */
  boolean merge(Notification next);

  class Partition {
    /**
     * 通知总数
     */
    private int total;
    /**
     * 当前index
     * 1-based
     */
    private int index;
    /**
     * part key
     */
    private String key;

    public Partition(int total, int index, String key) {
      this.total = total;
      this.index = index;
      this.key = key;
    }

    public boolean isPartOf() {
      return total > 1;
    }

    public int getTotal() {
      return total;
    }

    public int getIndex() {
      return index;
    }

    public String getKey() {
      return key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Partition partition = (Partition) o;
      return total == partition.total &&
          index == partition.index &&
          Objects.equals(key, partition.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(total, index, key);
    }

    @Override
    public String toString() {
      return "Partition{" +
          "total=" + total +
          ", index=" + index +
          ", key='" + key + '\'' +
          '}';
    }
  }
}
