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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.CharArrayMap;
import org.apache.lucene.util.Version;

public class HunspellDictionary {

  static final HunspellWord NOFLAGS = new HunspellWord();
  
  private static final String PREFIX_KEY = "PFX";
  private static final String SUFFIX_KEY = "SFX";

  private CharArrayMap<List<HunspellWord>> words;
  private CharArrayMap<List<HunspellAffix>> prefixes;
  private CharArrayMap<List<HunspellAffix>> suffixes;

  /**
   * Creates a new HunspellDictionary containing the information read from the provided InputStreams to hunspell affix
   * and dictionary files
   *
   * @param affix InputStream for reading the hunspell affix file
   * @param dictionary InputStream for reading the hunspell dictionary file
   * @throws IOException Can be thrown while reading from the InputStreams
   * @throws ParseException Can be thrown if the content of the files does not meet expected formats
   */
  public HunspellDictionary(InputStream affix, InputStream dictionary) throws IOException, ParseException {
    String encoding = getDictionaryEncoding(affix);
    CharsetDecoder decoder = getJavaEncoding(encoding);
    readAffixFile(affix, decoder);
    readDictionaryFile(dictionary, decoder);
  }

  public List<HunspellWord> lookupWord(char word[], int offset, int length) {
    return words.get(word, offset, length);
  }

  public List<HunspellAffix> lookupPrefix(char word[], int offset, int length) {
    return prefixes.get(word, offset, length);
  }

  public List<HunspellAffix> lookupSuffix(char word[], int offset, int length) {
    return suffixes.get(word, offset, length);
  }

  // ================================================= Helper Methods ================================================

  /**
   * Reads the affix file through the provided InputStream, building up the prefix and suffix maps
   *
   * @param affixStream InputStream to read the content of the affix file from
   * @param decoder CharsetDecoder to decode the content of the file
   * @throws IOException Can be thrown while reading from the InputStream
   */
  private void readAffixFile(InputStream affixStream, CharsetDecoder decoder) throws IOException {
    prefixes = new CharArrayMap<List<HunspellAffix>>(Version.LUCENE_31, 8, false);
    suffixes = new CharArrayMap<List<HunspellAffix>>(Version.LUCENE_31, 8, false);
    
    BufferedReader reader = new BufferedReader(new InputStreamReader(affixStream, decoder));
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith(PREFIX_KEY)) {
        parseAffix(prefixes, line, reader);
      } else if (line.startsWith(SUFFIX_KEY)) {
        parseAffix(suffixes, line, reader);
      }
    }
    reader.close();
  }

  /**
   * Parses a specific affix rule putting the result into the provided affix map
   * 
   * @param affixes Map where the result of the parsing will be put
   * @param header Header line of the affix rule
   * @param reader BufferedReader to read the content of the rule from
   * @throws IOException Can be thrown while reading the rule
   */
  private void parseAffix(CharArrayMap<List<HunspellAffix>> affixes,
                          String header,
                          BufferedReader reader) throws IOException {
    String args[] = header.split("\\s+");

    boolean crossProduct = args[2].equals("Y");
    
    int numLines = Integer.parseInt(args[3]);
    for (int i = 0; i < numLines; i++) {
      String line = reader.readLine();
      String ruleArgs[] = line.split("\\s+");

      HunspellAffix affix = new HunspellAffix();
      
      affix.setFlag(ruleArgs[1].charAt(0));
      affix.setStrip(ruleArgs[2].equals("0") ? "" : ruleArgs[2]);

      String affixArg = ruleArgs[3];
      
      int flagSep = affixArg.lastIndexOf('/');
      if (flagSep != -1) {
        char appendFlags[] = affixArg.substring(flagSep + 1).toCharArray();
        Arrays.sort(appendFlags);
        affix.setAppendFlags(appendFlags);
        affix.setAppend(affixArg.substring(0, flagSep));
      } else {
        affix.setAppend(affixArg);
      }

      affix.setCondition(ruleArgs[4]);
      affix.setCrossProduct(crossProduct);
      
      List<HunspellAffix> list = affixes.get(affix.getAppend());
      if (list == null) {
        list = new ArrayList<HunspellAffix>();
        affixes.put(affix.getAppend(), list);
      }
      
      list.add(affix);
    }
  }

  /**
   * Parses the encoding specificed in the affix file readable through the provided InputStream
   *
   * @param affix InputStream for reading the affix file
   * @return Encoding specified in the affix file
   * @throws IOException Can be thrown while reading from the InputStream
   * @throws ParseException Thrown if the line read from the file does not adhere to the format {@code SET <encoding>}
   */
  private String getDictionaryEncoding(InputStream affix) throws IOException, ParseException {
    StringBuilder encoding = new StringBuilder();
    int ch;
    while ((ch = affix.read()) > 0) {
      if (ch == '\n') {
        break;
      }
      if (ch != '\r') {
        encoding.append((char)ch);
      }
    }
    if ("SET ".equals(encoding.substring(0, 4))) {
      return encoding.substring(4);
    }
    else {
      throw new ParseException("expected SET <encoding>", 0);
    }
  }

  /**
   * Retrieves the CharsetDecoder for the given encoding.  Note, This isn't perfect as I think ISCII-DEVANAGARI and
   * MICROSOFT-CP1251 etc are allowed...
   *
   * @param encoding Encoding to retrieve the CharsetDecoder for
   * @return CharSetDecoder for the given encoding
   */
  private CharsetDecoder getJavaEncoding(String encoding) {
    Charset cs = Charset.forName(encoding);
    return cs.newDecoder();
  }

  /**
   * Reads the dictionary file through the provided InputStream, building up the words map
   *
   * @param dictionary InputStream to read the dictionary file through
   * @param decoder CharsetDecoder used to decode the contents of the file
   * @throws IOException Can be thrown while reading from the file
   */
  private void readDictionaryFile(InputStream dictionary, CharsetDecoder decoder) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(dictionary, decoder));
    // nocommit, don't create millions of strings.
    String line = reader.readLine(); // first line is number of entries
    int numEntries = Integer.parseInt(line);
    words = new CharArrayMap<List<HunspellWord>>(Version.LUCENE_31, numEntries, false);
    // nocommit, the flags themselves can be double-chars (long) or also numeric
    // either way the trick is to encode them as char... but they must be parsed differently
    while ((line = reader.readLine()) != null) {
      String entry;
      HunspellWord wordForm;
      
      int flagSep = line.lastIndexOf('/');
      if (flagSep == -1) {
        wordForm = NOFLAGS;
        entry = line;
      } else {
        wordForm = new HunspellWord(line.substring(flagSep + 1).toCharArray());
        Arrays.sort(wordForm.getFlags());
        entry = line.substring(0, flagSep);
      }
      
      List<HunspellWord> entries = words.get(entry);
      if (entries == null) {
        entries = new ArrayList<HunspellWord>();
        words.put(entry, entries);
      }
      entries.add(wordForm);
    }
  }

  // ================================================= Entry Point ===================================================

  public static void main(String args[]) throws Exception {
    InputStream dic = new FileInputStream("c:/users/rmuir/Downloads/en/en_US.dic");
    InputStream aff = new FileInputStream("c:/users/rmuir/Downloads/en/en_US.aff");
    HunspellDictionary hd = new HunspellDictionary(aff, dic);
  }
}