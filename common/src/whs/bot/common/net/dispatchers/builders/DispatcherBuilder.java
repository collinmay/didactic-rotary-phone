package whs.bot.common.net.dispatchers.builders;

import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.Dispatcher;

/**
 * Created by misson20000 on 10/27/16.
 */
public interface DispatcherBuilder<T extends Dispatcher> {
    T build(PacketTunnel tunnel);
}
