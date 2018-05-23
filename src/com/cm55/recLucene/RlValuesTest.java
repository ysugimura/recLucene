package com.cm55.recLucene;

import static org.junit.Assert.*;

import org.junit.*;

public class RlValuesTest {


  
  @Test
  public void test1() {
    RlField field = new RlField("test",  null);
    RlValues values = new RlValues();
    values.put("test",  "abc123");
    assertEquals("abc123", field.getValue(values));    
    field.setValue(values,  "xyz123");
    assertEquals("xyz123", values.get("test"));
  }
  
  @Test
  public void test2() {
    RlField field = new RlField("test",  new RlFieldAttr.Default() {
      public Class<? extends RlFieldConverter<?>> converter() {
        return RlFieldConverter.LongConv.class;
      }
    });
    RlValues values = new RlValues();
    values.put("test",  111L);
    assertEquals((Long)111L, (Long)field.getValue(values));    
    field.setValue(values,  222L);
    assertEquals((Long)222L, values.get("test"));
  }
}
