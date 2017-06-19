package outlinc.proxy;

import org.skife.config.Config;
import org.skife.config.Default;

/**
 * Created by wangkang on 19/06/2017
 */
public interface HttpProxyConfig {

    @Config("proxy.http.host")
    @Default("0.0.0.0")
    String getHost();

    @Config("proxy.http.port")
    @Default("8081")
    int getPort();

    @Config("proxy.upstream.retry.max")
    @Default("3")
    int getUpstreamRetryMax();

    @Config("proxy.http.aggregated.kilobyte.max")
    @Default("512")
    int getAggregatedKilobyteMax();

}
