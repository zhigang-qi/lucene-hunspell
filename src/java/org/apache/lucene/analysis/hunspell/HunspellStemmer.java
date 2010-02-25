package org.apache.lucene.analysis.hunspell;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

/**
 * HunspellStemmer uses the affix rules declared in the HunspellDictionary to generate one or more stems for a word.  It
 * conforms to the algorithm in the original hunspell algorithm, including recursive suffix stripping.
 * 
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
    List<String> stems = new ArrayList<String>();
    if (dictionary.lookupWord(word.toCharArray(), 0, word.length()) != null) {
      stems.add(word);
    }
    stems.addAll(stem(word, null));
    return stems;
  }

  // ================================================= Helper Methods ================================================

  /**
   * Generates a list of stems for the provided word
   *
   * @param word Word to generate the stems for
   * @param flags Flags from a previous stemming step that need to be cross-checked with any affixes in this recursive step
   * @return List of stems, pr an empty if no stems are found
   */
  private List<String> stem(String word, char[] flags) {
    char[] array = word.toCharArray();

    List<String> stems = new ArrayList<String>();

    for (int i = 0; i < array.length; i++) {
      List<HunspellAffix> suffixes = dictionary.lookupSuffix(array, i, array.length - i);
      if (suffixes != null) {
        for (HunspellAffix suffix : suffixes) {
          if (hasCrossCheckedFlag(suffix.getFlag(), flags)) {
            int deAffixedLength = array.length - suffix.getAppend().length();
            String strippedWord = new StringBuilder().append(array, 0, deAffixedLength).append(suffix.getStrip()).toString();
            stems.addAll(applyAffix(strippedWord, suffix));
          }
        }
      }
    }

    for (int i = array.length - 1; i >= 0; i--) {
      List<HunspellAffix> prefixes = dictionary.lookupPrefix(array, 0, i);
      if (prefixes != null) {
        for (HunspellAffix prefix : prefixes) {
          if (hasCrossCheckedFlag(prefix.getFlag(), flags)) {
            int deAffixedStart = prefix.getAppend().length();
            int deAffixedLength = array.length - deAffixedStart;

            String strippedWord = new StringBuilder().append(prefix.getStrip())
                .append(array, deAffixedStart, deAffixedLength)
                .toString();
            stems.addAll(applyAffix(strippedWord, prefix));
          }
        }
      }
    }

    return stems;
  }

  /**
   * Applies the affix rule to the given word, producing a list of stems if any are found
   *
   * @param strippedWord Word the affix has been removed and the strip added
   * @param affix HunspellAffix representing the affix rule itself
   * @return List of stems for the word, or an empty list if none are found
   */
  @SuppressWarnings("unchecked")
  public List<String> applyAffix(String strippedWord, HunspellAffix affix) {
    char[] word = strippedWord.toCharArray();

    if (!affix.checkCondition(word, 0, word.length)) {
      return Collections.EMPTY_LIST;
    }

    List<HunspellWord> words = dictionary.lookupWord(word, 0, word.length);
    if (words == null) {
      return Collections.EMPTY_LIST;
    }

    List<String> stems = new ArrayList<String>();

    for (HunspellWord hunspellWord : words) {
      if (hunspellWord.hasFlag(affix.getFlag())) {
        if (affix.isCrossProduct()) {
          List<String> recursiveStems = stem(strippedWord, affix.getAppendFlags());
          if (!recursiveStems.isEmpty()) {
            stems.addAll(recursiveStems);
          } else {
            stems.add(strippedWord);
          }
        } else {
          stems.add(strippedWord);
        }
      }
    }

    return stems;
  }

  /**
   * Checks if the given flag cross checks with the given array of flags
   *
   * @param flag Flag to cross check with the array of flags
   * @param flags Array of flags to cross check against.  Can be {@code null}
   * @return {@code true} if the flag is found in the array or the array is {@code null}, {@code false} otherwise
   */
  private boolean hasCrossCheckedFlag(char flag, char[] flags) {
    return flags == null || Arrays.binarySearch(flags, flag) >= 0;
  }

  // ================================================= Entry Point ===================================================

  /**
   * HunspellStemmer entry point.  Accepts two arguments: location of affix file and location of dic file
   *
   * @param args Program arguments.  Should contain location of affix file and location of dic file
   * @throws IOException Can be thrown while reading from the files
   * @throws ParseException Can be thrown while parsing the files
   */
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
