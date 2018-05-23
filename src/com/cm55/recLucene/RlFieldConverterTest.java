package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;

public class RlFieldConverterTest {


  @Test
  public void test() throws Exception {
    Field field = Target.class.getDeclaredField("booleanValue");
    RlField rlField = new RlField(field);
    RlValues target = new RlValues();
    target.put("booleanValue",  false);
    assertEquals("0", rlField.getStringValue(target));
    target.put("booleanValue", true);
    assertEquals("1", rlField.getStringValue(target));
    
    rlField.setStringValue(target,  "0");
    assertFalse(target.get("booleanValue"));
  }
  
  public static class Target {
    @RlFieldAttr(converter = RlFieldConverter.BooleanConv.class)
    public boolean booleanValue;
  }
}
