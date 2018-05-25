package com.cm55.recLucene.sample;

import java.util.*;
import java.util.stream.*;

import org.junit.*;
import static org.junit.Assert.*;

import com.cm55.recLucene.*;
import com.cm55.recLucene.RlAnalyzer.*;
import static com.cm55.recLucene.RlQuery.*;

public class SampleMain {

  static FooRecord[]recs0 = new FooRecord[] {
      new FooRecord(1L, "吾輩は猫である。名前はまだ無い。"),
      new FooRecord(2L, "どこで生れたかとんと見当がつかぬ。"),
      new FooRecord(3L, "何でも薄暗いじめじめした所でニャーニャー泣いていた事だけは記憶している。"),
      new FooRecord(4L, "吾輩はここで始めて人間というものを見た"),
      new FooRecord(5L, "しかもあとで聞くとそれは書生という人間中で一番獰悪な種族であったそうだ。")
  };
  static FooRecord[]recs1 = new FooRecord[] {
      new FooRecord(6L, "この書生というのは時々我々を捕えて煮て食うという話である。"),
      new FooRecord(7L, "しかしその当時は何という考もなかったから別段恐しいとも思わなかった。"),
      new FooRecord(8L, "ただ彼の掌に載せられてスーと持ち上げられた時何だかフワフワした感じがあったばかりである。"),
      new FooRecord(9L, "掌の上で少し落ちついて書生の顔を見たのがいわゆる人間というものの見始であろう。"),
      new FooRecord(10L, "この時妙なものだと思った感じが今でも残っている。第一毛をもって装飾されべきはずの顔がつるつるしてまるで薬缶だ。")
  };
  
  @Test
  public void test()  {

    // デフォルトの日本語アナライザを3gramにする
    RlDefaults.analyzerClass = JpnStandard3.class;

    // データベースを作成する
    RlDatabase db = new RlDatabase.Ram().add(FooRecord.class);
    RlWriter writer = db.createWriter();
    RlSearcher<FooRecord> searcher = db.createSearcher(FooRecord.class);
    
    // 前半の書き込み
    Arrays.stream(recs0).forEach(r->writer.write(r));
    
    // 検索
    checkIds(searcher, new Word("content", "人間"), 4L, 5L);
    checkIds(searcher, new Word("content", "人間　種族"), 5L);
    checkIds(searcher, new And(new Word("content", "人間"), new Word("content", "種族")), 5L);

    // 後半の書き込み
    Arrays.stream(recs1).forEach(r->writer.write(r));
    
    // 検索
    checkIds(searcher, new Word("content", "人間"), 4L, 5L, 9L);
    
    // 削除
    writer.delete("id", 5L);
    
    // 検索
    checkIds(searcher, new Word("content", "人間"), 4L, 9L);
    checkIds(searcher, new And(new Word("content", "人間"), new Not(new Word("content", "書生"))), 4L);
  }
  
  void checkIds(RlSearcher<FooRecord>searcher, RlQuery query, Long...ids) {
    Set<Long>set = searcher.searchPkSet(query);
    assertEquals(Arrays.stream(ids).collect(Collectors.toSet()), set);
  }
}
