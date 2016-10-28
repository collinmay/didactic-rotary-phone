package whs.bot.common.net.dispatchers.builders;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.PacketDispatcher;
import whs.bot.common.net.exceptions.DispatchException;
import whs.bot.common.net.exceptions.PacketHandlingException;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 10/27/16.
 */
public class SendingPacketDispatcherBuilder implements DispatcherBuilder<PacketDispatcher> {

    @Override
    public PacketDispatcher build(PacketTunnel tunnel) {
        return new PacketDispatcher() {
            @Override
            public void send(Packet p) {
                tunnel.send(p);
            }

            @Override
            public void dispatch(ByteBuffer packet) throws DispatchException, PacketHandlingException {

            }
        };
    }
}
