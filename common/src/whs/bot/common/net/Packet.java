package whs.bot.common.net;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 9/24/16.
 */
public abstract class Packet {
    public abstract int getType();
    public abstract void write(ByteBuffer buf);
}
