package ru.obolensk.afff.beetle.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Afff on 19.04.2017.
 */
public class StreamUtilTest {

    //TODO choose and add HTTP testing framework

    @Test
    public void streamCopyTest() throws IOException {
        byte[] buffIn = new byte[20];
        for (int i = 0; i < buffIn.length; i++) {
            buffIn[i] = (byte) i;
        }
        final InputStream in = new ByteArrayInputStream(buffIn);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(20, StreamUtil.copy(in, out, 1000));
        assertTrue(Arrays.equals(buffIn, out.toByteArray()));
        final InputStream in2 = new ByteArrayInputStream(buffIn);
        final ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        assertEquals(20, StreamUtil.copy(in2, out2, 20));
        assertTrue(Arrays.equals(buffIn, out2.toByteArray()));
        final InputStream in3 = new ByteArrayInputStream(buffIn);
        final ByteArrayOutputStream out3 = new ByteArrayOutputStream();
        assertEquals(10, StreamUtil.copy(in3, out3, 10));
        assertTrue(Arrays.equals(Arrays.copyOf(buffIn, 10), out3.toByteArray()));
        final InputStream in4 = new ByteArrayInputStream(buffIn);
        final ByteArrayOutputStream out4 = new ByteArrayOutputStream();
        assertEquals(0, StreamUtil.copy(in4, out4, 0));
        assertEquals(0, out4.toByteArray().length);
    }
}
