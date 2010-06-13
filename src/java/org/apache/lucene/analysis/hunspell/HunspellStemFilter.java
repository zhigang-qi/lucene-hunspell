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

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.hunspell.HunspellStemmer.Stem;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * TokenFilter that uses hunspell affix rules and words to stem tokens.  Since hunspell supports a word having multiple
 * stems, this filter can emit multiple tokens for each consumed token
 */
public final class HunspellStemFilter extends TokenFilter {
  
  private final TermAttribute termAtt = 
    (TermAttribute) addAttribute(TermAttribute.class);
  private final PositionIncrementAttribute posIncAtt =
    (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
  private final HunspellStemmer stemmer;
  
  private List<Stem> buffer;
  private State savedState;
  
  private final boolean dedup;

  /**
   * Creates a new HunspellStemFilter that will stem tokens from the given TokenStream using affix rules in the provided
   * HunspellDictionary
   *
   * @param input TokenStream whose tokens will be stemmed
   * @param dictionary HunspellDictionary containing the affix rules and words that will be used to stem the tokens
   */
  public HunspellStemFilter(TokenStream input, HunspellDictionary dictionary) {
    this(input, dictionary, true);
  }
  
  /**
   * Creates a new HunspellStemFilter that will stem tokens from the given TokenStream using affix rules in the provided
   * HunspellDictionary
   *
   * @param input TokenStream whose tokens will be stemmed
   * @param dictionary HunspellDictionary containing the affix rules and words that will be used to stem the tokens
   * @param dedup true if only unique terms should be output.
   */
  public HunspellStemFilter(TokenStream input, HunspellDictionary dictionary, boolean dedup) {
    super(input);
    this.dedup = dedup;
    this.stemmer = new HunspellStemmer(dictionary);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean incrementToken() throws IOException {
    if (buffer != null && !buffer.isEmpty()) {
      Stem nextStem = buffer.remove(0);
      restoreState(savedState);
      posIncAtt.setPositionIncrement(0);
      termAtt.setTermBuffer(nextStem.getStem(), 0, nextStem.getStemLength());
      return true;
    }
    
    if (!input.incrementToken()) {
      return false;
    }
    
    buffer = dedup ? stemmer.uniqueStems(termAtt.termBuffer(), termAtt.termLength()) : stemmer.stem(termAtt.termBuffer(), termAtt.termLength());

    if (buffer.isEmpty()) { // we do not know this word, return it unchanged
      return true;
    }     

    Stem stem = buffer.remove(0);
    termAtt.setTermBuffer(stem.getStem(), 0, stem.getStemLength());

    if (!buffer.isEmpty()) {
      savedState = captureState();
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset() throws IOException {
    super.reset();
    buffer = null;
  }
}
