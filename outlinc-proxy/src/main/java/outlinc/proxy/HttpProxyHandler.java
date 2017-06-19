package outlinc.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import outlinc.discovery.ServiceBroker;
import outlinc.discovery.ServiceEntity;
import outlinc.discovery.ServiceProducer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by wangkang on 19/06/2017
 */
public class HttpProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final static Logger log = LoggerFactory.getLogger(HttpProxyHandler.class);
    private final HttpProxyConfig config;
    private final ServiceProducer<String> service;
    private final ConcurrentMap<String, Channel> upstreams;

    private HttpProxyHandler(HttpProxyConfig config, ServiceProducer<String> service) {
        this.config = config;
        this.service = service;
        this.upstreams = new ConcurrentHashMap<String, Channel>();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest msg) throws Exception {
        log.debug(msg.toString());
        sendToUpstream(0, ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        for (Channel ch : upstreams.values()) {
            closeOnFlush(ch);
        }
        upstreams.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
        closeOnFlush(ctx.channel());
    }

    private void sendToUpstream(final int retryCount, final ChannelHandlerContext ctx, final FullHttpRequest msg) {
        if (retryCount >= config.getUpstreamRetryMax()) {
            closeOnFlush(ctx.channel());
            return;
        }
        try {
            final ServiceEntity<String> entity = fetchService(msg, service);
            if (entity == null) {
                closeOnFlush(ctx.channel());
                return;
            }
            final String remoteHost = entity.getAddress();
            final Integer remotePort = entity.getPort();
            final String key = remoteHost + ':' + remotePort;
            Channel upstream = upstreams.get(key);
            msg.retain();
            if (upstream != null) {
                if (upstream.isActive()) {
                    upstream.writeAndFlush(msg);
                    return;
                } else {
                    upstreams.remove(key);
                }
            }
            Bootstrap client = initUpstreamHandler(config, ctx.channel());
            client.connect(remoteHost, remotePort).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        Channel ch = future.channel();
                        ch.writeAndFlush(msg);
                        upstreams.put(ch.remoteAddress().toString().substring(1), ch);
                    } else {
                        log.error("Can't connect to upstream server {}:{}", remoteHost, remotePort);
                        service.reportError(entity);
                        Thread.sleep(100);
                        sendToUpstream(retryCount + 1, ctx, msg);
                    }
                }
            });
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            closeOnFlush(ctx.channel());
        }
    }

    static ServerBootstrap init(final HttpProxyConfig config, final ServiceBroker<String> service) {
        assert service != null;
        ServerBootstrap boot = new ServerBootstrap();
        boot.channel(NioServerSocketChannel.class);
        boot.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final int maxLen = config.getAggregatedKilobyteMax() * 1024;
                ch.pipeline()
                    .addLast(new HttpServerCodec())
                    .addLast(new HttpObjectAggregator(maxLen))
                    .addLast(new HttpProxyHandler(config, service.producer()));
            }
        });
        return boot;
    }

    private static Bootstrap initUpstreamHandler(final HttpProxyConfig config, final Channel downstream) {
        Bootstrap boot = new Bootstrap();
        boot.group(downstream.eventLoop());
        boot.channel(downstream.getClass());
        boot.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                final int maxLen = config.getAggregatedKilobyteMax() * 1024;
                ch.pipeline()
                    .addLast(new HttpClientCodec())
                    .addLast(new HttpObjectAggregator(maxLen))
                    .addLast(new HttpProxyUpstream(downstream));
            }
        });
        return boot;
    }

    private static ServiceEntity<String> fetchService(HttpRequest req, ServiceProducer<String> service) {
        String uri = req.uri().substring(1);
        int index = uri.indexOf('/');
        if (index >= 0) {
            uri = uri.substring(0, index);
        }
        if (uri.length() <= 0) {
            log.error("Bad uri: {}", uri);
            return null;
        }
        String serviceName = uri;
        ServiceEntity<String> entity = service.produce(serviceName);
        if (entity == null) {
            log.error("No service instance: {}", service.getConfig().getBasePath() + '/' + serviceName);
        }
        return entity;
    }

    private static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

}
