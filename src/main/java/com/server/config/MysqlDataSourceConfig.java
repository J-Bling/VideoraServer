package com.server.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(basePackages = {"com.server.dao"})
public class MysqlDataSourceConfig {

    @Bean(name = "mysqlDataSource")
    @Primary
    public DataSource mysqlDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.hikari.maximum-pool-size}") int maximum,
            @Value("${spring.datasource.hikari.minimum-idle}") int minimum
    ){
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        dataSource.setMaximumPoolSize(maximum);
        dataSource.setMinimumIdle(minimum);

        return dataSource;
    }

    @Bean(name = "mysqlSqlSessionFactory")
    public SqlSessionFactory createSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource)throws Exception{
        SqlSessionFactoryBean sqlSessionFactoryBean=new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(dataSource);
        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mappers/**/*.xml"));//配置xml文件路径
        sqlSessionFactoryBean.setTypeAliasesPackage("com.server.entity");//配置别名
        return sqlSessionFactoryBean.getObject();
    }

    @Bean("mysqlTransactionManager")
    public PlatformTransactionManager platformTransactionManager(@Qualifier("mysqlDataSource") DataSource dataSource){
        return new DataSourceTransactionManager(dataSource);
    }
}
