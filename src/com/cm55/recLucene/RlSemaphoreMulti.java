package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

public class RlSemaphoreMulti {

  List<RlSemaphore>handlers;
  public RlSemaphoreMulti(RlSemaphore...handlers) {
    this.handlers = Arrays.asList(handlers);
  }
  
  Ac acquireAll() {
    return new Ac(handlers.stream().map(h->h.acquireAll()).collect(Collectors.toList()));
  }

  Ac tryAcquireAll() {
    List<RlSemaphore.Ac>acs = 
      handlers.stream().map(h->h.acquireAll()).filter(ac->ac != null).collect(Collectors.toList());
    if (acs.size() == handlers.size()) return new Ac(acs);
    acs.forEach(ac->ac.release());
    return null;
  }
  
  class Ac {
    List<RlSemaphore.Ac>acs;
    public Ac(List<RlSemaphore.Ac>acs) {
      this.acs = acs;
    }
    public void release() {
      acs.forEach(ac->ac.release());
    }
  }
}
