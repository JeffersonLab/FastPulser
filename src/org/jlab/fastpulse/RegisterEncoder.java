package org.jlab.fastpulse;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by john on 7/15/15.
 */
public class RegisterEncoder extends MessageToByteEncoder<Registers> {

    @Override
    public void encode(ChannelHandlerContext ctx, Registers regs, ByteBuf out) throws Exception {

        out.writeShort(0x5a5a);
        out.writeShort(0x0100);
        out.writeByte(0x03);
        out.writeBytes(regs.encode());

    }
}