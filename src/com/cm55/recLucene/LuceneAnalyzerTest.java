package com.cm55.recLucene;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.ngram.*;
import org.apache.lucene.util.*;
import org.junit.*;


public class LuceneAnalyzerTest {

  
  @Before
  public void before() {

  }

  @Test
  public void test() throws Exception {
    @SuppressWarnings("resource")
    Analyzer analyzer = new Analyzer() {
      protected TokenStreamComponents createComponents(String fieldName) {
        assertNull(fieldName);
        // ystem.out.println("" + fieldName);
        Tokenizer tokenizer = new NGramTokenizer();
        return new TokenStreamComponents(tokenizer);
      }
    };

    Reader reader = spy(new StringReader("abc123"));
    
    TokenStream stream = analyzer.tokenStream(
      null, 
      reader
    );
    

    stream.reset();
    
    try {
      StringBuilder result = new StringBuilder();
      while (stream.incrementToken()) {
        Iterator<AttributeImpl> it = stream.getAttributeImplsIterator();
        result.append(":");
        while (it.hasNext()) {
          AttributeImpl o = it.next();
          result.append("," + o.toString());
        }
      }
      assertEquals(":,a:,ab:,b:,bc:,c:,c1:,1:,12:,2:,23:,3", result.toString());

    } finally {
      stream.close();
    }
   
    // 特にresetは呼び出されない
    verify(reader, never()).reset();
    
    // stream.close()の呼び出しによりreader.close()が呼び出される。
    verify(reader).close();
  }
}
