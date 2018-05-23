package com.cm55.recLucene.sample;

import java.io.*;

import com.cm55.recLucene.*;


public class IndexDatabase {


  /** キャッシュされたデータベース */
  RlDatabase cachedDatabase;

  /** ライタ */
  RlWriter cachedWriter;

  String path;
  
  public IndexDatabase(String path) {
    this.path = path;
    cachedDatabase = RlDatabase.Factory.createDir(path, IndexRecord.class);
  }

  /** {@inheritDoc} */
  public synchronized RlDatabase getDatabase() {
    return cachedDatabase;
  }

  /** {@inheritDoc} */
  public synchronized RlWriter getWriter() {
    boolean created = false;
    if (cachedWriter == null) {
      cachedWriter = getDatabase().createWriter();
      created = true;
    }

    return cachedWriter;
  }

  /** {@inheritDoc} */
  public synchronized void dropDatabase() {


    // 既にライタが存在する場合にはクローズしなければならない。
    if (cachedWriter != null) {
      cachedWriter.close();
      cachedWriter = null;
    }

    // データベースが存在する場合にはクローズしなければならない
    if (cachedDatabase != null) {
      cachedDatabase.close();
      cachedDatabase = null;
    }

    // luceneデータベースフォルダを削除する
    File dir = new File(path);
    delete(dir);
    if (dir.exists()) {
      System.err.println("!!! DIRECTORY LEFT !!!!");
    }
  }
  
  public boolean delete(File file) {
    return new Object() {
      boolean delete(File file) {
        
        // ファイルがもともと存在しなかければtrueを返す
        if (!file.exists()) 
          return true;
        
        // 通常ファイルのとき
        if (!file.isDirectory()) {
          return file.delete();            
        }
        
        // ディレクトリのとき、その中のファイルを削除する
        // 一つでも削除できないものがあったらfalseを返す
        for (File child: file.listFiles()) {
          if (!delete(child)) return false;
        }
        
        // ディレクトリ自体を削除する。削除できなければfalseを返す
        if (!file.delete()) return false;
        
        return true;
      }
    }.delete(file);
  }
}
