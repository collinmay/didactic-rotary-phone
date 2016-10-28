package whs.bot.common.net.dispatchers.impl;

import whs.bot.common.net.Packet;
import whs.bot.common.net.PacketHandler;
import whs.bot.common.net.PacketTunnel;
import whs.bot.common.net.dispatchers.AbstractPacketDispatcher;
import whs.bot.common.net.exceptions.PacketHandlingException;
import whs.bot.common.net.PacketReader;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by misson20000 on 9/24/16.
 */
