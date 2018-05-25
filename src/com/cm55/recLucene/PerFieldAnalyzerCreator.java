package com.cm55.recLucene;

import java.util.*;
import java.util.stream.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.*;

/**
 * {@link RlTableSet}からLuceneの{@link Analyzer}を作成する
 * @author ysugimura
 */
class PerFieldAnalyzerCreator {

  /** 
   * {@link RlTableSet}中のすべての{@link RlClassTable}のtokenizeされるすべてのフィールドについてのそれぞれの{@link Analyzer}
   * の集めた単一の{@link Analyzer}を作成する。
   * @param tableSet
   * @return
   */
  static Analyzer create(RlTableSet tableSet) {
    return new PerFieldAnalyzerWrapper(null, 
        createStream(tableSet).collect(Collectors.toMap(e->e.getKey(), e->e.getValue()))
     );
  }
  
  /** {@link RlTableSet}からフィールド名/{@link Analyzder}のマップエントリストリームを作成する */
  static Stream<Map.Entry<String, Analyzer>>createStream(RlTableSet tableSet) {
    return tableSet.getTables().flatMap(table->createStream(table));
  }

  /** {@link RlClassTable}からフィールド名/{@link Analyzer}のマップエントリストリームを作成する */
  static Stream<Map.Entry<String, Analyzer>>createStream(RlTable<?> table) {
    return table.getFields()
      .filter(f->f.isTokenized())
      .collect(Collectors.toMap(
        f->f.getName(),
        f->new Analyzer() {
          protected TokenStreamComponents createComponents(String fieldName) {                  
            return f.getAnalyzer().createComponents();
          }
          @Override
          public String toString() {
            return "Analyzer";
          }
        }
      )).entrySet().stream();
  }
}
