package com.cm55.recLucene;

import java.util.*;

import org.junit.*;
import static org.junit.Assert.*;

import com.cm55.recLucene.RlSemaphore.*;

public class RlSemaphoreTest {

  @Test
  public void test() throws Exception {
    List<String>verify = Collections.synchronizedList(new ArrayList<>());
    
    RlSemaphore semaphore = new RlSemaphore(2);
    
    Holder a = semaphore.acquire();
    Holder b = semaphore.acquire();
    new Thread() {
      public void run() {
        verify.add("1");
        Holder c = semaphore.acquire();
        verify.add("4");
        try { Thread.sleep(200); } catch (InterruptedException ex) {}
        verify.add("5");
        c.release();
      }
    }.start();
    Thread.sleep(200);
    verify.add("2");
    Thread.sleep(200);
    verify.add("3");
    a.release();
    Thread.sleep(200);
    Holder d = semaphore.acquire();
    verify.add("6");

    assertArrayEquals(new String[] { "1", "2", "3", "4", "5", "6"}, verify.toArray(new String[0]));
  }
}
