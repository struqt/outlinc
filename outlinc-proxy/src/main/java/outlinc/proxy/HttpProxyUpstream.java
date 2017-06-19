package outlinc.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wangkang on 19/06/2017
 */
public class HttpProxyUpstream extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final static Logger log = LoggerFactory.getLogger(HttpProxyUpstream.class);
    private final Channel downstream;

    HttpProxyUpstream(Channel downstream) {
        this.downstream = downstream;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse msg) throws Exception {
        if (downstream == null) {
            throw new Exception("frontChannel is null");
        }
        msg.retain();
        downstream.writeAndFlush(msg).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.debug(msg.toString());
                } else {
                    closeOnFlush(future.channel());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        log.error(cause.getMessage(), cause);
        closeOnFlush(ctx.channel());
    }

    private static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
