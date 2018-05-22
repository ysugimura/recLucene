package com.cm55.recLucene;

import java.lang.reflect.*;

import org.junit.*;
import static org.junit.Assert.*;

import com.google.inject.*;

public class RlFieldConverterTest {

  Injector injector;
  RlField.Factory fieldFactory;
  
  @Before
  public void before() {
    injector = Guice.createInjector();
    fieldFactory = injector.getInstance(RlField.Factory.class);
  }
  
  @Test
  public void test() throws Exception {
    Field field = Target.class.getDeclaredField("booleanValue");
    RlField lxField = fieldFactory.create(field);
    Target target = new Target();
    assertEquals("0", lxField.getStringValue(target));
    lxField.setValue(target,  true);
    assertEquals("1", lxField.getStringValue(target));
    
    lxField.setStringValue(target,  "0");
    assertFalse(target.booleanValue);
  }
  
  public static class Target {
    @RlFieldAttr(converter = RlFieldConverter.BooleanConv.class)
    public boolean booleanValue;
  }
}
