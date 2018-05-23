package com.cm55.recLucene.sample;

import com.cm55.recLucene.*;
import com.cm55.recLucene.SemaphoreHandler.*;

/**
 * メールハンドリングのためのロック。以下の種類がある。
 * <ul>
 * <li>readerロック：複数のリーダが同時に検索動作を行うことができるが、その際にロックの一つを取得する必要がある。
 * <li>writerロック：単一のライタだけが書き込みを行うことができる。これは主に新たなメールを検出した際に
 * 行われる。
 * <li>resetterロック：リーダのすべてのロックとライタのロックを取得しなければならない。データベースの全削除、
 * インデックスの全削除などを行って再構築する際などに用いられる。
 * </ul>
 * @author ysugimura
 */
public class Locker {

  /**
   * リーダ用ロック。複数のリーダが同時に取得可能
   */
  private static SemaphoreHandler readerSemaphore;

  /**
   * ライタ用ロック。単一のライタだけが取得可能。
   */
  private static SemaphoreHandler writerSemaphore;
  
  static {
    readerSemaphore = new SemaphoreHandler(30);
    writerSemaphore = new SemaphoreHandler(1);
  }

  /**
   * リーダ用のロックを取得する
   * @return
   */
  public static LockHolder getReaderLock() {
    return new LockHolder(readerSemaphore.acquire(), null);
  }

  /**
   * ライタ用のロックを取得する
   * @return
   */
  public static LockHolder getWriterLock() {
    return new LockHolder(null, writerSemaphore.acquire());
  }
  
  public static LockHolder tryWriterLock() {
    Acquisition ac = writerSemaphore.tryAcquire();
    if (ac == null) return null;
    return new LockHolder(null, ac);
  }

  /**
   * リセッタ用のロックを取得する。
   * 単一のライタロックを取得し、その後に全リーダロックを取得する。
   * 
   * @return
   */
  public static LockHolder getResetterLock() {
    
    // ライタロックを取得する。
    Acquisition writerAc = writerSemaphore.acquire();
    
    // 全リーダロックを取得する
    Acquisition readerAc = readerSemaphore.acquireAll();
    return new LockHolder(readerAc, writerAc);
  }
}
