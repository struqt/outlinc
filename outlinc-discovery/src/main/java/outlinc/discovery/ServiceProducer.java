package outlinc.discovery;

/**
 * Created by wangkang on 17/06/2017
 */
public interface ServiceProducer<Content> {

    enum Strategy {
        RoundRobin, Random, Sticky
    }

    Content produce(String serviceName);

    Content produce(String serviceName, Strategy strategy);

}
