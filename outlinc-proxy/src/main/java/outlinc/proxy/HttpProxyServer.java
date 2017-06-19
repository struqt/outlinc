package outlinc.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import outlinc.discovery.CuratorBroker;
import outlinc.discovery.ServiceBroker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by wangkang on 19/06/2017
 */
public class HttpProxyServer {

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : null;
        new HttpProxyServer(path).start();
    }

    private static final Logger log = LoggerFactory.getLogger(HttpProxyServer.class);
    private final Properties props;
    private final ServiceBroker<String> service;

    public HttpProxyServer() {
        this(null);
    }

    public HttpProxyServer(String configPath) {
        if (configPath == null || configPath.length() <= 0) {
            configPath = "/outlinc-proxy.properties";
        }
        InputStream inp = getClass().getResourceAsStream(configPath);
        props = new Properties();
        try {
            props.load(inp);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        this.service = new CuratorBroker<String>(props, String.class);
    }

    public void start() {
        HttpProxyConfig config = new ConfigurationObjectFactory(props).build(HttpProxyConfig.class);
        start(config);
    }

    public void start(HttpProxyConfig config) {
        final String inetHost = config.getHost();
        final int inetPort = config.getPort();
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        final EventLoopGroup workGroup = new NioEventLoopGroup();
        ServerBootstrap boot = HttpProxyHandler.init(config, service);
        boot.group(bossGroup, workGroup);
        try {
            log.info("Server is going to start ({}:{})", inetHost, inetPort);
            boot.bind(inetHost, inetPort).sync();
            log.info("Server is listening on {}:{}", inetHost, inetPort);
        } catch (Throwable e) {
            log.error(String.format("Failed to bind %s:%d", inetHost, inetPort), e);
            System.exit(80);
        } finally {
            Runtime.getRuntime().addShutdownHook(new Thread("ShutdownHook") {
                public void run() {
                    log.info("Server is going to stop");
                    bossGroup.shutdownGracefully();
                    workGroup.shutdownGracefully();
                    service.stop();
                    log.info("Server is stopped");
                }
            });
        }
    }

}
