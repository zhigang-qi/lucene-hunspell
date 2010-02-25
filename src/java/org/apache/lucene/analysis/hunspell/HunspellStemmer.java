package org.apache.lucene.analysis.hunspell;

import java.util.List;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Chris Male
 */
public class HunspellStemmer {

  private HunspellDictionary dictionary;

  /**
   * Constructs a new HunspellStemmer which will use the provided HunspellDictionary to create its stems
   *
   * @param dictionary HunspellDictionary that will be used to create the stems
   */
  public HunspellStemmer(HunspellDictionary dictionary) {
    this.dictionary = dictionary;
  }

  /**
   * Find the stem(s) of the provided word
   * 
   * @param word Word to find the stems for
   *
   * TODO (cmale) decide what this is going to return
   */
  public void stem(String word) {
    System.out.println("stem(" + word + ")");
    char[] array = word.toCharArray();
    
    for (int i = 0; i < array.length; i++) {
      List<HunspellAffix> suffixes = dictionary.lookupSuffix(array, i, array.length - i);
      if (suffixes != null) {
        for (HunspellAffix affix : suffixes) {
          applySuffix(array, affix);
        }
      }
    }
  }

  // ================================================= Helper Methods ================================================

  private void applySuffix(char[] word, HunspellAffix affix) {
    int deAffixLength = word.length - affix.getAppend().length();
    
    if (!affix.checkCondition(word, 0, deAffixLength)) {
      return;
    }
    
    List<HunspellWord> words = dictionary.lookupWord(word, 0, word.length - affix.getAppend().length());
    if (words == null) {
      return;
    }
    
    for (HunspellWord hunspellWord : words) {
      if (hunspellWord.hasFlag(affix.getFlag())) {
        System.out.println("Found stem " + new String(word, 0, word.length - affix.getAppend().length()));
      }
    }
  }
}
