package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * <h1>インデックスライタ</h1>
 * 
 * <h2>インデックスライタのロック</h2>
 * <p>
 * 一つLuceneインデックス（このパッケージではRlDatabase）については、
 * ただ一つのライタがオープンしうる。luceneはこれを保証するため、ロック
 * ファイルを用いている。RlWriter内でIndexWriterが生成されるとすぐに書き込み ロックが取得される。
 * </p>
 * <p>
 * つまり、他のRlWriterを作成しても前のRlWriterがクローズされるまでは使用 することはできない。
 * </p>
 * <h2>セグメント数とマージ</h2>
 * <p>
 * ライタがコミットやクローズを行うと、その度にインデックスセグメントは増加して いく。これがあまりに大量になると検索スピードに影響を及ぼす。
 * Luceneでは自動的にこの数を減らす方策が二つ用意されている。一つはmergeであり、
 * もう一つはoptimizeである。optimizeは基本的に、検索操作をしない夜間等に行う ものらしい。以下ではmergeについて説明する。
 * </p>
 * <p>
 * mergeとは複数のセグメントを束ね、単一のセグメントにすることである。これに より、検索の際にオープンしなければならないセグメント数が減り、インデックス
 * それ自体のサイズも現象する。これをいつどのようにしておこなうかはMergePolicy
 * によって決定されるが、デフォルトはLogByteSizeMergePolicyになっている。
 * </p>
 * <p>
 * 参考としては以下。
 * </p>
 * <ul>
 * <li>http://ameblo.jp/principia-ca/entry-10892036569.html
 * </ul>
 * 
 * <h2>バグ</h2> ※lucene3.3.0でのバグに注意：インデックスデータベースの作成時には、一度でも
 * commit（何も書きこむものがなくてもよい）しておかないと、インデックスファイル 構造が作成されず、リアルタイムサーチャが失敗してしまう。
 * 
 * @author ysugimura
 */
public class RlWriter implements Closeable {

  /** データベース */
  private RlDatabase database;

  /**
   * LuceneのIndexWriter。初期化時に作成される。クローズ時にnullが代入される。
   */
  private IndexWriter indexWriter;

  /** LuceneのIndexWriterConfig */
  private IndexWriterConfig config;

  private SemaphoreHandler.Acquisition acquisition;
  
  /** 初期化 */
  public RlWriter(RlDatabase database, IndexWriter indexWriter, SemaphoreHandler.Acquisition acquisition) {

    this.database = database;
    this.indexWriter = indexWriter;
    this.acquisition = acquisition;
  }

  /**
   * 内部的なIndexWriterを取得する
   */
  IndexReader getIndexReader() {
    try {        
      return DirectoryReader.open(indexWriter, true, true);
     } catch (IOException ex) { throw new RlException.IO(ex); }    
  }

  private synchronized RlWriter write(Term pkTerm, Document doc) {
    // 書込み
    try {
      if (pkTerm == null) {
        indexWriter.addDocument(doc);
      } else {
        indexWriter.updateDocument(pkTerm, doc);
      }

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }

    return this;
  }

