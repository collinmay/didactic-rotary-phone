package whs.bot.common.net.dispatchers.builders;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketHandler;
import whs.bot.common.net.PacketReader;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.AbstractPacketDispatcher;
import whs.bot.common.net.dispatchers.SynchronousPacketDispatcher;
import whs.bot.common.net.exceptions.PacketHandlingException;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 10/27/16.
 */
public class SynchronousPacketDispatcherBuilder implements DispatcherBuilder<SynchronousPacketDispatcher> {

    private final Map<Integer, PacketReader> readerMap = new HashMap<>();
    private final Map<Integer, PacketHandler> handlerMap = new HashMap<>();

    public <T extends Packet> void packet(int id, PacketReader<T> reader, PacketHandler<T> handler) {
        readerMap.put(id, reader);
        handlerMap.put(id, handler);
    }

    @Override
    public SynchronousPacketDispatcher build(PacketTunnel tunnel) {
        return new SynchronousPacketDispatcherImpl(tunnel, readerMap, handlerMap);
    }

    private class SynchronousPacketDispatcherImpl extends AbstractPacketDispatcher implements SynchronousPacketDispatcher {
        private final Map<Integer, PacketHandler> handlerMap;
        private final BlockingQueue<Packet> queue;
        private final LinkedList<WeakReference<Runnable>> enqueueListeners;

        private SynchronousPacketDispatcherImpl(PacketTunnel tunnel, Map<Integer, PacketReader> readerMap, Map<Integer, PacketHandler> handlerMap) {
            super(tunnel, readerMap);
            this.queue = new LinkedBlockingQueue<>();
            this.handlerMap = new HashMap<>(handlerMap);
            this.enqueueListeners = new LinkedList<>();
        }

        public void addEnqueuementListener(Runnable run) {
            enqueueListeners.add(new WeakReference<>(run));
        }

        @Override
        protected void dispatchImpl(Packet p) {
            queue.add(p);
            enqueueListeners.stream().map(WeakReference::get).filter(Objects::nonNull).forEach(Runnable::run);
        }

        public boolean dispatch() throws PacketHandlingException {
            Packet p;
            boolean handledAny = false;
            while((p = queue.poll()) != null) {
                handledAny = true;
                try {
                    //noinspection unchecked
                    handlerMap.get(p.getType()).handle(this, p);
                } catch (Exception e) {
                    throw new PacketHandlingException(p, e);
                }
            }
            return handledAny;
        }
    }

}
