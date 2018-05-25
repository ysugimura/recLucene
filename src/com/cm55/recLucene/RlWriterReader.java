package com.cm55.recLucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

public class RlWriterReader {

  IndexWriter indexWriter;
  SearcherManager searcherManager;
  
  public RlWriterReader(Directory directory, RlTableSet tableSet) {
    System.out.println("creating");
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


    System.out.println("created");
  }
  
  
  public void close() {
    System.out.println("closing");
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
    System.out.println("closed");
  }

}
