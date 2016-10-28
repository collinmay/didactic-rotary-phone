package whs.bot.common.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by misson20000 on 9/24/16.
 */
public class AsynchronousPacketDispatcher extends AbstractPacketDispatcher {
    private final Map<Integer, PacketHandler> handlerMap;

    public AsynchronousPacketDispatcher() {
        handlerMap = new HashMap<>();
    }

    public AsynchronousPacketDispatcher(Map<Integer, PacketReader> readerMap, Map<Integer, PacketHandler> handlerMap) {
        super(readerMap);
        this.handlerMap = new HashMap<>(handlerMap);
    }

    @Override
    protected void dispatchImpl(Packet p) throws Exception {
        //noinspection unchecked
        handlerMap.get(p.getType()).handle(p);
    }

    public <T extends Packet> void addPacket(int type, PacketReader<T> reader, PacketHandler<T> handler) {
        handlerMap.put(type, handler);
        addReader(type, reader);
    }
}
