package outlinc.discovery;

/**
 * Created by wangkang on 17/06/2017
 */
public interface ServiceProducer<Content> {

    enum Strategy {
        RoundRobin, Random, Sticky
    }

    CuratorConfig getConfig();

    ServiceEntity<Content> produce(String serviceName);

    ServiceEntity<Content> produce(String serviceName, Strategy strategy);

    /**
     * Report an error status when the instance is unavailable.
     * When error count reaches {@link CuratorConfig#getDownInstanceThreshold()}
     * the instance will be filtered locally.
     * When local time elapses {@link CuratorConfig#getDownInstanceTimeoutMs()} ms
     * the instance will be back again
     *
     * @param instanceId UUID of service instance
     * @see CuratorConfig#getDownInstanceThreshold()
     * @see CuratorConfig#getDownInstanceTimeoutMs()
     */
    boolean reportError(String instanceId);

    boolean reportError(ServiceEntity<Content> entity);

}
