package com.octo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.octo.web.NodeRPCConnection;
import net.corda.client.jackson.JacksonSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
public class Starter {
    /**
     * Starts our Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Starter.class);
        //app.setBannerMode(Banner.Mode.LOG);
        //app.setWebApplicationType(SERVLET);
        app.run(args);
    }

    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(@Autowired NodeRPCConnection rpcConnection) {
        ObjectMapper mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(mapper);
        return converter;
    }
}