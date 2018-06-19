package com.cm55.recLucene;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
public abstract class RlDatabase {

  /** ディレクトリ */
  protected Directory directory;

  /** テーブルセット */
  protected RlTableSet tableSet = new RlTableSet();
    
  /** ライタ取得セマフォ。ライターはただ一つしか取得することはできない */
  protected RlSemaphore writeｒSemaphore = new RlSemaphore(1);

  /** サーチャー取得セマフォ。最大100個 */
  protected RlSemaphore searcherSemaphore = new RlSemaphore(100);

  /** ライタ・サーチャー取得セマフォのすべて */
  protected RlSemaphoreMulti allSemaphore = new RlSemaphoreMulti(writeｒSemaphore, searcherSemaphore);

  /** インデックスライタホルダー */
  protected RlWriterHolder writerHolder = new RlWriterHolder();
  
  protected RlDatabase() {  
  }
  
  protected void setDirectory(Directory directory) {
    this.directory = directory;
    writerHolder.reset(directory, tableSet);
  }
  
  /**
   * このデータベースのテーブルセットを取得する
   * 
   * @return テーブルセット
   */
  RlTableSet getTableSet() {
    return tableSet;
  }

  /** データベースをクローズする */
  public void close() {
    RlSemaphoreMulti.Holder holder = allSemaphore.acquireAll();
    try {
      writerHolder.close();
      directory.close();
    } catch (IOException ex) {
      throw new RlException(ex);
    }
    holder.release();
    directory = null;
  }

  /**
   * テーブルとしてクラスを追加する
   * @param classes
   * @return
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public RlDatabase add(Class<?>...classes) {
    Arrays.stream(classes).map(c->new RlClassTable(c)).forEach(this::add);
    return this;
  }

  /**
   * テーブルを追加する
   * @param tables
   * @return
   */
  public RlDatabase add(RlTable<?>...tables) {
    RlSemaphoreMulti.Holder holder = allSemaphore.acquireAll();    
    try {
      Arrays.stream(tables).forEach(tableSet::add);
      writerHolder.reset(directory, tableSet);
    } finally {
      holder.release();
    }
    return this;
  }
  
  /**
   * このデータベースに対するライタを作成して返す。
   * ライタはただ一つしか存在できず、既にオープン中のライタがある場合は、close()されるまで待つ。
   * @return 新たなライタ
   */
  public RlWriter createWriter() {
    RlSemaphore.Holder holder = writeｒSemaphore.acquire();
    return new RlWriter(tableSet, writerHolder.getIndexWriter(), holder); 
  }

  /**
   * このデータベースに対するライタを作成して返す。
   * ライタはただ一つしか存在できず、既にオープン中のライタがある場合は何もせずにnullを返す。
   * @return
   */
  public RlWriter tryCreateWriter() {
    RlSemaphore.Holder holder = writeｒSemaphore.tryAcquire();
    if (holder == null) return null;
    return new RlWriter(tableSet, writerHolder.getIndexWriter(), holder); 
  }
  
  /**
   * 指定したクラスオブジェクトのテーブルに対するサーチャを取得する。
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャを作成する必要がある。
   * </p>
   * @param recordClass レコードクラス
   * @return サーチャ
   */
  public synchronized <T>RlSearcher<T> createSearcher(Class<T> recordClass) {
    RlClassTable<T> table = tableSet.getTable(recordClass);
    if (table == null)
      throw new RlException("no table for " + recordClass);
    return createSearcher(table);
  }

  /**
   * 指定したテーブルに対するサーチャを取得する。サーチ結果はレコードオブジェクトとして返されるため、
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャをリオープンする必要がある。
   * </p>
   * 
   * @param table テーブル
   * @return サーチャ
   */
  public synchronized <T>RlSearcher<T> createSearcher(RlTable<T>table) {
    RlSemaphore.Holder holder = searcherSemaphore.acquire();
    return new RlSearcher<T>(table, writerHolder.getSearcherManager(), holder);
  }

  /** 
   * このデータベースをリセットする。いったんすべてのデータを削除する。 
   * ライター・サーチャーのすべてのセマフォが取得できるまで待つ。
   */  
  public synchronized void reset() {
    RlSemaphoreMulti.Holder holder = allSemaphore.acquireAll();
    doingReset(holder);
  }

  /**
   * このデータベースをリセットする。いったんすべてのデータを削除する。
   * ライター・サーチャーのすべてのセマフォが取得できない場合はfalseを返す。
   * @return
   */
  public synchronized boolean tryReset() {
    RlSemaphoreMulti.Holder holder = allSemaphore.tryAcquireAll();
    if (holder == null) return false;
    doingReset(holder);
    return true;
  }

  /** 
   * リセットを行う。すべてのセマフォが取得できているので、
   * RlWriterHolderをいったん閉じてからデータベースリセットを行う 
   */
  private void doingReset(RlSemaphoreMulti.Holder holder) {
    try {
      writerHolder.close();
      doReset();
    } finally {
      holder.release();    
    }
  }
  
  /** 下位の実装でデータベースリセットを行う */
  protected abstract void doReset();
  
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

  /**
   * RAM上に作成されるデータベース
   */
  public static class Ram extends RlDatabase {

    public Ram() {
      setDirectory(new RAMDirectory());
    }

    protected void doReset() {
      setDirectory(new RAMDirectory());
    }
  }

  /**
   * 物理ディレクトリ用のデータベース
   */
  public static class Dir extends RlDatabase {

    private Path path;

    /**
     * テーブルセット、ディレクトリパスを指定する
     * 
     * @param tableSet
     *          テーブルセット
     * @param dirName
     *          ディレクトリパス
     */
    public Dir(String dirName) {
      path = FileSystems.getDefault().getPath(dirName);
      try {
        setDirectory(FSDirectory.open(path));
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
    }
    
    
    public Dir(File folder) {
      try {
        path = folder.toPath();
        setDirectory(FSDirectory.open(path));
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
    }

    protected void doReset() {

      // luceneデータベースフォルダを削除する
      File dir = path.toFile();
      delete(dir);
      if (dir.exists()) {
        System.err.println("!!! DIRECTORY LEFT !!!!");
      }
      try {
        setDirectory(FSDirectory.open(path));
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      
    }

    /** 指定されたファイル、あるいはフォルダ以下すべてを完全に削除する */
    public boolean delete(File file) {
      return new Object() {
        boolean delete(File file) {

          // ファイルがもともと存在しなかければtrueを返す
          if (!file.exists())
            return true;

          // 通常ファイルのとき
          if (!file.isDirectory()) {
            return file.delete();
          }

          // ディレクトリのとき、その中のファイルを削除する
          // 一つでも削除できないものがあったらfalseを返す
          for (File child : file.listFiles()) {
            if (!delete(child))
              return false;
          }

          // ディレクトリ自体を削除する。削除できなければfalseを返す
          if (!file.delete())
            return false;

          return true;
        }
      }.delete(file);
    }
  }
}
