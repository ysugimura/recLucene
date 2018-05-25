package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.index.*;



/**
 * ライタ用のニアリアルタイムサーチャ
 */
public class RlSearcherForWriter<T> extends RlSearcher<T> {

  /** このサーチャーを取得した元のライター */
  private RlWriter writer;
  
  /** ライタの書き込み数 */
  private int writerWrittenCount;

  /** Luceneのインデックスリーダ */
  private IndexReader indexReader;
  
  public RlSearcherForWriter(RlTable<T> fieldSet, RlWriter writer) {
    super(fieldSet);
    this.writer = writer;   
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