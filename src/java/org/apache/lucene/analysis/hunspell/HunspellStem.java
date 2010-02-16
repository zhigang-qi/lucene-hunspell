package org.apache.lucene.analysis.hunspell;

import java.util.Arrays;

public class HunspellStem {
  char flags[]; // sorted, can we represent more concisely?
  
  public boolean hasFlag(char ch) {
    return flags != null && Arrays.binarySearch(flags, ch) >= 0;
  }
}
