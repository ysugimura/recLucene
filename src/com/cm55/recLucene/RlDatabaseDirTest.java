package com.cm55.recLucene;

import java.io.*;

import org.junit.*;

public class RlDatabaseDirTest {

  @Before
  public void before() {
    
  }
  
  @Test
  public void test() {
    RlDatabase db = new RlDatabase.Dir(new File("testDir"));
    db.add(Table1.class);
    try (RlSearcher<Table1>s = db.createSearcher(Table1.class)) {
      s.search(new RlQuery.Word("id", "test"));
    }
    
    
    db.reset();
  }

  public static class Table1 {
    public String id;
  }
  
}
