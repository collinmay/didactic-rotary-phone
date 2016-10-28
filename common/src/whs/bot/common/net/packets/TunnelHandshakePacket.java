package whs.bot.common.net.packets;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketReader;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 9/24/16.
 */
public class TunnelHandshakePacket extends Packet {
    private final short tunnelID;
    private final boolean accepted;

    public TunnelHandshakePacket(short tunnelID, boolean accepted) {
        this.tunnelID = tunnelID;
        this.accepted = accepted;
    }

    @Override
    public int getType() {
        return 2;
    }

    @Override
    public void write(ByteBuffer buf) {
        buf.putShort(tunnelID);
        buf.put((byte) (accepted ? 1 : 0));
    }

    public short getTunnelID() {
        return tunnelID;
    }

    public boolean wasAccepted() {
        return accepted;
    }

    public static PacketReader<TunnelHandshakePacket> getReader() {
        return buf -> {
            short tunnelID = buf.getShort();
            boolean accepted = buf.get() != 0;
            return new TunnelHandshakePacket(tunnelID, accepted);
        };
    }
}
