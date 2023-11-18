/*
Copyright (c) 2022-2023 Steve Shering

All rights reserved.

As a special exception, the copyright holder of this software gives you permission
to use this software for personal, not-for-profit purposes.

For any other purpose, a license must be obtained from the copyright holder.

This copyright notice and this permission notice must be included in all copies 
of this software, including copies of parts of this software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHOR OR COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package net.sherst.io;

import java.io.IOException;
import java.io.Reader;

/**
 * Wrapper for any {@link java.io.Reader}
 * which provides look-ahead methods ({@code at}, {@code atSkip}, {@code atWordSkip}, {@code peek})
 * and some useful utility methods ({@link #readChar}, {@link #atEOF}, {@link #skipWhitespaceAndComments()}).
 * <p>
 * Makes most parsing tasks easy and efficient,
 * including parsing without a separate lexer 
 * and parsing nested grammars.
 * <p>
 * The capacity is the maximum lookahead (in number of characters).
 * This must be specified when the {@link LookAheadReader} is created.
 * {@link LookAheadReader} uses a fast but fixed-size ring buffer.
 * @author Steve Shering
 */
public class LookAheadReader extends Reader {
	private char[] arr;
	private int capacity=0;
	private boolean eof=false;
	private int head=0;
	private int mark=-1;
	private int count=0;
	private Reader reader;
	private int tail=0;
	
	static public boolean isDecimalDigit(char c) {
    return (c>='0') && (c<='9');
    }
	
	static public boolean isIdentifierChar(char c) {
    return isIdentifierStart(c) || isDecimalDigit(c);
    }
	
	static public boolean isIdentifierStart(char c) {
    return ((c>='A')&&(c<='Z')) || (c=='_')||((c>='a')&&(c<='z')||(c=='-'));
    }
	
	public static boolean isWhitespace(char c) {
    return (c==' ')||(c=='\n')||(c=='\r')||(c=='\t')||(c==65279);
    }

	/**
	 * Creates a new {@link LookAheadReader} 
	 * which wraps the provided {@link java.io.Reader}
	 * with the default lookahead limit of 128 characters.
	 * 
	 * @param reader the {@link java.io.Reader} to wrap
	 */
	public LookAheadReader(Reader reader) {
		this(reader, 128);
	  } 

	/**
	 * Creates a new {@link LookAheadReader} 
	 * which wraps the provided {@link java.io.Reader}
	 * with specified lookahead limit.
	 * 
	 * @param reader the {@link java.io.Reader} to wrap
	 * @param capacity the look ahead limit (number of characters)
	 */
	
	public LookAheadReader(Reader reader, int capacity) {
		this.reader=reader;
  	this.capacity=capacity;
  	arr=new char[capacity];
	  }
	
  private void add(char c) {
		if (count==capacity)
			throw new IllegalStateException("buffer full");
    arr[tail]=c;
    count++;
    tail++;
    if (tail==capacity)
    	tail=0;
    if (mark!=-1 && tail==mark+1)
    	mark=-1;
    ;} 
  
  /**
	 * Returns {@code true} if the next {@code char} that will be read is {@code c}.
	 * 
	 * @param c the {@code char} to test for
	 * @return {@code true} if the next {@code char} that will be read is {@code c}
	 * @throws IOException
	 */
	public boolean at(char c) throws IOException {
		if (eof)
			return false;
	  fill(1);
		return arr[head]==c;
		} 

	/**
	 * Returns {@code true} if the {@code n}<i>th</i> {@code char} that will be read 
	 * is {@code c}, 
	 * counting from 0 ({@code at(0, 'x')} tests the first {@code char} that will be read).
	 * 
	 * @param n
	 * @param c the {@code char} to test for
	 * @return {@code true} if the {@code n}<i>th</i> {@code char} that will be read 
	 * is {@code c}
	 * @throws IOException
	 */
	public boolean at(int n, char c) throws IOException {
		if (eof)
			return false;
		fill(n+1);
		n+=head;
		if (n>=capacity)
			n-=capacity;
		return arr[n]==c;
		} 

	/**
	 * Returns {@code true} if the next {@code chars}s that will be read 
	 * match the {@link java.lang.String} {@code s}.
	 * 
	 * @param s the {@link java.lang.String} to match
	 * @return {@code true} if the next {@code chars}s that will be read 
	 * match the {@link java.lang.String} {@code s}
	 * @throws IOException
	 */
	public boolean at(String s) throws IOException {
		if (eof) 
			return false;
    int len=s.length();
		for (int i=0; i<len; i++) 
      if (peek(i)!=s.charAt(i)) 
      	return false;
    return true;  
    }

	/**
	 * Returns {@code true} if the next {@code chars}s that will be read 
	 * is {@code #}.
   * <p>
	 * A "comment" starts with {@code #}
	 * and runs until the end of the line.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "comment".
	 * @throws IOException
	 */
	public boolean atComment() throws IOException {
		return at('#');
	  }

