package whs.bot.common.net;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import whs.bot.common.net.dispatchers.BlockingPacketDispatcher;
import whs.bot.common.net.dispatchers.Dispatcher;
import whs.bot.common.net.dispatchers.PacketDispatcher;
import whs.bot.common.net.dispatchers.SynchronousPacketDispatcher;
import whs.bot.common.net.dispatchers.builders.AsynchronousPacketDispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.DispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.NullDispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.BlockingPacketDispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.SendingPacketDispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.SynchronousPacketDispatcherBuilder;
import whs.bot.common.net.exceptions.BadTunnelHandshakeException;
import whs.bot.common.net.exceptions.DispatchException;
import whs.bot.common.net.exceptions.NoSuchTunnelException;
import whs.bot.common.net.exceptions.PacketHandlingException;
import whs.bot.common.net.exceptions.TunnelWasNotAcceptedException;
import whs.bot.common.net.packets.TunnelCreatePacket;
import whs.util.ChannelPair;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by misson20000 on 9/24/16.
 */
@SuppressWarnings("ALL")
public class PacketControllerTest {

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

    @Test
    public void closeTest() throws Exception {
        controllerB.close();
        assertTrue(controllerB.isClosed());
    }

    @Test
    public void canEstablishTunnel() throws Exception {
        String handshake = "Hello, World!";
        PacketTunnel.Connection connA = controllerA.openTunnel(handshake.getBytes(), new NullDispatcherBuilder());
        int id = connA.getId();
        PacketTunnel.Connection connB = controllerB.connectTunnel(id, handshake.getBytes(), new NullDispatcherBuilder());
        connA.getFuture().get();
        connB.getFuture().get();
    }

    @Test
    public void tunnelWaitLocalConn() throws Exception {
        String handshake = "Hello, World!";
        PacketTunnel.Connection connA = controllerA.openTunnel(handshake.getBytes(), new NullDispatcherBuilder());
        int id = connA.getId();
        Thread.sleep(50);
        PacketTunnel.Connection connB = controllerB.connectTunnel(id, handshake.getBytes(), new NullDispatcherBuilder());
        connA.getFuture().get();
        connB.getFuture().get();
    }

    @Test
    public void badTunnelHandshakeTest() throws Exception {
        PacketTunnel.Connection connA = controllerA.openTunnel("Foobie Bletch".getBytes(), new NullDispatcherBuilder());
        int id = connA.getId();
        Thread.sleep(50);
        PacketTunnel.Connection connB = controllerB.connectTunnel(id, "REEEEE".getBytes(), new NullDispatcherBuilder());
        expectedException.expectCause(isA(TunnelWasNotAcceptedException.class));
        connA.getFuture().get();
        connB.getFuture().get();
    }

    @Test
    public void badTunnelHandshakeTest2() throws Exception {
        PacketTunnel.Connection connA = controllerA.openTunnel("Foobie Bletch".getBytes(), new NullDispatcherBuilder());
        int id = connA.getId();
        PacketTunnel.Connection connB = controllerB.connectTunnel(id, "REEEEE".getBytes(), new NullDispatcherBuilder());
        expectedException.expectCause(isA(BadTunnelHandshakeException.class));
        connB.getFuture().get();
        connA.getFuture().get();
    }

    @Test
    public void canMultipleTunnelsBeEstablished() throws Exception {
        String handshake1 = "Hello, World, this is tunnel A!";
        String handshake2 = "Hello, World, this is tunnel B!";

        PacketTunnel.Connection connA1 = controllerA.openTunnel(handshake1.getBytes(), new NullDispatcherBuilder());
        PacketTunnel.Connection connA2 = controllerA.openTunnel(handshake2.getBytes(), new NullDispatcherBuilder());
        PacketTunnel.Connection connB1 = controllerB.connectTunnel(connA1.getId(), handshake1.getBytes(), new NullDispatcherBuilder());
        PacketTunnel.Connection connB2 = controllerB.connectTunnel(connA2.getId(), handshake2.getBytes(), new NullDispatcherBuilder());
        connA1.getFuture().get();
        connB1.getFuture().get();
        connA2.getFuture().get();
        connB2.getFuture().get();
    }

