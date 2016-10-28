package whs.bot.common.net;

import java.util.Arrays;

/**
 * Created by misson20000 on 9/24/16.
 */
public class BadTunnelHandshakeException extends Exception {
    public BadTunnelHandshakeException(byte[] got, byte[] expected) {
        super("Bad tunnel handshake. Got " + Arrays.toString(got) + ", expected " + Arrays.toString(expected));
    }
}
