package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.cm55.recLucene.RlQuery.*;

public class RlValuesTotalTest {




  
  private RlFieldAttr idAttr;
  private RlFieldAttr matchAttr;
  private RlFieldAttr tokenAttr;

  RlTable table;
  RlDatabase database;
  
  @Before
  public void before() {




    
    idAttr = new RlFieldAttr.Default().setPk(true).setConverter(RlFieldConverter.LongConv.class);
    matchAttr = new RlFieldAttr.Default().setStore(false).setAnalyzer(RlAnalyzer.Newlines.class);
    tokenAttr = new RlFieldAttr.Default().setStore(false);
    
    List<RlField>fields = new ArrayList<RlField>();

    fields.add(new RlField("id", idAttr));
    for (int i = 0; i < 2; i++) {
      fields.add(new RlField("match" + i, matchAttr));
    }
    for (int i = 0; i < 2; i++) {
      fields.add(new RlField("token" + i, tokenAttr));
    }
    
    table = new RlTable(fields);
    database = new RlDatabase.Ram().add(table);    
  }
  
  @Test
  public void test() {
    RlWriter writer = database.createWriter();
    writer.write(table, new Record(1L, "abc\ndef\nghi", "www\nxyz", "吾輩は猫である", "夏目漱石").getValues());
    writer.write(table, new Record(2L, "abc\ndef", "xxx", "吾輩は猫ではない", "テスト").getValues());
    writer.commit();
    
    RlSearcher searcher = database.createSearcher(table);
    
    // ID=1
    {
      List<RlValues>list = searcher.search(new Match("id", 1L));
      assertEquals(1, list.size());
      RlValues values = list.get(0);
      assertEquals(1L, (long)values.get("id"));
      assertNull(values.get("match0"));
      assertNull(values.get("match1"));
      assertNull(values.get("token0"));
      assertNull(values.get("token1"));
    }

    // match0 = abc & def
    {
      List<RlValues>list = searcher.search(new Word("match0", "abc\ndef"));      
      assertEquals(2, list.size());
    }
    
    // match0 = abc & ghi
    {
      List<RlValues>list = searcher.search(new Word("match0", "abc\nghi"));      
      assertEquals(1, list.size());
    }
        
    // match0 = abc & match1 = xyz
    {
      List<RlValues>list = searcher.search(
          new And(new Word("match0", "abc"), new Word("match1", "xyz")));
      assertEquals(1, list.size());
      assertEquals(1L, (long)list.get(0).get("id"));      
    }    
    
    // "吾輩は猫" in token0
    {
      List<RlValues>list = searcher.search(
        new Word("token0", "吾輩は猫"));
      assertEquals(2, list.size());
    }
    
    // "吾輩は猫" in token0 & match0 = ghi
    {
      List<RlValues>list = searcher.search(new And(
        new Word("token0", "吾輩は猫"),
        new Word("match0", "ghi")
      ));
      assertEquals(1, list.size());
    }
    
    // "吾輩は猫" in token0 and "夏目" in token1
    {
      List<RlValues>list = searcher.search(new And(
          new Word("token0", "吾輩は猫"), 
          new Word("token1", "夏目")
      ));
      assertEquals(1, list.size());
    }
  }
  
  public static class Record {
    public Long id;
    public String match0;
    public String match1;
    public String token0;
    public String token1;
    
    public Record(Long id, String match0, String match1, String token0, String token1) {
      this.id = id;
      this.match0 = match0;
      this.match1 = match1;
      this.token0 = token0;
      this.token1 = token1;
    }
    
    public RlValues getValues() {
      RlValues values = new RlValues();
      values.put("id",  id);
      values.put("match0", match0);
      values.put("match1", match1);
      values.put("token0", token0);
      values.put("token1", token1);
      return values;
    }
  }
}
