package com.cm55.recLucene;

import java.util.concurrent.*;

/**
 * Javaの{@link Semaphore}をラップしたもの。
 * セマフォを取得するとそのホルダーである{@link Holder}を返すので、その{@link Holder#release()}を呼び出すことにより
 * セマフォを解放することができる。
 * @author ysugimura
 */
class RlSemaphore {

  /** Javaのセマフォ */
  java.util.concurrent.Semaphore semaphore;

  /** 最大許可数 */
  private int permits;

  /**
   * 最大許可数を指定する
   * @param permits 最大許可数
   */
  RlSemaphore(int permits) {
    this.permits = permits;
    semaphore = new Semaphore(permits);
  }

  /**
   * セマフォを取得する。取得するまで待つ。
   * @return {@link Holder}を返すので、解放する場合は{@link Holder#release()}を呼ぶこと。
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
   * セマフォを取得する。取得できた場合は{@link Holder}オブジェクトを返す。 できない場合はnullを返す。
   * @return セマフォを取得した場合は{@link Holder}、そうでなければnull
   */
  Holder tryAcquire() {
    if (!semaphore.tryAcquire())
      return null;
    return new Holder(semaphore, 1);
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
   * セマフォのホルダオブジェクト。リリースを一度だけ行うことができる。
   * @author ysugimura
   */
  class Holder {

    /** セマフォ */
    private Semaphore semaphore;

    /** 許可数 */
    private final int permits;

    private Holder(Semaphore semaphore, int permits) {
      this.semaphore = semaphore;
      this.permits = permits;
    }

    /**
     * セマフォをリリースする どのように呼び出されても一度しかリリースしない。
     */
    synchronized void release() {
      if (semaphore == null) return;
      semaphore.release(permits);
      semaphore = null;
    }
  }
}