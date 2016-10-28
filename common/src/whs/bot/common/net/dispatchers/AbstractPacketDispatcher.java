package whs.bot.common.net.dispatchers.impl;

import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.PacketDispatcher;
import whs.bot.common.net.exceptions.DispatchException;
import whs.bot.common.net.Packet;
import whs.bot.common.net.exceptions.PacketHandlingException;
import whs.bot.common.net.PacketReader;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by misson20000 on 9/24/16.
 */
public abstract class AbstractPacketDispatcher implements PacketDispatcher {
    private final Map<Integer, PacketReader> readerMap;
    private final PacketTunnel tunnel;

    protected AbstractPacketDispatcher(PacketTunnel tunnel, Map<Integer, PacketReader> readerMap) {
        this.readerMap = new HashMap<>(readerMap);
        this.tunnel = tunnel;
    }

    public PacketTunnel getTunnel() {
        return tunnel;
    }

    @Override
    public void dispatch(ByteBuffer buf) throws DispatchException, PacketHandlingException {
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
    }

    @Override
    public void send(Packet p) {
        tunnel.send(p);
    }

    protected abstract void dispatchImpl(Packet p) throws Exception;
}
