package com.cm55.recLucene;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.lucene.analysis.*;
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
  
  protected RlWriterReader writerReader;
  

  /** ライタ取得セマフォ。ライターはただ一つしか取得することはできない */
  protected SemaphoreHandler writeSemaphore = new SemaphoreHandler(1);

  /** リーダ取得セマフォ。最大100個 */
  protected SemaphoreHandler searchSemaphore = new SemaphoreHandler(100);

  protected RlDatabase() {
    
  }
  /**
   *  ※lucene3.3.0でのバグに注意：インデックスデータベースの作成時には、一度でも
   * commit（何も書きこむものがなくてもよい）しておかないと、インデックスファイル 構造が作成されず、リアルタイムサーチャが失敗してしまう。
   * 
   */
  protected void init(SemaphoreHandler.Acquisition ac) {
    RlWriter writer;
    if (ac == null) {
      writer = this.createWriter();
    } else {
      writer = newWriter(ac);
    }
    writer.commit();
    writer.close();
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
    try {
      directory.close();
    } catch (IOException ex) {
      throw new RlException(ex);
    }
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
    SemaphoreHandler.Acquisition ac = this.writeSemaphore.acquire();    
    try {
      Arrays.stream(tables).forEach(tableSet::add);
      if (writerReader != null) {        
      
        writerReader.close();
        writerReader = null;
      }
      
    } finally {
      ac.release();
    }
    return this;
  }
  
  /**
   * このデータベースに対するライタを作成して返す。
   * ライタはただ一つしか存在できず、既にオープン中のライタがある場合は、close()されるまで待つ。
   * @return 新たなライタ
   */
  public RlWriter createWriter() {
    SemaphoreHandler.Acquisition ac = writeSemaphore.acquire();
    return newWriter(ac);
  }

  /**
   * このデータベースに対するライタを作成して返す。
   * ライタはただ一つ歯科存在できず、既にオープン中のライタがある場合は何もせずにnullを返す。
   * @return
   */
  public RlWriter tryCreateWriter() {
    SemaphoreHandler.Acquisition ac = writeSemaphore.tryAcquire();
    if (ac == null) return null;
    return newWriter(ac);
  }

  /**
   * ライタを作成する。既にライタセマフォは取得している。
   * @param ac
   * @return
   */
  private RlWriter newWriter(SemaphoreHandler.Acquisition ac) {
    
    // アナライザがまだなければ作成する。既にライタセマフォを取得しているため、synchronizedは不要
    if (writerReader == null) {
      writerReader = new RlWriterReader(getDirectory(), tableSet);
    }
    
    // ライタを作成
    return new RlWriter(this, writerReader.indexWriter, ac);    
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
   * @param table
   *          テーブル
   * @return サーチャ
   */
  public synchronized <T>RlSearcher<T> createSearcher(RlTable<T>table) {
    if (writerReader == null) {
      writerReader = new RlWriterReader(getDirectory(), tableSet);
    }
    SemaphoreHandler.Acquisition ac = searchSemaphore.acquire();
    return new RlSearcher<T>(table, writerReader.indexReader, ac);
  }

  /** このデータベースをリセットする */
  public synchronized void reset() {
    reset( writeSemaphore.acquireAll(), searchSemaphore.acquireAll());
  }

  public synchronized boolean tryReset() {
    SemaphoreHandler.Acquisition write = writeSemaphore.tryAcquireAll();
    if (write == null)
      return false;
    SemaphoreHandler.Acquisition search = searchSemaphore.tryAcquireAll();
    if (search == null) {
      write.release();
      return false;
    }
    reset(write, search);
    return true;
  }

  private void reset(SemaphoreHandler.Acquisition write, SemaphoreHandler.Acquisition search) {
    try {
      reset(write);
    } finally {
      search.release();
    }
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

  protected abstract void reset(SemaphoreHandler.Acquisition ac);

  /**
   * RAM上に作成されるデータベース
   */
  public static class Ram extends RlDatabase {

    public Ram() {
      this.directory = new RAMDirectory();
      super.init(null);
    }

    protected void reset(SemaphoreHandler.Acquisition ac) {
      this.directory = new RAMDirectory();
      super.init(ac);
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
        this.directory = FSDirectory.open(path);
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      super.init(null);
    }
    
    public Dir(File folder) {
      try {
        this.directory = FSDirectory.open(folder.toPath());
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      super.init(null);
    }

    protected void reset(SemaphoreHandler.Acquisition ac) {

      // luceneデータベースフォルダを削除する
      File dir = path.toFile();
      delete(dir);
      if (dir.exists()) {
        System.err.println("!!! DIRECTORY LEFT !!!!");
      }
      try {
        this.directory = FSDirectory.open(path);
      } catch (IOException ex) {
        throw new RlException.IO(ex);
      }
      
      super.init(ac);
    }

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
