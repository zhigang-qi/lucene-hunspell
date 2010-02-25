package org.apache.lucene.analysis.hunspell;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link org.apache.lucene.analysis.hunspell.HunspellStemmer}
 * 
 * @author Chris Male
 */
public class HunspellStemmerTest {

  /**
   * Pass condition: Word 'drinkable' should be stemmed to 'drink' with the suffix 'able' being stripped
   * 
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_simpleSuffixEnUS() throws IOException, ParseException {
    InputStream affixStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.aff");
    InputStream dictStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.dic");

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<String> stems = stemmer.stem("drinkable");

    assertEquals(1, stems.size());
    assertEquals("drink", stems.get(0));

    affixStream.close();
    dictStream.close();
  }

  /**
   * Pass condition: Word 'remove' should be stemmed to 'move' with the prefix 're' being stripped
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_simplePrefixEnUS() throws IOException, ParseException {
    InputStream affixStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.aff");
    InputStream dictStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.dic");

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<String> stems = stemmer.stem("remove");

    assertEquals(1, stems.size());
    assertEquals("move", stems.get(0));
    
    affixStream.close();
    dictStream.close();
  }

  /**
   * Pass condition: Word 'drinkables' should be stemmed to 'drink' with the suffixes 's' and 'able' being removed recursively
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_recursiveSuffixEnUS() throws IOException, ParseException {
    InputStream affixStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.aff");
    InputStream dictStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("dicts/en_US/en_US.dic");

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<String> stems = stemmer.stem("drinkables");

    assertEquals(1, stems.size());
    assertEquals("drink", stems.get(0));

    affixStream.close();
    dictStream.close();
  }


}
