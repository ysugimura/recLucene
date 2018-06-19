package com.cm55.recLucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

/**
 * Luceneの{@link IndexWriter}と{@link SearchManager}を保持する。
 * データベースがオープンしているあいだは、これらがクローズされることはない。
 * resetによってデータベース変更がされるときのみに、クローズされてから再オープンされる。
 * @author ysugimura
 *
 */
public class RlWriterHolder {

  private Directory directory;
  private RlTableSet tableSet;
  private IndexWriter indexWriter;
  private SearcherManager searcherManager;

  /**
   * データベースディレクトリと、その中のテーブル定義を指定してリセットする。
   * @param directory ディレクトリ
   * @param tableSet テーブル定義集合
   */
  public synchronized void reset(Directory directory, RlTableSet tableSet) {
    close();
    this.directory = directory;
    this.tableSet = tableSet;
  }

  /** Luceneの{@link IndexWriter}を取得する。これはオープン中データベースにおける唯一のものになる。 */
  public IndexWriter getIndexWriter() {
    ensure();
    return indexWriter;
  }
  
  /** Luceneの{@link SearchManager}を取得する。これはオープン中データベースにおける唯一のものになる。 */
  public SearcherManager getSearcherManager() {
    ensure();
    return searcherManager;
  }

  /** 未作成であれば{@link IndexWriter}と{@link SearchManager}を作成する */
  private synchronized void ensure() {
    if (indexWriter != null) return;
    
    Analyzer analyzer = tableSet.getPerFieldAnalyzer();
    
    // コンフィギュレーションを作成。これは使い回せるものなのだろうか？
    IndexWriterConfig config = new IndexWriterConfig(analyzer);

    // クローズ時にコミットするモードになっていることを確認
    assert config.getCommitOnClose();

    try {
      indexWriter = new IndexWriter(directory, config);
    } catch (Exception ex) {
      throw new RlException(ex);
    }
    try {
      searcherManager = new SearcherManager(indexWriter, true, true, null);
    } catch (Exception ex) {
      throw new RlException(ex);
    }
  }
  
  /** 
   * クローズする。
   * データベースオープン中に呼び出されることは無い。
   * 上位の{@link RlWriter}においてクローズを行っても、このメソッドが呼び出されることはない。
   */
  public synchronized void close() {
    if (indexWriter == null) return;
    try {     
      searcherManager.close();
    } catch (Exception ex) {
      throw new RlException(ex);
    }
    try {
      indexWriter.close();
    } catch (java.nio.file.AccessDeniedException ex) {
      // そもそもフォルダに何も無い場合はこの例外が発生する。"write.lock"にアクセスできないという。
      // 無視する
    } catch (Exception ex) {
      throw new RlException(ex);
    }
    indexWriter = null;
    searcherManager = null;
  }
}