	/**
   * Returns {@code true} if all the {@code chars}s have been read.
   * 
   * @return {@code true} if all the {@code chars}s have been read
   * @throws IOException
   */
  public boolean atEOF() throws IOException {
    return eof || peek()==(char) -1;
    }

	/**
   * Returns {@code true} if all the next <{@code chars} that will be read is '\n' 
   * or '\r' or if {@link #atEOF()}.
   * 
   * @return {@code true} if all the next <{@code chars} that will be read is '\n' 
   * or '\r' or if {@link #atEOF()}
   * @throws IOException
   */
  public boolean atEOL() throws IOException {
    if (eof)
    	return true;
  	char next=peek();
    return ((next=='\n')||(next=='\r'));
    }

	/**
	 * If the next {@code char} that will be read is {@code c},
	 * returns {@code true} and removes {@code c} from the input.
	 * 
	 * @param c the {@code true} to test for
	 * @return {@code true} if the next {@code char} that will be read is {@code c}
	 * @throws IOException
	 */
	public boolean atSkip(char c) throws IOException {
		if (eof)
			return false;
		fill(1);
		if (arr[head]!=c) 
		  return false;
		head++;
		if (head==capacity)
			head=0;
		count--;
		return true;
		}

	/**
	 * If the next {@code char}s that will be read match the {@link java.lang.String} {@code s},
	 * returns {@code true} and removes the  {@code char}s from the input.
	 * 
	 * @param s the {@link java.lang.String} to match
	 * @return {@code true} if the next {@code char} that will be read match the {@link java.lang.String} {@code s}
	 * @throws IOException
	 */
	public boolean atSkip(String s) throws IOException {
    if (!at(s)) 
    	return false;
    skip(s.length());
    return true;
    }

	/**
	 * If the next characters to be read will be a "comment",
	 * returns {@code true}
	 * and removes the comment from the input.
   * <p>
	 * A "comment" starts with {@code #}
	 * and runs until the end of the line.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "comment".
	 * @throws IOException
	 */
	public boolean atSkipComment() throws IOException {
    if (!atSkip('#'))
    	return false;
    while (!atEOL())
    	skip(1);
    return true;
    }
  
  /**
	 * If the next {@code char} that will be read will be a white space character,
	 * returns {@code true}
	 * and removes the {@code char} from the input.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "white space".
	 * @return {@code true} if the next {@code char} that will be read will be a white space character
	 * @throws IOException
	 */	
	public boolean atSkipWhitespaceChar() throws IOException {
		if (atWhitespace()) {
			skip(1);
			return true;
			}
		return false;
	  }
	
  /**
	 * If the "word" that will be read matches {@code w} (and no other word).
	 * returns true
	 * and removes the word from the input.
	 * Case sensitive.
   * <p>
	 * A "word" starts with a letter or underscore
	 * and contains only letters, underscores and digits.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "word".
	 * 
	 * @param w the "word" to match
	 * @return {@code true} if the "word" that will be read matches {@code w} (and no other word)
	 * @throws IOException
	 */
  public boolean atSkipWord(String w) throws IOException {
    if (!atWord(w)) 
      return false;
    skip(w.length());
    return true;
    }
  
  /**
	 * Returns {@code true} if the next {@code char} that will be read will be a white space character.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "white space".
	 * @return {@code true} if the next {@code char} that will be read will be a white space character
	 * @throws IOException
	 */	
  public boolean atWhitespace() throws IOException {
    return isWhitespace(peek());
    }
  
  /**
	 * Returns true if the "word" that will be read matches {@code w} (and no other word).
	 * Case sensitive.
   * <p>
	 * A "word" starts with a letter or underscore
	 * and contains only letters, underscores and digits.
   * <p>
	 * Can easily be overridden to accommodate alternative definitions of "word". 
   * <p>
	 * @param w the "word" to match
	 * @return {@code true} if the "word" that will be read matches {@code w} (and no other word)
	 * @throws IOException
	 */
  public boolean atWord(String w) throws IOException {
    if (!at(w))
    	return false;
    if (isIdentifierChar(peek(w.length())))
      return false;
    return true; 
    }
  
  /**
   * Closes the {@link LookAheadReader} and the wrapped {@link java.lang.Reader}.
   * 
   * @throws IOException
   */
  @Override
	public void close() throws IOException {
    reader.close();
    arr=null;
    eof=true;
    }
  
  private void fill(int n) throws IOException {
  	if (eof)
  	  return;
  	if (n>capacity)
  		throw new IOException("requested lookAhead exceeds buffer capacity");
  	while (count<n) 
    	add((char) reader.read());
    }

  /**
   * Marks the present position in the stream. 
   * Subsequent calls to {@link #reset()} will reposition the stream to this point. 
   *
   * @param readAheadLimit Limit on the number of characters that may be read while still preserving the mark. 
   * After reading this many characters, attempting to reset the stream may fail.
   * @throws IOException if {@code readAheadLimit} exceeds buffer capacity
   */
  @Override
  public void mark(int readAheadLimit) throws IOException {
  	if (readAheadLimit>capacity)
  		throw new IOException("requested readAheadLimit exceeds buffer capacity");
  	mark=head;
  	;}
  