    @Test
    public void pollingDispatcher() throws Exception {
        String handshake = "beep beep, son. beep, boop.";

        BlockingPacketDispatcherBuilder dispatchBuilder = new BlockingPacketDispatcherBuilder();

        dispatchBuilder.packet(4, TestPacket.getReader());
        dispatchBuilder.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<BlockingPacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilder);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilder);
        BlockingPacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;

        dispatchA.send(new TestPacket(400));
        dispatchA.send(new TestPacket2(800));
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket.class));
        assertEquals(((TestPacket) packet).getTestValue(), 400);
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 800);

        dispatchB.send(new TestPacket(500));
        dispatchB.send(new TestPacket2(900));
        while((packet = dispatchA.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket.class));
        assertEquals(((TestPacket) packet).getTestValue(), 500);
        while((packet = dispatchA.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 900);
    }

    @Test
    public void pollingDispatcherExplicitTunID() throws Exception {
        String handshake = "beep beep, son. beep, boop.";

        BlockingPacketDispatcherBuilder dispatchBuilder = new BlockingPacketDispatcherBuilder();

        dispatchBuilder.packet(4, TestPacket.getReader());
        dispatchBuilder.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<BlockingPacketDispatcher> connA = controllerA.openTunnel(80, handshake.getBytes(), dispatchBuilder);
        assertEquals(connA.getId(), 80);
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(80, handshake.getBytes(), dispatchBuilder);
        BlockingPacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;

        dispatchA.send(new TestPacket(400));
        dispatchA.send(new TestPacket2(800));
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket.class));
        assertEquals(((TestPacket) packet).getTestValue(), 400);
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 800);

        dispatchB.send(new TestPacket(500));
        dispatchB.send(new TestPacket2(900));
        while((packet = dispatchA.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket.class));
        assertEquals(((TestPacket) packet).getTestValue(), 500);
        while((packet = dispatchA.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 900);
    }

    @Test
    public void badDispatch() throws Exception {
        String handshake = "to dispatch, or not to dispatch?";

        DispatcherBuilder<PacketDispatcher> dispatchBuilderA = new SendingPacketDispatcherBuilder();
        BlockingPacketDispatcherBuilder dispatchBuilderB = new BlockingPacketDispatcherBuilder();

        dispatchBuilderB.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<PacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilderA);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilderB);
        PacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        dispatchA.send(new TestPacket(5));
        dispatchA.send(new TestPacket2(6));

        Packet packet;
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 6);
        assertThat(controllerB.getWarningQueue(), Matchers.hasItem(Matchers.instanceOf(DispatchException.class)));
        controllerB.getWarningQueue().clear();
    }

    @Test
    public void synchronousDispatcher() throws Exception {
        String handshake = "a shoe, a hue, and a fu manchu.";

        SynchronousPacketDispatcherBuilder dispatchBuilderA = new SynchronousPacketDispatcherBuilder();
        BlockingPacketDispatcherBuilder dispatchBuilderB = new BlockingPacketDispatcherBuilder();
        dispatchBuilderA.packet(4, TestPacket.getReader(), (dispatch, tp) -> {
            assertEquals(tp.getTestValue(), 404);
            dispatch.send(new TestPacket2(505));
        });
        dispatchBuilderB.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<SynchronousPacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilderA);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilderB);
        SynchronousPacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;
        dispatchB.send(new TestPacket(404));
        while(!dispatchA.dispatch()) { }
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 505);
    }

    @Test
    public void synchronousDispatcherExceptionTest() throws Exception {
        String handshake = "In capitalist America, BANK ROBS YOU!";

        SynchronousPacketDispatcherBuilder dispatchBuilderA = new SynchronousPacketDispatcherBuilder();
        BlockingPacketDispatcherBuilder dispatchBuilderB = new BlockingPacketDispatcherBuilder();
        dispatchBuilderA.packet(4, TestPacket.getReader(), (dispatch, tp) -> {
            assertEquals(tp.getTestValue(), 404);
            throw new TestException();
        });
        dispatchBuilderB.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<SynchronousPacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilderA);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilderB);
        SynchronousPacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;
        dispatchB.send(new TestPacket(404));
        expectedException.expect(PacketHandlingException.class);
        expectedException.expectCause(isA(TestException.class));
        while(!dispatchA.dispatch()) { }
    }

    @Test
    public void asyncDispatcherTest() throws Exception {
        String handshake = "Pete and Repete are in a boat. Pete jumps out. Who is left in the boat?";

        AsynchronousPacketDispatcherBuilder dispatchBuilderA = new AsynchronousPacketDispatcherBuilder();
        BlockingPacketDispatcherBuilder dispatchBuilderB = new BlockingPacketDispatcherBuilder();
        dispatchBuilderA.packet(4, TestPacket.getReader(), (dispatch, tp) -> {
            assertEquals(tp.getTestValue(), 404);
            dispatch.send(new TestPacket2(505));
        });
        dispatchBuilderB.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<PacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilderA);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilderB);
        PacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;
        dispatchB.send(new TestPacket(404));
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 505);
    }

    @Test
    public void asyncDispatcherExceptionTest() throws Exception {
        String handshake = "Pete and Repete are in a boat. Pete jumps out. Who is left in the boat?";

        AsynchronousPacketDispatcherBuilder dispatchBuilderA = new AsynchronousPacketDispatcherBuilder();
        BlockingPacketDispatcherBuilder dispatchBuilderB = new BlockingPacketDispatcherBuilder();
        dispatchBuilderA.packet(4, TestPacket.getReader(), (dispatch, tp) -> {
            assertEquals(tp.getTestValue(), 404);
            dispatch.send(new TestPacket2(505));
            throw new TestException();
        });
        dispatchBuilderB.packet(5, TestPacket2.getReader());

        PacketTunnel.Connection<PacketDispatcher> connA = controllerA.openTunnel(handshake.getBytes(), dispatchBuilderA);
        int id = connA.getId();
        PacketTunnel.Connection<BlockingPacketDispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), dispatchBuilderB);
        PacketDispatcher dispatchA = connA.getFuture().get();
        BlockingPacketDispatcher dispatchB = connB.getFuture().get();

        Packet packet;
        dispatchB.send(new TestPacket(404));
        while((packet = dispatchB.poll()) == null) {}
        assertThat(packet, Matchers.instanceOf(TestPacket2.class));
        assertEquals(((TestPacket2) packet).getTestValue(), 505);
        assertThat(controllerA.getWarningQueue(), Matchers.hasItem(Matchers.instanceOf(PacketHandlingException.class)));
        controllerA.getWarningQueue().clear();
    }

    @Test
    public void noSuchTunnelDispatch() throws Exception {
        Class[] innerClasses = PacketController.class.getDeclaredClasses();
        Class tunneledPacket = Arrays.stream(innerClasses).filter(clazz -> clazz.getName().contains("TunneledPacket")).findFirst().get();
        assertNotNull(tunneledPacket);
        Constructor ctor = tunneledPacket.getConstructor(PacketController.class, PacketTunnel.class, Packet.class);
        assertNotNull(ctor);
        ctor.setAccessible(true);

        Method send = PacketController.class.getDeclaredMethod("send", tunneledPacket);
        send.setAccessible(true);

        send.invoke(controllerA, ctor.newInstance(controllerA, new PacketTunnel() {
            @Override
            public void send(Packet p) {
                throw new RuntimeException("This should never be called.");
            }

            @Override
            public int getId() {
                return 456;
            }
        }, new TestPacket(999)));

        String handshake = "Hello, World!";
        PacketTunnel.Connection<Dispatcher> connA = controllerA.openTunnel(handshake.getBytes(), new NullDispatcherBuilder());
        int id = connA.getId();
        PacketTunnel.Connection<Dispatcher> connB = controllerB.connectTunnel(id, handshake.getBytes(), new NullDispatcherBuilder());
        Dispatcher dispatchA = connA.getFuture().get();
        Dispatcher dispatchB = connB.getFuture().get();

        assertThat(controllerB.getWarningQueue(), Matchers.hasItem(Matchers.instanceOf(NoSuchTunnelException.class)));
        controllerB.getWarningQueue().clear();
    }

    @Test
    public void noSuchTunnelHandshake() throws Exception {
        String handshake = "????";
        Class[] innerClasses = PacketController.class.getDeclaredClasses();
        Class tunneledPacket = Arrays.stream(innerClasses).filter(clazz -> clazz.getName().contains("TunneledPacket")).findFirst().get();
        assertNotNull(tunneledPacket);
        Constructor ctor = tunneledPacket.getConstructor(PacketController.class, PacketTunnel.class, Packet.class);
        assertNotNull(ctor);
        ctor.setAccessible(true);

        Method send = PacketController.class.getDeclaredMethod("send", tunneledPacket);
        send.setAccessible(true);

        send.invoke(controllerA, ctor.newInstance(controllerA, new PacketTunnel() {
            @Override
            public void send(Packet p) {
                throw new RuntimeException("This should never be called.");
            }

            @Override
            public int getId() {
                return 0;
            }
        }, new TunnelCreatePacket((short) 80, handshake.getBytes())));

        PacketTunnel.Connection<Dispatcher> connB = controllerB.connectTunnel(80, handshake.getBytes(), new NullDispatcherBuilder());
        Dispatcher dispatchB = connB.getFuture().get();
        expectedException.expect(PacketHandlingException.class);
        expectedException.expectCause(isA(NoSuchTunnelException.class));
        throw controllerA.getWarningQueue().take();
    }

    @SuppressWarnings("WeakerAccess")
    public static class TestPacket extends Packet {
        private int testValue;

        public TestPacket(int testValue) {
            this.testValue = testValue;
        }

        @Override
        public int getType() {
            return 4;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.putInt(testValue);
        }

        public int getTestValue() {
            return testValue;
        }

        public static PacketReader<TestPacket> getReader() {
            return (buf) -> new TestPacket(buf.getInt());
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class TestPacket2 extends Packet {
        private int testValue;

        public TestPacket2(int testValue) {
            this.testValue = testValue;
        }

        @Override
        public int getType() {
            return 5;
        }

        @Override
        public void write(ByteBuffer buf) {
            buf.putInt(testValue);
        }

        public int getTestValue() {
            return testValue;
        }

        public static PacketReader<TestPacket2> getReader() {
            return (buf) -> new TestPacket2(buf.getInt());
        }
    }

    private class TestException extends Exception {

    }
}