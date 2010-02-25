package org.apache.lucene.analysis.hunspell;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

/**
 * Tests for {@link org.apache.lucene.analysis.hunspell.HunspellStemmer}
 * 
 * @author Chris Male
 */
public class HunspellStemmerTest {

  @Test
  public void testStem() throws IOException, ParseException {
    InputStream affixStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.aff");
    InputStream dictStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.dic");

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    stemmer.stem("drinkable");

    affixStream.close();
    dictStream.close();
  }
}
