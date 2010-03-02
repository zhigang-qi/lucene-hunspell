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
 * Hunspell stemfilter
 */
public final class HunspellStemFilter extends TokenFilter {
  private final TermAttribute termAtt = 
    (TermAttribute) addAttribute(TermAttribute.class);
  private final PositionIncrementAttribute posIncAtt =
    (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
  private final HunspellStemmer stemmer;
  private List<Stem> buffer;
  private State savedState;
  
  public HunspellStemFilter(TokenStream input, HunspellDictionary dictionary) {
    super(input);
    this.stemmer = new HunspellStemmer(dictionary); 
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (buffer != null && !buffer.isEmpty()) {
      Stem nextStem = buffer.remove(0);
      restoreState(savedState);
      posIncAtt.setPositionIncrement(0);
      termAtt.setTermBuffer(nextStem.getStem());
      return true;
    } else {
      if (input.incrementToken()) {
        buffer = stemmer.stem(termAtt.term());
        if (buffer.isEmpty()) // we do not know this word, return it unchanged
          return true;
        
        termAtt.setTermBuffer(buffer.remove(0).getStem());

        if (!buffer.isEmpty())
          savedState = captureState();
        
        return true;
      } else
        return false;
    }
  }
  
  @Override
  public void reset() throws IOException {
    super.reset();
    buffer = null;
  }
}
