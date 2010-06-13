package org.apache.lucene.analysis.hunspell;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
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
      return new HunspellStemFilter(new LowerCaseFilter(new StandardTokenizer(reader)), dictionary);
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
        streams.filter = new HunspellStemFilter(new LowerCaseFilter(streams.tokenizer), dictionary);
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
  
  String text = "Op grond daarvan proclameert de Algemene Vergadering deze Universele Verklaring van de Rechten van de Mens als het gemeenschappelijk door alle volkeren en alle naties te bereiken ideaal, opdat ieder individu en elk orgaan van de gemeenschap, met deze verklaring voortdurend voor ogen, er naar zal streven door onderwijs en opvoeding de eerbied voor deze rechten en vrijheden te bevorderen, en door vooruitstrevende maatregelen, op nationaal en internationaal terrein, deze rechten algemeen en daadwerkelijk te doen erkennen en toepassen, zowel onder de volkeren van Staten die Lid van de Verenigde Naties zijn, zelf, als onder de volkeren van gebieden, die onder hun jurisdictie staan";
  
  public void testPerformance() throws Exception {
    int numIterations = 100000000;
    Reader r = new StringReader(text);
    long startMS = System.currentTimeMillis();
    for (int i = 0; i < numIterations; i++) {
      TokenStream ts = dutchAnalyzer.reusableTokenStream("foobar", r);
      ts.reset();
      while (ts.incrementToken())
        ;
    }
    long endMS = System.currentTimeMillis();
    double rate = (endMS - startMS);
    System.err.println("rate: " + rate);
  }
}
