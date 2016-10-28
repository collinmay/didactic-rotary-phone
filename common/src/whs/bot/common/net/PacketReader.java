package whs.bot.common.net;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketReader<T extends Packet> {
    T read(ByteBuffer buffer);
}
