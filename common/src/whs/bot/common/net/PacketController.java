package whs.bot.common.net;

import whs.bot.common.net.dispatchers.PacketDispatcher;
import whs.bot.common.net.dispatchers.builders.AsynchronousPacketDispatcherBuilder;
import whs.bot.common.net.dispatchers.builders.DispatcherBuilder;
import whs.bot.common.net.dispatchers.Dispatcher;
import whs.bot.common.net.exceptions.BadTunnelHandshakeException;
import whs.bot.common.net.exceptions.DispatchException;
import whs.bot.common.net.exceptions.InvalidTunnelStateException;
import whs.bot.common.net.exceptions.NoSuchTunnelException;
import whs.bot.common.net.exceptions.PacketHandlingException;
import whs.bot.common.net.exceptions.TunnelWasNotAcceptedException;
import whs.bot.common.net.packets.TunnelCreatePacket;
import whs.bot.common.net.packets.TunnelHandshakePacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 9/24/16.
 */
public class PacketController {
    private final ByteChannel channel;
    private final PacketTunnelImpl controllerTun;
    private final Map<Integer, PacketTunnelImpl> tunnels = new HashMap<>();
    private final BlockingQueue<TunneledPacket> outputQueue;
    private final BlockingQueue<Exception> warningQueue;

    private final Map<Integer, TunnelConnectionState> tunnelStates = new HashMap<>();
    private boolean isClosed;

    private short tunnelID = 1;

