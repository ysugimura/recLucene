package com.cm55.recLucene;

import org.apache.lucene.analysis.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.*;

public class RlWriterReader {

  IndexWriter indexWriter;
  IndexReader indexReader;
  
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
    indexReader = DirectoryReader.open(indexWriter, true, true);
    System.out.println("" + indexReader);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    System.out.println("created");
  }
  
  public void close() {
    System.out.println("closing");
    try {
      indexReader.close();
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
