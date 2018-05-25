package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.ngram.*;
import org.apache.lucene.analysis.util.*;
import org.apache.lucene.util.*;
import org.junit.*;

import com.cm55.jpnutil.*;


/**
 * Tokenizerの機能がよくわからない
 * @author ysugimura
 *
 */
public class LuceneTokenizerTest {
  
  @Test
  public void NGramTokenizerの使い方() throws Exception {
    
    // tokenizerを作成する
    Tokenizer tok = new NGramTokenizer();  
    
    // 入力を指定する。reset()がこの順序で必要。
    tok.setReader(new StringReader("吾輩は猫であるの研究"));
    tok.reset();

    // 読み込み
    assertEquals(
      ":,吾:,吾輩:,輩:,輩は:,は:,は猫:,猫:,猫で:,で:,であ:,あ:,ある:,る:,るの:,の:,の研:,研:,研究:,究",
      read(tok)
    );
    
    // 連続して使う場合は、end()ではなくclose()でないといけない
    tok.close();

    
    tok.setReader(new StringReader("吾輩は猫であるの研究"));
    tok.reset();
    assertEquals(
        ":,吾:,吾輩:,輩:,輩は:,は:,は猫:,猫:,猫で:,で:,であ:,あ:,ある:,る:,るの:,の:,の研:,研:,研究:,究",
        read(tok)
      );
    tok.close();
  }
  
  
  @Test
  public void WhitespaceTokenizerの使い方() throws Exception {
    
    TokenFilter t;
    CharFilter a;
    
    // tokenizerを作成する
    Tokenizer tok = new WhitespaceTokenizer();  
    
    // 入力を指定する。reset()がこの順序で必要。
    tok.setReader(new StringReader("吾輩　は 猫である\tの\n研究"));
    tok.reset();

    // 読み込み
    assertEquals(
      ":,吾輩:,は:,猫である:,の:,研究",
      read(tok)
    );
    
    // 連続して使う場合は、end()ではなくclose()でないといけない
    tok.close();
  }
  
  
  @Test
  public void LowerCaseTokenizerの使い方() throws Exception {
    // tokenizerを作成する
    Tokenizer tok = new LowerCaseTokenizer();  
    
    // 入力を指定する。reset()がこの順序で必要。
    tok.setReader(new StringReader("Sunday's SON"));
    tok.reset();

    // 読み込み
    assertEquals(
      ":,sunday:,s:,son",
      read(tok)
    );
    
    // 連続して使う場合は、end()ではなくclose()でないといけない
    tok.close();
  }

  @Test
  public void NormalTokenizerの使い方() throws Exception {
    // tokenizerを作成する
    Tokenizer tok = new NormalTokenizer();  
    
    // 入力を指定する。reset()がこの順序で必要。
    tok.setReader(new StringReader("吾輩　ハ 猫ﾃﾞある\tの\n研究ab'"));
    tok.reset();

    // 読み込み
    String s= read(tok);
    //ystem.out.println("" + s);
    assertEquals(
      ":,吾輩:,は:,猫て゛ある:,の:,研究ＡＢ＇",
      s
    );
    
    // 連続して使う場合は、end()ではなくclose()でないといけない
    tok.close();
  }
  
  public static class NormalTokenizer extends CharTokenizer {
    
    protected boolean isTokenChar(int c)
    {      
      return (!(Character.isWhitespace(c)));
    }

    @Override
    protected int normalize(int c) {
      String s = Normalizer.normalize("" +(char)c);
      //ystem.out.println("" + s);
      return s.charAt(0);
    }
    
  }
  
  private String read(Tokenizer tokenizer) throws IOException {
    StringBuilder result = new StringBuilder();
    while (tokenizer.incrementToken()) {      
      Iterator<AttributeImpl>it = tokenizer.getAttributeImplsIterator();
      result.append(":");
      while (it.hasNext()) {
        AttributeImpl o = it.next();
        result.append("," + o.toString());
      }
    }
    return result.toString();
  }
}