    public PacketController(ByteChannel channel) {
        this.channel = channel;
        this.outputQueue = new LinkedBlockingQueue<>(100);
        this.warningQueue = new LinkedBlockingQueue<>();
        this.controllerTun = new PacketTunnelImpl(0, new AsynchronousPacketDispatcherBuilder().packet(1, TunnelCreatePacket.getReader(), (dispatch, packet) -> {
            synchronized (tunnelStates) {
                int id = packet.getTunnelID();
                byte[] handshake = packet.getHandshake();
                TunnelConnectionState state = tunnelStates.get(id);
                if (state == null) {
                    state = new TunnelConnectionState();
                    state.id = id;
                    state.state = TunnelConnState.WAITING_FOR_LOCAL_CONNECT;
                    state.handshake = handshake;
                    tunnelStates.put(id, state);
                } else {
                    if (state.state == TunnelConnState.WAITING_FOR_REMOTE_OPEN) {
                        if (Arrays.equals(handshake, state.handshake)) {
                            state.completableFuture.complete(new PacketTunnelImpl(id, state.dispatcherBuilder));
                        } else {
                            state.completableFuture.completeExceptionally(new BadTunnelHandshakeException(handshake, state.handshake));
                        }
                    } else {
                        throw new InvalidTunnelStateException();
                    }
                }
            }
        }).packet(2, TunnelHandshakePacket.getReader(), (dispatch, packet) -> {
            synchronized (tunnelStates) {
                int id = packet.getTunnelID();
                boolean accepted = packet.wasAccepted();
                TunnelConnectionState state = tunnelStates.get(id);
                if (state == null) {
                    throw new NoSuchTunnelException(id);
                }
                if (accepted) {
                    state.completableFuture.complete(new PacketTunnelImpl(id, state.dispatcherBuilder));
                } else {
                    state.completableFuture.completeExceptionally(new TunnelWasNotAcceptedException());
                }
            }
        }));
        this.tunnels.put(0, controllerTun);

        Thread inputThread = new Thread(() -> {
            try {
                ByteBuffer inputBuffer = ByteBuffer.allocate(8192);
                while (channel.isOpen() && !isClosed) {
                    inputBuffer.limit(4);
                    while (inputBuffer.hasRemaining()) {
                        channel.read(inputBuffer);
                    }
                    inputBuffer.flip();

                    int tunnelID = inputBuffer.getShort();
                    int length = inputBuffer.getShort();
                    inputBuffer.clear();
                    inputBuffer.limit(length);
                    while (inputBuffer.position() < length) {
                        channel.read(inputBuffer);
                    }
                    inputBuffer.flip();
                    if (tunnels.containsKey(tunnelID)) {
                        try {
                            tunnels.get(tunnelID).dispatch(inputBuffer);
                        } catch (DispatchException | PacketHandlingException e) {
                            warningQueue.add(e);
                        }
                    } else {
                        warningQueue.add(new NoSuchTunnelException(tunnelID));
                    }
                    inputBuffer.clear();
                }
            } catch (IOException e) {
                isClosed = true;
                if (!(e instanceof ClosedChannelException)) {
                    e.printStackTrace();
                }
            }
        });
        inputThread.setDaemon(true);
        inputThread.setName("PacketController input thread");
        inputThread.start();
        Thread outputThread = new Thread(() -> {
            ByteBuffer headerBuffer = ByteBuffer.allocate(4);
            ByteBuffer outputBuffer = ByteBuffer.allocate(8192);

            try {
                while (channel.isOpen() && !isClosed) {
                    try {
                        TunneledPacket packet = outputQueue.take();
                        outputBuffer.putShort((short) packet.getPacket().getType());
                        packet.getPacket().write(outputBuffer);
                        outputBuffer.flip();

                        headerBuffer.putShort((short) packet.getTunnel().getId());
                        headerBuffer.putShort((short) outputBuffer.limit());
                        headerBuffer.flip();

                        while (headerBuffer.hasRemaining()) {
                            channel.write(headerBuffer);
                        }

                        while (outputBuffer.hasRemaining()) {
                            channel.write(outputBuffer);
                        }

                        headerBuffer.clear();
                        outputBuffer.clear();

                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (IOException e) {
                isClosed = true;
                if (!(e instanceof ClosedChannelException)) {
                    e.printStackTrace();
                }
            }
        });
        outputThread.setDaemon(true);
        outputThread.setName("PacketController output thread");
        outputThread.start();
    }

    public BlockingQueue<Exception> getWarningQueue() {
        return warningQueue;
    }

    private short allocTunnelID() {
        while(tunnelStates.containsKey((int) tunnelID) || tunnels.containsKey((int) tunnelID)) { tunnelID++; }
        return tunnelID;
    }

    public <T extends Dispatcher> PacketTunnel.Connection<T> openTunnel(byte[] handshake, DispatcherBuilder<T> dispatcherBuilder) {
        synchronized(tunnelStates) {
            return openTunnel(allocTunnelID(), handshake, dispatcherBuilder);
        }
    }

    public <T extends Dispatcher> PacketTunnel.Connection<T> openTunnel(int id, byte[] handshake, DispatcherBuilder<T> dispatcherBuilder) {
        synchronized(tunnelStates) {
            TunnelConnectionState state = new TunnelConnectionState();
            state.id = id;
            state.state = TunnelConnState.WAITING_FOR_REMOTE_CONNECT;
            state.dispatcherBuilder = dispatcherBuilder;
            tunnelStates.put(id, state);

            controllerTun.send(new TunnelCreatePacket((short) id, handshake));

            return state;
        }
    }

    public <T extends Dispatcher> PacketTunnel.Connection<T> connectTunnel(int id, byte[] handshake, DispatcherBuilder<T> dispatcher) {
        synchronized (tunnelStates) {
            TunnelConnectionState state = tunnelStates.get(id);
            if (state == null) {
                state = new TunnelConnectionState();
                state.id = id;
                state.state = TunnelConnState.WAITING_FOR_REMOTE_OPEN;
                state.handshake = handshake;
                state.dispatcherBuilder = dispatcher;
                tunnelStates.put(id, state);
            } else {
                if (state.state == TunnelConnState.WAITING_FOR_LOCAL_CONNECT) {
                    if (Arrays.equals(handshake, state.handshake)) {
                        state.completableFuture.complete(new PacketTunnelImpl(id, dispatcher));
                    } else {
                        state.completableFuture.completeExceptionally(new BadTunnelHandshakeException(state.handshake, handshake));
                    }
                }
            }

            return state;
        }
    }

    private void send(TunneledPacket p) {
        outputQueue.add(p);
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() throws IOException {
        isClosed = true;
        channel.close();
    }

    private class PacketTunnelImpl implements PacketTunnel {
        private int id;
        private Dispatcher dispatch;

        private PacketTunnelImpl(int id, DispatcherBuilder dispatch) {
            this.id = id;
            this.dispatch = dispatch.build(this);
        }

        @Override
        public void send(Packet p) {
            PacketController.this.send(new TunneledPacket(this, p));
        }

        private void dispatch(ByteBuffer packet) throws DispatchException, PacketHandlingException {
            dispatch.dispatch(packet);
        }

        @Override
        public int getId() {
            return id;
        }

        public Dispatcher getDispatcher() {
            return dispatch;
        }
    }

    private class TunneledPacket {
        private final PacketTunnel tunnel;
        private final Packet packet;

        public TunneledPacket(PacketTunnel tunnel, Packet p) {
            this.tunnel = tunnel;
            this.packet = p;
        }

        private PacketTunnel getTunnel() {
            return tunnel;
        }

        private Packet getPacket() {
            return packet;
        }
    }

    private class TunnelConnectionState<T extends Dispatcher> implements PacketTunnel.Connection<T> {
        private final CompletableFuture<PacketTunnelImpl> completableFuture = new CompletableFuture<>();
        private final CompletableFuture<T> dispatcherFuture;
        private TunnelConnState state;
        private int id;
        private byte[] handshake;
        private DispatcherBuilder dispatcherBuilder;

        private TunnelConnectionState() {
            dispatcherFuture = completableFuture.thenApply((tun) -> {
                tunnels.put(id, tun);
                if(state != TunnelConnState.WAITING_FOR_REMOTE_CONNECT) {
                    controllerTun.send(new TunnelHandshakePacket((short) id, true));
                }
                state = TunnelConnState.CONNECTED;
                return tun;
            }).thenApply((tun) -> (T) tun.getDispatcher());
            dispatcherFuture.exceptionally(throwable -> {
                if(state != TunnelConnState.WAITING_FOR_REMOTE_CONNECT) {
                    controllerTun.send(new TunnelHandshakePacket((short) id, false));
                }
                state = TunnelConnState.ERROR;
                throwable.printStackTrace();
                return null;
            });
        }

        @Override
        public int getId() {
            return id;
        }

        public CompletableFuture<T> getFuture() {
            return dispatcherFuture;
        }

    }

    private enum TunnelConnState {
        WAITING_FOR_REMOTE_OPEN, WAITING_FOR_REMOTE_CONNECT, WAITING_FOR_LOCAL_CONNECT, CONNECTED, ERROR
    }
}
