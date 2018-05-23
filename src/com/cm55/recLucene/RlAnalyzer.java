package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.Analyzer.*;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.ngram.*;
import org.apache.lucene.analysis.util.*;
import org.apache.lucene.util.*;


/**
 * アナライザ
 * <p>
 * Luceneの{@link Analyzer}の使用する{@link TokenStreamComponents}を新規作成する
 * オブジェクト。
 * 特別な理由が無い限り、このクラスはSingletonでよい。{@link #createComponents()}が
 * 呼び出されるたびに新たな{@link TokenStreamComponents}を生成すればよい。
 * </p>
 * @author ysugimura
 */
public abstract class RlAnalyzer {
 
  /** luceneのAnalyzerが出力する{@link TokenStreamComponents}を代わりに作成する */
  public abstract TokenStreamComponents createComponents();

  /**
   * 文字列をトークン文字列に変換する
   * @param input
   * @return
   */
  public String[]expandString(String input) {
    Reader reader = new StringReader(input);
    try {
      return expandString(reader);
    } finally {
      try {
        reader.close();
      } catch (Exception ex) {
        throw new RlException(ex);
      }
    }
  }
  
  /**
   * {@link Reader}から読み取った文字列をトークン文字列リストに分割する
   * @param reader
   * @return
   */
  public String[]expandString(Reader reader) {
    
    // luceneのアナライザを作成する

    try (Analyzer analyzer = new LuceneAnalyzerWrapper(this)) {

    TokenStream stream = null;
    try {
      stream = analyzer.tokenStream(
        null,  // フィールド名は不要
        reader
      );
      stream.reset();
      List<String>list = new ArrayList<String>();
      while (stream.incrementToken()) {
        Iterator<AttributeImpl> it = stream.getAttributeImplsIterator();
        while (it.hasNext()) {
          AttributeImpl o = it.next();
          list.add(o.toString());
        }
      }
      return list.toArray(new String[0]);
    } catch (IOException ex) {
      throw new RlException(ex);
    } finally {
      try { stream.close(); } catch (IOException ex) {}
    }
    }
  }
  
  /**
   * {@link Defaults}にあるアナライザを使用するためのマーカ
   * @author ysugimura
   */
  public static class Default extends RlAnalyzer {
    @Override
    public TokenStreamComponents createComponents() {
      throw new RlException("this class is only as a marker");
    }
  }
  
  /**
   * このアナライザをLuceneのアナライザに変換するためのラッパ
   * @author ysugimura
   */
  public static class LuceneAnalyzerWrapper extends Analyzer {
    private RlAnalyzer analyzer;
    public LuceneAnalyzerWrapper(RlAnalyzer analyzer) {
      this.analyzer = analyzer;
    }
    protected TokenStreamComponents createComponents(String fieldName) {
      
      // ここではフィールド名は指定されない
      if (fieldName != null) throw new RlException("program error");
      return analyzer.createComponents();
    }
  }
  
  /**
   * 日本語用標準アナライザ
   * <ul>
   * <li>whitespaceでトークン分割される。
   * <li>各トークンについて、半角->全角変換、カナ->かな変換、小文字->大文字変換を行う。
   * <li>各トークンをNGram12によりさらに分割する。
   * </ul>
   * @author ysugimura
   */
  public static class JpnStandard extends RlAnalyzer { 
    @Override
    public TokenStreamComponents createComponents() {
      
      // 空白、改行で分割するtokenizer
      Tokenizer tokenizer = new WhitespaceTokenizer();  

      // トークンストリーム
      TokenStream tokenStream = new JpnNormalizeFilter(tokenizer);
      
      // フィルタ
      NGramTokenFilter filter = new NGramTokenFilter(tokenStream);
      
      return new TokenStreamComponents(tokenizer, filter);
    }
  }
  
  public static class AbstractNewlines extends RlAnalyzer {
    @Override
    public TokenStreamComponents createComponents() {
      
      // 改行で分割するtokenizer
      Tokenizer tokenizer = new NewlineTokenizer() {
        @Override
        public int normalize(int c) {
          return AbstractNewlines.this.normalize(c);
        }
      };
      
      return new TokenStreamComponents(tokenizer);
    }
    protected int normalize(int c) {      
      return c;
    }
  }
  
  /**
   * 改行による分割を行うアナライザ
   */
  public static class Newlines extends AbstractNewlines {
  }
  
  public class NewlineTokenizer extends CharTokenizer {
    public NewlineTokenizer() {
    }
    public NewlineTokenizer(AttributeFactory factory) {
      super(factory);
    }    
    protected int normalize(int c) {
      return super.normalize(c);
    }
    @Override
    protected boolean isTokenChar(int c) {
      return c != '\n';
    }
  }
}
