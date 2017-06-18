package outlinc.discovery;

/**
 * Created by wangkang on 17/06/2017
 */
public interface ServiceProducer<Content> {

    enum Strategy {
        RoundRobin, Random, Sticky
    }

    ServiceEntity<Content> produce(String serviceName);

    ServiceEntity<Content> produce(String serviceName, Strategy strategy);

}
