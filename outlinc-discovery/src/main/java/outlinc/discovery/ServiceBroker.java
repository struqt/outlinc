package outlinc.discovery;

import java.io.Closeable;

/**
 * Created by wangkang on 17/06/2017
 */
public interface ServiceBroker<Content> extends Closeable {

    ServiceRegistry<Content> registry();

    ServiceProducer<Content> producer();

    CharSequence dumpInstanceAll();

}
