package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.lang.reflect.*;

import org.junit.*;

public class RlFieldTest {

  
  @SuppressWarnings("rawtypes")
  @Test
  public void test0() throws Exception {
    Class<?>clazz = Test0.class;
    Field field1 = clazz.getDeclaredField("value");
    try {
      new RlField.Builder(field1).build();
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
    
    @SuppressWarnings("rawtypes")
    RlField<?> rlField1 = new RlField.Builder(field1).build();
    assertTrue(rlField1.isPk());
    assertTrue(rlField1.isStore());
    
    @SuppressWarnings("rawtypes")
    RlField<?> rlField2 = new RlField.Builder(field2).build();
    assertFalse(rlField2.isPk()); 
    assertFalse(rlField2.isStore());

    @SuppressWarnings("rawtypes")
    RlField<?> rlField3 = new RlField.Builder(field3).build();
    assertFalse(rlField3.isPk());
    assertTrue(rlField3.isStore());
    
    Sample sample = new Sample(new Id(123L), "456", "789");
    //assertEquals("123", rlField1.getStringValue(sample));
    
    //rlField1.setStringValue(sample,  "333");
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
    @SuppressWarnings("rawtypes")
    RlField<?> field1 = new RlField.Builder("testField",  null).build();
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
