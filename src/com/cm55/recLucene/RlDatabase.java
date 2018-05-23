package com.cm55.recLucene;

import java.io.*;
import java.nio.file.*;

import org.apache.lucene.index.*;
import org.apache.lucene.store.*;




/**
 * インデックスデータベース。
 * <p>
 * Luceneではインデックスと呼ばれているものを表す。現在のところRAMベース とディレクトリベースをサポートしている。
 * </p>
 * <p>
 * 特にディレクトリデータベースの場合、同じディレクトリを指定して複数の{@link RlDatabase}を作成することが
 * 可能だが、これは意味が無い。なぜなら、ある一つのデータベースに対する{@link RlWriter}はただ一つしか
 * 存在できないからである。以前に取得した{@link RlWriter}をclose()しない限り、別の{@link RlWriter}
 * を作成することはできない。
 * </p>
 * 
 * @author ysugimura
 */
public interface RlDatabase {

  /**
   * {@link RlDatabase}のファクトリ
   * @author ysugimura
   */
  public static class Factory {
        
    public static RlDatabase createRam(Class<?>...classes) {
      return createRam(new RlTableSet(classes));
    }
    
    /**
     * RAMデータベースを作成する
     * @param tableSet テーブルセット
     * @return RAMデータベース
     */
    public static RlDatabase createRam(RlTableSet tableSet) {
      Ram ram = new Ram();
      ram.setup(tableSet);
      return ram;
    }

    /**
     * RAMデータベースを作成する
     * @param tables データベースを構成するテーブル
     * @return RAMデータベース
     */
    public static RlDatabase createRam(RlTable...tables) {
      return createRam(new RlTableSet(tables));
    }

    /**
     * ディレクトリデータベースを作成する
     * @param dirName ディレクトリ名称
     * @param classes 対象レコードクラス配列
     * @return ディレクトリデータベース
     */
    public static RlDatabase createDir(String dirName, Class<?>...classes) {
      return createDir(dirName, new RlTableSet(classes));
    }

    /**
     * ディレクトリデータベースを作成する
     * @param dirName ディレクトリ名称
     * @param tables 対象テーブル配列
     * @return ディレクトリデータベース
     */
    public static RlDatabase createDir(String dirName, RlTable...tables) {
      return createDir(dirName, new RlTableSet(tables));
    }
    
    /**
     * ディレクトリデータベースを作成する
     * @param dirName ディレクトリ名称
     * @param tableSet テーブルセット
     * @return ディレクトリデータベース
     */
    public static RlDatabase createDir(String dirName, RlTableSet tableSet) {
      Dir dir = new Dir();
      dir.setup(tableSet, dirName);
      return dir;
    }
  }
  
  /**
   * このデータベースのテーブルセットを取得する
   * @return テーブルセット
   */
  public RlTableSet getTableSet();

  /** データベースをクローズする */
  public void close();
  
  /** 
   * このデータベースに対するライタを作成して返す。
   * <p>
   * ただし、一つのディレクトリデータベース対する、生きている(closeされていない){@link RlWriter}は、同時には
   * ただ一つしか存在できないことに注意する。たとえ、そのディレクトリを指定して{@link RlDatabase}を
   * 複数作成したとしても、ロックはファイルレベルで行われるため、複数の{@link RlWriter}を作成することは
   * できない。
   * </p>
   * @return 新たなライタ
   */
  public RlWriter createWriter();

  /**
   * 指定したクラスオブジェクトのテーブルに対するサーチャを取得する。
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャをリオープンする必要がある。
   * </p>
   * @param recordClass レコードクラス
   * @return サーチャ
   */
  public RlSearcher createSearcher(Class<?>recordClass);

  /**
   * 指定したテーブルに対するサーチャを取得する
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャをリオープンする必要がある。
   * </p>
   * @param table テーブル
   * @return サーチャ
   */
  public RlSearcher createSearcher(RlTable table);
  
  /**
   * テーブルのフィールド名からRlFieldを取得する。
   * <p>
   * フィールド名はデータベース中で一意であるので、フィールドも一意に決定する。
   * </p>
   */
  public RlField getFieldFromName(String fieldName);
  
  /**
   * RlDatabase実装
   * 
   * @author ysugimura
   *
   */
  public abstract class AbstractImpl implements RlDatabase {

    /** ディレクトリ */
    protected Directory directory;
    
    /** テーブルセット */
    protected RlTableSet tableSet;
    
    public AbstractImpl() {
    }

    /** {@inheritDoc} */
    @Override
    public RlTableSet getTableSet() {
      return tableSet;
    }
    
    /** IndexReaderを取得する */
    synchronized IndexReader getIndexReader() {
      try {
        return DirectoryReader.open(directory);
      } catch (IOException ex) {
        throw new RlException(ex);
      }
    }

    /** LuceneのDirectoryを取得する */
    public Directory getDirectory() {
      return directory;
    }

    @Override
    public void close() {
      try {
        directory.close();
      } catch (IOException ex) {
        throw new RlException(ex);
      }
      directory = null;
    }

    /** このデータベース用のサーチャを取得する */
    @Override
    public synchronized RlSearcher createSearcher(Class<?>recordClass) {      
      RlTable table = tableSet.getTable(recordClass);
      if (table == null) throw new RlException("no table for " + recordClass);
      return createSearcher(table);
    }
    
    /** {@inheritDoc} */
    @Override
    public synchronized RlSearcher createSearcher(RlTable table) {
      return new RlSearcherForDatabase(table,  this);
    }
    
    /** {@inheritDoc} */
    @Override
    public synchronized RlWriter createWriter() {
      return new RlWriter(this);
    }

    /** フィールド名からRlFieldを取得する */
    @Override
    public RlField getFieldFromName(String fieldName) {
      return tableSet.getFieldByName(fieldName);
    }
  }

  /**
   * RAM上に作成されるデータベース
   */
  public static class Ram extends AbstractImpl {
    
    public Ram() {
      super();
      this.directory = new RAMDirectory();
    }

    /**
     * テーブルセットを指定する
     * @param tableSet テーブルセット
     */
    private void setup(RlTableSet tableSet) {
      this.tableSet = tableSet;
    }
  }

  /**
   * 物理ディレクトリ用のデータベース
   */
  public static class Dir extends AbstractImpl {

    public Dir() {}

    /**
     * テーブルセット、ディレクトリパスを指定する
     * @param tableSet テーブルセット
     * @param dirName ディレクトリパス
     */
    private void setup(RlTableSet tableSet, String dirName) {
      this.tableSet = tableSet;
      Path path = FileSystems.getDefault().getPath(dirName);
      try {
        this.directory = FSDirectory.open(path);
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
    }
  }
}
