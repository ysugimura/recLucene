package com.cm55.recLucene.sample;

import com.cm55.recLucene.*;

public class IndexRecord {

  /** 翻訳ID */
  @RlFieldAttr(pk = true, converter = TranslatedIdConverter.class)
  public Long translatedId;

  /** 言語 */
  @RlFieldAttr(store = false, tokenized = false)
  public String lang;

  /** 本文。タイトル＋字幕 */
  @RlFieldAttr(store = false)
  public String content;

  @Override
  public String toString() {
    return "translatedId:" + translatedId + ",lang:" + lang + ",content:" + content;
  }

  public static class TranslatedIdConverter extends RlFieldConverter.Abstract<Long> {
    public TranslatedIdConverter() {
      super(Long.class);
    }

    @Override
    public String toString(Long id) {
      return id.toString();
    }

    @Override
    public Long fromString(String string) {
      return Long.parseLong(string);
    }
  }
}