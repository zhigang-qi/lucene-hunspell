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

import org.apache.lucene.util.Version;

/**
 * HunspellStemmer uses the affix rules declared in the HunspellDictionary to generate one or more stems for a word.  It
 * conforms to the algorithm in the original hunspell algorithm, including recursive suffix stripping.
 * 
 * @author Chris Male
 */
public class HunspellStemmer {

  private static final int RECURSION_CAP = 2;
  
  private HunspellDictionary dictionary;
  private final StringBuilder segment = new StringBuilder();

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
  public List<Stem> stem(String word) {
    return stem(word.toCharArray(), word.length());
  }

  /**
   * Find the stem(s) of the provided word
   * 
   * @param word Word to find the stems for
   * @return List of stems for the word
   */
  public List<Stem> stem(char word[], int length) {
    List<Stem> stems = new ArrayList<Stem>();
    if (dictionary.lookupWord(word, 0, length) != null) {
      stems.add(new Stem(word, length));
    }
    stems.addAll(stem(word, length, null, 0));
    return stems;
  }
  
  /**
   * Find the unique stem(s) of the provided word
   * 
   * @param word Word to find the stems for
   * @return List of stems for the word
   */
  public List<Stem> uniqueStems(char word[], int length) {
    List<Stem> stems = new ArrayList<Stem>();
    CharArraySet terms = new CharArraySet(Version.LUCENE_29, 8, false);
    if (dictionary.lookupWord(word, 0, length) != null) {
      stems.add(new Stem(word, length));
      terms.add(word);
    }
    List<Stem> otherStems = stem(word, length, null, 0);
    for (int i = 0; i < otherStems.size(); i++) {
      Stem s = otherStems.get(i);
      if (!terms.contains(s.stem)) {
        stems.add(s);
        terms.add(s.stem);
      }
    }
    return stems;
  }

  // ================================================= Helper Methods ================================================

