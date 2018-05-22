package com.cm55.recLucene;

import com.google.inject.*;

@Singleton
public class Defaults {

  public Class<? extends RlAnalyzer>analyzerClass = RlAnalyzer.JpnStandard.class;

}
