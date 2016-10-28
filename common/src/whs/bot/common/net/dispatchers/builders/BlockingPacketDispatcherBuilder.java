package whs.bot.common.net.dispatchers.builders;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketReader;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.BlockingPacketDispatcher;
import whs.bot.common.net.dispatchers.AbstractPacketDispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 10/27/16.
 */
public class BlockingPacketDispatcherBuilder implements DispatcherBuilder<BlockingPacketDispatcher> {

    private Map<Integer, PacketReader> readers = new HashMap<>();

    public BlockingPacketDispatcherBuilder packet(int id, PacketReader reader) {
        readers.put(id, reader);
        return this;
    }

    @Override
    public BlockingPacketDispatcher build(PacketTunnel tunnel) {
        return new BlockingPacketDispatcherImpl(tunnel, readers);
    }

    private class BlockingPacketDispatcherImpl extends AbstractPacketDispatcher implements BlockingPacketDispatcher {
        private final BlockingQueue<Packet> queue;

        private BlockingPacketDispatcherImpl(PacketTunnel tunnel, Map<Integer, PacketReader> readers) {
            super(tunnel, readers);
            this.queue = new LinkedBlockingQueue<>(50);
        }

        public Packet poll() {
            return queue.poll();
        }

        @Override
        protected void dispatchImpl(Packet p) {
            queue.add(p);
        }
    }
}
