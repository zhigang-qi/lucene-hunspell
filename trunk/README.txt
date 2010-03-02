Solr use:

1. add the lucene-hunspell jar file to your Solr home lib/ directory.
2. go to http://wiki.services.openoffice.org/wiki/Dictionaries and 
   download a dictionary for your language.
3. place the .dic/.aff files into your conf/ directory.

in your schema, use the solr.HunspellStemFilterFactory.

Examples:

<fieldtype name="ukrainian" stored="false" indexed="true" class="solr.TextField" >
  <analyzer>
    <tokenizer class="solr.StandardTokenizerFactory"/>
    <filter class="solr.LowerCaseFilterFactory"/>
    <filter class="solr.HunspellStemFilterFactory"
            dictionary="uk_UA.dic"
            affix="uk_UA.aff"/>
  </analyzer>
</fieldtype>

<fieldtype name="nepali" stored="false" indexed="true" class="solr.TextField" >
  <analyzer>
    <tokenizer class="solr.WhitespaceTokenizerFactory"/>
    <filter class="solr.WordDelimiterFilterFactory"
         generateWordParts="1" generateNumberParts="1" catenateWords="0"
         catenateNumbers="0" catenateAll="0" splitOnCaseChange="0"/>
    <filter class="solr.HunspellStemFilterFactory"
            dictionary="ne_NP.dic"
            affix="ne_NP.aff"/>
  </analyzer>
</fieldtype>

Lucene Use:

You can create a stemmer of your own with code like this:
    InputStream aff = new FileInputStream(new File("foo.aff"));
    InputStream dic = new FileInputStream(new File("foo.dic"));
    HunspellDictionary dictionary = new HunspellDictionary(aff, dic);
    TokenStream ts = new HunspellStemFilter(someTokenStream, dictionary);

