package whs.bot.common.net;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketTunnel {
    void send(Packet p);
    int getId();

    interface Connection {
        int getId();
        CompletableFuture<PacketTunnel> getFuture();
    }
}
