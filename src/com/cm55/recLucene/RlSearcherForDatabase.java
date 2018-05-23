package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.index.*;

import com.cm55.recLucene.RlDatabase.*;

/**
 * データベース用のサーチャ
 * <p>
 * 
 * </p>
 */
public class RlSearcherForDatabase extends RlSearcher.Impl
    implements RlSearcher {
  
  private RlDatabase database;
  
  /** Luceneのインデックスリーダ */
  protected IndexReader indexReader;


  public RlSearcherForDatabase() {
  }
  
  public RlSearcherForDatabase(RlTable table, RlDatabase  database) {
    this.database = database;
    this.table = table;
  }
  
  /** Luceneのインデックスリーダを取得する */
  @Override
  protected IndexReader getIndexReader() {
    if (indexReader != null)
      return indexReader;
    return indexReader = database.getIndexReader();
  }

  @Override
  protected void closeIndexReader() {
    if (indexReader != null) {
      try {
        indexReader.close();
      } catch (IOException ex) {
      }
      indexReader = null;
    }
  }
}
