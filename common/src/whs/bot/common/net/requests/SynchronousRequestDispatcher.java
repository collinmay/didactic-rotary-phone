package whs.bot.common.net.requests;

import whs.bot.common.net.DispatchException;
import whs.bot.common.net.PacketDispatcher;
import whs.bot.common.net.PacketHandlingException;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 10/26/16.
 */
public class SynchronousRequestDispatcher implements PacketDispatcher {

    @Override
    public void dispatch(ByteBuffer packet) throws DispatchException, PacketHandlingException {

    }
}
