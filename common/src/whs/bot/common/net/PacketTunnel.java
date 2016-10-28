package whs.bot.common.net;

import whs.bot.common.net.dispatchers.Dispatcher;

import java.util.concurrent.CompletableFuture;

/**
 * Created by misson20000 on 9/24/16.
 */
public interface PacketTunnel {
    void send(Packet p);
    int getId();

    interface Connection<T extends Dispatcher> {
        int getId();
        CompletableFuture<T> getFuture();
    }
}
