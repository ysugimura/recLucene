package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.*;

import com.cm55.jpnutil.*;
import com.google.inject.*;


/**
 * 日本語をノーマライズするフィルタ
 * @author ysugimura
 */
public final class JpnNormalizeFilter extends TokenFilter {

  @Singleton
  public static class Factory {
    @Inject private Normalizer jpnConverter;
    public JpnNormalizeFilter create(TokenStream in) {
      return new JpnNormalizeFilter(in, jpnConverter);
    }
  }
  
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
 
  private Normalizer jpnConverter;
  

  private JpnNormalizeFilter(TokenStream in, Normalizer jpnConverter) {
    super(in); 
    this.jpnConverter = jpnConverter;
  }

  /** {@inheritDoc} */
  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;
    String original = new String(termAtt.buffer(), 0, termAtt.length());
    @SuppressWarnings("static-access")
    String normalized = jpnConverter.normalize(original);
    termAtt.setLength(0);
    termAtt.append(normalized);
    return true;
  }

}
