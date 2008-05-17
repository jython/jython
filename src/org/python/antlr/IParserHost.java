package org.python.antlr;

/**
 * 
 * literal creation callbacks from the parser to the its host
 * 
 **/

public interface IParserHost {

       public Object newLong(String s);

       public Object newLong(java.math.BigInteger i);

       public Object newFloat(double v);
       
       public Object newImaginary(double v);
       
       public Object newInteger(int i);
       
       public String decode_UnicodeEscape(String str, int start, int end,
               String errors, boolean unicode);
}
