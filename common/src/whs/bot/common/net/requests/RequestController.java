package whs.bot.common.net.requests;

import whs.bot.common.net.PacketHandler;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.PacketDispatcher;

import java.util.concurrent.Future;

/**
 * Created by misson20000 on 10/26/16.
 */
public class RequestController implements PacketHandler<Response> {

    private final PacketTunnel tun;

    public RequestController(PacketTunnel t) {
        this.tun = t;
    }

    //public <T extends Response> Future<T> request(Request<T> request) {
    //    tun.send(request);
    //}

    @Override
    public void handle(PacketDispatcher dispatch, Response p) throws Exception {

    }
}
