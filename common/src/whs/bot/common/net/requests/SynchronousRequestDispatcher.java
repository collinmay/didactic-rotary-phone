package whs.bot.common.net.requests;

import whs.bot.common.net.exceptions.DispatchException;
import whs.bot.common.net.dispatchers.Dispatcher;
import whs.bot.common.net.exceptions.PacketHandlingException;

import java.nio.ByteBuffer;

/**
 * Created by misson20000 on 10/26/16.
 */
public class SynchronousRequestDispatcher implements Dispatcher {

    @Override
    public void dispatch(ByteBuffer packet) throws DispatchException, PacketHandlingException {

    }
}
