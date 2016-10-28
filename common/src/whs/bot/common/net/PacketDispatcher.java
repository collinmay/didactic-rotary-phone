package whs.bot.common.net;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketDispatcher {
    void dispatch(ByteBuffer packet) throws DispatchException, PacketHandlingException;
}
