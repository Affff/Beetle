package ru.obolensk.afff.beetle.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Afff on 19.04.2017.
 */
public class StreamUtilTest {

    @Test
    public void streamCopyTest() throws IOException {
        byte[] buffIn = new byte[20];
        for (int i = 0; i < buffIn.length; i++) {
            buffIn[i] = (byte) i;
        }
        final Reader in = new InputStreamReader(new ByteArrayInputStream(buffIn));
        final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        final Writer out = new OutputStreamWriter(resultStream);
        assertEquals(20, StreamUtil.copy(in, out, 1000));
        assertTrue(Arrays.equals(buffIn, resultStream.toByteArray()));
        final Reader in2 = new InputStreamReader(new ByteArrayInputStream(buffIn));
        final ByteArrayOutputStream resultStream2 = new ByteArrayOutputStream();
        final Writer out2 = new OutputStreamWriter(resultStream2);
        assertEquals(20, StreamUtil.copy(in2, out2, 20));
        assertTrue(Arrays.equals(buffIn, resultStream2.toByteArray()));
        final Reader in3 = new InputStreamReader(new ByteArrayInputStream(buffIn));
        final ByteArrayOutputStream resultStream3 = new ByteArrayOutputStream();
        final Writer out3 = new OutputStreamWriter(resultStream3);
        assertEquals(10, StreamUtil.copy(in3, out3, 10));
        assertTrue(Arrays.equals(Arrays.copyOf(buffIn, 10), resultStream3.toByteArray()));
        final Reader in4 = new InputStreamReader(new ByteArrayInputStream(buffIn));
        final ByteArrayOutputStream resultStream4 = new ByteArrayOutputStream();
        final Writer out4 = new OutputStreamWriter(resultStream4);
        try {
            StreamUtil.copy(in4, out4, 0);
            Assert.fail();
        } catch (IllegalArgumentException e){
        }
        try {
            StreamUtil.copy(in4, out4, -1);
            Assert.fail();
        } catch (IllegalArgumentException e){
        }
    }
}
