package com.cm55.recLucene.sample;

import java.util.*;

import com.cm55.recLucene.*;

public class SampleMain {

  static FooRecord[]recs = new FooRecord[] {
      new FooRecord(1L, "吾輩は猫である。名前はまだ無い。"),
      new FooRecord(2L, "どこで生れたかとんと見当がつかぬ。"),
      new FooRecord(3L, "何でも薄暗いじめじめした所でニャーニャー泣いていた事だけは記憶している。"),
      new FooRecord(4L, "吾輩はここで始めて人間というものを見た"),
      new FooRecord(5L, "しかもあとで聞くとそれは書生という人間中で一番獰悪な種族であったそうだ。")
  };
  
  
  public static void main(String[]args) {
    RlDatabase db = RlDatabase.createDir("sampleDb").add(FooRecord.class);
    
    db.reset();
    
    {
      RlWriter writer = db.createWriter();
      Arrays.stream(recs).forEach(r->writer.write(r));
      writer.close();
    }
    
    RlSearcher searcher = db.createSearcher(FooRecord.class);
    {
      List<FooRecord>list = searcher.search(new RlQuery.Word("content", "人間"));
      System.out.println("first");
      list.stream().forEach(System.out::println);
    }
    {
      List<FooRecord>list = searcher.search(
          new RlQuery.And(new RlQuery.Word("content", "人間"), new RlQuery.Word("content",  "種族")));
      System.out.println("second");
      list.stream().forEach(System.out::println); 
    }
    searcher.close();
    
    {
      RlWriter writer = db.createWriter();
      writer.write(new FooRecord(5L, "この書生というのは時々我々を捕えて煮て食うという話である。"));      
      writer.close();
    }
    
    searcher = db.createSearcher(FooRecord.class);    
    {
      List<FooRecord>list = searcher.search(new RlQuery.Word("content", "人間"));
      System.out.println("first");
      list.stream().forEach(System.out::println);
    }
    searcher.close();
  }

}
