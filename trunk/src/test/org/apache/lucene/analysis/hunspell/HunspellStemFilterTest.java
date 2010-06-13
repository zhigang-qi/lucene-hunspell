package org.apache.lucene.analysis.hunspell;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;

public class HunspellStemFilterTest extends BaseTokenStreamTestCase {
  
  private class DutchAnalyzer extends Analyzer {
    private final HunspellDictionary dictionary;
    
    public DutchAnalyzer() {
      super();
      try {
      dictionary = new HunspellDictionary(
          getClass().getResourceAsStream("dicts/nl_NL/nl_NL.aff"),
          getClass().getResourceAsStream("dicts/nl_NL/nl_NL.dic"));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    
    @Override
    public TokenStream tokenStream(String field, Reader reader) {
      return new HunspellStemFilter(new StandardTokenizer(reader), dictionary);
    }

    private class SavedStreams {
      Tokenizer tokenizer;
      TokenStream filter;
    }
    
    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader)
        throws IOException {
      SavedStreams streams = (SavedStreams) getPreviousTokenStream();
      if (streams == null) {
        streams = new SavedStreams();
        streams.tokenizer = new StandardTokenizer(reader);
        streams.filter = new HunspellStemFilter(streams.tokenizer, dictionary);
        setPreviousTokenStream(streams);
      } else {
        streams.tokenizer.reset(reader);
        streams.filter.reset();
      }
      return streams.filter;
    } 
  };
  
  DutchAnalyzer dutchAnalyzer = new DutchAnalyzer();
  
  public void testDutch() throws Exception {
    assertAnalyzesTo(dutchAnalyzer, "huizen", 
        new String[] { "huizen", "huis" },
        new int[] { 1, 0 });
    assertAnalyzesTo(dutchAnalyzer, "huis", 
        new String[] { "huis", "hui" },
        new int[] { 1, 0 });
    assertAnalyzesToReuse(dutchAnalyzer, "huizen huis", 
        new String[] { "huizen", "huis", "huis", "hui" },
        new int[] { 1, 0, 1, 0 });
    assertAnalyzesToReuse(dutchAnalyzer, "huis huizen", 
        new String[] { "huis", "hui", "huizen", "huis" },
        new int[] { 1, 0, 1, 0 });
  }
}
