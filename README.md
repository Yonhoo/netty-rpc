[![Coverage Status](https://codecov.io/gh/YonHoo/netty-rpc/branch/master/graph/badge.svg)](https://codecov.io/gh/YonHoo/netty-rpc)

this is a Netty-based RPC framework


add zookeeper registry extension in META-INF/extensions
> registry=com.yonhoo.nettyrpc.registry.ZookeeperRegistry

registry client config:
```properties
registry.address=127.0.0.1
registry.port=2181
registry.application=test-application
```

