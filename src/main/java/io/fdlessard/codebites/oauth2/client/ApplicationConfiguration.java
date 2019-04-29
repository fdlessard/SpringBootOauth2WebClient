package io.fdlessard.codebites.oauth2.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;


@Slf4j
@Configuration
public class ApplicationConfiguration implements WebMvcConfigurer {

    public final static String MESSAGE_BASE_URL = "http://localhost:8081";

    @Bean
    public WebClient messageWebClient(
            ClientRegistrationRepository clientRegistrations,
            OAuth2AuthorizedClientRepository authorizedClients,
            ClientHttpConnector clientHttpConnector
    ) {

        log.info("WebClientConfiguration.messageWebClient()");
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

       // oauth.setDefaultOAuth2AuthorizedClient(true);
        oauth.setDefaultClientRegistrationId("message");


        return WebClient.builder()
                .baseUrl(MESSAGE_BASE_URL)
                .clientConnector(clientHttpConnector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .apply(oauth.oauth2Configuration())
                .filter(logRequest())
                .build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrations() {

        log.info("WebClientConfiguration.clientRegistrations()");

        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("message")
                .clientId("client")
                .clientSecret("secret")
                .scope("read,write")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8081/oauth/token")
                .build();

        return new InMemoryClientRegistrationRepository(clientRegistration);
    }

    @Bean
    public ClientHttpConnector clientHttpConnector() {

        log.info("WebClientConfiguration.clientHttpConnector()");

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .doOnConnected(connection ->
                        connection.addHandlerLast(new ReadTimeoutHandler(1))
                                .addHandlerLast(new WriteTimeoutHandler(1))
                );

        return new ReactorClientHttpConnector(HttpClient.from(tcpClient));
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers()
                    .forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return next.exchange(clientRequest);
        };
    }
}