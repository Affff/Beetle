package ru.obolensk.afff.beetle.stream;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Created by Afff on 20.04.2017.
 */
public class LimitedBufferedReader extends BufferedReader {

    private final int maxLineLenght;

    private boolean lineOverflow;

    public LimitedBufferedReader(@Nonnull final Reader reader, final int maxLineLenght) {
        super(reader);
        if (maxLineLenght <= 0) {
            throw new IllegalArgumentException("maxLineLenght must be positive!");
        }
        this.maxLineLenght = maxLineLenght;
    }

    @Override
    public String readLine() throws IOException {
        final char[] data = new char[maxLineLenght];
        final int CR = 13;
        final int LF = 10;

        int currentPos = 0;
        int currentCharVal = super.read();

        while( (currentCharVal != CR) && (currentCharVal != LF) && (currentCharVal >= 0)) {
            data[currentPos++] = (char) currentCharVal;
            if (currentPos < maxLineLenght) {
                currentCharVal = super.read();
            } else {
                lineOverflow = true;
                return new String(data, 0, currentPos);
            }
        }

        if (currentCharVal < 0 ) {
            // stream is over, return current buffer or null if the buffer is empty
            if (currentPos > 0) {
                return new String(data, 0, currentPos);
            } else {
                return null;
            }
        } else {
            if (currentCharVal == CR) {
                //Check for LF and remove from buffer
                super.mark(1);
                if (super.read() != LF) {
                    super.reset();
                }
            }
            return new String(data, 0, currentPos);
        }
    }

    /**
     * Returns true if last line was too long and false otherwise.
     * WARNING! Overflow status is raised by call this method.
     */
    public boolean isLineOverflow() {
        boolean overflow = lineOverflow;
        if (overflow) {
            lineOverflow = false;
        }
        return overflow;
    }
}
