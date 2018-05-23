package com.cm55.recLucene;

import java.io.*;
import java.util.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * LuceneのSearcherのラッパ
 * <p>
 * サーチャーには二種類ある。 データベースから取得できる通常のサーチャと ライタから取得できるニアリアル タイムサーチャである。 が、使い方はどちらも同じ。
 * </p>
 * <p>
 * Second Edition P156を参照のこと。
 * </p>
 * <p>
 * Lucene-Searcherのsearchメソッドのうち、search(Query, int)を使うと、スコアに
 * よるソートを行うが、search(Query, Filter, int Sort)をつかうと、スコアは使用
 * せずに指定されたソート順でソートする。このため、（スコアを計算せずに済む
 * という）パフォーマンス上の利得がある。Filterは使用しない場合はnullでよい。
 * </p>
 * 
 * 
 * @author ysugimura
 */
public abstract class RlSearcher implements Closeable {

  protected RlTable table;

  /** Luceneのインデックスサーチャ */
  private IndexSearcher indexSearcher;

  /** 最大出力結果数。初期値は実質無制限 */
  private int maxCount = Integer.MAX_VALUE / 2;

  /**
   * IndexReaderを取得する。下位クラスで実装する。
   * 
   * @return
   */
  protected abstract IndexReader getIndexReader();

  /** IndexReaderを強制的にクローズする。下位クラスで実装する。 */
  protected abstract void closeIndexReader();

  /** {@inheritDoc} */
  public RlTable getTable() {
    return table;
  }

  /**
   * LuceneのIndexSearcherを取得する。
   * <p>
   * 検索を行う都度このメソッドが呼ばれる。既に取得済みのIndexSearcherで 問題がなければそれが返されるが、問題があれば新たに作成される。
   * </p>
   * <p>
   * 問題のある場合とは、IndexReaderが再取得されるケースである。何らかの
   * 理由でIndexReaderが古くなった場合、getIndexReader()は新しいものを返す
   * ので、それが以前と異なる場合にはIndexSearcherは再生成される。
   * </p>
   * 
   * @return
   */
  private IndexSearcher getIndexSearcher() {

    // IndexReaderを取得する。
    IndexReader newIndexReader = getIndexReader();

    // IndexSearcherが取得済みで、そのIndexReaderが新しいものと異なる場合
    // このIndexSearcherを捨てる。
    if (indexSearcher != null && indexSearcher.getIndexReader() != newIndexReader) {
      /*
       * try { indexSearcher.close(); } catch (IOException ex) { }
       */
      indexSearcher = null;
    }

    // IndexSearcherが無ければ作成する。
    if (indexSearcher == null) {
      indexSearcher = new IndexSearcher(newIndexReader);
    }

    return indexSearcher;

  }

  /**
   * サーチャをリオープンする。
   * <p>
   * 通常のサーチャでは、ライタがインデックス書き込みを行い、さらにcommit/close を行った後でリオープンしないと書き込みが反映されない
   * （もちろんリオープンの代わりに、一度クローズして再作成してもよい）。
   * </p>
   */
  public synchronized void reopen() {
    closeIndexReader();
  }

  /** 検索結果最大数の取得 */
  public synchronized int getMaxCount() {
    return maxCount;
  }

  /** 検索結果最大数の設定 */
  public synchronized RlSearcher setMaxCount(int value) {
    maxCount = value;
    return this;
  }

  /**
   * 指定条件で検索を行い、結果をオブジェクトリストとして返す。
   * @param query
   *          クエリ
   * @return 検索結果レコードリスト
   */
  public synchronized <T> List<T> search(RlQuery query) {
    return search(query, new RlSortFields());
  }

  /**
   * 検索してプライマリキーセットを取得する
   */
  public <P, T> Set<P> searchPkSet(RlQuery query) {
    RlField field = table.getPkField();
    if (field == null)
      throw new RlException("プライマリキーフィールドがありません");
    return searchFieldSet(field, query);
  }

  public <P, T> Set<P> searchFieldSet(String fieldName, RlQuery query) {
    RlField field = table.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    return searchFieldSet(field, query);
  }

  <P, T> Set<P> searchFieldSet(RlField field, RlQuery query) {
    if (!field.isStore()) {
      throw new RlException("フィールド値にストア指定がありません：" + field.getName());
    }
    /*
     * List<T>objectList = search(query); Set<P>set = new HashSet<P>(); for (T
     * object: objectList) { set.add(field.getValue(object)); }
     */
    TopDocs hits = searchHits(query, null);
    Set<P> set = new HashSet<P>();
    IndexSearcher indexSearcher = getIndexSearcher();
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
  public synchronized <T> List<T> search(RlQuery query, RlSortFields sorts) {

    try {
      TopDocs hits = searchHits(query, sorts);

      List<T> result = new ArrayList<T>();
      for (ScoreDoc scoreDoc : hits.scoreDocs) {
        Document doc = getIndexSearcher().doc(scoreDoc.doc);
        result.add(table.fromDocument(doc));
      }

      return result;

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  @SuppressWarnings("deprecation")
  private synchronized TopDocs searchHits(RlQuery query, RlSortFields sorts) {
    try {
      TopDocs hits;
      Query luceneQuery = query.getLuceneQuery(table);
      if (sorts == null || sorts.rlSortFields.length == 0) {
        hits = getIndexSearcher().search(luceneQuery, maxCount);
      } else {
        hits = getIndexSearcher().search(luceneQuery, maxCount, sorts.getSort());
      }
      return hits;

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  public synchronized <T> List<T> getAllByPk() {
    RlField field = table.getPkField();
    if (field == null)
      throw new RlException("プライマリキーフィールドがありません");
    return getAllByField(field);
  }

  public <T> List<T> getAllByField(String fieldName) {
    RlField field = table.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    return getAllByField(field);
  }

  <T> List<T> getAllByField(RlField field) {
    if (field.isTokenized()) {
      throw new RlException("トークン化フィールドは指定できません");
    }
    try {
      Term pkWildTerm = new Term(field.getName(), "*");
      TopDocs hits = getIndexSearcher().search(new WildcardQuery(pkWildTerm), maxCount);
      return getObjects(hits);
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  public <T> List<T> getObjects(TopDocs hits) throws IOException {
    List<T> result = new ArrayList<T>();
    for (ScoreDoc scoreDoc : hits.scoreDocs) {
      Document doc = getIndexSearcher().doc(scoreDoc.doc);
      result.add(table.fromDocument(doc));
    }
    return result;
  }

  /** クローズする */
  public synchronized void close() {

    if (indexSearcher != null) {
      /*
       * このバージョンではクローズがない try { indexSearcher.close(); } catch (IOException ex)
       * { }
       */
      indexSearcher = null;
    }
    closeIndexReader();

  }

  private RlField getField(String fieldName) {
    RlField field = this.table.getFieldByName(fieldName);
    if (field == null)
      throw new RlException(fieldName + "というフィールドがありません");
    return field;
  }

}
