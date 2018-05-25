package com.cm55.recLucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

public class RlWriterHolder {

  private Directory directory;
  private RlTableSet tableSet;
  private IndexWriter indexWriter;
  private SearcherManager searcherManager;
  
  public synchronized void reset(Directory directory, RlTableSet tableSet) {
    close();
    this.directory = directory;
    this.tableSet = tableSet;
  }
  
  public IndexWriter getIndexWriter() {
    ensure();
    return indexWriter;
  }
  
  public SearcherManager getSearcherManager() {
    ensure();
    return searcherManager;
  }
  
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
      throw new RuntimeException(ex);
    }
    try {
      searcherManager = new SearcherManager(indexWriter, true, true, null);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public synchronized void close() {
    if (indexWriter == null) return;
    try {     
      searcherManager.close();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    try {
      indexWriter.close();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    indexWriter = null;
    searcherManager = null;
  }
}
