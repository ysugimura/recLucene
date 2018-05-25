package com.cm55.recLucene;

import org.apache.lucene.search.*;

/**
 * クエリ時のソート順指定配列。
 * @author ysugimura
 */
public class RlSortFields {


  public final RlSortField[]rlSortFields;

  /** 
   * 任意の数のソートフィールドを指定する。
   * テーブル定義は一致していなければならない。
   * @param rlSortFields
   */
  public RlSortFields(RlSortField...rlSortFields) {
    this.rlSortFields = rlSortFields;
  }

  
  /** LuceneのSortオブジェクトを取得する */
  public Sort getSort() {
    throw new RuntimeException();
  }
}