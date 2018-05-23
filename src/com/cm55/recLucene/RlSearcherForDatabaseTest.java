package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

public class RlSearcherForDatabaseTest {


  RlDatabase database;
  
  @Before
  public void before() {
    RlTableSet tableSet;
    tableSet = new RlTableSet(Foo.class, Bar.class);
    database = RlDatabase.createRam(tableSet);
  }
  
  @Test
  public void writerの重複稼働() {
    
    // ライタを作成する
    RlWriter writer = database.createWriter();
    
    // ライタが稼働中に、他のライタを作成することはできない。
    
      assertNull(database.tryCreateWriter());

    
    writer.close();
    
    // 前のライタをクローズした後に作成することが可能
    database.createWriter();
  }

  @Test
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
}
