package whs.bot.common.net;

/**
 * Created by misson20000 on 9/24/16.
 */
public class NoSuchTunnelException extends Exception {
    public NoSuchTunnelException(int id) {
        super("No such tunnel with ID " + id + " was created from this side");
    }
}
