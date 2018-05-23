package com.cm55.recLucene;

public class RlResetter {

  private RlDatabase database;
  private SemaphoreHandler.Acquisition write;
  private SemaphoreHandler.Acquisition search;
  
  public RlResetter(RlDatabase database, SemaphoreHandler.Acquisition write, SemaphoreHandler.Acquisition search) {
    this.database = database;
    this.write = write;
    this.search = search;
  }

  public void resetAndClose() {
    this.database.reset(write);
    this.search.release();
  }
}
