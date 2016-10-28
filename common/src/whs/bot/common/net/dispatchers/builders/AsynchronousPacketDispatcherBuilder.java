package whs.bot.common.net.dispatchers.builders;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketHandler;
import whs.bot.common.net.PacketReader;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.PacketDispatcher;
import whs.bot.common.net.dispatchers.impl.AbstractPacketDispatcher;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by misson20000 on 9/24/16.
 */
public class AsyncDispatcherBuilder implements DispatcherBuilder<PacketDispatcher> {
    private Map<Integer, PacketReader> readerMap = new HashMap<>();
    private Map<Integer, PacketHandler> handlerMap = new HashMap<>();

    public <T extends Packet> AsyncDispatcherBuilder packet(int id, PacketReader<T> reader, PacketHandler<T> handler) {
        readerMap.put(id, reader);
        handlerMap.put(id, handler);
        return this;
    }

    @Override
    public AsyncPacketDispatcherImpl build(PacketTunnel tunnel) {
        return new AsyncPacketDispatcherImpl(tunnel, readerMap, handlerMap);
    }

    private class AsyncPacketDispatcherImpl extends AbstractPacketDispatcher {
        private final Map<Integer, PacketHandler> handlerMap;

        private AsyncPacketDispatcherImpl(PacketTunnel tunnel, Map<Integer, PacketReader> readerMap, Map<Integer, PacketHandler> handlerMap) {
            super(tunnel, readerMap);
            this.handlerMap = new HashMap<>(handlerMap);
        }

        @Override
        protected void dispatchImpl(Packet p) throws Exception {
            //noinspection unchecked
            handlerMap.get(p.getType()).handle(p);
        }
    }
}
