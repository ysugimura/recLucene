package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.ngram.*;
import org.apache.lucene.util.*;
import org.junit.*;

import com.google.inject.*;

public class LuceneTokenFilterTest {
  Injector injector;

  JpnNormalizeFilter.Factory normalizeFilterFactory;
  
  @Before
  public void before() {
    injector = Guice.createInjector();    
    normalizeFilterFactory = injector.getInstance(JpnNormalizeFilter.Factory.class);
  }
  
  @Test
  public void NGramTokenizerの使い方() throws Exception {
    
    // tokenizerを作成する
    Tokenizer tokenizer = new WhitespaceTokenizer();  
    NGramTokenFilter filter = new NGramTokenFilter(
        normalizeFilterFactory.create(tokenizer));
    
    // 入力を指定する。reset()がこの順序で必要。
    tokenizer.setReader(new StringReader("吾1ab輩 は 猫デある ﾉ 研究"));
    tokenizer.reset();

    // 読み込み
    
    assertEquals(
      ":,吾:,吾輩:,輩:,輩は:,は:,は猫:,猫:,猫で:,で:,であ:,あ:,ある:,る:,るの:,の:,の研:,研:,研究:,究",
      read(filter)
    );

    
    
    // 連続して使う場合は、end()ではなくclose()でないといけない
    tokenizer.close();
  }
  

  
  private String read(TokenStream tokenizer) throws IOException {
    StringBuilder result = new StringBuilder();
    while (tokenizer.incrementToken()) {      
      Iterator<AttributeImpl>it = tokenizer.getAttributeImplsIterator();
      result.append(":");
      while (it.hasNext()) {
        AttributeImpl o = it.next();
        result.append("," + o.toString());
      }
    }
    System.out.println("" + result);
    return result.toString();
  }
}
