package org.apache.lucene.analysis.hunspell;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link org.apache.lucene.analysis.hunspell.HunspellDictionary}
 * 
 * @author Chris Male
 */
public class HunspellDictionaryTest {

  /**
   * Pass condition: en_US affix and dict files are loaded without error, with 2 suffixes for 'ings' being loaded,
   *                 2 prefixes for 'in' and 1 word for 'drink' 
   * @throws IOException Can be thrown while reading from the aff and dic files
   * @throws ParseException Can be thrown while parsing the files
   */
  @Test
  public void testHunspellDictionary_loadEnUSDict() throws IOException, ParseException {
    InputStream affixStream = getClass().getResourceAsStream("/dicts/en_US/en_US.aff");
    InputStream dictStream = getClass().getResourceAsStream("/dicts/en_US/en_US.dic");

    HunspellDictionary dictionary = new HunspellDictionary(affixStream, dictStream);

    assertEquals(2, dictionary.lookupSuffix(new char[]{'i', 'n', 'g', 's'}, 0, 4).size());
    assertEquals(1, dictionary.lookupPrefix(new char[]{'i', 'n'}, 0, 2).size());
    assertEquals(1, dictionary.lookupWord(new char[]{'d', 'r', 'i', 'n', 'k'}, 0, 5).size());

    affixStream.close();
    dictStream.close();
  }
}
