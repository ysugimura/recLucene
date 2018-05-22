package com.cm55.recLucene;

import java.io.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import com.google.inject.*;

/**
 * <h1>インデックスライタ</h1>
 * 
 * <h2>インデックスライタのロック</h2>
 * <p>
 * 一つLuceneインデックス（このパッケージではLxDatabase）については、
 * ただ一つのライタがオープンしうる。luceneはこれを保証するため、ロック
 * ファイルを用いている。LxWriter内でIndexWriterが生成されるとすぐに書き込み ロックが取得される。
 * </p>
 * <p>
 * つまり、他のLxWriterを作成しても前のLxWriterがクローズされるまでは使用 することはできない。
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
@ImplementedBy(RlWriter.Impl.class)
public interface RlWriter {

  /**
   * {@link RlWriter}のファクトリ
   * @author ysugimura
   */
  @Singleton
  public static class Factory {
    @Inject private Provider<Impl>provider;
    
    /**
     * データベースを指定してそのライタを作成する
     * <p>
     * 現在のところ、既にオープン中のライタが存在する場合には、必ずエラーが発生する。
     * つまり、一つのデータベースについては、たとえ{@link RlDatabase}を新たに作成したとしても
     * {@link RlWriter}はただ一つしか稼働できない。
     * </p>
     * @param database
     * @return
     */
    public RlWriter create(RlDatabase database) {
      return provider.get().setup(database);
    }
  }
  
  /**
   * レコードの内容を書き込む
   * <p>
   * プライマリキーが必ず指定されているはずなので、データベースにキーが なければ挿入し、あれば更新する。
   * </p>
   * 
   * @param record 書き込みレコード
   * @return このインデックスライタ
   */
  public <T>RlWriter write(T o);

  /**
   * 自由形式の値を書き込む。テーブルを指定する必要がある。
   * @param table テーブル
   * @param values 値マップ
   * @return このインデックスライタ
   */
  public RlWriter write(RlTable table, RlValues values);
  
  public <T>Document getLuceneDocument(T o);

  /**
   * テーブルと値マップを指定してLuceneの{@link Document}を取得する
   * @param table テーブル
   * @param values 値マップ
   * @return Luceneドキュメント
   */
  public <T>Document getLuceneDocument(RlTable table, RlValues values);
  
  /**
   * 指定フィールドが指定値のレコードを削除する
   * @param field フィールド名称
   * @param value 値
   * @return　
   */
  public <T>RlWriter delete(String field, T value);

  /**
   * 指定フィールドのあるすべてのレコードを削除する
   * @param field　フィールド名称
   * @return
   */
  public <T>RlWriter deleteAll(String field);

  /**
   * このインデックスデータベースのすべてのレコードを削除する
   */
  public <T>RlWriter deleteAll();
  
  /**
   * 書込みあるいは削除を行った回数を取得する。
   * <p>
   * ライタから取得したサーチャを内部的にリオープンするためのもの。
   * </p>
   */
  public int writtenCount();

  /**
   * サーチャを取得する。
   * <p>
   * ここで取得されるサーチャはライタの書き込みに即座に追随する。 たとえそれがcommitあるいはcloseされてなくてもよい。
   * </p>
   */
  public RlSearcher getSearcher(Class<?>recordClass);

  /**
   * サーチャを取得する。
   * <p>
   * ここで取得されるサーチャはライタの書き込みに即座に追随する。 たとえそれがcommitあるいはcloseされてなくてもよい。
   * </p>
   */
  public RlSearcher getSearcher(RlTable table);
  
  /**
   * コミットする。
   * <p>
   * これまでの書き込みをフラッシュする。通常のサーチャ（このライタから 取得したサーチャではなくデータベースから取得したもの）は、コミット後に
   * リオープンすることで、フラッシュされたデータを検索することができる。
   * </p>
   */
  public RlWriter commit();

  /**
   * クローズする。
   * <p>
   * 書き込みをフラッシュし、ライタを終了する。 通常のデータベースシステムとは異なり、ロールバック操作は無いため、 書き込みをキャンセルする方法はない。
   * </p>
   */
  public void close();

  /**
   * 内部的なIndexWriterConfigを取得する
   */
  public <T> T getIndexWriterConfig();

  /** クローズされたか */
  public boolean isClosed();
  
  /**
   * LxWriter実装
   * 
   * @author ysugimura
   */
  public class Impl implements RlWriter {

    /** インジェクタ */
    @Inject Injector injector;

    /** データベース */
    private RlDatabase database;

    /** LuceneのIndexWriter。初期化時に作成される。クローズ時にnullが代入される。
     *  */
    private IndexWriter indexWriter;

    /** LuceneのIndexWriterConfig */
    private IndexWriterConfig config;

    /** 書込み回数 */
    private int writtenCount;

    @Inject private PerFieldAnalyzerCreator creator;
    
    /** 初期化 */
    Impl setup(RlDatabase database) {
      
      this.database = database;
      try {                
        config = new IndexWriterConfig(creator.create(database));
        
        // クローズ時にコミットする
        assert config.getCommitOnClose();
        
        indexWriter = new IndexWriter(
            ((RlDatabase.AbstractImpl)database).getDirectory(), config);

        // setMaxFieldLengthもdeprecatedとなり、その代わりにLimitTokenCountAnalyzer
        // を使えとドキュメントにあるが、使い方がわからない。
//        indexWriter.setMaxFieldLength(MaxFieldLength.UNLIMITED.getLimit());
        
        
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      return this;

    }

    /**
     * 内部的なIndexWriterを取得する
     */
    @SuppressWarnings("unchecked")
    public <T> T getIndexWriterConfig() {
      return (T) config;
    }

    @Inject private RlSearcherForWriter.Factory searcherFactory;
    
    /**
     * サーチャを取得する。
     * <p>
     * ここで取得されるのはニアリアルタイムサーチャである。
     * </p>
     */
    public synchronized RlSearcher getSearcher(Class<?>recordClass) {
      RlTable table = database.getTableSet().getTable(recordClass);
      if (table == null) throw new RlException("テーブルがありません：" + recordClass);
      return getSearcher(table);
    }
    
    public synchronized RlSearcher getSearcher(RlTable table) {
      if (table == null) throw new NullPointerException();
      return searcherFactory.create(table, this);
    }

    IndexReader getIndexReader() {
      throw new RuntimeException();
      /*
      try {
        // return indexWriter.getReader();
        // 上の呼び出しはdeprecatedになった
        return IndexReader.open(indexWriter, true);

      } catch (IOException ex) {
        throw new LxException.IO(ex);
      }
      */
      
    }

    @Override
    public <T>Document getLuceneDocument(T rec) {
      
      if (rec instanceof RlValues) {
        throw new RlException("LxValuesは使用できません");
      }
      
      // オブジェクトのクラスを取得する
      Class<T>clazz = (Class<T>)rec.getClass();

      // クラスのマッピング情報を取得する
      RlTable table = database.getTableSet().getTable(clazz);
      if (table == null) {
        throw new RlException(clazz.getName() + "は登録されていません");
      }

      // ドキュメントを作成する
      return table.getDocument(rec);
    }

    /** {@inheritDoc} */
    @Override
    public Document getLuceneDocument(RlTable table, RlValues values) {
      return table.getDocument(values);      
    }    
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    public synchronized <T>RlWriter write(T rec) {

      Document doc = getLuceneDocument(rec);
      
      // プライマリキータームを作成する
      RlTable table = database.getTableSet().getTable(rec.getClass());
      Term pkTerm = table.getPkTerm(rec);

      return write(pkTerm, doc);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized RlWriter write(RlTable table, RlValues values) {
      return write(table.getPkTerm(values), getLuceneDocument(table, values));
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

      writtenCount++;
      return this;
    }
    
    /** 書込み回数を取得 */
    public int writtenCount() {
      return writtenCount;
    }

    /** コミットする */
    public synchronized RlWriter commit() {
      try {
        indexWriter.commit();
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      return this;
    }

    /** クローズする */
    @Override
    public synchronized void close() {
      try {
        indexWriter.close();
        indexWriter = null;
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
    }

    @Override
    public synchronized boolean isClosed() {
      return indexWriter == null;
    }
    
    /** {@inheritDoc} */
    @Override
    public <T>RlWriter delete(String fieldName, T value) {
      RlField field = database.getFieldFromName(fieldName);
      if (field == null) throw new RlException("フィールドがありません:" + fieldName);
      return delete(field, value);
    }
    
    /**
     * レコードを削除する。
     * <p>
     * フィールドとその値を指定する。
     * フィールドはtokenized=falseでなければいけない。
     * </p>
     */
    public synchronized <T>RlWriter delete(RlField field, T value) {
      if (field.isTokenized()) {
        throw new RlException("tokenized=trueのフィールドを指定して削除はできません");
      }
      try {
        String string = field.toString(value);
        indexWriter.deleteDocuments(new Term(field.getName(), string));
        writtenCount++;
        return this;
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
    }
    
    /** {@inheritDoc} */
    @Override
    public synchronized <T>RlWriter deleteAll(String fieldName) {
      RlField field = database.getFieldFromName(fieldName);
      if (field == null) throw new RlException("フィールドがありません：" + fieldName);
      return deleteAll(field);
    }
    
    /** {@inheritDoc} */
    public synchronized<T>RlWriter deleteAll(RlField field) {
      try {
        Term term = new Term(field.getName(), "*");
        Query query = new WildcardQuery(term);
        indexWriter.deleteDocuments(query);
        writtenCount++;
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public synchronized <T>RlWriter deleteAll() {
      try {
        indexWriter.deleteAll();
        writtenCount++;
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      return this;
    }
  }

}
