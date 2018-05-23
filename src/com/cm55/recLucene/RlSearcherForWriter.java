package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.index.*;



/**
 * ライタ用のニアリアルタイムサーチャ
 */
public class RlSearcherForWriter extends RlSearcher.Impl {

  
  private RlWriter writer;
  
  /** ライタの書き込み数 */
  private int writerWrittenCount;

  /** Luceneのインデックスリーダ */
  private IndexReader indexReader;
  
  public RlSearcherForWriter(RlTable table, RlWriter writer) {
    this.writer = writer;
    this.table = table;
   
  }
  
  /**
   * Luceneのインデックスリーダを取得する
   */
  @Override
  protected IndexReader getIndexReader() {

    // ライタが一つでも書き込みを行っていれば、リーダの再取得のために
    // クローズしておく。
    if (writerWrittenCount != writer.writtenCount()) {
      closeIndexReader();
    }

    // リーダがあればそれを返す。
    if (indexReader != null)
      return indexReader;

    // ライタの書き込み数を取得し、ライタからリーダを取得
    writerWrittenCount = writer.writtenCount();
    return indexReader = writer.getIndexReader();
  }

  /**
   * インデックスリーダをクローズする
   */
  @Override
  protected void closeIndexReader() {
    if (indexReader == null)
      return;
    try {
      indexReader.close();
    } catch (IOException ex) {
    }
    indexReader = null;
  }
}