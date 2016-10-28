package whs.bot.common.net;

import whs.bot.common.net.dispatchers.PacketDispatcher;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketHandler<T extends Packet> {
    void handle(PacketDispatcher dispatch, T p) throws Exception;
}
