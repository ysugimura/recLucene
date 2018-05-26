package com.cm55.recLucene;

import org.junit.*;
import static org.junit.Assert.*;

import com.cm55.recLucene.RlSemaphoreMulti.*;

public class RlSemaphoreMultiTest {

  @Test
  public void test() {
    RlSemaphore a = new RlSemaphore(1);
    RlSemaphore b = new RlSemaphore(2);
    RlSemaphoreMulti multi = new RlSemaphoreMulti(a, b);
    
    Holder mHolder = multi.acquireAll();
    assertNull(a.tryAcquire());
    assertNull(b.tryAcquire());
    
    mHolder.release();
    assertNotNull(a.acquire());
    assertNotNull(b.acquire());
    assertNotNull(b.acquire());
  }

}
