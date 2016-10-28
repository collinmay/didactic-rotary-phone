package whs.bot.common.net.dispatchers;

import whs.bot.common.net.PacketTunnel;

/**
 * Created by misson20000 on 10/27/16.
 */
public class NullDispatcherBuilder implements DispatcherBuilder<Dispatcher> {

    @Override
    public Dispatcher build(PacketTunnel tunnel) {
        return null;
    }
}
