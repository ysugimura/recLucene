package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.*;

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
  List<RlTable<?>>tables = new ArrayList<>();

  /** フィールド名/テーブルマップ */
  Map<String, RlTable<?>> fieldToTable = new HashMap<>();

  /** レコードクラス/テーブルマップ */
  Map<Class<?>, RlTable<?>> recordToTable = new HashMap<>();
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  RlTableSet add(Class<?>...classes) {
    Arrays.stream(classes).map(c->new RlClassTable(c)).forEach(this::add);
    return this;
  }
  
  RlTableSet add(RlTable<?>... _tables) {
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
      if (table instanceof RlClassTable) {
        Class<?> recordClass = ((RlClassTable<?>)table).getRecordClass(); 
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
   * @return {@link RlClassTable}
   */
   @SuppressWarnings("unchecked")
  <T> RlClassTable<T> getTable(Class<T> clazz) {
    return (RlClassTable<T>)recordToTable.get(clazz);
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
    RlTable<?> table = fieldToTable.get(fieldName);
    if (table == null)
      return null;
    return table.getFieldByName(fieldName);
  }

  /**
   * テーブルコレクションを取得する
   * 
   * @return
   */
  Stream<RlTable<?>> getTables() {
    return tables.stream();
  }
  
  /**
   * このテーブルセット中の全テーブルの「各フィールドAnalyzer}オブジェクトを取得する。
   * @return
   */
  public Analyzer getPerFieldAnalyzer() {
    return new PerFieldAnalyzerWrapper(null, 
        getFieldAnalyzers().collect(Collectors.toMap(e->e.getKey(), e->e.getValue()))
     );
  }

  /**
   * このテーブルセット中の全テーブルのトークン化されるフィールドの{@link Analyzer}のマップストリームを取得する
   * @return
   */
  public Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers() {
    return tables.stream().flatMap(table->table.getFieldAnalyzers());
  }
}
