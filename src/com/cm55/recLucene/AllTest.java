package com.cm55.recLucene;

import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Suite.*;

@RunWith(Suite.class) 
@SuiteClasses( { 
  DuplicatedIdTest.class,
  RlAnalyzerTest.class,
  RlAnyTableTest.class,
  RlDatabaseDirTest.class,
  RlDatabaseResetTest.class,
  RlDatabaseTest.class,
  RlFieldConverterTest.class,
  RlFieldTest.class,
  RlQueryTest.class,
  RlSearcherTest.class,
  RlSemaphoreTest.class,
  RlSemaphoreMultiTest.class,
  RlTableTest.class,
  RlTableSetTest.class, 
  RlValuesTest.class,
  RlValuesTotalTest.class,
  RlWriterTest.class,
  LuceneAnalyzerTest.class,
  LuceneTokenizerTest.class,
})
public class AllTest {
  public static void main(String[] args) {
    JUnitCore.main(AllTest.class.getName());
  }
}