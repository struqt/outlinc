package outlinc.test;

import org.apache.curator.test.TestingServer;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import outlinc.discovery.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ServiceDiscoveryTest {

    private static final Logger log = LoggerFactory.getLogger(ServiceDiscoveryTest.class);

    @Test
    public void test_01_registry() throws Exception {
        ServiceRegistry<String> registry = service.registry();
        String[] id = new String[3];
        id[0] = registry.register("test.a", "Service Contents a1");
        id[1] = registry.register("test.a", "Service Contents a2");
        id[2] = registry.register("test.b", "Service Contents b1");
        Assert.assertEquals(36, id[0].length());
        Assert.assertEquals(36, id[1].length());
        Assert.assertEquals(36, id[2].length());
        Assert.assertNotEquals(id[0], id[1]);
        Assert.assertNotEquals(id[0], id[2]);
        Assert.assertNotEquals(id[1], id[2]);
        log.debug(service.dumpInstanceAll().toString());
        registry.unregister(id[0]);
        registry.unregister(id[1]);
        registry.unregister(id[2]);
        log.debug(service.dumpInstanceAll().toString());
    }

    @Test
    public void test_02_provider() throws Exception {
        final String serviceName = "test.c";
        List<ServiceEntity<String>> nodes = new LinkedList<ServiceEntity<String>>();
        for (int i = 1; i < 6; i++) {
            ServiceEntity<String> node = new ServiceEntity.Builder<String>()
                .name(serviceName)
                .content("Service test.c#" + i)
                .build();
            nodes.add(node);
        }
        ServiceRegistry<String> r = service.registry();
        List<String> ids = r.register(nodes);
        ServiceProducer<String> producer = service.producer();
        for (int i = 0; i < 20; i++) {
            ServiceEntity<String> entity = producer.produce(serviceName);
            Assert.assertNotNull(entity);
            log.info(entity.toString());
        }
        r.unregister(ids);
    }

    @Test
    public void test_03_error_reporting() throws InterruptedException {
        final String serviceName = "test.e";
        service.registry().register(serviceName, "Service Contents e");
        ServiceProducer<String> producer = service.producer();
        ServiceEntity<String> entity = producer.produce(serviceName);
        Assert.assertNotNull(entity);
        assertTrue(producer.reportError(entity));
    }

    private ServiceBroker<String> service = null;

    @Before
    public void beforeEach() throws IOException {
        InputStream inp = getClass().getResourceAsStream("/discovery.properties");
        Properties props = new Properties();
        props.load(inp);
        props.setProperty("discovery.curator.connectString", connectString());
        service = new CuratorBroker<String>(props, String.class);
    }

    @After
    public void afterEach() throws IOException {
        service.close();
    }

    static private TestingServer zkServer = null;

    static private String connectString() {
        int retryCount = 0;
        while (zkServer == null) {
            try {
                retryCount++;
                if (retryCount > 10) {
                    return null;
                }
                zkServer = new TestingServer();
            } catch (Exception e) {
                zkServer = null;
                System.err.println("Getting bind exception - retrying to allocate server");
            }
        }
        return zkServer.getConnectString();
    }

}
