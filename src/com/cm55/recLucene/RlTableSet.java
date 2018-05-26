package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.*;

/**
 * データベース内でテーブルの集合を保持する
 * <p>
 * 特に注意が必要な点は、通常のRDBとは異なり、一つのデータベース全体において、同じフィールド名があってはなならない。
 * たとえ異なるテーブルであっても同じフィールド名は許されない。
 * このため、フィールド名からテーブルを特定できる。
 * </p>
 * @author ysugimura
 */
class RlTableSet {

  /** 全テーブルのリスト */
  List<RlTable<?>>tables = new ArrayList<>();

  /** フィールド名からテーブルを特定するためのマップ */
  Map<String, RlTable<?>> fieldToTable = new HashMap<>();

  /** 
   * Javaのレコードクラスからテーブルを特定するためのマップ。
   * これは{@link RlClassTable}についてしか作成されない。 
   * {@link AnyTable}にはJavaのレコードクラスが存在しない
   */
  Map<Class<?>, RlTable<?>> recordToTable = new HashMap<>();

  /**
   * Javaクラスを指定してテーブルを追加する。
   * @param classes Javaクラス配列
   * @return このオブジェクト
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  RlTableSet add(Class<?>...classes) {
    Arrays.stream(classes).map(c->new RlClassTable(c)).forEach(this::add);
    return this;
  }
  
  /**
   * テーブルを追加する
   * @param tableArray テーブル配列
   * @return このオブジェクト
   */
  RlTableSet add(RlTable<?>... tableArray) {
    Arrays.stream(tableArray).forEach(table -> {
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
   * 指定クラスに対応する{@link RlClassTable}を返す。
   * 存在しなければnullを返す。{@link AnyTable}の場合はレコードクラス自体が存在しないことに注意
   * @param clazz レコードクラス
   * @return {@link RlClassTable}
   */
   @SuppressWarnings("unchecked")
  <T> RlClassTable<T> getTable(Class<T> clazz) {
    return (RlClassTable<T>)recordToTable.get(clazz);
  }

  /**
   * フィールド名からRlFieldを取得する。存在しなければnullを返す。
   * {@link RlTableSet}中のすべてのテーブルのフィールドは一意名称であるため、フィールド名を指定すればテーブルが特定される。
   * @param fieldName フィールド名称
   * @return {@link RlField}
   */
   RlField<?> getFieldByName(String fieldName) {
    RlTable<?> table = fieldToTable.get(fieldName);
    if (table == null)
      return null;
    return table.getFieldByName(fieldName);
  }

  /**
   * 全テーブルのストリームを取得する
   * @return 全テーブルのストリーム
   */
  Stream<RlTable<?>> getTables() {
    return tables.stream();
  }
  
  /**
   * このテーブルセット中の全テーブルの「各フィールドAnalyzer」オブジェクトを取得する。
   * @return
   */
  Analyzer getPerFieldAnalyzer() {
    return new PerFieldAnalyzerWrapper(null, 
        getFieldAnalyzers().collect(Collectors.toMap(e->e.getKey(), e->e.getValue()))
     );
  }

  /**
   * このテーブルセット中の全テーブルのトークン化されるフィールドの{@link Analyzer}のマップストリームを取得する
   * @return
   */
  Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers() {
    return tables.stream().flatMap(table->table.getFieldAnalyzers());
  }
}
