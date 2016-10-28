package whs.bot.common.net;

import whs.bot.common.net.packets.TunnelCreatePacket;
import whs.bot.common.net.packets.TunnelHandshakePacket;
import whs.bot.common.net.requests.Request;
import whs.bot.common.net.requests.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
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
        this.controllerTun = new PacketTunnelImpl(0, new AsyncDispatcherBuilder()
                .packet(1, TunnelCreatePacket.getReader(), (packet) -> {
                    synchronized(tunnelStates) {
                        int id = packet.getTunnelID();
                        byte[] handshake = packet.getHandshake();
                        TunnelConnectionState state = tunnelStates.get(id);
                        if(state == null) {
                            state = new TunnelConnectionState();
                            state.id = id;
                            state.state = TunnelConnState.WAITING_FOR_LOCAL_CONNECT;
                            state.handshake = handshake;
                            tunnelStates.put(id, state);
                        } else {
                            if(state.state == TunnelConnState.WAITING_FOR_REMOTE_OPEN) {
                                if(Arrays.equals(handshake, state.handshake)) {
                                    state.completableFuture.complete(new PacketTunnelImpl(id, state.dispatcher));
                                } else {
                                    state.completableFuture.completeExceptionally(new BadTunnelHandshakeException(handshake, state.handshake));
                                }
                            } else {
                                throw new InvalidTunnelStateException();
                            }
                        }
                    }
                })
                .packet(2, TunnelHandshakePacket.getReader(), (packet) -> {
                    synchronized(tunnelStates) {
                        int id = packet.getTunnelID();
                        boolean accepted = packet.wasAccepted();
                        TunnelConnectionState state = tunnelStates.get(id);
                        if(state == null) {
                            throw new NoSuchTunnelException(id);
                        }
                        if(accepted) {
                            state.completableFuture.complete(new PacketTunnelImpl(id, state.dispatcher));
                        } else {
                            state.completableFuture.completeExceptionally(new TunnelWasNotAcceptedException());
                        }
                    }
                })
                .build().getAsDispatcher());
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
                    while(inputBuffer.position() < length) {
                        channel.read(inputBuffer);
                    }
                    inputBuffer.flip();
                    if(tunnels.containsKey(tunnelID)) {
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
            } catch(IOException e) {
                isClosed = true;
                if(!(e instanceof ClosedChannelException)) {
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
            } catch(IOException e) {
                isClosed = true;
                if(!(e instanceof ClosedChannelException)) {
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

    public PacketTunnel.Connection openTunnel(byte[] handshake, PacketDispatcher dispatcher) {
        synchronized(tunnelStates) {
            return openTunnel(allocTunnelID(), handshake, dispatcher);
        }
    }

    public PacketTunnel.Connection openTunnel(int id, byte[] handshake, PacketDispatcher dispatcher) {
        synchronized(tunnelStates) {
            TunnelConnectionState state = new TunnelConnectionState();
            state.id = id;
            state.state = TunnelConnState.WAITING_FOR_REMOTE_CONNECT;
            state.dispatcher = dispatcher;
            tunnelStates.put(id, state);

            controllerTun.send(new TunnelCreatePacket((short) id, handshake));

            return state;
        }
    }

    public PacketTunnel.Connection connectTunnel(int id, byte[] handshake, PacketDispatcher dispatcher) {
        synchronized(tunnelStates) {
            TunnelConnectionState state = tunnelStates.get(id);
            if(state == null) {
                state = new TunnelConnectionState();
                state.id = id;
                state.state = TunnelConnState.WAITING_FOR_REMOTE_OPEN;
                state.handshake = handshake;
                state.dispatcher = dispatcher;
                tunnelStates.put(id, state);
            } else {
                if(state.state == TunnelConnState.WAITING_FOR_LOCAL_CONNECT) {
                    if(Arrays.equals(handshake, state.handshake)) {
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
        private PacketDispatcher dispatch;

        private PacketTunnelImpl(int id, PacketDispatcher dispatch) {
            this.id = id;
            this.dispatch = dispatch;
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

    private class TunnelConnectionState implements PacketTunnel.Connection {
        private CompletableFuture<PacketTunnel> completableFuture = new CompletableFuture<>();
        private TunnelConnState state;
        private int id;
        private byte[] handshake;
        private PacketDispatcher dispatcher;

        private TunnelConnectionState() {
            completableFuture.thenAccept((tun) -> {
                tunnels.put(id, (PacketTunnelImpl) tun);
                if(state != TunnelConnState.WAITING_FOR_REMOTE_CONNECT) {
                    controllerTun.send(new TunnelHandshakePacket((short) id, true));
                }
                state = TunnelConnState.CONNECTED;
            });
            completableFuture.exceptionally(throwable -> {
                if(state != TunnelConnState.WAITING_FOR_REMOTE_CONNECT) {
                    controllerTun.send(new TunnelHandshakePacket((short) id, false));
                }
                state = TunnelConnState.ERROR;
                return null;
            });
        }

        @Override
        public int getId() {
            return id;
        }

        public CompletableFuture<PacketTunnel> getFuture() {
            return completableFuture;
        }

    }

    private enum TunnelConnState {
        WAITING_FOR_REMOTE_OPEN, WAITING_FOR_REMOTE_CONNECT, WAITING_FOR_LOCAL_CONNECT, CONNECTED, ERROR
    }
}
