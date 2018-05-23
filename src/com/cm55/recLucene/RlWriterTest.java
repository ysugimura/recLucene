package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class RlWriterTest {

  RlDatabase database;
  
  @Before
  public void before() {


    database = RlDatabase.createRam(Foo.class, Bar.class, FooBar.class);
  }
  
  @Test
  public void writerの重複稼働() {
    
    // ライタを作成する
    RlWriter writer = database.createWriter();
    
    // ライタが稼働中に、他のライタを作成することはできない。

      assertNull(database.tryCreateWriter());

    writer.close();
    
    // 前のライタをクローズした後に作成することが可能
    assertNotNull(database.tryCreateWriter());
  }

  //@Test
  public void writerの機能() {
    
    // 重複して同じプライマリキーのデータを書き込んでも一つしか書き込まれない
    RlWriter writer = database.createWriter();
    writer.write(new Foo("1", "test1"));
    writer.write(new Foo("1", "test1"));
    writer.write(new Bar("1", "sample1"));    
    writer.commit();
    
    // ライタをクローズ。あるいはコミットしてからデータベースサーチャを作成
    // その際、どのテーブルをサーチするのか指定する。
    // テーブル間のjoin 等の機能はない。
    RlSearcher searcher = database.createSearcher(Foo.class);
    
    // 全レコードを取得
    List<Foo>list = searcher.getAllByPk();
    assertEquals(1, list.size());
    assertEquals(new Foo("1", null), list.get(0));
    
    // もう一つレコードを書き込みしてコミット
    writer.write(new Foo("2", "test2"));
    writer.commit();

    // しかし探せるのは一つだけ
    assertEquals(1, searcher.getAllByPk().size());
    
    // 以前のものを削除し、新たなサーチャを佐作成する
    searcher.close();
    searcher = database.createSearcher(Foo.class);
    
    // 結局のところ、ライタをコミット（あるいはクローズ）した後に作成したサーチャでしか探せない
    assertEquals(2, searcher.getAllByPk().size());
  }
  
  public static class Foo {
    @RlFieldAttr(pk=true)
    public String id1;
    
    public String testField;
    
    @Override
    public String toString() {
      return id1 + "," + testField;      
    }
    
    public Foo() {}
    public Foo(String id1, String testField) {
      this.id1 = id1;
      this.testField = testField;
    }
    
    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Foo)) return false;
      Foo that = (Foo)o;
      return Misc.equals(this.id1, that.id1)&&
          Misc.equals(this.testField, that.testField);
    }
  }
  
  public static class Bar {
    @RlFieldAttr(pk=true)
    public String id2;
    
    public String longField;
    
    public Bar() {}
    public Bar(String id2, String longField) {
      this.id2 = id2;
      this.longField = longField;
    }
  }

  
  @Test
  public void PK無しテスト() {
    RlWriter writer = database.createWriter();
    
    writer.write(new FooBar(100, "test1 sample"));
    writer.write(new FooBar(100, "sample fooBar"));
    writer.write(new FooBar(222, "test"));
    
    writer.commit();
    
    RlSearcher s = database.createSearcher(FooBar.class);

    assertEquals(3, s.getAllByField("id3").size());
    
    {
      List<FooBar>list = s.search(new RlQuery.Word("value",  "sample"));
      assertEquals(2, list.size());
      assertEquals(new FooBar(100, null), list.get(0));
      assertEquals(new FooBar(100, null), list.get(1));
    }
    
    {
      Set<Long>set = s.searchFieldSet("id3",  new RlQuery.Word("value",  "sample"));
      assertEquals(new HashSet<Long>() {{
        add(100L);
      }}, set);
    }
    
    writer.delete("id3", 100L);
    writer.commit();
    
    s = database.createSearcher(FooBar.class);
    
    assertEquals(1, s.getAllByField("id3").size());

    writer.deleteAll("id3");
    writer.commit();
    
    s = database.createSearcher(FooBar.class);
    assertEquals(0, s.getAllByField("id3").size());
  }
  
  public static class FooBar {
    @RlFieldAttr(tokenized=false, store=true, converter=RlFieldConverter.LongConv.class)
    public long id3;
    
    public String value;
    
    public FooBar() {}
    public FooBar(long id3, String value) {
      this.id3 = id3;
      this.value = value;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof FooBar)) return false;
      FooBar that = (FooBar)o;
      return 
        this.id3 == that.id3 &&
        Misc.equals(this.value, that.value);
    }
    
    @Override
    public String toString() {
      return id3 + "," + value;
    }
  }
  
  @Test
  public void deleteAllのテスト() {
    RlWriter writer = database.createWriter();
    writer.write(new Foo("1", "test1"));
    writer.write(new Foo("2", "test2"));
    writer.write(new Bar("1", "sample1"));    
    writer.write(new FooBar(1, "foobar1"));
    writer.write(new FooBar(1, "foobar2"));
    writer.write(new FooBar(1, "foobar3"));
    writer.commit();
 
    RlSearcher fooSearcher;
    RlSearcher barSearcher;
    RlSearcher fooBarSearcher;
    
    
    fooSearcher =  this.database.createSearcher(Foo.class);
    assertEquals(2, fooSearcher.getAllByPk().size());
    fooSearcher.close();
    
    barSearcher =  this.database.createSearcher(Bar.class);
    assertEquals(1, barSearcher.getAllByPk().size());
    fooSearcher.close();
    
    fooBarSearcher =  this.database.createSearcher(FooBar.class);
    try {
      assertEquals(3, fooBarSearcher.getAllByPk().size());
      fail();
    } catch (Exception ex) {}
    assertEquals(3, fooBarSearcher.getAllByField("id3").size());
    fooSearcher.close();
    
    writer.deleteAll("id1");
    writer.commit();
    
    fooSearcher =  this.database.createSearcher(Foo.class);
    assertEquals(0, fooSearcher.getAllByPk().size());
    fooSearcher.close();
    
    barSearcher =  this.database.createSearcher(Bar.class);
    assertEquals(1, barSearcher.getAllByPk().size());
    fooSearcher.close();
    
    fooBarSearcher =  this.database.createSearcher(FooBar.class);
    assertEquals(3, fooBarSearcher.getAllByField("id3").size());
    fooSearcher.close();
    
    writer.deleteAll("id2");
    writer.commit();
    
    fooSearcher =  this.database.createSearcher(Foo.class);
    assertEquals(0, fooSearcher.getAllByPk().size());
    fooSearcher.close();
    
    barSearcher =  this.database.createSearcher(Bar.class);
    assertEquals(0, barSearcher.getAllByPk().size());
    fooSearcher.close();
    
    fooBarSearcher =  this.database.createSearcher(FooBar.class);
    assertEquals(3, fooBarSearcher.getAllByField("id3").size());
    fooSearcher.close();
    
    writer.deleteAll();
    writer.commit();
    
    fooSearcher =  this.database.createSearcher(Foo.class);
    assertEquals(0, fooSearcher.getAllByPk().size());
    fooSearcher.close();
    
    barSearcher =  this.database.createSearcher(Bar.class);
    assertEquals(0, barSearcher.getAllByPk().size());
    fooSearcher.close();
    
    fooBarSearcher =  this.database.createSearcher(FooBar.class);
    assertEquals(0, fooBarSearcher.getAllByField("id3").size());
    fooSearcher.close();
  }
}
