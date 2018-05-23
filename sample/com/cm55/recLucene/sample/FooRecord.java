package com.cm55.recLucene.sample;

import com.cm55.recLucene.*;

public class FooRecord {

  /** ID */
  @RlFieldAttr(pk = true, converter = IdConverter.class)
  public Long id;

  /** 自由文字列 */
  @RlFieldAttr(store = false)
  public String content;
  
  public FooRecord() {    
  }
  
  public FooRecord(Long id, String content) {
    this.id = id;
    this.content = content;
  }

  @Override
  public String toString() {
    return "id:" + id + ",content:" + content;
  }
  
  public static class IdConverter extends RlFieldConverter.Abstract<Long> {
    public IdConverter() {
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