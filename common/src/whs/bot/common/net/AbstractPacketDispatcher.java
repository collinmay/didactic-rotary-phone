package whs.bot.common.net;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by misson20000 on 9/24/16.
 */
public abstract class AbstractPacketDispatcher {
    private final Map<Integer, PacketReader> readerMap;

    protected AbstractPacketDispatcher() {
        this.readerMap = new HashMap<>();
    }

    protected AbstractPacketDispatcher(Map<Integer, PacketReader> readerMap) {
        this.readerMap = new HashMap<>(readerMap);
    }

    public PacketDispatcher getAsDispatcher() {
        return buf -> {
            int type = buf.getShort();
            if(!readerMap.containsKey(type)) {
                throw new DispatchException("No such packet handler for ID " + type);
            }
            Packet p = readerMap.get(type).read(buf);
            try {
                dispatchImpl(p);
            } catch (Exception e) {
                throw new PacketHandlingException(p, e);
            }
        };
    }

    protected abstract void dispatchImpl(Packet p) throws Exception;

    protected void addReader(int id, PacketReader reader) {
        readerMap.put(id, reader);
    }
}
