package outlinc.discovery;

/**
 * Created by wangkang on 17/06/2017
 */
public class ServiceEntity<T> {

    private final String instanceId;
    private final String name;
    private final String address;
    private final Integer port;
    private final Integer sslPort;
    private final String uri;
    private final boolean enabled;
    private final Long registerUTC;
    private final T content;

    private ServiceEntity
        (String instanceId, String name, String address, Integer port, Integer sslPort,
         String uri, boolean enabled, Long registeredUTC, T content) {
        this.instanceId = instanceId;
        this.name = name;
        this.address = address;
        this.port = port;
        this.sslPort = sslPort;
        this.uri = uri;
        this.enabled = enabled;
        this.registerUTC = registeredUTC;
        this.content = content;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getSslPort() {
        return sslPort;
    }

    public String getUri() {
        return uri;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Long getRegisterUTC() {
        return registerUTC;
    }

    public T getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ServiceEntity{" +
            "instanceId='" + instanceId + '\'' +
            ", name='" + name + '\'' +
            ", address='" + address + '\'' +
            ", port=" + port +
            ", sslPort=" + sslPort +
            ", uri='" + uri + '\'' +
            ", enabled=" + enabled +
            ", registerUTC=" + registerUTC +
            ", content=" + content +
            '}';
    }

    static public class Builder<T> {

        private String instanceId = "";
        private String name = "nameless";
        private String host = "127.0.0.1";
        private Integer port = 8080;
        private Integer sslPort = null;
        private String uri = "{scheme}://{address}:{port}";
        private boolean enabled = true;
        private Long registerUTC = 0L;
        private T content = null;

        public ServiceEntity<T> build() {
            return new ServiceEntity<T>
                (instanceId, name, host, port, sslPort, uri, enabled, registerUTC, content);
        }

        public Builder<T> instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
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

        public Builder<T> uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder<T> enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder<T> registerUTC(Long registeredUTC) {
            this.registerUTC = registeredUTC;
            return this;
        }

        public Builder<T> content(T content) {
            this.content = content;
            return this;
        }
    }

}
