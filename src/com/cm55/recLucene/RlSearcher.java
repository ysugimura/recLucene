package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * 
 * @author ysugimura
 *
 * @param <T> 検索対象のテーブルオブジェクトの型
 */
public class RlSearcher<T> implements Closeable {

  /** 対象とするテーブル */
  protected RlTable<T> table;

  SearcherManager searcherManager;
  
  /** Luceneのインデックスサーチャ */
  private IndexSearcher indexSearcher;

  /** 最大出力結果数。初期値は実質無制限 */
  private int maxCount = Integer.MAX_VALUE / 2;

  SemaphoreHandler.Acquisition ac;
  /**
   * 
   * このサーチャー対象とするテーブルを指定する
   */
  protected RlSearcher(RlTable<T>table, SearcherManager searcherManager, SemaphoreHandler.Acquisition ac) {
    this.table = table;
    
    this.searcherManager = searcherManager;

    this.ac = ac;
  }

  void open() {
    closeSearcher();
    try {
      searcherManager.maybeRefreshBlocking();
      indexSearcher = searcherManager.acquire();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }    
  }
  
  /** 対象とするテーブルを取得する */
  public RlTable<T> getTable() {
    return table;
  }

  /** 検索結果最大数の取得 */
  public synchronized int getMaxCount() {
    return maxCount;
  }

  /** 検索結果最大数の設定 */
  public synchronized RlSearcher<T> setMaxCount(int value) {
    maxCount = value;
    return this;
  }

  
  void closeSearcher() {
    if (indexSearcher == null) return;
    try {
    searcherManager.release(indexSearcher);
    indexSearcher = null;
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
  /** クローズする */
  public synchronized void close() {
    closeSearcher();
    ac.release();
  }

  /////////////////////////////////////////////////////////////////
  
  /**
   * 指定条件で検索を行い、結果をオブジェクトリストとして返す。
   * @param query クエリ
   * @return 検索結果オブジェクトリスト
   */
  public synchronized List<T> search(RlQuery query) {
    return search(query, new RlSortFields());
  }

  /**
   * 検索してプライマリキーセットを取得する
   */
  public <P> Set<P> searchPkSet(RlQuery query) {
    @SuppressWarnings("unchecked")
    RlField<P> field = (RlField<P>)table.getPkField();
    if (field == null)
      throw new RlException("プライマリキーフィールドがありません");
    return searchFieldSet(field, query);
  }

  public <P> Set<P> searchFieldSet(String fieldName, RlQuery query) {
    @SuppressWarnings("unchecked")
    RlField<P> field = (RlField<P>)table.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    return searchFieldSet(field, query);
  }

  private <P> Set<P> searchFieldSet(RlField<P> field, RlQuery query) {
    if (!field.isStore()) {
      throw new RlException("フィールド値にストア指定がありません：" + field.getName());
    }
    /*
     * List<T>objectList = search(query); Set<P>set = new HashSet<P>(); for (T
     * object: objectList) { set.add(field.getValue(object)); }
     */
    TopDocs hits = searchHits(query, null);
    Set<P> set = new HashSet<P>();

    try {
      for (ScoreDoc scoreDoc : hits.scoreDocs) {
        Document doc = indexSearcher.doc(scoreDoc.doc);
        // result.add(table.fromDocument(doc));
        String string = doc.get(field.getName());
        set.add(field.fromString(string));
      }
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }

    return set;
  }

  /** 検索する。ソート指定あり */
  public synchronized List<T> search(RlQuery query, RlSortFields sorts) {
    try {
      TopDocs hits = searchHits(query, sorts);
      List<T> result = new ArrayList<T>();
      for (ScoreDoc scoreDoc : hits.scoreDocs) {
        Document doc = indexSearcher.doc(scoreDoc.doc);
        result.add(table.fromDocument(doc));
      }
      return result;
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  private synchronized TopDocs searchHits(RlQuery query, RlSortFields sorts) {
    try {
      TopDocs hits;
      Query luceneQuery = query.getLuceneQuery(table);
      open();
      if (sorts == null || sorts.rlSortFields.length == 0) {
        hits = indexSearcher.search(luceneQuery, maxCount);        
      } else {
        hits = indexSearcher.search(luceneQuery, maxCount, sorts.getSort());
      }
      return hits;

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  public synchronized List<T> getAllByPk() {
    RlField<?> field = table.getPkField();
    if (field == null)
      throw new RlException("プライマリキーフィールドがありません");
    return getAllByField(field);
  }

  public List<T> getAllByField(String fieldName) {
    RlField<?> field = table.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    return getAllByField(field);
  }

  List<T> getAllByField(RlField<?> field) {
    if (field.isTokenized()) {
      throw new RlException("トークン化フィールドは指定できません");
    }
    try {
      Term pkWildTerm = new Term(field.getName(), "*");
      open();
      TopDocs hits = indexSearcher.search(new WildcardQuery(pkWildTerm), maxCount);
      return getObjects(hits);
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  public List<T> getObjects(TopDocs hits) throws IOException {
    List<T> result = new ArrayList<T>();
    for (ScoreDoc scoreDoc : hits.scoreDocs) {
      Document doc = indexSearcher.doc(scoreDoc.doc);
      result.add(table.fromDocument(doc));
    }
    return result;
  }

}
