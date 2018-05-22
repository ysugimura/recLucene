package com.cm55.recLucene;

import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.Analyzer.*;
import org.apache.lucene.analysis.miscellaneous.*;

import com.google.inject.*;

/**
 * 
 * @author ysugimura
 */
@Singleton
public class PerFieldAnalyzerCreator {

  public Analyzer create(RlDatabase database) {
    Map<String, Analyzer> analyzerMap = new HashMap<String, Analyzer>();
    for (RlTable table: database.getTableSet().getTables()) {
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
    }    
    return new PerFieldAnalyzerWrapper(null, analyzerMap);
  }
}
