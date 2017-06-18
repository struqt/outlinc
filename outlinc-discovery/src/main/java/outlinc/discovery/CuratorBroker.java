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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangkang on 17/06/2017
 */
public class CuratorBroker<Content>
    implements
    CuratorListener,
    ServiceBroker<Content>,
    ServiceRegistry<Content>,
    ServiceProducer<Content> {

    private final static Logger log = LoggerFactory.getLogger(CuratorBroker.class);
    private final CuratorConfig config;
    private final Class<Content> contentClass;
    private final ConcurrentMap<String, ServiceInstance<Content>> instanceMap;
    private final ConcurrentMap<String, ServiceProvider<Content>> providerMap;
    private ServiceDiscovery<Content> discovery;

    public CuratorBroker(Class<Content> contentClass) {
        this(new Properties(), contentClass);
    }

    public CuratorBroker(Properties props, Class<Content> contentClass) {
        this(new ConfigurationObjectFactory(props).build(CuratorConfig.class), contentClass);
    }

    public CuratorBroker(CuratorConfig config, Class<Content> contentClass) {
        this.config = config;
        this.contentClass = contentClass;
        this.instanceMap = new ConcurrentHashMap<String, ServiceInstance<Content>>();
        this.providerMap = new ConcurrentHashMap<String, ServiceProvider<Content>>();
    }

    @Override
    public void close() throws IOException {
        for (ServiceProvider<Content> provider : providerMap.values()) {
            CloseableUtils.closeQuietly(provider);
        }
        providerMap.clear();
        instanceMap.clear();
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
    public ServiceEntity<Content> produce(String serviceName) {
        return produce(serviceName, config.getProviderStrategy());
    }

    @Override
    public ServiceEntity<Content> produce(String serviceName, Strategy strategy) {
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
                        //.threadFactory()
                        //.additionalFilter()
                        .downInstancePolicy(new DownInstancePolicy(
                            config.getDownInstanceTimeoutMs(),
                            TimeUnit.MILLISECONDS,
                            config.getDownInstanceThreshold()))
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
            instanceMap.put(instance.getId(), instance);
            return new ServiceEntity.Builder<Content>()
                .instanceId(instance.getId())
                .name(instance.getName())
                .host(instance.getAddress())
                .port(instance.getPort())
                .sslPort(instance.getSslPort())
                .content(instance.getPayload())
                .uri(instance.buildUriSpec())
                .enabled(instance.isEnabled())
                .registerUTC(instance.getRegistrationTimeUTC())
                .build();
        }
        return null;
    }

    @Override
    public void reportError(ServiceEntity<Content> entity) {
        ServiceProvider<Content> provider = providerMap.get(entity.getName());
        ServiceInstance<Content> instance = instanceMap.get(entity.getInstanceId());
        if (provider != null && instance != null) {
            provider.noteError(instance);
        }
    }

    @Override
    public String register(String serviceName, Content content) {
        ServiceEntity<Content> node = new ServiceEntity.Builder<Content>()
            .name(serviceName)
            .content(content)
            .build();
        return this.register(node);
    }

    @Override
    public String register(ServiceEntity<Content> node) {
        if (discovery == null) {
            return "";
        }
        ServiceInstance<Content> instance = null;
        try {
            ServiceInstanceBuilder<Content> b = ServiceInstance.<Content>builder()
                .name(node.getName())
                .address(node.getAddress())
                .port(node.getPort())
                .payload(node.getContent())
                .enabled(true);
            if (node.getSslPort() != null) {
                b.sslPort(node.getSslPort());
            }
            if (node.getUri() != null && node.getUri().length() > 0) {
                b.uriSpec(new UriSpec(node.getUri()));
            }
            instance = b.build();
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
    public List<String> register(Collection<ServiceEntity<Content>> nodes) {
        ArrayList<String> ids = new ArrayList<String>(nodes.size());
        for (ServiceEntity<Content> node : nodes) {
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
