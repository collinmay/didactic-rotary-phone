package whs.bot.common.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by misson20000 on 9/24/16.
 */
public class AsyncDispatcherBuilder {
    private Map<Integer, PacketReader> readerMap = new HashMap<>();
    private Map<Integer, PacketHandler> handlerMap = new HashMap<>();

    public <T extends Packet> AsyncDispatcherBuilder packet(int id, PacketReader<T> reader, PacketHandler<T> handler) {
        readerMap.put(id, reader);
        handlerMap.put(id, handler);
        return this;
    }

    public AsynchronousPacketDispatcher build() {
        return new AsynchronousPacketDispatcher(readerMap, handlerMap);
    }
}
