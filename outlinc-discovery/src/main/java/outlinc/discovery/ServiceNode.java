package outlinc.discovery;

/**
 * Created by wangkang on 17/06/2017
 */
public class ServiceNode<T> {

    private final String name;
    private final String host;
    private final Integer port;
    private final Integer sslPort;
    private final String rawUriSpec;
    private final boolean enabled;
    private final T content;

    private ServiceNode
        (String name, String address, Integer port, Integer sslPort,
         String rawUriSpec, boolean enabled, T content) {
        this.name = name;
        this.host = address;
        this.port = port;
        this.sslPort = sslPort;
        this.rawUriSpec = rawUriSpec;
        this.enabled = enabled;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getSslPort() {
        return sslPort;
    }

    public String getRawUriSpec() {
        return rawUriSpec;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public T getContent() {
        return content;
    }

    static public class Builder<T> {

        private String name = "nameless";
        private String host = "127.0.0.1";
        private Integer port = 8080;
        private Integer sslPort = 443;
        private String rawUriSpec = "{scheme}://{host}:{port}";
        private boolean enabled = true;
        private T content = null;

        public ServiceNode<T> build() {
            return new ServiceNode<T>
                (name, host, port, sslPort, rawUriSpec, enabled, content);
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> host(String address) {
            this.host = address;
            return this;
        }

        public Builder<T> port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder<T> sslPort(Integer sslPort) {
            this.sslPort = sslPort;
            return this;
        }

        public Builder<T> rawUriSpec(String rawUriSpec) {
            this.rawUriSpec = rawUriSpec;
            return this;
        }

        public Builder<T> enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder<T> content(T content) {
            this.content = content;
            return this;
        }
    }

}
