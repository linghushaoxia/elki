package de.lmu.ifi.dbs.elki.datasource.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.Iter;

/**
 * String tokenizer.
 * 
 * @author Erich Schubert
 */
public class Tokenizer implements Iter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Tokenizer.class);

  /**
   * Separator pattern.
   */
  Pattern colSep;

  /**
   * A quote pattern
   */
  public static final char QUOTE_CHAR = '\"';

  /**
   * Stores the quotation character
   */
  protected char quoteChar = QUOTE_CHAR;

  /**
   * Constructor.
   * 
   * @param colSep Column separator pattern.
   * @param quoteChar Quotation character.
   */
  public Tokenizer(Pattern colSep, char quoteChar) {
    super();
    this.colSep = colSep;
    this.quoteChar = quoteChar;
  }

  /**
   * Regular expression match helper.
   */
  private Matcher m = null;

  /**
   * Data currently processed.
   */
  private String input;

  /**
   * Current positions of result and iterator.
   */
  private int start, end, index;

  /**
   * Initialize parser with a new string.
   * 
   * @param input New string to parse.
   */
  public void initialize(String input) {
    this.input = input;
    this.m = colSep.matcher(input);
    this.index = 0;
    this.start = 0;
    this.end = input.length();
    advance();
  }

  @Override
  public boolean valid() {
    return start < input.length();
  }

  @Override
  public void advance() {
    int inquote = isQuote(index);
    while(m.find()) {
      // Quoted code path vs. regular code path
      if(inquote == 1) {
        // Closing quote found?
        if(m.start() > index + 1 && input.charAt(m.start() - 1) == quoteChar) {
          this.start = index + 1;
          this.end = m.start() - 1;
          this.index = m.end();
          return;
        }
        continue;
      }
      else {
        this.start = index;
        this.end = m.start();
        this.index = m.end();
        return;
      }
    }
    // Add tail after last separator.
    this.start = index;
    this.end = input.length();
    this.index = end + 1;
    if(inquote == 1) {
      final int last = input.length() - 1;
      if(isQuote(last) == 1) {
        ++this.start;
        --this.end;
      }
      else {
        LOG.warning("Invalid quoted line in input: no closing quote found in: " + input);
      }
    }
  }

  /**
   * Get the current part as substring
   * 
   * @return Current value as substring.
   */
  public String getSubstring() {
    // TODO: detect Java <6 and make sure we only return the substring?
    // With java 7, String.substring will arraycopy the characters.
    return input.substring(start, end);
  }

  /**
   * Get current value as double.
   * 
   * @return double value
   * @throws NumberFormatException when current value cannot be parsed as double
   *         value.
   */
  public double getDouble() throws NumberFormatException {
    return FormatUtil.parseDouble(input, start, end);
  }

  /**
   * Get current value as long.
   * 
   * @return double value
   * @throws NumberFormatException when current value cannot be parsed as long
   *         value.
   */
  public long getLongBase10() throws NumberFormatException {
    return FormatUtil.parseLongBase10(input, start, end);
  }

  /**
   * Detect quote characters.
   * 
   * TODO: support more than one quote character, make sure opening and closing
   * quotes match then.
   * 
   * @param index Position
   * @return {@code 1} when a quote character, {@code 0} otherwise.
   */
  private int isQuote(int index) {
    return (index < input.length()) && (input.charAt(index) == quoteChar) ? 1 : 0;
  }
}
