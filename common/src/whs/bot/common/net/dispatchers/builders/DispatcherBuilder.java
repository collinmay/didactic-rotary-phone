package whs.bot.common.net.dispatchers;

import whs.bot.common.net.PacketTunnel;

/**
 * Created by misson20000 on 10/27/16.
 */
public interface DispatcherBuilder<T extends Dispatcher> {
    T build(PacketTunnel tunnel);
}
