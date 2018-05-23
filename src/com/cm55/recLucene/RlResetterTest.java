package com.cm55.recLucene;

import org.junit.*;

public class RlResetterTest {

  @Test
  public void test() {
    RlDatabase database = RlDatabase.createRam(Sample.class);
    RlWriter writer = database.createWriter();
    writer.write(new Sample());
    writer.commit();
    writer.close();
    RlSearcher searcher = database.createSearcher(Sample.class);
   
    database.createResetter().resetAndClose();
  }

  public static class Sample {
    String test = "abc";
  }
}
