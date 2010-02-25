package org.apache.lucene.analysis.hunspell;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

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
   * @return List of stems for the word
   */
  public List<String> stem(String word) {
    return stem(word, null);
  }

  // ================================================= Helper Methods ================================================

  // TODO (cmale) replace word with a char array + index + offset
  public List<String> stem(String word, char[] flags) {
    char[] array = word.toCharArray();

    List<String> stems = new ArrayList<String>();

    for (int i = 0; i < array.length; i++) {
      List<HunspellAffix> suffixes = dictionary.lookupSuffix(array, i, array.length - i);
      if (suffixes != null) {
        for (HunspellAffix suffix : suffixes) {
          if (hasCrossCheckedFlag(suffix.getFlag(), flags)) {
            stems.addAll(applySuffix(array, suffix));
          }
        }
      }
    }

    for (int i = array.length - 1; i >= 0; i--) {
      List<HunspellAffix> prefixes = dictionary.lookupPrefix(array, 0, i);
      if (prefixes != null) {
        for (HunspellAffix prefix : prefixes) {
          if (hasCrossCheckedFlag(prefix.getFlag(), flags)) {
            stems.addAll(applyPrefix(array, prefix));
          }
        }
      }
    }

    return stems;
  }

  @SuppressWarnings("unchecked")
  private List<String> applyPrefix(char[] word, HunspellAffix prefix) {
    int deAffixStart = prefix.getAppend().length();
    int deAffixLength = word.length - deAffixStart;

    if (!prefix.checkCondition(word, deAffixStart, deAffixLength)) {
      return Collections.EMPTY_LIST;
    }

    List<HunspellWord> words = dictionary.lookupWord(word, deAffixStart, deAffixLength);
    if (words == null) {
      return Collections.EMPTY_LIST;
    }

    List<String> stems = new ArrayList<String>();

    for (HunspellWord hunspellWord : words) {
      if (hunspellWord.hasFlag(prefix.getFlag())) {
        if (prefix.isCrossProduct()) {
          List<String> recursiveStems = stem(new String(word, deAffixStart, deAffixLength), prefix.getAppendFlags());
          if (!recursiveStems.isEmpty()) {
            stems.addAll(recursiveStems);
          } else {
            stems.add(new String(word, deAffixStart, deAffixLength));
          }
        } else {
          stems.add(new String(word, deAffixStart, deAffixLength));
        }
      }
    }

    return stems;
  }

  @SuppressWarnings("unchecked")
  private List<String> applySuffix(char[] word, HunspellAffix suffix) {
    int deAffixLength = word.length - suffix.getAppend().length();
    
    if (!suffix.checkCondition(word, 0, deAffixLength)) {
      return Collections.EMPTY_LIST;
    }
    
    List<HunspellWord> words = dictionary.lookupWord(word, 0, deAffixLength);
    if (words == null) {
      return Collections.EMPTY_LIST;
    }

    List<String> stems = new ArrayList<String>();

    for (HunspellWord hunspellWord : words) {
      if (hunspellWord.hasFlag(suffix.getFlag())) {
        if (suffix.isCrossProduct()) {
          List<String> recursiveStems = stem(new String(word, 0, deAffixLength), suffix.getAppendFlags());
          if (!recursiveStems.isEmpty()) {
            stems.addAll(recursiveStems);
          } else {
            stems.add(new String(word, 0, deAffixLength));
          }
        } else {
          stems.add(new String(word, 0, deAffixLength));
        }
      }
    }

    return stems;
  }

  private boolean hasCrossCheckedFlag(char flag, char[] flags) {
    return flags == null || Arrays.binarySearch(flags, flag) >= 0;
  }

  // ================================================= Entry Point ===================================================

  public static void main(String[] args) throws IOException, ParseException {
    if (args.length != 2) {
      System.out.println("usage: HunspellStemmer <affix location> <dic location>");
    }

    InputStream affixInputStream = new FileInputStream(args[0]);
    InputStream dicInputStream = new FileInputStream(args[1]);

    HunspellDictionary dictionary = new HunspellDictionary(affixInputStream, dicInputStream);

    affixInputStream.close();
    dicInputStream.close();
    
    HunspellStemmer stemmer = new HunspellStemmer(dictionary);

    Scanner scanner = new Scanner(System.in);
    
    System.out.print("> ");
    while (scanner.hasNextLine()) {
      String word = scanner.nextLine();
      if ("exit".equals(word)) {
        break;
      }
      System.out.println("stem(" + word + ")");
      List<String> stems = stemmer.stem(word);
      for (String stem : stems) {
        System.out.println("- " + stem);
      }
      System.out.print("> ");
    }
  }
}
