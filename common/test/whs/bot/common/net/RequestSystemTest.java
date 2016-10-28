package whs.bot.common.net;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import whs.bot.common.net.requests.Request;
import whs.bot.common.net.requests.RequestField;
import whs.bot.common.net.requests.RequestHandler;
import whs.bot.common.net.requests.Response;
import whs.bot.common.net.requests.ResponseField;
import whs.util.ChannelPair;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;

/**
 * Created by misson20000 on 10/26/16.
 */
public class RequestSystemTest {
    private ChannelPair pair = null;

    private PacketController controllerA;
    private PacketController controllerB;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        pair = new ChannelPair();
        controllerA = new PacketController(pair.getChannelA());
        controllerB = new PacketController(pair.getChannelB());
    }

    @After
    public void tearDown() throws Exception {
        pair.close();
        pair = null;
        if(!controllerA.getWarningQueue().isEmpty()) {
            if(controllerA.getWarningQueue().size() == 1) {
                throw new WarningsLeftException(controllerA.getWarningQueue().take());
            }
            throw new WarningsLeftException(new LinkedList<>(controllerA.getWarningQueue()));
        }
        if(!controllerB.getWarningQueue().isEmpty()) {
            if(controllerB.getWarningQueue().size() == 1) {
                throw new WarningsLeftException(controllerB.getWarningQueue().take());
            }
            throw new WarningsLeftException(new LinkedList<>(controllerB.getWarningQueue()));
        }
        controllerA = null;
        controllerB = null;
    }

    @SuppressWarnings("WeakerAccess")
    private class WarningsLeftException extends Exception {
        public WarningsLeftException(List<Exception> exceptions) {
            super("Warnings left: " + exceptions.stream().map(Exception::getMessage).collect(Collectors.toList()).toString());
        }
        public WarningsLeftException(Exception exception) {
            super(exception);
        }
    }
/*
    @Test
    public void synchronousRequestHandler() throws Exception {
        String handshake = "In capitalist America, BANK ROBS YOU!";

        SynchronousRequestDispatcher dispatchA = new SynchronousRequestDispatcher(new SynchronousRequestHandlerTest());
        SynchronousRequestDispatcher dispatchB = new SynchronousRequestDispatcher(new SynchronousRequestHandlerTest());

        PacketTunnel.Connection connA = controllerA.openTunnel(handshake.getBytes(), dispatchA.getAsDispatcher());
        int id = connA.getId();
        PacketTunnel.Connection connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchB.getAsDispatcher());
        PacketTunnel tunA = connA.getFuture().get();
        PacketTunnel tunB = connB.getFuture().get();


    }

    private class SynchronousRequestHandlerTest {
        @RequestHandler
        public NumericResponse addTwo(AddTwoRequest rq) {
            return new NumericResponse(rq.number + 2);
        }
    }

    private class AddTwoRequest extends Request<NumericResponse> {
        public int number;
    }

    private class NumericResponse extends Response {
        public int number;
    }
    */
}
