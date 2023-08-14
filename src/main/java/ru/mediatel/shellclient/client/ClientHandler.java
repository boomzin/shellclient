package ru.mediatel.shellclient.client;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mediatel.shellclient.config.ShellHelper;

@Component
@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {
    @Autowired
    private ShellHelper shellHelper;

    private String serverAnswer;

    public void setServerAnswer(String serverAnswer) {
        this.serverAnswer = serverAnswer;
    }

    public String getServerAnswer() {
        return serverAnswer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        serverAnswer = (String) msg;
        shellHelper.printInfo(serverAnswer);
    }
}