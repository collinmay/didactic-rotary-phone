package whs.bot.common.net.dispatchers;

import whs.bot.common.net.Packet;
import whs.bot.common.net.exceptions.PacketHandlingException;

/**
 * Created by misson20000 on 10/27/16.
 */
public interface SynchronousPacketDispatcher extends PacketDispatcher {
    boolean dispatch() throws PacketHandlingException;
}
