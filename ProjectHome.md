100% pure java project to integrate hunspell .aff/.dic files with Apache Lucene.

We aim to provide features such as stemming, decompounding, spellchecking, normalization, term expansion, etc. that take advantage of the existing lexical resources already created and widely-used in projects like OpenOffice ([Available support by language](http://wiki.services.openoffice.org/wiki/Dictionaries))

These files are commonly used for spellchecking purposes, but many have a wide range of uses for word analysis, and the necessarily language-specific support is represented in the files themselves.

For more background on how these resources can be used for open-source word analysis, see these papers:
  * [Hunmorph: open source word analysis](http://www.ldc.upenn.edu/Catalog/docs/LDC2008T01/acl05software.pdf)
  * [Leveraging the open source ispell codebase for minority language analysis](http://www.ldc.upenn.edu/Catalog/docs/LDC2008T01/saltmil04szsz.pdf)
  * [Open Source morphological analyzer](http://www.ldc.upenn.edu/Catalog/docs/LDC2008T01/acta04.pdf)

Our goal is to brainstorm and develop here, but to finally create patches that can be integrated into Lucene/Solr.

_Note: we do not aim to produce or include any of these language support files, which are available under a variety of licenses and maintained elsewhere. We aim to produce the language-independent code to support the file format, with features geared specifically towards full-text search._