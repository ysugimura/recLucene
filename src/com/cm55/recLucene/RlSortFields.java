package com.cm55.recLucene;

import org.apache.lucene.search.*;

/**
 * クエリ時のソート順指定配列。
 * <p>
 * 指定される複数のフィールドのテーブル定義は同一でなければならないことに注意。
 * </p>
 * 
 * @author ysugimura
 */
public class RlSortFields {

  public final RlTable tableDef;
  public final RlSortField[]rlSortFields;

  /** 
   * 任意の数のソートフィールドを指定する。
   * テーブル定義は一致していなければならない。
   * @param rlSortFields
   */
  public RlSortFields(RlSortField...rlSortFields) {
    this.rlSortFields = rlSortFields;
    if (rlSortFields.length == 0) {
      tableDef = null;
      return;
    }
    tableDef = rlSortFields[0].getTableDef();
    for (RlSortField field: rlSortFields) {
      if (tableDef != field.getTableDef()) {
        throw new RlException.Usage("テーブル定義不一致");
      }
    }
  }

  /** テーブル定義を取得する */
  public RlTable getTableDef() {
    return tableDef;
  }
  
  /** LuceneのSortオブジェクトを取得する */
  public Sort getSort() {
    /*
    SortField[]sortFields = new SortField[rlSortFields.length];
    for (int i = 0; i < sortFields.length; i++) {
      LxSortField rlSortField = rlSortFields[i];
      LxField field = rlSortField.getField();
      sortFields[i] = new SortField(field.getName(), SortField.STRING, rlSortField.getDesc());
    }
    return new Sort(sortFields);
    */
    throw new RuntimeException();
  }
}