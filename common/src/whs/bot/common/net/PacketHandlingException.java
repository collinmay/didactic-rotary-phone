package whs.bot.common.net;

/**
 * Created by misson20000 on 9/24/16.
 */
public class PacketHandlingException extends Exception {
    public PacketHandlingException(Packet packet, Exception exception) {
        super("Error handling packet of type " + packet.getClass().getName() + " (" + packet.getType() + ")", exception);
    }
}
