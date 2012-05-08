package dcpu.antlr;

import java.io.IOException;
import java.io.Reader;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CharStream;

public class ANTLRNoCaseReaderStream extends ANTLRReaderStream {
    
    public ANTLRNoCaseReaderStream(Reader r) throws IOException {
        super(r);
    }

    public int LA(int i) {
        if ( i==0 ) {
            return 0; // undefined
        }
        if ( i<0 ) {
            i++; // e.g., translate LA(-1) to use offset 0
        }

        if ((p + i - 1) >= n) {

            return CharStream.EOF;
        }
        return Character.toUpperCase(data[p + i - 1]);
    }
}
