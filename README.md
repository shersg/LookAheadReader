# LookAheadReader

Wrapper for any **java.io.Reader** which provides look-ahead methods (**at()**, **atSkip()**, **atWordSkip()**, **peek()**, etc) and some useful utility methods (**readChar()**, **atEOF()**, **skipWhitespaceAndComments()**).

Makes most parsing tasks easy and efficient, including parsing without a separate lexer and parsing nested grammars.

The capacity is the maximum lookahead (in number of characters). This must be specified when the **LookAheadReader** is created. **LookAheadReader** uses a fast but fixed-size ring buffer.

The intended use is parsing source code from start to finish in one thread. Not thread safe.

The javadoc are [here](https://www.sherst.net/javadoc/net/sherst/io/LookAheadReader.html).

Author:
    sherstDotNet@yahoo.com 
