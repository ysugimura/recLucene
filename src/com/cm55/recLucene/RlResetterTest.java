package com.cm55.recLucene;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

public class RlResetterTest {

  @Test
  public void testRam() {
    RlDatabase database = RlDatabase.createRam().add(Sample.class);
    RlWriter writer = database.createWriter();
    writer.write(new Sample());
    writer.commit();
    writer.close();
    RlSearcher searcher = database.createSearcher(Sample.class);
    List<Sample>list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(1, list.size());
    searcher.close();
    
    database.createResetter().resetAndClose();
    
    searcher = database.createSearcher(Sample.class);
    list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(0, list.size());
    searcher.close();
  }
  
  @Test
  public void test() {
    RlDatabase database = RlDatabase.createDir("sampleDb").add(Sample.class);
    RlWriter writer = database.createWriter();
    writer.write(new Sample());
    writer.commit();
    writer.close();
    RlSearcher searcher = database.createSearcher(Sample.class);
    List<Sample>list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(1, list.size());
    searcher.close();
    
    database.createResetter().resetAndClose();
    
    searcher = database.createSearcher(Sample.class);
    list = searcher.search(new RlQuery.Word("test", "abc"));
    assertEquals(0, list.size());
    searcher.close();
  }
  

  public static class Sample {
    String test = "abc";
  }
}
