package com.cm55.recLucene;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.cm55.recLucene.RlAnalyzer.*;

public class RlAnalyzerTest {
  @Test
  public void jpnStandardのテスト() {
    RlAnalyzer analyzer = new JpnStandard2();    
    
    String[]expanded = analyzer.expandString(
      "ｺﾚﾊ 日本語　漢字カナ混じり\n" +
      "this SHOLDn't REAL!"
    );
    //Arrays.stream(expanded).forEach(System.out::println);
    assertArrayEquals(new String[] {
        "こ","これ","れ","れは","は","日","日本","本","本語","語",
        "漢","漢字","字","字か","か","かな","な","な混","混","混じ",
        "じ","じり","り","Ｔ","ＴＨ","Ｈ","ＨＩ","Ｉ","ＩＳ","Ｓ","Ｓ","ＳＨ",
        "Ｈ","ＨＯ","Ｏ","ＯＬ","Ｌ","ＬＤ","Ｄ","ＤＮ","Ｎ","Ｎ＇","＇",
        "＇Ｔ","Ｔ","Ｒ","ＲＥ","Ｅ","ＥＡ","Ａ","ＡＬ","Ｌ","Ｌ！","！",
    }, expanded);
  }
  
  @Test
  public void newlinesのテスト() {
    RlAnalyzer analyzer = new Newlines();
    String[]expanded = analyzer.expandString(
      "1テスト2\n" +
    "サンプルｶﾀｶﾅ");
    assertArrayEquals(new String[] {
        "1テスト2",
       "サンプルｶﾀｶﾅ" 
    }, expanded);
    /*
    for (String s: expanded) {
      //ystem.out.println("" + s);
    }
    */
  }
  
  @Test
  public void 変更newlinesのテスト() {
    RlAnalyzer analyzer = new LowercaseNewlines();
    String[]expanded = analyzer.expandString(
      "ABC@cm55.COM\n" +
    "SAMPLE@AaA.com");
    assertArrayEquals(new String[] {
        "abc@cm55.com",
       "sample@aaa.com" 
    }, expanded);
  }
  
  public static class LowercaseNewlines extends AbstractNewlines {
    @Override
    public int normalize(int c) {
      return Character.toLowerCase(c);
    }
  }

}
