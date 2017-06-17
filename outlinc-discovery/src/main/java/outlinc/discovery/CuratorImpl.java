package outlinc.discovery;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.curator.retry.RetryOneTime;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.strategies.RandomStrategy;
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy;
import org.apache.curator.x.discovery.strategies.StickyStrategy;
import org.skife.config.ConfigurationObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wangkang on 17/06/2017
 */
public class CuratorImpl<Content>
    implements
    CuratorListener,
    ServiceBroker<Content>,
    ServiceRegistry<Content>,
    ServiceProducer<Content> {

    private final static Logger log = LoggerFactory.getLogger(CuratorImpl.class);
    private ServiceDiscovery<Content> discovery;
    private final Class<Content> contentClass;
    private final CuratorConfig config;
    private final Map<String, ServiceInstance<Content>> instanceMap;
    private final Map<String, ServiceProvider<Content>> providerMap;

    public CuratorImpl(Class<Content> contentClass) {
        this(new Properties(), contentClass);
    }

    public CuratorImpl(Properties props, Class<Content> contentClass) {
        this(new ConfigurationObjectFactory(props).build(CuratorConfig.class), contentClass);
    }

    public CuratorImpl(CuratorConfig config, Class<Content> contentClass) {
        this.contentClass = contentClass;
        this.config = config;
        this.instanceMap = new ConcurrentHashMap<String, ServiceInstance<Content>>();
        this.providerMap = new ConcurrentHashMap<String, ServiceProvider<Content>>();
    }

    @Override
    public void close() throws IOException {
        for (ServiceProvider<Content> provider : providerMap.values()) {
            CloseableUtils.closeQuietly(provider);
        }
        providerMap.clear();
        //TODO close??
        this.instanceMap.clear();
        if (discovery != null) {
            CloseableUtils.closeQuietly(discovery);
            discovery = null;
        }
    }

    @Override
    public ServiceRegistry<Content> registry() {
        startDiscovery();
        return this;
    }

    @Override
    public ServiceProducer<Content> producer() {
        startDiscovery();
        return this;
    }

    @Override
    public Content produce(String serviceName) {
        return produce(serviceName, config.getProviderStrategy());
    }

    @Override
    public Content produce(String serviceName, Strategy strategy) {
        if (discovery == null) {
            return null;
        }
        ServiceInstance<Content> instance = null;
        if (providerMap.containsKey(serviceName)) {
            try {
                instance = providerMap.get(serviceName).getInstance();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                providerMap.remove(serviceName);
            }
        } else {
            try {
                Collection<ServiceInstance<Content>> instances =
                    discovery.queryForInstances(serviceName);
                if (instances.size() > 0) {
                    ServiceProvider<Content> provider = discovery
                        .serviceProviderBuilder()
                        .serviceName(serviceName)
                        .providerStrategy(makeProviderStrategy(strategy))
                        .build();
                    provider.start();
                    providerMap.put(serviceName, provider);
                    instance = provider.getInstance();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        if (instance != null) {
            return instance.getPayload();
        }
        return null;
    }

    @Override
    public String register(String serviceName, Content content) {
        ServiceNode<Content> node = new ServiceNode.Builder<Content>()
            .name(serviceName)
            .content(content)
            .build();
        return this.register(node);
    }

    @Override
    public String register(ServiceNode<Content> node) {
        if (discovery == null) {
            return "";
        }
        ServiceInstance<Content> instance = null;
        try {
            instance = ServiceInstance.<Content>builder()
                .name(node.getName())
                .address(node.getHost())
                .port(node.getPort())
                .sslPort(node.getSslPort())
                .uriSpec(new UriSpec(node.getRawUriSpec()))
                .enabled(node.isEnabled())
                .payload(node.getContent())
                .build();
            discovery.registerService(instance);
            log.info("register {}", instance);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        if (instance != null) {
            instanceMap.put(instance.getId(), instance);
            return instance.getId();
        }
        return "";
    }

    @Override
    public List<String> register(Collection<ServiceNode<Content>> nodes) {
        ArrayList<String> ids = new ArrayList<String>(nodes.size());
        for (ServiceNode<Content> node : nodes) {
            ids.add(register(node));
        }
        return ids;
    }

    @Override
    public Boolean unregister(String instanceId) {
        if (discovery == null) {
            return Boolean.FALSE;
        }
        if (!instanceMap.containsKey(instanceId)) {
            return Boolean.FALSE;
        }
        try {
            ServiceInstance<Content> instance = instanceMap.get(instanceId);
            instanceMap.remove(instanceId);
            discovery.unregisterService(instance);
            log.info("unregister {}", instance);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    public List<Boolean> unregister(Collection<String> instanceIds) {
        ArrayList<Boolean> results = new ArrayList<Boolean>(instanceIds.size());
        for (String id : instanceIds) {
            results.add(unregister(id));
        }
        return results;
    }

    @Override
    public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
        if (event == null || event.getWatchedEvent() == null) {
            return;
        }
        log.debug(event.getWatchedEvent().toString()); /*
        if (Watcher.Event.KeeperState.SyncConnected.equals(event.getWatchedEvent().getState())) {
            System.err.println(event);
        } //*/
    }

    @Override
    public CharSequence dumpInstanceAll() {
        StringBuilder s = new StringBuilder();
        s.append('\n').append("List all services and all instances").append('\n');
        if (discovery == null) {
            return s;
        }
        try {
            Collection<String> serviceNames = discovery.queryForNames();
            s.append("Found ").append(serviceNames.size()).append(" service name(s)").append('\n');
            for (String serviceName : serviceNames) {
                Collection<ServiceInstance<Content>> instances
                    = discovery.queryForInstances(serviceName);
                s.append("  ").append(serviceName).append(" : ").append(instances.size())
                    .append(" instance(s)").append('\n');
                for (ServiceInstance<Content> instance : instances) {
                    s.append("    ").append(instance).append('\n');
                }
            }
        } catch (Exception ignored) {
        }
        return s;
    }

    private void startDiscovery() {
        synchronized (this) {
            if (discovery != null) {
                return;
            }
        }
        int timeoutSess = Math.max(3000, config.getSessionTimeoutMs());
        int timeoutConn = Math.max(2000, config.getConnectionTimeoutMs());
        CuratorFramework client = CuratorFrameworkFactory
            .newClient(config.getConnectString(), timeoutSess, timeoutConn, new RetryOneTime(1));
        client.getCuratorListenable().addListener(this);
        client.start();
        discovery = ServiceDiscoveryBuilder
            .builder(contentClass)
            .client(client)
            .serializer(new JsonInstanceSerializer<Content>(contentClass))
            .watchInstances(config.getWatchInstances())
            .basePath(config.getBasePath())
            .build();
        try {
            discovery.start();
        } catch (Exception e) {
            discovery = null;
            log.error("Curator Init failed", e);
        }
    }

    protected ProviderStrategy<Content> makeProviderStrategy(Strategy strategy) {
        switch (strategy) {
            case RoundRobin:
                return new RoundRobinStrategy<Content>();
            case Random:
                return new RandomStrategy<Content>();
            case Sticky:
                return new StickyStrategy<Content>(new RandomStrategy<Content>());
            default:
                return new RoundRobinStrategy<Content>();
        }
    }

}
