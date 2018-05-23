package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class RlDatabaseTest {


  
  //@Test
  public void オープンテスト() {
    /*
    // 
    Util.deleteAll(new File("testOut"));
    
    RlDatabase database1 = databaseFactory.createDir("testOut", Table3.class, Table4.class);
    RlWriter writer1 = database1.createWriter();

    writer1.write(new Table3("1", "this is test"));
    writer1.write(new Table3("2", "that is sample"));
    writer1.write(new Table3("1", "this is test"));
    writer1.write(new Table3("2", "that is sample"));
    writer1.write(new Table4("1", "asdf"));
    writer1.commit();
    
    //RlDatabase database2 = databaseFactory.createDir(tableSet, "testOut");
    //RlWriter writer2 = database2.createWriter();
    
    RlSearcher searcher = database1.createSearcher(Table3.class);
    List<Object>list = searcher.getAllByPk(); //
    //List<Object>list = searcher.search(searcher.matchQuery("id3", "1"));
    for (Object o: list) {
      System.out.println("" + o);
    }
    */
  }
  
  //@Test
  public void アクセステスト() {
    RlDatabase database = RlDatabase.Factory.createRam(Table3.class, Table4.class);
    
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
    
    RlSearcher searcher = database.createSearcher(Table3.class);
    //List<Object>list = searcher.getAll(); //
    List<Object>list = searcher.search(searcher.matchQuery("id3", "1"));
    
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
      return Misc.equals(this.id3, that.id3)&&
          Misc.equals(this.testField, that.testField);
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
    RlDatabase database = RlDatabase.Factory.createRam(LongTest.class);
    RlWriter writer = database.createWriter();
    
    writer.write(new LongTest(10001, "test1"));
    writer.write(new LongTest(10001, "test2"));
    writer.commit();
    
    RlSearcher s = database.createSearcher(LongTest.class);
    Set<Long>ids = s.searchFieldSet("id", s.andQuery(s.matchQuery("id", 10001L)));
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
