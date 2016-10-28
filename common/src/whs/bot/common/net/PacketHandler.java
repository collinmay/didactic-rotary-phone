package whs.bot.common.net;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketHandler<T extends Packet> {
    void handle(T p) throws Exception;
}
