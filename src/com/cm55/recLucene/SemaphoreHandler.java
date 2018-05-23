package com.cm55.recLucene;

import java.util.concurrent.*;

public class SemaphoreHandler {

  /** Javaのセマフォ */
  Semaphore semaphore;

  /** 最大許可数 */
  private int permits;

  public SemaphoreHandler() {
    this(1);
  }
  
  public SemaphoreHandler(int permits) {
    this.permits = permits;
    semaphore = new Semaphore(permits);
  }

  /**
   * セマフォを取得する。取得できた場合は{@link Acquisition}オブジェクトを返す。 できない場合はnullを返す。
   * 
   * @return セマフォを取得した場合は{@link Acquisition}、そうでなければnull
   */
  public Acquisition tryAcquire() {
    if (!semaphore.tryAcquire())
      return null;
    return new Acquisition(semaphore, 1);
  }

  /**
   * セマフォを取得する。取得するまで待つ。
   * 
   * @return
   * @throws InterruptedException
   */
  public Acquisition acquire() {
    try {
      semaphore.acquire();
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
    return new Acquisition(semaphore, 1);
  }

  /**
   * 全セマフォを取得してみる。できなければただちにnullを返す。
   * 
   * @return
   */
  public Acquisition tryAcquireAll() {
    if (!semaphore.tryAcquire(permits)) {
      return null;
    }
    return new Acquisition(semaphore, permits);
  }

  /**
   * 全取得する。取得するまで待つ
   * 
   * @return
   */
  public Acquisition acquireAll() {
    try {
      semaphore.acquire(permits);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
    return new Acquisition(semaphore, permits);
  }

  /**
   * セマフォの獲得標識オブジェクト。リリースを一度だけ行うことができる。
   * 
   * @author ysugimura
   */
  public class Acquisition {

    /** セマフォ */
    private final Semaphore semaphore;

    /** 許可数 */
    private final int permits;

    /** リリース済フラグ */
    private boolean released;

    private Acquisition(Semaphore semaphore, int permits) {
      this.semaphore = semaphore;
      this.permits = permits;
    }

    /**
     * セマフォをリリースする どのように呼び出されても一度しかリリースしない。
     */
    public synchronized void release() {
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
    public synchronized boolean notReleased() {
      return !released;
    }
  }
}