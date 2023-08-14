package ru.mediatel.shellclient.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mediatel.shellclient.config.ShellHelper;

@Slf4j
@Component
public class NettyClient implements Runnable{
    public ChannelFuture future;
    private EventLoopGroup eventGroup = new NioEventLoopGroup();

    private String host;
    private int port;

    private final ClientHandler clientHandler;

    public NettyClient(ClientHandler clientHandler) {
        this.host = host;
        this.port = port;
        this.clientHandler = clientHandler;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void run() {


        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new StringDecoder(), new StringEncoder(), clientHandler);
            }
        });

        try {
            future = bootstrap.connect(host, port).sync();
        } catch (InterruptedException e) {
            clientHandler.setServerAnswer("Error: " + e.getMessage());
        }

    }

    public void shutdown() throws Exception {
        eventGroup.shutdownGracefully();
        future.channel().closeFuture().sync();
    }
}