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
    HunspellDictionary dictionary = loadDictionary("dicts/en_US/en_US.aff", "dicts/en_US/en_US.dic");

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<HunspellStemmer.Stem> stems = stemmer.stem("drinkable");

    assertEquals(2, stems.size());
    assertEquals("drinkable", stems.get(0).getStemString());
    assertEquals("drink", stems.get(1).getStemString());
  }

  /**
   * Pass condition: Word 'remove' should be stemmed to 'move' with the prefix 're' being stripped
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_simplePrefixEnUS() throws IOException, ParseException {
    HunspellDictionary dictionary = loadDictionary("dicts/en_US/en_US.aff", "dicts/en_US/en_US.dic");

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<HunspellStemmer.Stem> stems = stemmer.stem("remove");

    assertEquals(1, stems.size());
    assertEquals("move", stems.get(0).getStemString());
  }

  /**
   * Pass condition: Word 'drinkables' should be stemmed to 'drink' with the suffixes 's' and 'able' being removed recursively
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_recursiveSuffixEnUS() throws IOException, ParseException {
    HunspellDictionary dictionary = loadDictionary("dicts/en_US/en_US.aff", "dicts/en_US/en_US.dic");

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<HunspellStemmer.Stem> stems = stemmer.stem("drinkables");

    assertEquals(1, stems.size());
    assertEquals("drink", stems.get(0).getStemString());
  }

  /**
   * Pass condition: Word 'fietsen' should be stemmed to 'fiets' ('en' suffix stripped) while fiets should be stemmed to
   *                 itself
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_fietsenFietsNlNL() throws IOException, ParseException {
    HunspellDictionary dictionary = loadDictionary("dicts/nl_NL/nl_NL.aff", "dicts/nl_NL/nl_NL.dic");

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<HunspellStemmer.Stem> stems = stemmer.stem("fietsen");

    assertEquals(2, stems.size());
    assertEquals("fietsen", stems.get(0).getStemString());
    assertEquals("fiets", stems.get(1).getStemString());

    stems = stemmer.stem("fiets");
    assertEquals(1, stems.size());
    assertEquals("fiets", stems.get(0).getStemString());
  }

  /**
   * Pass condition: Word 'huizen' should be stemmed to 'huis' ('en' suffix stripped) while huis should be stemmed to huis
   *                 and hui
   *
   * @throws IOException Can be thrown while reading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testStem_huizenHuisNlNL() throws IOException, ParseException {
    HunspellDictionary dictionary = loadDictionary("dicts/nl_NL/nl_NL.aff", "dicts/nl_NL/nl_NL.dic");

    HunspellStemmer stemmer = new HunspellStemmer(dictionary);
    List<HunspellStemmer.Stem> stems = stemmer.stem("huizen");

    assertEquals(2, stems.size());
    assertEquals("huizen", stems.get(0).getStemString());
    assertEquals("huis", stems.get(1).getStemString());

    stems = stemmer.stem("huis");
    assertEquals(2, stems.size());
    assertEquals("huis", stems.get(0).getStemString());
    assertEquals("hui", stems.get(1).getStemString());
  }

  // ================================================= Helper Methods ================================================

  /**
   * Loads the HunspellDictionary created from the affix and dic files found at the given path on the classpath
   *
   * @param affixFile Location of the affix file on the classpath
   * @param dicFile Location of the dic file on the classpath
   * @return HunspellDictionary created by loading and parsing the files
   * @throws IOException Can be thrown while loading the files
   * @throws ParseException Can be thrown while parsing the files
   */
  private HunspellDictionary loadDictionary(String affixFile, String dicFile) throws IOException, ParseException {
    InputStream affixStream = getClass().getResourceAsStream(affixFile);
    InputStream dictStream = getClass().getResourceAsStream(dicFile);

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    affixStream.close();
    dictStream.close();

    return dictionary;
  }

}
