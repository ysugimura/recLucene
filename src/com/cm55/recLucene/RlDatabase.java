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
  public RlTableSet getTableSet() {
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

  public RlDatabase add(Class<?>...classes) {
    Arrays.stream(classes).map(c->new RlTable(c)).forEach(this::add);
    return this;
  }
  
  public RlDatabase add(RlTable...tables) {
    SemaphoreHandler.Acquisition ac = this.writeSemaphore.acquire();    
    try {
      Arrays.stream(tables).forEach(tableSet::add);
    } finally {
      ac.release();
    }
    return this;
  }
  
  /**
   * このデータベースに対するライタを作成して返す。
   * <p>
   * ただし、一つのディレクトリデータベース対する、生きている(closeされていない){@link RlWriter}は、同時には
   * ただ一つしか存在できないことに注意する。たとえ、そのディレクトリを指定して{@link RlDatabase}を
   * 複数作成したとしても、ロックはファイルレベルで行われるため、複数の{@link RlWriter}を作成することは できない。
   * </p>
   * 
   * @return 新たなライタ
   */
  public synchronized RlWriter createWriter() {
    SemaphoreHandler.Acquisition ac = writeSemaphore.acquire();
    return newWriter(ac);
  }

  public synchronized RlWriter tryCreateWriter() {
    SemaphoreHandler.Acquisition ac = writeSemaphore.tryAcquire();
    if (ac == null)
      return null;
    return newWriter(ac);
  }

  private RlWriter newWriter(SemaphoreHandler.Acquisition ac) {
    IndexWriterConfig config = new IndexWriterConfig(PerFieldAnalyzerCreator.create(tableSet));

    // クローズ時にコミットする
    assert config.getCommitOnClose();
    return new RlWriter(this, ac, config);    
  }
  
  /**
   * 指定したクラスオブジェクトのテーブルに対するサーチャを取得する。
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャをリオープンする必要がある。
   * </p>
   * 
   * @param recordClass
   *          レコードクラス
   * @return サーチャ
   */
  public synchronized RlSearcher createSearcher(Class<?> recordClass) {
    RlTable table = tableSet.getTable(recordClass);
    if (table == null)
      throw new RlException("no table for " + recordClass);
    return createSearcher(table);
  }

  /**
   * 指定したテーブルに対するサーチャを取得する
   * <p>
   * ここで取得されるサーチャはライタの書き込みに追随しない。 たとえ、それがcommitやcloseされても反映されない。
   * 反映するには、新たなサーチャをリオープンする必要がある。
   * </p>
   * 
   * @param table
   *          テーブル
   * @return サーチャ
   */
  public synchronized RlSearcher createSearcher(RlTable table) {
    SemaphoreHandler.Acquisition ac = searchSemaphore.acquire();
    return new RlSearcherForDatabase(table, this, ac);
  }

  /** このデータベースに対するリセッタを取得する */
  public synchronized RlResetter createResetter() {
    //ystem.out.println(" " + writeSemaphore.semaphore.availablePermits() + ","
    // + searchSemaphore.semaphore.availablePermits());
    SemaphoreHandler.Acquisition write = writeSemaphore.acquireAll();
    SemaphoreHandler.Acquisition search = searchSemaphore.acquireAll();
    return new RlResetter(this, write, search);
  }

  public synchronized RlResetter tryCreateResetter() {
    SemaphoreHandler.Acquisition write = writeSemaphore.tryAcquireAll();
    if (write == null)
      return null;
    SemaphoreHandler.Acquisition search = searchSemaphore.tryAcquireAll();
    if (search == null) {
      write.release();
      return null;
    }
    return new RlResetter(this, write, search);
  }

  /**
   * テーブルのフィールド名からRlFieldを取得する。
   * <p>
   * フィールド名はデータベース中で一意であるので、フィールドも一意に決定する。
   * </p>
   */
  public RlField getFieldFromName(String fieldName) {
    return tableSet.getFieldByName(fieldName);
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
    Dir(String dirName) {
      path = FileSystems.getDefault().getPath(dirName);
      try {
        this.directory = FSDirectory.open(path);
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

  /**
   * RAMデータベースを作成する
   * 
   * @param tableSet
   *          テーブルセット
   * @return RAMデータベース
   */
  public static RlDatabase createRam() {
    Ram ram = new Ram();
    return ram;
  }


  /**
   * ディレクトリデータベースを作成する
   * 
   * @param dirName
   *          ディレクトリ名称
   * @param tableSet
   *          テーブルセット
   * @return ディレクトリデータベース
   */
  public static RlDatabase createDir(String dirName) {
    return new Dir(dirName);
  }
}
