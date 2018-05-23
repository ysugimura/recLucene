package com.cm55.recLucene;

import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.miscellaneous.*;

/**
 * 
 * @author ysugimura
 */
class PerFieldAnalyzerCreator {
  
  static Analyzer create(RlTableSet tableSet) {
    Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
    tableSet.getTables().forEach(table-> {
      for (RlField f: table.getFields()) {
        final RlField field = f;
        if (!field.isTokenized()) continue;
        Analyzer analyzer = new Analyzer() {
          protected TokenStreamComponents createComponents(String fieldName) {                  
            return field.getAnalyzer().createComponents();
          }
        };
        analyzerMap.put(field.getName(), analyzer);        
      }     
    });    
    return new PerFieldAnalyzerWrapper(null, analyzerMap);
  }
}
