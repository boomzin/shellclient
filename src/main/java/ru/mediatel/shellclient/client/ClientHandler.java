package ru.mediatel.shellclient.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.springframework.stereotype.Component;

@Component
@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private String serverAnswer;

    public void setServerAnswer(String serverAnswer) {
        this.serverAnswer = serverAnswer;
    }

    public String getServerAnswer() {
        return serverAnswer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        serverAnswer = (String) msg;
    }
}