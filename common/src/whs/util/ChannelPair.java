package whs.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Created by misson20000 on 9/24/16.
 */
public class ChannelPair {
    private final BidirectionalPipeChannel a;
    private final BidirectionalPipeChannel b;
    private boolean isClosed = false;

    public ChannelPair() throws IOException {
        Pipe aToB = Pipe.open();
        Pipe bToA = Pipe.open();

        a = new BidirectionalPipeChannel(bToA.source(), aToB.sink());
        b = new BidirectionalPipeChannel(aToB.source(), bToA.sink());
    }

    public void close() throws IOException {
        a.close();
        b.close();
    }

    public ByteChannel getChannelA() {
        return a;
    }

    public ByteChannel getChannelB() {
        return b;
    }

    private class BidirectionalPipeChannel implements ByteChannel {
        private final ReadableByteChannel in;
        private final WritableByteChannel out;

        private BidirectionalPipeChannel(ReadableByteChannel in, WritableByteChannel out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            if(!in.isOpen() || isClosed) {
                throw new ClosedChannelException();
            }
            return in.read(dst);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            if(!out.isOpen() || isClosed) {
                throw new ClosedChannelException();
            }
            return out.write(src);
        }

        @Override
        public boolean isOpen() {
            return in.isOpen() && out.isOpen() && !isClosed;
        }

        @Override
        public void close() throws IOException {
            in.close();
            out.close();
            isClosed = true;
        }
    }
}
