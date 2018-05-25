package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

/**
 * テーブルの集合を扱う。
 * <p>
 * 通常のRDBとは異なり、異なるテーブル内に同じフィールド名が存在してはならない.
 * </p>
 * 
 * @author ysugimura
 */
class RlTableSet {

  /** 全テーブル */
  List<RlFieldSet<?>>tables = new ArrayList<>();

  /** フィールド名/テーブルマップ */
  Map<String, RlFieldSet<?>> fieldToTable = new HashMap<>();

  /** レコードクラス/テーブルマップ */
  Map<Class<?>, RlFieldSet<?>> recordToTable = new HashMap<>();

  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  RlTableSet add(Class<?>...classes) {
    Arrays.stream(classes).map(c->new RlTable(c)).forEach(this::add);
    return this;
  }
  
  RlTableSet add(RlFieldSet<?>... _tables) {
    Arrays.stream(_tables).forEach(table -> {
      tables.add(table);

      // フィールド名/テーブル名マップを作成
      for (String fieldName : table.getFieldNames()) {
        if (fieldToTable.containsKey(fieldName)) {
          throw new RlException("フィールド名が重複しています：" + fieldName);
        }
        fieldToTable.put(fieldName, table);
      }

      // レコードクラス/テーブルマップに格納する。ただしレコードクラスがあるもののみ。
      if (table instanceof RlTable) {
        Class<?> recordClass = ((RlTable<?>)table).getRecordClass(); 
        if (recordToTable.containsKey(recordClass)) {
          throw new RlException("レコードクラスが重複しています:" + recordClass);
        }
        recordToTable.put(recordClass, table);
      }
    });
    return this;
  }
  
  /**
   * 指定クラスのマッピングを取得する。存在しなければnullを返す。 自由形式の場合はレコードクラス自体が存在しないことに注意
   * 
   * @param clazz
   *          レコードクラス
   * @return {@link RlTable}
   */
   @SuppressWarnings("unchecked")
  <T> RlTable<T> getTable(Class<T> clazz) {
    return (RlTable<T>)recordToTable.get(clazz);
  }

  /**
   * フィールド名からRlFieldを取得する。存在しなければnullを返す。
   * {@link RlTableSet}中のすべてのテーブルのフィールドは一意名称であるため、フィールド名を
   * 指定すれば、テーブルが決まり、フィールドも決まる。\
   * 
   * @param fieldName
   * @return
   */
  RlField getFieldByName(String fieldName) {
    RlFieldSet<?> table = fieldToTable.get(fieldName);
    if (table == null)
      return null;
    return table.getFieldByName(fieldName);
  }

  /**
   * テーブルコレクションを取得する
   * 
   * @return
   */
  Stream<RlFieldSet<?>> getTables() {
    return tables.stream();
  }
}
