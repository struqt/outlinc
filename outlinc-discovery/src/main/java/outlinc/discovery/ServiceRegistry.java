package outlinc.discovery;

import java.util.Collection;
import java.util.List;

/**
 * Created by wangkang on 17/06/2017
 */
public interface ServiceRegistry<Content> {

    /**
     * Register a service instance
     *
     * @param serviceName Name of a service instance
     * @param content     Payload content of a service instance
     * @return UUID of a service instance
     */
    String register(String serviceName, Content content);

    /**
     * Register a service instance
     *
     * @param node Config of a service instance
     * @return UUID of a service instance
     */
    String register(ServiceNode<Content> node);

    /**
     * Register several service instances
     *
     * @param nodes Configs of several service instances
     * @return UUIDs of service instances
     */
    List<String> register(Collection<ServiceNode<Content>> nodes);

    /**
     * Unregister a service instance
     *
     * @param instanceId UUID of a service instance
     */
    Boolean unregister(String instanceId);

    /**
     * Unregister several service instances
     *
     * @param instanceIds UUIDs of service instances
     */
    List<Boolean> unregister(Collection<String> instanceIds);

}
