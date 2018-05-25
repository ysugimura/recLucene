package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.junit.*;

public class RlAnyTableTest {


  @Test
  public void testForTable() {
    RlClassTable<Foo> table = new RlClassTable<Foo>(Foo.class);
    Stream<Map.Entry<String, Analyzer>>stream = table.getFieldAnalyzers();    
    assertEquals("a=Analyzer\nb=Analyzer", stream.map(s->s.toString()).sorted().collect(Collectors.joining("\n")));    
  }
  
  @Test
  public void testForTableSet() {
    RlTableSet set = new RlTableSet().add(Foo.class, Bar.class);
    Stream<Map.Entry<String, Analyzer>>stream = set.getFieldAnalyzers();
    assertEquals(
      "a=Analyzer\n" + 
      "b=Analyzer\n" + 
      "x=Analyzer\n" + 
      "y=Analyzer", 
      stream.map(s->s.toString()).sorted().collect(Collectors.joining("\n"))
    );
  }
  
  public static class Foo {
    @RlFieldAttr(pk=true)
    public String id;
    
    @RlFieldAttr()
    public String a;
    
    @RlFieldAttr()
    public String b;
  }
  
  public static class Bar {
    @RlFieldAttr(pk=true)
    public String pk;
    
    @RlFieldAttr()
    public String x;
    
    @RlFieldAttr()
    public String y;
  }
}
