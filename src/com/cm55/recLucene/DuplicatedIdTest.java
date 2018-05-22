package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;
import org.junit.*;

import com.cm55.recLucene.RlAnalyzer.*;

import static org.junit.Assert.*;

/**
 * 複数のレコードに重複したIDがある場合の検索
 * @author ysugimura
 */
public class DuplicatedIdTest {

  Directory directory;
  IndexReader indexReader;
  IndexSearcher indexSearcher;
  
  @Before
  public void before() throws IOException {
    
    // Ramデータベースを作成する
    directory = new RAMDirectory();
    Analyzer analyzer = new LuceneAnalyzerWrapper(new JpnStandard());
    IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));

    {
      Document doc = new Document();
      doc.add(new StringField("id", "123", Field.Store.YES));
      doc.add(new StringField("field1", "field1", Field.Store.NO));
      indexWriter.addDocument(doc);
    }
    
    {
      Document doc = new Document();
      doc.add(new StringField("id", "123", Field.Store.YES));
      doc.add(new StringField("field2", "field2", Field.Store.NO));
      indexWriter.addDocument(doc);
    }
    
    indexWriter.commit();
    indexWriter.close();    
    
    indexReader = DirectoryReader.open(directory);
    indexSearcher = new IndexSearcher(indexReader);
  }
  
  @Test
  public void test() throws IOException {
    BooleanQuery booleanQuery = new BooleanQuery();
    Query query1 = new TermQuery(new Term("field1", "field1"));
    Query query2 = new TermQuery(new Term("field2", "field2"));
    booleanQuery.add(query1, BooleanClause.Occur.SHOULD);
    booleanQuery.add(query2, BooleanClause.Occur.SHOULD);
    
    TopDocs docs = indexSearcher.search(booleanQuery, 10);
    assertEquals(2, docs.totalHits);
    
    Set<String>set = new HashSet<String>();
    for (ScoreDoc scoreDoc : docs.scoreDocs) {
      Document doc = indexSearcher.doc(scoreDoc.doc);
      String id = doc.get("id");
      set.add(id);     
    }
    
    assertEquals(new HashSet<String>() {{
      add("123");
    }}, set);
    
  }
  
}
