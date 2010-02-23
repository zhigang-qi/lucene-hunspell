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
  CharArrayMap<List<HunspellWord>> words;
  CharArrayMap<List<HunspellAffix>> prefixes;
  CharArrayMap<List<HunspellAffix>> suffixes;
  
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
  
  // nocommit, maybe we can use a parser-generator for this.
  private void readAffixFile(InputStream affixStream, CharsetDecoder decoder) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(affixStream, decoder));
    prefixes = new CharArrayMap<List<HunspellAffix>>(Version.LUCENE_31, 8, false);
    suffixes = new CharArrayMap<List<HunspellAffix>>(Version.LUCENE_31, 8, false);
    String line = null;
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("PFX")) {
        parseAffix(prefixes, line, reader);
      } else if (line.startsWith("SFX")) {
        parseAffix(suffixes, line, reader);
      }
    }
    reader.close();
  }
  
  private void parseAffix(CharArrayMap<List<HunspellAffix>> affixes, 
      String header, BufferedReader reader) throws IOException {
    String args[] = header.split("\\s+");
    boolean crossProduct = args[2].equals("Y");
    int numLines = Integer.parseInt(args[3]);
    for (int i = 0; i < numLines; i++) {
      String line = reader.readLine();
      String affixArgs[] = line.split("\\s+");
      HunspellAffix affix = new HunspellAffix();
      affix.setFlag(affixArgs[1].charAt(0));
      affix.setStrip(affixArgs[2].equals("0") ? "" : affixArgs[2]);
      
      int flagSep = affixArgs[3].lastIndexOf('/');
      if (flagSep != -1) {
        char appendFlags[] = affixArgs[3].substring(flagSep + 1).toCharArray();
        Arrays.sort(appendFlags);
        affix.setAppendFlags(appendFlags);
        affix.setAppend(affixArgs[3].substring(0, flagSep));
      } else {
        affix.setAppend(affixArgs[3]);
      }

      affix.setCondition(affixArgs[4]);
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
   * Read the encoding of the dictionary from the affix file.
   */
  private String getDictionaryEncoding(InputStream affix) throws IOException, ParseException {
    StringBuilder encoding = new StringBuilder();
    int ch;
    while ((ch = affix.read()) > 0) {
      if (ch == '\n')
        break;
      if (ch != '\r')
        encoding.append((char)ch);
    }
    if (encoding.substring(0, 4).equals("SET "))
      return encoding.substring(4);
    else
      throw new ParseException("expected SET <encoding>", 0);
  }
  
  /**
   * This isn't perfect as I think ISCII-DEVANAGARI and MICROSOFT-CP1251 etc are
   * allowed...
   */
  private CharsetDecoder getJavaEncoding(String encoding) {
    Charset cs = Charset.forName(encoding);
    return cs.newDecoder();
  }
  
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
        wordForm = new HunspellWord();
        wordForm.flags = line.substring(flagSep + 1).toCharArray();
        Arrays.sort(wordForm.flags);
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
  
  static class HunspellWord {
    char flags[]; // sorted, can we represent more concisely?
    
    public boolean hasFlag(char ch) {
      return flags != null && Arrays.binarySearch(flags, ch) >= 0;
    }
  }
  
  static final HunspellWord NOFLAGS = new HunspellWord();
  
  public static void main(String args[]) throws Exception {
    InputStream dic = new FileInputStream("c:/users/rmuir/Downloads/en/en_US.dic");
    InputStream aff = new FileInputStream("c:/users/rmuir/Downloads/en/en_US.aff");
    HunspellDictionary hd = new HunspellDictionary(aff, dic);
  }
}
