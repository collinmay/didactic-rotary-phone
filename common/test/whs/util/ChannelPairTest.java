package whs.util;

import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by misson20000 on 9/24/16.
 */
public class ChannelPairTest {
    @Test
    public void dataPassesAToB() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        ByteBuffer src = ByteBuffer.allocate(512);
        ByteBuffer dst = ByteBuffer.allocate(512);
        byte[] srcBytes = new byte[src.remaining()];
        byte[] dstBytes = new byte[dst.remaining()];

        new Random().nextBytes(srcBytes);

        src.put(srcBytes);
        src.flip();
        a.write(src);
        while(dst.remaining() > 0) { b.read(dst); }
        dst.flip();
        dst.get(dstBytes);

        assertArrayEquals(srcBytes, dstBytes);
    }

    @Test
    public void dataPassesBToA() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        ByteBuffer src = ByteBuffer.allocate(512);
        ByteBuffer dst = ByteBuffer.allocate(512);
        byte[] srcBytes = new byte[src.remaining()];
        byte[] dstBytes = new byte[dst.remaining()];

        new Random().nextBytes(srcBytes);

        src.put(srcBytes);
        src.flip();
        b.write(src);
        while(dst.remaining() > 0) { a.read(dst); }
        dst.flip();
        dst.get(dstBytes);

        assertArrayEquals(srcBytes, dstBytes);
    }

    @Test
    public void closeTestA() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        a.close();
        assertFalse(a.isOpen());
    }

    @Test
    public void closeTestB() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        b.close();
        assertFalse(b.isOpen());
    }

    @Test(expected = IOException.class)
    public void cannotWriteToClosed() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();

        ByteBuffer src = ByteBuffer.allocate(512);
        byte[] srcBytes = new byte[src.remaining()];
        new Random().nextBytes(srcBytes);
        src.put(srcBytes);
        src.flip();

        a.close();
        a.write(src);
    }

    @Test(expected = IOException.class)
    public void cannotReadFromClosed() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();

        ByteBuffer dst = ByteBuffer.allocate(512);

        a.close();
        a.read(dst);
    }

    @Test(expected = IOException.class)
    public void cannotReadFromOtherEndClosed() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        ByteBuffer dst = ByteBuffer.allocate(512);

        a.close();
        b.read(dst);
    }

    @Test
    public void closeTestAB() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        a.close();
        assertFalse(b.isOpen());
    }

    @Test
    public void closeTestBA() throws Exception {
        ChannelPair pair = new ChannelPair();
        ByteChannel a = pair.getChannelA();
        ByteChannel b = pair.getChannelB();

        b.close();
        assertFalse(a.isOpen());
    }
}