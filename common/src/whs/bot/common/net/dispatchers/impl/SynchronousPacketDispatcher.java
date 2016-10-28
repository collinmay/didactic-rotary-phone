package whs.bot.common.net.dispatchers;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketHandler;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.exceptions.PacketHandlingException;
import whs.bot.common.net.PacketReader;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 9/24/16.
 */
public class SynchronousPacketDispatcher extends AbstractPacketDispatcher {
    private final Map<Integer, PacketHandler> handlerMap;
    private final BlockingQueue<Packet> queue;
    private final LinkedList<WeakReference<Runnable>> enqueueListeners;

    public SynchronousPacketDispatcher(PacketTunnel tunnel) {
        super(tunnel);
        this.queue = new LinkedBlockingQueue<>();
        this.handlerMap = new HashMap<>();
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

    public <T extends Packet> void addPacket(int type, PacketReader<T> reader, PacketHandler<T> handler) {
        handlerMap.put(type, handler);
        addReader(type, reader);
    }

    public boolean dispatch() throws PacketHandlingException {
        Packet p = queue.poll();
        if(p != null) {
            try {
                //noinspection unchecked
                handlerMap.get(p.getType()).handle(p);
            } catch (Exception e) {
                throw new PacketHandlingException(p, e);
            }
            return true;
        } else {
            return false;
        }
    }
}
