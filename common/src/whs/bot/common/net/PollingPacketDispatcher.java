package whs.bot.common.net;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 9/24/16.
 */
public class PollingPacketDispatcher extends AbstractPacketDispatcher {
    private final BlockingQueue<Packet> queue;

    public PollingPacketDispatcher() {
        this.queue = new LinkedBlockingQueue<>(50);
    }

    public Packet poll() {
        return queue.poll();
    }

    @Override
    protected void dispatchImpl(Packet p) {
        queue.add(p);
    }

    public void addPacket(int id, PacketReader reader) {
        this.addReader(id, reader);
    }
}
