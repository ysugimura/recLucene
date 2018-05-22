package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.index.*;

import com.google.inject.*;

/**
 * ライタ用のニアリアルタイムサーチャ
 */
public class RlSearcherForWriter extends RlSearcher.Impl {

  @Singleton
  public static class Factory {
    @Inject private Provider<RlSearcherForWriter>provider;
    
    public RlSearcher create(RlTable table, RlWriter.Impl writer) {
      return provider.get().setup(table,  writer);
    }
  }
  
  private RlWriter.Impl writer;
  
  /** ライタの書き込み数 */
  private int writerWrittenCount;

  /** Luceneのインデックスリーダ */
  private IndexReader indexReader;

  public RlSearcherForWriter() {
  }
  
  private RlSearcherForWriter setup(RlTable table, RlWriter.Impl writer) {
    this.writer = writer;
    this.table = table;
    return this;
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