package com.cm55.recLucene;

import java.util.concurrent.*;

class RlSemaphore {

  /** Javaのセマフォ */
  java.util.concurrent.Semaphore semaphore;

  /** 最大許可数 */
  private int permits;

  RlSemaphore(int permits) {
    this.permits = permits;
    semaphore = new Semaphore(permits);
  }

  /**
   * セマフォを取得する。取得できた場合は{@link Holder}オブジェクトを返す。 できない場合はnullを返す。
   * 
   * @return セマフォを取得した場合は{@link Holder}、そうでなければnull
   */
  Holder tryAcquire() {
    if (!semaphore.tryAcquire())
      return null;
    return new Holder(semaphore, 1);
  }

  /**
   * セマフォを取得する。取得するまで待つ。
   * 
   * @return
   * @throws InterruptedException
   */
  Holder acquire() {
    try {
      semaphore.acquire();
    } catch (InterruptedException ex) {
      throw new RlException(ex);
    }
    return new Holder(semaphore, 1);
  }

  /**
   * 全セマフォを取得してみる。できなければただちにnullを返す。
   * 
   * @return
   */
  Holder tryAcquireAll() {
    if (!semaphore.tryAcquire(permits)) {
      return null;
    }
    return new Holder(semaphore, permits);
  }

  /**
   * 全取得する。取得するまで待つ
   * 
   * @return
   */
  Holder acquireAll() {
    try {
      semaphore.acquire(permits);
    } catch (InterruptedException ex) {
      throw new RlException(ex);
    }
    return new Holder(semaphore, permits);
  }

  /**
   * セマフォの獲得標識オブジェクト。リリースを一度だけ行うことができる。
   * 
   * @author ysugimura
   */
  class Holder {

    /** セマフォ */
    private final Semaphore semaphore;

    /** 許可数 */
    private final int permits;

    /** リリース済フラグ */
    private boolean released;

    private Holder(Semaphore semaphore, int permits) {
      this.semaphore = semaphore;
      this.permits = permits;
    }

    /**
     * セマフォをリリースする どのように呼び出されても一度しかリリースしない。
     */
    synchronized void release() {
      if (released)
        return;
      semaphore.release(permits);
      released = true;
    }

    /**
     * リリースされていないことを確認
     * 
     * @return
     */
    synchronized boolean notReleased() {
      return !released;
    }
  }

}