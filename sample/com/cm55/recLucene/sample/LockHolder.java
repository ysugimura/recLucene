package com.cm55.recLucene.sample;

import com.cm55.recLucene.sample.MultiSemaphore.*;

/**
 * {@link Locker｝によって取得されたロックのホルダ
 * @author ysugimura
 */
public class LockHolder {
  
  /** リーダロックオブジェクト */
  private Acquisition readerAc;
  
  /** ライタロックオブジェクト */
  private Acquisition writerAc;
  
  LockHolder(Acquisition readerAc, Acquisition writerAc) {
    this.readerAc = readerAc;
    this.writerAc = writerAc;
  }

  /** リーダロックを保持していることを確認する。保持してなければ例外 */
  public void hasReadLock() {
    if (readerAc == null) throw new RuntimeException("no read lock");
  }

  /** ライタロックを保持していることを確認する。保持してなければ例外 */
  public void hasWriteLock() {
    if (writerAc == null) throw new RuntimeException("no write lock");
  }

  /** リセッタロックを保持していることを確認する。保持してなければ例外 */
  public void hasResetLock() {
    if (readerAc == null || writerAc == null) 
      throw new RuntimeException("no reset lock");
  }
  
  /**
   * 保持しているロックをすべてリリースする。
   */
  public void release() {
    if (readerAc != null) readerAc.release();
    if (writerAc != null) writerAc.release();
  }
  
  /**
   * 文字列化。デバッグ用
   */
  @Override
  public String toString() {
    return "reader:" + (readerAc != null) + " writer:" + (writerAc != null);
  }
}