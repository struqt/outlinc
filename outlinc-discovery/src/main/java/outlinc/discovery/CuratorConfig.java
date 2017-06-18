package outlinc.discovery;

import org.skife.config.Config;
import org.skife.config.Default;

/**
 * Created by wangkang on 17/06/2017
 */
public interface CuratorConfig {

    @Config("discovery.curator.connectString")
    @Default("127.0.0.1:2181")
    String getConnectString();

    @Config("discovery.curator.connectionTimeoutMs")
    @Default("50000")
    int getConnectionTimeoutMs();

    @Config("discovery.curator.sessionTimeoutMs")
    @Default("60000")
    int getSessionTimeoutMs();

    @Config("discovery.curator.watchInstances")
    @Default("false")
    boolean getWatchInstances();

    @Config("discovery.curator.basePath")
    @Default("/outlinc-discovery")
    String getBasePath();


    @Config("discovery.curator.providerStrategy")
    @Default("RoundRobin")
    ServiceProducer.Strategy getProviderStrategy();

    @Config("discovery.curator.downInstanceTimeout")
    @Default("30000")
    long getDownInstanceTimeoutMs();

    @Config("discovery.curator.downInstanceThreshold")
    @Default("2")
    int getDownInstanceThreshold();

}
