package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.tokenattributes.*;

import com.cm55.jpnutil.*;


/**
 * 日本語をノーマライズするフィルタ
 * @author ysugimura
 */
public final class JpnNormalizeFilter extends TokenFilter {
  
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public JpnNormalizeFilter(TokenStream in) {
    super(in); 
  }

  /** {@inheritDoc} */
  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) return false;
    String original = new String(termAtt.buffer(), 0, termAtt.length());
    @SuppressWarnings("static-access")
    String normalized = Normalizer.normalize(original);
    termAtt.setLength(0);
    termAtt.append(normalized);
    return true;
  }

}
