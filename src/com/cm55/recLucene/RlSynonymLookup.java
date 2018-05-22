package com.cm55.recLucene;


import java.util.*;

/**
 * 指定された語句のシノニムを取得する。
 * <p>
 * ※注意
 * </p>
 * <p>
 * これは検索時にのみ使用される。インデックス書き込み時には使用されない。
 * あらかじめフィールドに指定されたノーマライザにより正規化された文字列
 * について適用される。
 * </p>
 * @author ysugimura
 *
 */
public interface RlSynonymLookup {

  /**
   * シノニム文字列の集合を返す。
   * <p>
   * 返される文字列集合には元の文字が含まれていてもよい（含まれなくてもよい）。
   * したがって、シノニムが存在しない場合でも元の文字が格納された集合が
   * 返される場合がある。もちろん、nullや０個の集合を返してもよい。
   * </p>
   * @param string
   * @return
   */
  public Set<String>getSynonyms(String string);
}
