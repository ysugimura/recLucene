package com.cm55.recLucene;

import org.apache.lucene.index.*;



/**
 * ライタ用のニアリアルタイムサーチャ
 */
public class RlSearcherForWriter<T> extends RlSearcher<T> {

  /** このサーチャーを取得した元のライター */
  private RlWriter writer;
  
  /** ライタの書き込み数 */
  private int writerWrittenCount;

  public RlSearcherForWriter(RlTable<T>table, RlWriter writer) {
    super(table);
    this.writer = writer;   
  }
  
  /**
   * Luceneのインデックスリーダを取得する
   */
  @Override
  protected IndexReader createIndexReader() {
    return writer.getIndexReader();
  }
}