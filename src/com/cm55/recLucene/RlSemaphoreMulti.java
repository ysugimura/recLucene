package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

/**
 * 複数のセマフォについてそのすべてを獲得するためのオブジェクト
 * @author ysugimura
 */
public class RlSemaphoreMulti {

  List<RlSemaphore>semaphores;
  
  /** 対象のセマフォを指定する */
  public RlSemaphoreMulti(RlSemaphore...semaphores) {
    this.semaphores = Arrays.asList(semaphores);
  }
  
  /** 
   * すべてを取得する。すべてを獲得できるまで待つ
   * {@link Holder}を返すので、{@link Holder#release()}を呼ぶことを解放する。
   * @return セマフォのホルダー
   */
  Holder acquireAll() {
    return new Holder(semaphores.stream().map(h->h.acquireAll()).collect(Collectors.toList()));
  }

  /**
   * すべてを取得すべく試すが、できない場合はnullを返す。
   * @return
   */
  Holder tryAcquireAll() {
    List<RlSemaphore.Holder>acs = 
      semaphores.stream().map(h->h.acquireAll()).filter(ac->ac != null).collect(Collectors.toList());
    if (acs.size() == semaphores.size()) return new Holder(acs);
    acs.forEach(ac->ac.release());
    return null;
  }
  
  /**
   * セマフォのホルダ
   */
  class Holder {
    List<RlSemaphore.Holder>subHolders;
    public Holder(List<RlSemaphore.Holder>subHolders) {
      this.subHolders = subHolders;
    }
    public void release() {
      subHolders.forEach(ac->ac.release());
    }
  }
}
