# adcp

a database connection pool focused on:

* simplicity
* high performance

## use in spring

* config

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
public DataSource dataSource(@Autowired SpringDataSourceConfig springDataSourceConfig)throws SQLException{
        return AdcpStaticFactory.createDataSource(springDataSourceConfig.getUrl(),springDataSourceConfig);
        }
```
