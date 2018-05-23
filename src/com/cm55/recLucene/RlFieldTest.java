package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;

public class RlFieldTest {

  
  @Test
  public void test0() throws Exception {
    Class<?>clazz = Test0.class;
    Field field1 = clazz.getDeclaredField("value");
    try {
      new RlField(field1);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().startsWith("フィールドがString以外の場合には"));
    }
  }
  
  public static class Test0 {
    int value;
  }
  
  
  @Test
  public void test1() throws Exception {
    Class<?>clazz = Sample.class;
    Field field1 = clazz.getDeclaredField("field1");
    Field field2 = clazz.getDeclaredField("field2");
    Field field3 = clazz.getDeclaredField("field3");
    
    RlField lxField1 = new RlField(field1);
    assertTrue(lxField1.isPk());
    assertTrue(lxField1.isStore());
    
    RlField lxField2 = new RlField(field2);
    assertFalse(lxField2.isPk()); 
    assertFalse(lxField2.isStore());

    RlField lxField3 = new RlField(field3);
    assertFalse(lxField3.isPk());
    assertTrue(lxField3.isStore());
    
    Sample sample = new Sample(new Id(123L), "456", "789");
    //assertEquals("123", lxField1.getStringValue(sample));
    
    //lxField1.setStringValue(sample,  "333");
    //assertEquals(new Id(333), sample.field1);
  }

  public static class Id {
    public long value;
    public Id(long id) {
      this.value = id;
    }
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Id)) return false;
      return ((Id)o).value == this.value;
    }
  }

  public static class IdConverter extends RlFieldConverter.Abstract<Id> {

    public IdConverter() {
      super(Id.class);
    }
    
    @Override
    public String toString(Id value) {
      return value.value + "";
    }

    @Override
    public Id fromString(String string) {
      return new Id(Long.parseLong(string));
    }
    
  }
  
  public static class Sample {
    @RlFieldAttr(pk=true, converter=IdConverter.class)
    Id field1;
    
    String field2;
    
    @RlFieldAttr(store=true)
    String field3;
    
    public Sample() {}
    
    public Sample(Id field1, String field2, String field3) {
      this.field1 = field1;
      this.field2 = field2;
      this.field3 = field3;
    }
  }
  
  @Test
  public void testValues0() {
    RlField field1 = new RlField("testField",  null);
    assertEquals(
      "java:none,type:java.lang.String,name:testField,pk:false,sto:false,tok:true,conv:none,analy:none", 
      field1.toString()
    );  
    /*
    RlField field2 = factory.create("testField", String.class, null);
    assertEquals(
      "java:none,type:java.lang.String,name:testField,pk:false,sto:false,tok:true,conv:none,analy:none", 
      field1.toString()
    );
    */     
  }
}
