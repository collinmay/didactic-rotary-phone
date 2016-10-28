package whs.bot.common.net.dispatchers;

import whs.bot.common.net.Packet;

/**
 * Created by misson20000 on 10/27/16.
 */
public interface PacketDispatcher extends Dispatcher {
    void send(Packet p);
}
