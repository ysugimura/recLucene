package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class RlDatabaseTest {


  
  //@Test
  public void オープンテスト() {

  }
  
  //@Test
  public void アクセステスト() {
    RlDatabase database = new RlDatabase.Ram().add(Table3.class, Table4.class);
    
    RlWriter writer = database.createWriter();
    try {
      writer.write(new Table1());
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().endsWith("Table1は登録されていません"));
    }
    
    writer.write(new Table3("1", "this is test"));
    writer.write(new Table3("2", "that is sample"));
    writer.write(new Table3("1", "this is test"));
    writer.write(new Table3("2", "that is sample"));
    
    //writer.commit();
    
    RlSearcher<Table3> searcher = database.createSearcher(Table3.class);
    //List<Object>list = searcher.getAll(); //
    List<Table3>list = searcher.search(new RlQuery.Match("id3", "1"));
    
    assertEquals(1, list.size());
    assertEquals(new Table3("1", null), list.get(0));
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
    
    public Table3() {}
    public Table3(String id3, String testField) {
      this.id3 = id3;
      this.testField = testField;
    }
    
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Table3)) return false;
      Table3 that = (Table3)o;
      return Objects.equals(this.id3, that.id3)&&
          Objects.equals(this.testField, that.testField);
    }
  }
  
  public static class Table4 {
    @RlFieldAttr(pk=true)
    public String id4;
    
    public String longField;
    
    public Table4() {}
    public Table4(String id4, String longField) {
      this.id4 = id4;
      this.longField = longField;
    }
  }

  @Test
  public void longTest() {
    RlDatabase database = new RlDatabase.Ram().add(LongTest.class);
    RlWriter writer = database.createWriter();
    
    writer.write(new LongTest(10001, "test1"));
    writer.write(new LongTest(10001, "test2"));
    writer.commit();
    
    RlSearcher<LongTest> s = database.createSearcher(LongTest.class);
    Set<Long>ids = s.searchFieldSet("id", new RlQuery.And(new RlQuery.Match("id", 10001L)));
    //ystem.out.println("" + ids.size());
  }
  
  public static class LongTest {
    @RlFieldAttr(converter=RlFieldConverter.LongConv.class, store=true, tokenized=false)
    public long id;
    
    public String value;
    
    public LongTest() {}
    public LongTest(long id, String value) {
      this.id = id;
      this.value = value;
    }
  }
}
