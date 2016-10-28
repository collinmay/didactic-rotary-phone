package whs.bot.common.net.packets;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketReader;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 9/24/16.
 */
public class TunnelCreatePacket extends Packet {
    private final byte[] handshake;
    private final short tunnelID;

    public TunnelCreatePacket(short tunnelID, byte[] handshake) {
        this.tunnelID = tunnelID;
        this.handshake = handshake;
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public void write(ByteBuffer buf) {
        buf.putShort(tunnelID);
        buf.putShort((short) handshake.length);
        buf.put(handshake);
    }

    public byte[] getHandshake() {
        return handshake;
    }

    public int getTunnelID() {
        return tunnelID;
    }

    public static PacketReader<TunnelCreatePacket> getReader() {
        return buf -> {
            short tunnelID = buf.getShort();
            int handshakeLength = buf.getShort();
            byte[] handshake = new byte[handshakeLength];
            buf.get(handshake);
            return new TunnelCreatePacket(tunnelID, handshake);
        };
    }
}
