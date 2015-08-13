package org.jlab.fastpulse;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.oio.OioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Created by john on 7/31/15.
 */
public class SocketListener {

    private Channel m_Channel;
    private ChannelFuture m_CloseFuture;
    private EventLoopGroup m_Group;
    private int m_Port;

    public List<Connector.ConnectFuture> m_ConnectionList = null;

    private static SocketListener s_Instance = null;


    static SocketListener instance() {

        if (s_Instance == null) {
            s_Instance = new SocketListener(FastPulserCLI.PORT);

            try {
                s_Instance.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return s_Instance;
    }

    SocketListener(int port) {
        m_Port = port;
    }

    public Channel channel() {
        return m_Channel;
    }

    /*
     * I'm thinking that the 'handler' should be an internal object used to track the receipt of registers
     * The user doesn't necessarily need to know what is going on behind the scenes, they should be able
     * to get a reference to a device by calling connect() which returns a future, from which they can get
     * a device interface
     *
     * Therefore the socket listener is a singleton, hidden from the end user.
     */

    public void connect(Connector.ConnectFuture cf) {
        if (m_ConnectionList == null) {
            m_ConnectionList = new ArrayList<>();
        }

        m_ConnectionList.add(cf);

        //connect procedure
        channel().writeAndFlush(new DatagramPacket(Command.ReadRegisters(), new InetSocketAddress(cf.getAddress(), FastPulserCLI.PORT)));
    }

    public void start(/*final FastPulserHandler handler*/) throws InterruptedException {

        // Needs group.shutdownGracefully as cleanup
        m_Group = new OioEventLoopGroup();

        Bootstrap b = new Bootstrap();
        b.group(m_Group)
                .channel(OioDatagramChannel.class)
                        //.option(ChannelOption.SO_BROADCAST, true)

                .handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {

                        System.out.println("initChannel");

                        ch.pipeline().addLast("encoder", new RegisterEncoder());
                        ch.pipeline().addLast("handler", new DatagramHandler());
                    }
                });


        System.out.println("Binding stream listener...");

        m_Channel = b.bind(m_Port).sync().channel();

        m_CloseFuture = m_Channel.closeFuture();

        // Link the close future to the handler?

        // This should really be a custom future which times out unless it receives registers
    }
}
