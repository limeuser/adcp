# adcp

a database connection pool focused on:

* simplicity
* high performance
* observability

# metrics

* idle connections
* using connections
* wait threads
* connections using count
* thread avg wait time when there is no idle connection
* avg time of creating jdbc connection
* avg connection using time

## example

* com.fishqq.adcp.test.Example

```java
public static AdcpDataSource createByJdbcUrl()throws SQLException {
    String url="jdbc:postgresql://localhost:5432/mc_dev";
    String username="modeling";
    String password="modeling";

    // config jdbc
    AdcpDataSourceConfig config=new AdcpDataSourceConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);

    // config pool
    config.setMaxPoolSize(10);
    config.setMinIdle(2);
    config.setLogWarning(true);
    config.setLogMetricsPeriodSeconds(120);

    // metrics warning config
    config.getWarningConfig().setAvgWaitTimeMs(1000);

    // set driver properties
    config.setDriverProperty(PGProperty.APPLICATION_NAME.getName(),"my-app");
    config.setDriverProperty(PGProperty.ADAPTIVE_FETCH_MAXIMUM.getName(),100);

    return AdcpStaticFactory.createDataSource(config);
}

private static AdcpDataSource createByDataSource() {
    PGSimpleDataSource dataSource=new PGSimpleDataSource();
    dataSource.setUser("modeling");
    dataSource.setPassword("modeling");
    dataSource.setUrl("jdbc:postgresql://localhost:5432/mc_dev");
    dataSource.setDatabaseName("mc_dev");

    dataSource.setApplicationName("my-app");

    AdcpPoolConfig config=new AdcpPoolConfig();

    config.setPoolName("adcp");
    config.setMinIdle(2);
    config.setMaxPoolSize(10);

    // metrics warning config
    config.setLogWarning(true);
    config.setLogMetricsPeriodSeconds(120);

    return new AdcpDataSource(config,new WarningConfig(),dataSource);
}
```

## use in spring

```java

@Component
@ConfigurationProperties(
        prefix = "spring.datasource"
)
public class SpringDataSourceConfig extends AdcpDataSourceConfig {
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getPoolName() {
        return this.getName();
    }

    @Override
    public String setPoolName(String name) {
        this.setName(name);
    }
}
```

* create datasource bean

```java
@Bean
public DataSource dataSource(@Autowired SpringDataSourceConfig springDataSourceConfig)throws SQLException {
    return AdcpStaticFactory.createDataSource(springDataSourceConfig.getUrl(),springDataSourceConfig);
}
```
