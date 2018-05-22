package com.cm55.recLucene;

import static org.junit.Assert.*;

import org.junit.*;

import com.google.inject.*;

public class RlTableSetTest {

  RlTableSet.Factory tableSetFactory;
  
  @Before
  public void before() {
    Injector i = Guice.createInjector();
    tableSetFactory = i.getInstance(RlTableSet.Factory.class);
  }
  


  @Test
  public void プライマリキー指定あり() {
    RlTableSet tableSet = tableSetFactory.create(Table2.class);
    /*
    LxField idField = tableSet.getFieldFromName("id");
    //ystem.out.println("" + idField);
  */
  }
  
  @Test
  public void フィールド名重複() {
    try {
      tableSetFactory.create(Table2.class, Table3.class);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().startsWith("フィールド名が重複しています：testField"));
    }
  }

  
  public static class Table1 {
    public String id;
  }
  
  public static class Table2 {
    @RlFieldAttr(pk=true)
    public String id;
    
    public String testField;
  }
  
  public static class Table3 {
    @RlFieldAttr(pk=true)
    public String id3;
    
    public String testField;
  }
  
  public static class Table4 {
    @RlFieldAttr(pk=true)
    public String id4;
    
    public String longField;
  }
}