  /**
   * Tells whether this stream supports the {@link #mark(int)} operation, which it does, 
   * but only up to the maximum capacity of the buffer.
   *  
   * @return {@code true}
   */
  @Override
  public boolean markSupported() {
  	return true;
  	}

	/**
	 * Returns the next {@code char} that will be read.
	 * 
	 * @return the next {@code char} that will be read
	 * @throws IOException
	 */
	public char peek() throws IOException {
    if (eof)
    	return (char) -1;
    fill(1);
		return arr[head];
	  }

	/**
	 * Returns the {@code n}<i>th</i> {@code char} that will be read,
	 * counting from 0 ({@code peek(0)} returns the first {@code char} that will be read).
	 * 
	 * @return the {@code n}<i>th</i> {@code char} that will be read
	 * @throws IOException
	 */	
	public char peek(int n) throws IOException {
    if (eof)
    	return (char) -1;
    fill(n+1);
		n+=head;
		if (n>=capacity)
			n-=capacity;
		return arr[n];
	  }

	/**
	 * Reads a single character. 
	 * This method will block until a character is available, 
	 * an I/O error occurs, 
	 * or the end of the stream is reached. 
	 * 
	 * @return The character read, as an integer in the range 0 to 65535 (0x00-0xffff), 
	 * or -1 if the end of the stream has been reached
	 * @throws IOException
	 */		
  @Override
	public int read() throws IOException {
    if (eof)
      return -1;
    int c;
    if (count>0)
      c=remove();
    else
      c=reader.read();
    if (c==-1)
      eof=true;
    return c;
    }

	/**
	 * Reads characters into a portion of an array. 
	 * This method will block until some input is available, 
	 * an I/O error occurs, 
	 * or the end of the stream is reached.
   * If len is zero, then no characters are read and 0 is returned; otherwise, 
   * there is an attempt to read at least one character. 
   * If no character is available because the stream is at its end, the value -1 is returned; otherwise, 
   * at least one character is read and stored into cbuf.
	 * 
	 * @param cbuf Destination buffer
	 * @param off Offset at which to start storing characters
	 * @param len Maximum number of characters to read
	 * @return The number of characters read, 
	 * or -1 if the end of the stream has been reached
	 * @throws IOException
	 */
	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (len==0)
			return 0;
		if (eof)
			return -1;
		int cread=0;
		int oldSize=count;
		while (cread<len && cread<oldSize) {
			cbuf[off+cread]=remove();
			cread++;
		  }
		return reader.read(cbuf, off+cread, len-cread)+cread;
	  }

	/**
	 * Reads a single character. 
	 * This method will block until a character is available, 
	 * an I/O error occurs, 
	 * or the end of the stream is reached. 
	 * 
	 * @return The character read, as {@code char}, 
	 * or ({@code char})-1 if the end of the stream has been reached
	 * @throws IOException
	 */		
	public char readChar() throws IOException {
  	return (char) read();
  	}
	
	/**
	 * Tells whether this stream is ready to be read.
	 * 
	 * @return {@code true} if the next read() is guaranteed not to block for input,
	 * {@code false} otherwise. 
	 * Note that returning {@code false} does not guarantee that the next read will block.
	 * @throws IOException
	 */
	@Override
	public boolean ready() throws IOException {
		return count>0 || reader.ready();
	  }
	
	private char remove() {
		if (count==0)
			throw new IllegalStateException("buffer empty");
		char c=arr[head];
		head++;
		if (head==capacity)
			head=0;
		count--;
		return c;
	  }
	
	/**
	 * Repositions the reader to the mark.
	 * 
	 * @throws IOException if the reader was not marked or 
	 * if the mark was invalidated by reading past the read ahead limit.
	 */
	@Override
	public void reset() throws IOException {
		if (mark==-1)
			throw new IOException("not marked or mark invalidated");
		head=mark;
		}
	
	/**
	 * Skips characters.
	 * 
	 * @param n the number of characters to skip
	 * @return the number of characters actually skipped
	 * @throws IOException
	 */
	@Override
	public long skip(long n) throws IOException {
		if (eof)
		  return 0;
		if (n>=count) {
			head=0;
			tail=0;
			var oldSize=count;
			count=0;
			return reader.skip(n-oldSize)+oldSize;
		  }
    head+=n;
		while (head>=capacity)
			head-=capacity;
    count-=n;
    return n;
    }
	
	/**
	 * Skips white space and comments.
   * <p>
	 * The class can easily be subclassed to accommodate alternative definitions of 
	 * white space and comments. 
	 */
  public void skipWhitespaceAndComments() throws IOException {
    while (true) {
    	if (atSkipWhitespaceChar())
    		;
    	else if (atSkipComment())
    		;
    	else
    		break;
      }
    }
  }
