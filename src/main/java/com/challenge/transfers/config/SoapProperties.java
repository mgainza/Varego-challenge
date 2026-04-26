package com.challenge.transfers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "soap.transfers")
public record SoapProperties (String url, String channel, String terminal, String password, String ip) {}