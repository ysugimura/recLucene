package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;

public class RlFieldConverterTest {


  @Test
  public void test() throws Exception {
    Field field = Target.class.getDeclaredField("booleanValue");
    RlField lxField = RlField.Factory.create(field);
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
