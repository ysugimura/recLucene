package com.cm55.recLucene;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.inject.*;

public class RlValuesTest {

  RlField.Factory fieldFactory;
  
  @Before
  public void before() {
    Injector i = Guice.createInjector();
    fieldFactory = i.getInstance(RlField.Factory.class);
  }
  
  @Test
  public void test1() {
    RlField field = fieldFactory.create("test",  null);
    RlValues values = new RlValues();
    values.put("test",  "abc123");
    assertEquals("abc123", field.getValue(values));    
    field.setValue(values,  "xyz123");
    assertEquals("xyz123", values.get("test"));
  }
  
  @Test
  public void test2() {
    RlField field = fieldFactory.create("test",  new RlFieldAttr.Default() {
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
