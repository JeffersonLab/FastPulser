package org.jlab.fastpulse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.lang.ref.WeakReference;

/**
 * Created by john on 7/15/15.
 */
public class DatagramHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final AttributeKey<ChannelContext> CONTEXT_KEY = AttributeKey.valueOf("chan_ctx");

    private WeakReference<FastPulserHandler> m_Handler;

    private String m_Address;

    //private WeakReference<SocketListener> mw_Listener;

    DatagramHandler(/*SocketListener sl*/) {
        m_Address = "0.0.0.0";
        //mw_Listener = new WeakReference(sl);
    }

    public void setHandler(FastPulserHandler handler) {
        m_Handler = new WeakReference<>(handler);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Channel was just connected and became active

        System.out.println("channelActive");

        Attribute<ChannelContext> attr = ctx.attr(CONTEXT_KEY);

        ChannelContext channelContext = attr.get();

        if (channelContext == null) {
            channelContext = new ChannelContext(ctx);
            attr.set(channelContext);
        }

        // Create new device, tell the user that we have a new device
        //DeviceImpl device = new DeviceImpl(channelContext);

        /*
        FastPulserHandler handler = m_Handler.get();

        if (handler != null) {

            //handler.initDevice(device);

            // Do we need the handler to have a back reference to the context?
            //((AbstractClientHandler)pubHandler).setContext(channelContext);

            channelContext.setHandler(handler);
            //channelContext.setListener(mw_Listener.get());
            //channelContext.setDevice(device);

            attr.set(channelContext);
        }
        */

        // Send request for registers
        //ctx.writeAndFlush(new DatagramPacket(Command.ReadRegisters(), channelContext.getAddress()));

    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramPacket evt) throws Exception {
        ByteBuf buf = evt.content();

        System.out.println("channelRead");

        // Return without modifying the reader index of we don't have enough in the buffer
        if (buf.readableBytes() < 4)
            return;

        // 5a5a0303 is registers
        int mark = buf.readerIndex();

        int header = buf.getUnsignedShort(mark);

        if (header != 0x5a5a)
            return;

        int type = buf.getUnsignedShort(mark + 2);

        if (type == 0x0303) {   // This should be the only type this device sends

            buf.skipBytes(4);

            Attribute<ChannelContext> attr = ctx.attr(CONTEXT_KEY);

            ChannelContext channelContext = attr.get();

            // Parse registers into the specific devices register set
            channelContext.parseToDevice(buf, evt.sender());

            //DeviceImpl dev = channelContext.getDevice();

            // Sync the register values with the device
            //dev.getRegisters().decode(buf);

            //Lookup device based on ip address

        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        // We don't close the channel because we can keep serving requests.
    }
}
