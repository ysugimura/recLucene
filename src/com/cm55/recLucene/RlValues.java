package com.cm55.recLucene;

import java.util.*;

/**
 * 任意のフィールド名/値のマップを格納するオブジェクト
 * @author ysugimura
 */
public class RlValues {

  /** 値マップ */
  Map<String, Object>valueMap = new HashMap<String, Object>();

  /** フィールド名を指定して値を格納する */
  public <T>void put(String fieldName, T value) {
    valueMap.put(fieldName, value);
  }

  /** フィールド名を指定して値を取得する */
  @SuppressWarnings("unchecked")
  public <T>T get(String fieldName) {
    return (T)valueMap.get(fieldName);
  }
}
