package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class RlDatabaseResetTest {

  @Test
  public void testRam() {
    RlDatabase database = new RlDatabase.Ram().add(Sample.class);
    RlWriter writer = database.createWriter();
    writer.write(new Sample());

    writer.close();
    RlSearcher<Sample> searcher = database.createSearcher(Sample.class);
    List<Sample>list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(1, list.size());
    searcher.close();
    
    database.reset();
    
    searcher = database.createSearcher(Sample.class);
    list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(0, list.size());
    searcher.close();
  }
  
  @Test
  public void test() {
    RlDatabase database = new RlDatabase.Dir("sampleDb").add(Sample.class);
    RlWriter writer = database.createWriter();
    writer.write(new Sample());

    writer.close();
    RlSearcher<Sample> searcher = database.createSearcher(Sample.class);
    List<Sample>list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(1, list.size());
    searcher.close();
    
    database.reset();
    
    searcher = database.createSearcher(Sample.class);
    list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(0, list.size());
    searcher.close();
  }
  

  public static class Sample {
    String test = "abc";
  }
}
