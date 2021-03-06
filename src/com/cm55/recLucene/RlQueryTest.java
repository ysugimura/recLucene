package com.cm55.recLucene;

import static org.junit.Assert.*;

import org.junit.*;

import com.cm55.recLucene.RlQuery.*;

public class RlQueryTest {

  @Before
  public void before() {
    
  }
  
  @Test
  public void fieldNameTest() {
    RlDatabase database = new RlDatabase.Ram().add(Sample.class);
    try (RlSearcher<Sample> searcher = database.createSearcher(Sample.class)) {
      try {
        searcher.search(new RlQuery.Match("test", "test"));
        fail();
      } catch (RlException ex) {
        assertEquals("Match field not found: test in com.cm55.recLucene.RlQueryTest$Sample", 
            ex.getMessage());
      }      
      try {
        searcher.search(new RlQuery.Word("test", "test"));
        fail();
      } catch (RlException ex) {
        assertEquals(
            "Word field not found: test in com.cm55.recLucene.RlQueryTest$Sample"
            , ex.getMessage());
      } 
    }
  }
  
  public static class Sample {
    @RlFieldAttr(tokenized=true)
    public String field0;
    
    @RlFieldAttr(tokenized=false)
    public String field1;
  }
  
  @Test
  public void test0() {
    assertEquals(
        new Match("field1", "1234"),
        new Match("field1", "1234"));
    assertEquals(
        new Word("field1", "1234"),
        new Word("field1", "1234"));
    assertEquals(
      new Range("field1", "10", "20"),
      new Range("field1", "10", "20")
    );
    
    assertNotEquals(
        new Match("field1", "1234"),
        new Match("field2", "1234"));
    assertNotEquals(
        new Word("field1", "1234"),
        new Word("field2", "1234"));
    assertNotEquals(
      new Range("field1", "10", "20"),
      new Range("field2", "10", "20")
    );
    
    assertNotEquals(
        new Match("field1", "1234"),
        new Match("field1", "1230"));
    assertNotEquals(
        new Word("field1", "1234"),
        new Word("field1", "1230"));
    assertNotEquals(
      new Range("field1", "10", "20"),
      new Range("field1", "10", "21")
    );
    
  }
  
  @Test
  public void test1() {
    
    assertEquals(
      new And(
        new Match("field1", "1234"),
        new Match("field2", "1234")
      ),
      new And(
        new Match("field1", "1234"),
        new Match("field2", "1234")
      )
    );
    assertEquals(
      new Or(
        new Word("field1", "1234"),
        new Word("field2", "1234")
      ),
      new Or(
        new Word("field1", "1234"),
        new Word("field2", "1234")
      )
    );

  }
  
  
  @Test
  public void debugStringTest() {
    
    RlQuery query = new And(
      new Word("field1", "value1"),
      new Match("field2", "value2"),
      new Or(
        new Prefix("field3", "value3"),
        new Range("field4", "value4", "value5")
      )
    );
    
//    System.out.println("" + query);
    assertEquals(
      "And:(Word:field1=value1),(Match:field2=value2),(Or:(Prefix:field3=value3),(Range:field4=value4,value5,true,true))",
      query.toString()
    );
  }
  

}
