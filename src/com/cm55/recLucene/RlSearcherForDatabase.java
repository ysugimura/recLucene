package com.cm55.recLucene;

import org.apache.lucene.index.*;

/**
 * データベース用のサーチャ
 * <p>
 * このオブジェクトでは、ライターによって書き込まれたものは、たとえライターがcommitしてもcloseしても読み込むことは無い。
 * いったんサーチャーをクローズして、再度作成する必要がある。
 * </p>
 */
public class RlSearcherForDatabase<T> extends RlSearcher<T> {

  /** 検索対象のデータベース */
  private RlDatabase database;

  /** 読み込み用セマフォ */
  protected SemaphoreHandler.Acquisition ac;

  public RlSearcherForDatabase(RlTable<T>table, RlDatabase database, SemaphoreHandler.Acquisition ac) {
    super(table);
    this.database = database;
    this.ac = ac;
  }
  
  /** Luceneのインデックスリーダを取得する */
  @Override
  protected IndexReader createIndexReader() {
    return database.getIndexReader();
  }

  /** クローズする */
  @Override
  public void close() {
    super.close();
    ac.release();
  }
}
