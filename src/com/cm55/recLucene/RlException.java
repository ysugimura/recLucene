package com.cm55.recLucene;


public class RlException extends RuntimeException {

  public RlException(String message) {
    super(message);
  }
  
  public RlException(Throwable ex) {
    super(ex);
  }
  
  public static class Config extends RlException {
    public Config(String message) {
      super(message);
    }
    public Config(Throwable ex) {
      super(ex);
    }
  }
  
  public static class IO extends RlException {
    public IO(String message) {
      super(message);
    }
    public IO(Throwable ex) {
      super(ex);
    }
  }
  
  public static class Usage extends RlException {
    public Usage(String message) {
      super(message);
    }
    public Usage(Throwable ex) {
      super(ex);
    }
  }
}
