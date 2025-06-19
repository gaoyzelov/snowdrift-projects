package com.snowdrift.protocol.jt808.server;

import com.snowdrift.core.exception.BaseException;
import com.snowdrift.protocol.jt808.codec.JT808MessageDecoder;
import com.snowdrift.protocol.jt808.codec.JT808MessageEncoder;
import com.snowdrift.protocol.jt808.consts.JT808Const;
import com.snowdrift.protocol.jt808.handler.JT808MessageInputHandler;
import com.snowdrift.protocol.jt808.properties.JT808Properties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JT808TcpServer
 *
 * @author gaoye
 * @date 2025/06/16 16:02:26
 * @description xxxxxxxx
 * @since 1.0
 */
@Slf4j
@AllArgsConstructor
public class JT808TcpServer implements Runnable {

    private final JT808Properties jt808Properties;

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(jt808Properties.getBossThreadCount());
        EventLoopGroup workGroup = new NioEventLoopGroup(jt808Properties.getWorkThreadCount());
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, JT808Const.DELIMITER));
                            ch.pipeline().addLast(new JT808MessageDecoder());
                            ch.pipeline().addLast(new JT808MessageEncoder());
                            ch.pipeline().addLast(new JT808MessageInputHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(jt808Properties.getServerPort()).sync();
            f.addListener((ChannelFutureListener) cf -> {
                if (cf.isSuccess()) {
                    log.info("JT808TcpServer启动成功！端口：{}", jt808Properties.getServerPort());
                } else {
                    log.error("JT808TcpServer启动失败！{}", cf.cause().getLocalizedMessage());
                }
            });
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new BaseException("JT808TcpServer启动异常", e);
        } finally {
            workGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}