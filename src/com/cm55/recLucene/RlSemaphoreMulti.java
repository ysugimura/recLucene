package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

public class RlSemaphoreMulti {

  List<RlSemaphore>handlers;
  public RlSemaphoreMulti(RlSemaphore...handlers) {
    this.handlers = Arrays.asList(handlers);
  }
  
  Holder acquireAll() {
    return new Holder(handlers.stream().map(h->h.acquireAll()).collect(Collectors.toList()));
  }

  Holder tryAcquireAll() {
    List<RlSemaphore.Holder>acs = 
      handlers.stream().map(h->h.acquireAll()).filter(ac->ac != null).collect(Collectors.toList());
    if (acs.size() == handlers.size()) return new Holder(acs);
    acs.forEach(ac->ac.release());
    return null;
  }
  
  class Holder {
    List<RlSemaphore.Holder>acs;
    public Holder(List<RlSemaphore.Holder>acs) {
      this.acs = acs;
    }
    public void release() {
      acs.forEach(ac->ac.release());
    }
  }
}
