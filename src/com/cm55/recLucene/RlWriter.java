package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

/**
 * インデックスライタは{@link RlDatabase}から取得され、{@link #close()}するまで使用することができる。
 * 内部で使用するLuceneの{@link IndexWriter}はスレッドセーフであるが、このオブジェクトはスレッドセーフではなく、
 * 単一のスレッドで使用することを前提としている。
 * また、一度に複数の{@link RlWriter}を取得することはできない。{@link #close()}されていない{@link RlWriter}が
 * 存在する場合は、{@link RlDatabase}の取得メソッドでブロックされる。
 * @author ysugimura
 */
public class RlWriter implements Closeable {

  /** テーブルセット */
  private RlTableSet tableSet;

  /** LuceneのIndexWriter */
  private IndexWriter indexWriter;

  /** セマフォ保持オブジェクト。クローズ時にリリースされる */
  private RlSemaphore.Ac acquisition;
  
  /** 初期化 */
  RlWriter(RlTableSet tableSet, IndexWriter indexWriter, RlSemaphore.Ac acquisition) {
    this.tableSet = tableSet;
    this.indexWriter = indexWriter;
    this.acquisition = acquisition;
  }

  private RlWriter write(Term pkTerm, Document doc) {
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
  public <T> RlWriter delete(RlField<T> field, T value) {
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
  public <T> RlWriter deleteAll(RlField<?> field) {
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
  public <T> RlWriter write(T rec) {
    Document doc = getLuceneDocument(rec);

    // プライマリキータームを作成する
    @SuppressWarnings("unchecked")
    RlClassTable<T> table = tableSet.getTable((Class<T>)rec.getClass());
    Term pkTerm = table.getPkTerm(rec);

    return write(pkTerm, doc);
  }

  /**
   * 自由形式の値を書き込む。テーブルを指定する必要がある。
   * @param table テーブル
   * @param values 値マップ
   */
  public void write(RlAnyTable table, RlValues values) {
    write(table.getPkTerm(values), getLuceneDocument(table, values));
  }

  /**
   * 指定フィールドが指定値のレコードを削除する
   * 
   * @param field フィールド名称
   * @param value 値
   */
  public <T> void delete(String fieldName, T value) {
    @SuppressWarnings("unchecked")
    RlField<T> field = (RlField<T>)tableSet.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません:" + fieldName);
    delete(field, value);
  }

  /**
   * 指定フィールドのあるすべてのレコードを削除する
   * @param field フィールド名称
   */
  public <T> void deleteAll(String fieldName) {
    RlField<?> field = tableSet.getFieldByName(fieldName);
    if (field == null)
      throw new RlException("フィールドがありません：" + fieldName);
    deleteAll(field);
  }

  /**
   * このインデックスデータベースのすべてのレコードを削除する
   */
  public <T> void deleteAll() {
    try {
      indexWriter.deleteAll();
    } catch (IOException ex) {
      throw new RlException.IO(ex);
    }
  }

  /**
   * クローズする
   */
  @Override
  public void close() {
    try {
      indexWriter.commit();    
      indexWriter = null;
    } catch (Exception ex) {}
      acquisition.release();
  }
  
  ////////////////////////////////////////////////////////
  
  <T> Document getLuceneDocument(T rec) {
    if (rec instanceof RlValues) {
      throw new RlException("RlValuesは使用できません");
    }

    // オブジェクトのクラスを取得する
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) rec.getClass();

    // クラスのマッピング情報を取得する
    RlClassTable<T> table = tableSet.getTable(clazz);
    if (table == null) {
      throw new RlException(clazz.getName() + "は登録されていません");
    }

    // ドキュメントを作成する
    return table.getDocument(rec);
  }

  /**
   * テーブルと値マップを指定してLuceneの{@link Document}を取得する
   * 
   * @param table テーブル
   * @param values 値マップ
   * @return Luceneドキュメント
   */
  Document getLuceneDocument(RlAnyTable table, RlValues values) {
    return table.getDocument(values);
  }
}
