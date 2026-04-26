package com.challenge.transfers.config;

import com.challenge.transfers.client.TransfersSoapClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
@EnableConfigurationProperties(SoapProperties.class)
public class SoapClientConfig {


    private final SoapProperties soapProperties;

    public SoapClientConfig(SoapProperties soapProperties) {
        this.soapProperties = soapProperties;
    }


    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.challenge.transfers.client.soap");
        return marshaller;
    }

    @Bean
    public TransfersSoapClient transfersSoapClient(Jaxb2Marshaller marshaller) {
        TransfersSoapClient client = new TransfersSoapClient(soapProperties.channel(), soapProperties.terminal(), soapProperties.password(), soapProperties.ip());
        client.setDefaultUri(soapProperties.url());
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }

}
