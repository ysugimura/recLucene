package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;

/**
 * テーブルの定義
 * @author ysugimura
 *
 * @param <T>　このテーブルのレコードオブジェクトのタイプ
 */
public interface RlTable<T> {
  
  /** 
   * プライマリキーフィールドを取得する。存在しない場合はnull
   * @return プライマリキーフィールド定義
   */
  public RlField<?> getPkField();
  
  /** 
   * このテーブル中のすべてのフィールド名称を取得する
   * @return　すべてのフィールド名称集合
   */
  public Set<String> getFieldNames();
  
  /**
   * 指定名称のフィールドを取得する。存在しない場合はnull
   * @param fieldName　フィールド名称
   * @return フィールド定義
   */
  public RlField<?> getFieldByName(String fieldName);
  
  /** 
   * 全フィールドのストリームを取得する
   * @return 全フィールドのストリーム
   */
  public Stream<RlField<?>> getFields();
  
  /**
   * Luceneのドキュメントオブジェクトを、このテーブルのレコードオブジェクトに変換する
   * @param doc　Luceneのドキュメントオブジェクト
   * @return このテーブルのレコードオブジェクト
   */
  public T fromDocument(Document doc);

  /**
   * このテーブルのレコードオブジェクトをLuceneのドキュメントオブジェクトに変換する
   * @param record このテーブルのレコードオブジェクト
   * @return Luceneのドキュメントオブジェクト
   */
  public Document getDocument(T record);
  
  /**
   * このテーブルのすべてのトークン化されｒフィールド名称と、そのフィールドアナライザのマップを取得する。
   * @return マップ
   */
  public Stream<Map.Entry<String, Analyzer>>getFieldAnalyzers();
  
  /** デバッグ用のテーブル名称。クラステーブルの場合はクラス名称 */
  public String getTableName();
}
