package com.cm55.recLucene;

import java.util.*;

/**
 * 任意オブジェクトのフィールド名/値マップ
 * @author ysugimura
 */
public class RlValues {

  /** 値マップ */
  Map<String, Object>map = new HashMap<String, Object>();
  
  public <T>void put(String fieldName, T value) {
    map.put(fieldName, value);
  }
  
  @SuppressWarnings("unchecked")
  public <T>T get(String fieldName) {
    return (T)map.get(fieldName);
  }
}
