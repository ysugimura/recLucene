package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.cm55.recLucene.RlFieldConverter.*;

public class RlSearcherTest {

  
  @Test
  public void test() {
    RlTableSet tableSet = new RlTableSet(BookData.class);
    RlDatabase database = RlDatabase.createRam(tableSet);
    
    RlWriter writer = database.createWriter();
    for (BookData bookData: BOOK_DATA) {
      writer.write(bookData);
    }
    writer.close();
    
    RlSearcher searcher = database.createSearcher(BookData.class);
    
    // プライマリキーでの検索
    {
      Set<Long>pkSet = searcher.searchPkSet(new RlQuery.Match("id",  1L));
      assertEquals(new HashSet<Long>() {{
        add(1L);
      }}, pkSet);
    }
    
    // 説明での検索
    {
      Set<Long>pkSet = searcher.searchPkSet(new RlQuery.Word("desc", "wiki pedia"));
      assertEquals(new HashSet<Long>() {{
        add(2L);
      }}, pkSet);
    }
    
    // ページ数での検索 
    {
      Set<Long>pkSet = searcher.searchPkSet(new RlQuery.Range("pages",  100,  500));
      assertEquals(new HashSet<Long>() {{
        add(1L);
        add(2L);
      }}, pkSet);
    }
  }

  
  public static final BookData[] BOOK_DATA = new BookData[] {
      new BookData(1, "夏目漱石", "吾輩は猫である",
          "「吾輩は猫である。名前はまだない。どこで生れたかとんと見当がつかぬ。」から始まる有名な小説", 344),
      new BookData(2, "森鴎外", "舞姫",
          "『舞姫』（まいひめ）は、森鴎外の短編小説。1890年（明治23年）、「国民之友」に発表。(Wikipediaより)", 452),
      new BookData(3, "鈴木一郎", "「吾輩は猫である」の研究", "夏目漱石「吾輩は猫である」の研究", 30),
      new BookData(4, "小林太郎", "「吾輩は猫である」の感想文", "夏目漱石「吾輩は猫である」の感想文", 555),
      new BookData(5, "鈴木一郎", "舞姫は吾輩", "舞姫は実は吾輩であった", 698), };


  public static class IdConverter extends RlFieldConverter.Abstract<Long> {
    public IdConverter() {
      super(Long.class);
    }
    
    public String toString(Long value) {
      return "" + value;
    }
    public Long fromString(String string) {
      return Long.parseLong(string);
    }    
  }

  public static class PagesConverter extends RlFieldConverter.Abstract<Integer> {
    public PagesConverter() {
      super(Integer.class);
    }
    
    public String toString(Integer value) {
      return String.format("%010d", value);
    }
    public Integer fromString(String string) {
      return Integer.parseInt(string);
    }    
  }
  
  public static class BookData {

    public static final String ID = "id";
    public static final String AUTHOR = "author";
    public static final String TITLE = "title";
    public static final String DESC = "desc";

    @RlFieldAttr(pk= true, converter=IdConverter.class)
    public long id;
    
    public String author;
    public String title;
    public String desc;

    @RlFieldAttr(tokenized=false, converter=PagesConverter.class)
    public int pages;

    @RlFieldAttr(tokenized=false, converter=LongConv.class)
    public Long a;
    
    public BookData() {}
    public BookData(long id, String author, String title, String desc, int pages) {
      this.id = id;
      this.author = author;
      this.title = title;
      this.desc = desc;
      this.pages = pages;
    }

    @Override
    public String toString() {
      return id + "," + author + "," + title + "," + desc;
    }

  }

}
