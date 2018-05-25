package com.cm55.recLucene;

import static org.junit.Assert.*;

import org.junit.*;

public class RlValuesTest {


  
  @Test
  public void test1() {
    RlField field = new RlField.Builder("test",  null).build();
    RlValues values = new RlValues();
    values.put("test",  "abc123");
    assertEquals("abc123", values.get("test"));    
    values.put("test",  "xyz123");
    assertEquals("xyz123", values.get("test"));
  }
  
  @Test
  public void test2() {
    RlField field = new RlField.Builder("test",  new RlFieldAttr.Default() {
      public Class<? extends RlFieldConverter<?>> converter() {
        return RlFieldConverter.LongConv.class;
      }
    }).build();
    RlValues values = new RlValues();
    values.put("test",  111L);
    assertEquals((Long)111L, (Long)values.get("test"));    
    values.put("test", 222L);
    assertEquals((Long)222L, values.get("test"));
  }
}
