/*  Ritika Maknoor
    CS 1501
    Assignment 2 */

/*************************************************************************
 *  Compilation:  javac LZW.java
 *  Execution:    java LZW - < input.txt   (compress)
 *  Execution:    java LZW + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *
 *  Compress or expand binary input from standard input using LZW.
 *
 *  WARNING: STARTING WITH ORACLE JAVA 6, UPDATE 7 the SUBSTRING
 *  METHOD TAKES TIME AND SPACE LINEAR IN THE SIZE OF THE EXTRACTED
 *  SUBSTRING (INSTEAD OF CONSTANT SPACE AND TIME AS IN EARLIER
 *  IMPLEMENTATIONS).
 *
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 *************************************************************************/

public class MyLZW {

//Global variables 
    private static final int R = 256;                       // Number of input chars
    private static int L = 512;                             // Number of codewords = 2^W
    private static int W = 9;                               // Codeword width; in bits
    private static String mode = "n";                       // Default mode is "Do Nothing"

    private static float origCompRatio = 0;                 // Comp ratio when entered m mode
    private static float newCompRatio = 0;                  // Comp ratio current
    private static float sizeOrigData = 0;                  // Size of orig data consumed so far; not in bits though
    private static float sizeOutputData = 0;                // Size of compressed output data prod so far; starts at 9
    private static int enteringM = 0;                       // Signals when to save newCompRatio as origCompRatio also



//Compress method
    public static void compress() { 

    //Store mode at beginning of output file
        if (mode.equals("n")){
            BinaryStdOut.write('n', 8); 
        }
        if (mode.equals("r")){
            BinaryStdOut.write('r', 8); 
        }
        if (mode.equals("m")){
            BinaryStdOut.write('m', 8); 
        }

        String input = BinaryStdIn.readString();            // Takes data read in & builds into string until EOF reached
        TST<Integer> st = new TST<Integer>();               // New symbol table with keys created
        for (int i = 0; i < R; i++)                         // Places each char from string into symbol table st until reach R
            st.put("" + (char) i, i);
        int code = R+1;                                     // R is codeword for EOF

        while (input.length() > 0) {                        // Loops while input length not 0 bc means still have input to deal with
            L = (int)Math.pow(2,W);
            String s = st.longestPrefixOf(input);           // Find max prefix match from full input found in st
            BinaryStdOut.write(st.get(s), W);               // Print s's encoding in bits; as long as W valid: not <1 or >16
            int t = s.length();                             // Number of chars in current s
            sizeOrigData += t;                              // Size consumed so far; not in bits though
            sizeOutputData += W;                            // Size of output codeword width so far
            newCompRatio =  sizeOrigData / sizeOutputData;  // Current ratio

    // Default Mode; for all n, r, m
            if ( (t < input.length()) && (code < L) ){      
                st.put(input.substring(0, t + 1), code++);      // Add s to symbol table using imcremented code value
            }
            if ( (W < 16) && (code == (int)Math.pow(2,W)) ){    // Until W==16
                W++;                                            // Increment codeword width; starts as 9; max 16
                L = (int)Math.pow(2,W);                         // Set new L=2^W
                st.put(input.substring(0, t + 1), code++);      // Add s to symbol table using imcremented code value
            }

    // Reset Mode; reset dict back to empty
            if ( (mode.equals("r")) && (code == (int)Math.pow(2,16)) ){     // If mode "r" && code is 65536=last available codeword for L=16
                W = 9;                                          // Reset W to starting value
                st = new TST<Integer>();                        // New symbol table st
                for (int i = 0; i < R; i++){
                    st.put("" + (char) i, i);
                }
                code = R+1;                                     // R is codeword for EOF
                L = (int) Math.pow(2,9);                        // Reset L to starting value
            }

    // Monitor Mode; keep using full codebook, monitor comp ratios, reset dict to empty if threshold > 1.1
            if ( (mode.equals("m")) && (code == (int)Math.pow(2,16)) ){     // If mode "m" && code is 65536=last available codeword for L=16
                if (enteringM == 0){                            // If just entering monitor mode; sets ratio as origCompRatio
                    origCompRatio = newCompRatio;
                    enteringM = 1;                              // Set as 1 so subsequent ratios are only current/newCompRatio
                }
                else{
                    if( (origCompRatio/newCompRatio) > 1.1 ){   // Reset if threshold > 1.1
                        W = 9;                                  // Reset W to starting value
                        st = new TST<Integer>();                // New symbol table st
                        for (int i = 0; i < R; i++){
                            st.put("" + (char) i, i);
                        }
                        code = R+1;                             // R is codeword for EOF
                        enteringM = 0;                          // Reset to starting value
                        sizeOutputData = 0;                     // Reset to starting value
                        sizeOrigData = 0;                       // Reset to starting value
                        newCompRatio = 0;                       // Reset to starting value
                        origCompRatio = 0;                      // Reset to starting value
                        L = (int) Math.pow(2,9);                // Reset L to starting value
                    }
                }
            }

            input = input.substring(t);                     // Scan past s in input
        }                                                   // End while loop
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    }                                                       



//Expand method
    public static void expand() {

    //Retrieve mode stored at beginning of output file during compression
        char whatMode = BinaryStdIn.readChar(8);
        if (whatMode == 'n'){
            mode = "n";
        }
        if (whatMode == 'r'){
            mode = "r";
        }
        if (whatMode == 'm'){
            mode = "m";
        }

        int maxL = (int)Math.pow(2,16);                     // Max value of L possible; when W=16; 2^16
        String[] st = new String[maxL];                     // New symbol table of size 2^16
        int i;                                              // Next available codeword value 
        for (i = 0; i < R; i++){                            // Initializes symbol table with all 1-char strings; index 0 to 255
            st[i] = "" + (char) i;
        }
        st[i++] = "";                                       // (Unused) lookahead for EOF
        int codeword = BinaryStdIn.readInt(W);              // Get next 9 bits from standard input as 9-bit int value
        if (codeword == R) return;                          // Expanded message is empty string
        String val = st[codeword];                          // First 9 chars from symbol table

        while (true) {
            BinaryStdOut.write(val);                        // Writes string of 8 bit values to output
            codeword = BinaryStdIn.readInt(W);              // Set as next 9 bits of input as 9-bit int value
            sizeOrigData += (val.length());                 // Size consumed so far; not in bits though
            sizeOutputData += W;                            // Size of output codeword
            newCompRatio = sizeOrigData / sizeOutputData;   // Current ratio

    // Default Mode; for all n, r, m
            if (codeword == R){                                 // Break; codeword == 256
                break;
            }
            String s = st[codeword];                            // Set as 9 bits of input as 9-bit int value
            if (i == codeword){                                 
                s = val + val.charAt(0);                        // special case hack
            }
            if (i < L-1){                                       // L-1 bc size is 1 more then index values
                st[i++] = val + s.charAt(0);                    // Concatenate val + new char and store in next index
            }
            if ( (W < 16) && (i == (((int)Math.pow(2,W))-1)) ){ // Until W==16
                W++;                                            // Increment codeword width; starts at 9; max 16
                L = (int)Math.pow(2,W);                         // Set new L=2^W
                st[i++] = val + s.charAt(0);                    // Concatenate val + new char and store in next index
            }
            val = s;

    // Reset Mode; reset dict back to empty
            if ( (mode.equals("r")) && (i == (int)Math.pow(2,16)-1) ){  // If mode "r" && i is (65536=last available codeword for L=16)-1
                W = 9;                                          // Reset W to starting value
                st = new String[maxL];                          // New symbol table st
                for (i = 0; i < R; i++)
                    st[i] = "" + (char) i;
                st[i++] = "";                                   // (Unused) lookahead for EOF
                codeword = BinaryStdIn.readInt(W);              // Get next 9 bits from standard input as 9-bit int value
                if (codeword == R) return;                      // Expanded message is empty string
                val = st[codeword];                             // Set as chars from st
                L = (int) Math.pow(2,9);                        // Reset L to starting value
            }

    // Monitor Mode; keep using full codebook, monitor comp ratios, reset dict to empty if threshold > 1.1
            if ( (mode.equals("m")) && (i == (int)Math.pow(2,16)-1) ){  // If mode "m" && i is (65536=last available codeword for L=16)-1
                if (enteringM == 0){                            // If just entering monitor mode; sets ratio as origCompRatio
                    origCompRatio = newCompRatio;
                    enteringM = 1;                              // Set as 1 so subsequent ratios are only current/newCompRatio
                }
                else{
                    if( (origCompRatio/newCompRatio) > 1.1 ){   // Reset if threshold > 1.1
                        W = 9;                                  // Reset W to starting value
                        st = new String[maxL];                  // New symbol table st
                        for (i = 0; i < R; i++)
                            st[i] = "" + (char) i;
                        st[i++] = "";                           // (Unused) lookahead for EOF
                        codeword = BinaryStdIn.readInt(W);      // Get next 9 bits from standard input as 9-bit int value
                        if (codeword == R) return;              // Expanded message is empty string
                        val = st[codeword];                     // Set as chars from st
                        enteringM = 0;                          // Reset to starting value
                        sizeOutputData = 0;                     // Reset to starting value
                        sizeOrigData = 0;                       // Reset to starting value
                        newCompRatio = 0;                       // Reset to starting value
                        origCompRatio = 0;                      // Reset to starting value
                        L = (int) Math.pow(2,9);                // Reset L to starting value
                    }
                }
            }  

        }                                                   // End while loop
        BinaryStdOut.close();
    }



//Main method
    public static void main(String[] args) {

    //Determining which mode to do is done at compression only
        if (args[0].equals("-") && args[1].equals("n")){ 
            mode = "n";
            compress();
        }
        else if (args[0].equals("-") && args[1].equals("r")){ 
            mode = "r";
            compress();
        }
        else if (args[0].equals("-") && args[1].equals("m")){ 
            mode = "m";
            compress();
        }
    //Expanding only needs command line argument "+"
        else if (args[0].equals("+")){
            expand();
        }
        else throw new IllegalArgumentException("Illegal command line argument");
    }

}