  /**
   * 指定レコードを削除する。
   * <p>
   * フィールドとその値を指定する。 フィールドはtokenized=falseでなければいけない。
   * ※Luceneでは、一つのデータベース中においてフィールド名がユニークであることに注意。
   * このため、この操作は結局のところ、本システムで言うところの「一つのテーブルのあるフィールドが特定の値のレコードすべて」を削除することになる。
   * </p>
   */
  public synchronized <T> RlWriter delete(RlField<T> field, T value) {
    if (field.isTokenized()) {
      throw new RlException("tokenized=trueのフィールドを指定して削除はできません");
    }
    try {
      String string = field.toString(value);
      indexWriter.deleteDocuments(new Term(field.getName(), string));

      return this;
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  /**
   * 指定フィールドが存在するレコードをすべて削除する。
   * ※Luceneでは、一つのデータベース中においてフィールド名がユニークであることに注意。
   * このため、この操作は結局のところ、本システムで言うところの「一つのテーブルのレコードすべて」を削除することになる。
   * @param field
   * @return
   */
  public synchronized <T> RlWriter deleteAll(RlField<?> field) {
    try {
      Term term = new Term(field.getName(), "*");
      Query query = new WildcardQuery(term);
      indexWriter.deleteDocuments(query);

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
    return this;
  }
  
  /**
   * レコードの内容を書き込む
   * <p>
   * プライマリキーが必ず指定されているはずなので、データベースにキーが なければ挿入し、あれば更新する。
   * </p>
   * 
   * @param record
   *          書き込みレコード
   * @return このインデックスライタ
   */
  public synchronized <T> RlWriter write(T rec) {

    Document doc = getLuceneDocument(rec);

    // プライマリキータームを作成する
    @SuppressWarnings("unchecked")
    RlClassTable<T> table = database.getTableSet().getTable((Class<T>)rec.getClass());
    Term pkTerm = table.getPkTerm(rec);

    return write(pkTerm, doc);
  }

  /**
   * 自由形式の値を書き込む。テーブルを指定する必要がある。
   * 
   * @param table
   *          テーブル
   * @param values
   *          値マップ
   * @return このインデックスライタ
   */
  public synchronized RlWriter write(RlAnyTable table, RlValues values) {
    return write(table.getPkTerm(values), getLuceneDocument(table, values));
  }

  public <T> Document getLuceneDocument(T rec) {

    if (rec instanceof RlValues) {
      throw new RlException("RlValuesは使用できません");
    }

    // オブジェクトのクラスを取得する
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) rec.getClass();

    // クラスのマッピング情報を取得する
    RlClassTable<T> table = database.getTableSet().getTable(clazz);
    if (table == null) {
      throw new RlException(clazz.getName() + "は登録されていません");
    }

    // ドキュメントを作成する
    return table.getDocument(rec);
  }

  /**
   * テーブルと値マップを指定してLuceneの{@link Document}を取得する
   * 
   * @param table
   *          テーブル
   * @param values
   *          値マップ
   * @return Luceneドキュメント
   */
  public Document getLuceneDocument(RlAnyTable table, RlValues values) {
    return table.getDocument(values);
  }

  /**
   * 指定フィールドが指定値のレコードを削除する
   * 
   * @param field
   *          フィールド名称
   * @param value
   *          値
   * @return
   */
  public <T> RlWriter delete(String fieldName, T value) {
    @SuppressWarnings("unchecked")
    RlField<T> field = (RlField<T>)database.getTableSet().getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません:" + fieldName);
    return delete(field, value);
  }

  /**
   * 指定フィールドのあるすべてのレコードを削除する
   * 
   * @param field
   *          フィールド名称
   * @return
   */
  public synchronized <T> RlWriter deleteAll(String fieldName) {
    RlField<?> field = database.getTableSet().getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    return deleteAll(field);
  }

  /**
   * このインデックスデータベースのすべてのレコードを削除する
   */
  public synchronized <T> RlWriter deleteAll() {
    try {
      indexWriter.deleteAll();

    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
    return this;
  }

  /**
   * コミットする。
   * <p>
   * これまでの書き込みをフラッシュする。通常のサーチャ（このライタから 取得したサーチャではなくデータベースから取得したもの）は、コミット後に
   * リオープンすることで、フラッシュされたデータを検索することができる。
   * </p>
   */
  public synchronized RlWriter commit() {
    try {
      indexWriter.commit();
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
    return this;
  }

  /**
   * クローズする。コミットされていなければ自動的にコミットする。
   * <p>
   * 書き込みをコミットし、ライタを終了する。 
   * 通常のデータベースシステムとは異なり、ロールバック操作は無いため、 書き込みをキャンセルする方法はない。
   * </p>
   */
  @Override
  public synchronized void close() {
      acquisition.release();
  }

  /**
   * 内部的なIndexWriterConfigを取得する
   */
  @SuppressWarnings("unchecked")
  public <T> T getIndexWriterConfig() {
    return (T) config;
  }

  /** クローズされたか */
  public synchronized boolean isClosed() {
    return indexWriter == null;
  }


}
