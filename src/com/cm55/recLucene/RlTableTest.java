package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.junit.*;

public class RlTableTest {
  
  public static class Table1 {    
  }
  
  @Test
  public void test2() {
    try {
      new RlClassTable<Table2>(Table2.class);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().startsWith("プライマリキー指定が複数あります"));
    }
  }
  
  public static class Table2 {
    @RlFieldAttr(pk=true)
    public String id1;
    @RlFieldAttr(pk=true)
    public String id2;
  }
  
  @Test
  public void test3() {
    try {
      new RlClassTable<Table3>(Table3.class);
      fail();
    } catch (Exception ex) {
      assertTrue(ex.getMessage().startsWith("フィールドがString以外の場合には"));
    }
  }
  
  public static class Table3 {
    @RlFieldAttr(pk=true)
    public String id;
    
    public int i;
  }
 
  @Test
  public void test4() {
    RlClassTable<Table4> table = new RlClassTable<>(Table4.class);
    
    // フィールドの数と名称
    assertEquals(new HashSet<String>() {{
      add("id");
      add("fld1");
    }}, table.getFieldNames());


    // 各フィールド
    RlField idField = table.getFieldByName("id");
    assertTrue(idField.isPk());
    assertEquals("id", idField.getName());
    
    RlField fld1Field = table.getFieldByName("fld1");
    assertFalse(fld1Field.isPk());
    assertEquals("fld1", fld1Field.getName());

    
    // オブジェクトからドキュメントへ変換
    Table4 object = new Table4();
    object.id = "ID";
    object.fld1 = "FLD1";
    Document doc = table.getDocument(object);
    assertEquals(2, doc.getFields().size());
    assertEquals("ID", doc.get("id"));
    assertEquals("FLD1", doc.get("fld1"));
    
    IndexableField f1 = doc.getField("id");
    assertEquals("stored,indexed,omitNorms,indexOptions=DOCS<id:ID>", f1.toString());
    //ystem.out.println("" + f1);
    
    IndexableField f2 = doc.getField("fld1");   
    assertEquals("indexed,tokenized<fld1:FLD1>", f2.toString());
    //ystem.out.println("" + f2);
  }
  
  @Test
  public void test5() {
    
    Document doc = new Document();
    
    doc.add(new StringField("id", "ID", Field.Store.YES));
    doc.add(new TextField("fld3", "FLD2", Field.Store.NO));
    
    RlClassTable<Table4> table = new RlClassTable<>(Table4.class);
    Table4 object = table.fromDocument(doc);
    
    assertEquals("id:ID,fld1:null,fld2:null", object.toString());
  }
  
  public static class Table4 {
    @RlFieldAttr(pk=true)
    public String id;
    
    public String fld1;
    
    public transient String fld2;
    
    @Override
    public String toString() {
      return "id:" + id + ",fld1:" + fld1 + ",fld2:" + fld2;
    }
  }
  
  @Test
  public void test6() {
    RlField aField, bField;
    RlAnyTable table = new RlAnyTable(
      aField = new RlField("a", new RlFieldAttr.Default() {
        @Override
        public boolean pk() {
          return true;
        }
        @Override
        public Class<? extends RlFieldConverter<?>> converter() {
          return RlFieldConverter.IntConv.class;
        }
      }),
      bField = new RlField("b", null)
    );
    
    RlValues values = new RlValues();
    values.put("a",  123);
    values.put("b",  "abc");
    
    Document document = table.getDocument(values);
    assertEquals("123", document.get("a")); // 文字列に変換されていることに注意
    assertEquals("abc", document.get("b"));
    
    RlValues dup = table.fromDocument(document);
    assertEquals(123, (int)dup.get("a"));
    assertEquals("abc", dup.get("b"));
   
    Term term = table.getPkTerm(values);
    assertEquals("a:123", term.toString());
    
    assertSame(aField, table.getFieldByName("a"));
    assertSame(bField, table.getFieldByName("b"));
    
    assertSame(aField, table.getPkField());
    
    assertEquals(new HashSet<String>() {{
      add("a"); add("b");
    }}, table.getFieldNames());
    
   
  }
}