  /**
   * Generates a list of stems for the provided word
   *
   * @param word Word to generate the stems for
   * @param flags Flags from a previous stemming step that need to be cross-checked with any affixes in this recursive step
   * @param recursionDepth Level of recursion this stemming step is at
   * @return List of stems, pr an empty if no stems are found
   */
  private List<Stem> stem(char word[], int length, char[] flags, int recursionDepth) {
    List<Stem> stems = new ArrayList<Stem>();

    for (int i = 0; i < length; i++) {
      List<HunspellAffix> suffixes = dictionary.lookupSuffix(word, i, length - i);
      if (suffixes != null) {
        for (HunspellAffix suffix : suffixes) {
          if (hasCrossCheckedFlag(suffix.getFlag(), flags)) {
            int deAffixedLength = length - suffix.getAppend().length();
            // TODO: can we do this in-place?
            String strippedWord = new StringBuilder().append(word, 0, deAffixedLength).append(suffix.getStrip()).toString();

            List<Stem> stemList = applyAffix(strippedWord.toCharArray(), strippedWord.length(), suffix, recursionDepth);
            for (Stem stem : stemList) {
              stem.addSuffix(suffix);
            }

            stems.addAll(stemList);
          }
        }
      }
    }

    for (int i = length - 1; i >= 0; i--) {
      List<HunspellAffix> prefixes = dictionary.lookupPrefix(word, 0, i);
      if (prefixes != null) {
        for (HunspellAffix prefix : prefixes) {
          if (hasCrossCheckedFlag(prefix.getFlag(), flags)) {
            int deAffixedStart = prefix.getAppend().length();
            int deAffixedLength = length - deAffixedStart;

            String strippedWord = new StringBuilder().append(prefix.getStrip())
                .append(word, deAffixedStart, deAffixedLength)
                .toString();

            List<Stem> stemList = applyAffix(strippedWord.toCharArray(), strippedWord.length(), prefix, recursionDepth);
            for (Stem stem : stemList) {
              stem.addPrefix(prefix);  
            }

            stems.addAll(stemList);
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
   * @param recursionDepth Level of recursion this stemming step is at
   * @return List of stems for the word, or an empty list if none are found
   */
  @SuppressWarnings("unchecked")
  public List<Stem> applyAffix(char strippedWord[], int length, HunspellAffix affix, int recursionDepth) {
    segment.setLength(0);
    segment.append(strippedWord, 0, length);
    if (!affix.checkCondition(segment)) {
      return Collections.EMPTY_LIST;
    }

    List<HunspellWord> words = dictionary.lookupWord(strippedWord, 0, length);
    if (words == null) {
      return Collections.EMPTY_LIST;
    }

    List<Stem> stems = new ArrayList<Stem>();

    for (HunspellWord hunspellWord : words) {
      if (hunspellWord.hasFlag(affix.getFlag())) {
        if (affix.isCrossProduct() && recursionDepth < RECURSION_CAP) {
          List<Stem> recursiveStems = stem(strippedWord, length, affix.getAppendFlags(), ++recursionDepth);
          if (!recursiveStems.isEmpty()) {
            stems.addAll(recursiveStems);
          } else {
            stems.add(new Stem(strippedWord, length));
          }
        } else {
          stems.add(new Stem(strippedWord, length));
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

  // ================================================= Helper Methods ================================================

  /**
   * Stem represents all information known about a stem of a word.  This includes the stem, and the prefixes and suffixes
   * that were used to change the word into the stem.
   */
  public static class Stem {

    private final List<HunspellAffix> prefixes = new ArrayList<HunspellAffix>();
    private final List<HunspellAffix> suffixes = new ArrayList<HunspellAffix>();
    private final char stem[];
    private final int stemLength;

    /**
     * Creates a new Stem wrapping the given word stem
     *
     * @param stem Stem of a word
     */
    public Stem(char stem[], int stemLength) {
      this.stem = stem;
      this.stemLength = stemLength;
    }

    /**
     * Adds a prefix to the list of prefixes used to generate this stem.  Because it is assumed that prefixes are added
     * depth first, the prefix is added to the front of the list
     *
     * @param prefix Prefix to add to the list of prefixes for this stem
     */
    public void addPrefix(HunspellAffix prefix) {
      prefixes.add(0, prefix);
    }

    /**
     * Adds a suffix to the list of suffixes used to generate this stem.  Because it is assumed that suffixes are added
     * depth first, the suffix is added to the end of the list
     *
     * @param suffix Suffix to add to the list of suffixes for this stem
     */
    public void addSuffix(HunspellAffix suffix) {
      suffixes.add(suffix);
    }

    /**
     * Returns the list of prefixes used to generate the stem
     *
     * @return List of prefixes used to generate the stem or an empty list if no prefixes were required
     */
    public List<HunspellAffix> getPrefixes() {
      return prefixes;
    }

    /**
     * Returns the list of suffixes used to generate the stem
     *
     * @return List of suffixes used to generate the stem or an empty list if no suffixes were required
     */
    public List<HunspellAffix> getSuffixes() {
      return suffixes;
    }

    /**
     * Returns the actual word stem itself
     *
     * @return Word stem itself
     */
    public char[] getStem() {
      return stem;
    }

    /**
     * @return the stemLength
     */
    public int getStemLength() {
      return stemLength;
    }
    
    public String getStemString() {
      return new String(stem, 0, stemLength);
    }
    
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
      System.exit(1);
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

      printStemResults(word, stemmer.stem(word.toCharArray(), word.length()));
      
      System.out.print("> ");
    }
  }

  /**
   * Prints the results of the stemming of a word
   *
   * @param originalWord Word that has been stemmed
   * @param stems Stems of the word
   */
  private static void printStemResults(String originalWord, List<Stem> stems) {
    StringBuilder builder = new StringBuilder().append("stem(").append(originalWord).append(")").append("\n");

    for (Stem stem : stems) {
      builder.append("- ").append(stem.getStem()).append(": ");

      for (HunspellAffix prefix : stem.getPrefixes()) {
        builder.append(prefix.getAppend()).append("+");

        if (hasText(prefix.getStrip())) {
          builder.append(prefix.getStrip()).append("-");
        }
      }

      builder.append(stem.getStem());

      for (HunspellAffix suffix : stem.getSuffixes()) {
        if (hasText(suffix.getStrip())) {
          builder.append("-").append(suffix.getStrip());
        }
        
        builder.append("+").append(suffix.getAppend());
      }
      builder.append("\n");
    }

    System.out.println(builder);
  }

  /**
   * Simple utility to check if the given String has any text
   *
   * @param str String to check if it has any text
   * @return {@code true} if the String has text, {@code false} otherwise
   */
  private static boolean hasText(String str) {
    return str != null && str.length() > 0;
  }
}
