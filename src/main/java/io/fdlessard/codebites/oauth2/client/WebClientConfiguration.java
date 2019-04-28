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
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.UnAuthenticatedServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;


@Slf4j
@Configuration
public class WebClientConfiguration {

    public final static String MESSAGE_BASE_URL = "http://localhost:8081";

    @Bean
    public WebClient ratePlanWebClient(
            ClientRegistrationRepository clientRegistrations,
            OAuth2AuthorizedClientRepository authorizedClients,
            ClientHttpConnector clientHttpConnector
    ) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrations, authorizedClients);

        oauth.setDefaultClientRegistrationId("message");

        return WebClient.builder()
                .baseUrl(MESSAGE_BASE_URL)
                .clientConnector(clientHttpConnector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(oauth)
                .filter(logRequest())
                .build();
    }

    @Bean
    public ClientRegistrationRepository  clientRegistrations() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("message")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8081/oauth/token")
                .build();
        return new InMemoryClientRegistrationRepository(clientRegistration);
    }
/*
    @Bean
    public ReactiveClientRegistrationRepository clientRegistrations() {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId("message")
                .clientId("client")
                .clientSecret("adeec876-86bb-4194-8953-8233ec20d2fe")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8081/oauth/token")
                .redirectUriTemplate("http://localhost:8081/login")
                .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        return new InMemoryReactiveClientRegistrationRepository(clientRegistration);
    }

    @Bean
    public WebClient ratePlanWebClient(
            ReactiveClientRegistrationRepository clientRegistrations,
            ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository,
            ClientHttpConnector clientHttpConnector
    ) {

        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                        clientRegistrations,
                        new UnAuthenticatedServerOAuth2AuthorizedClientRepository());

        oauth.setDefaultClientRegistrationId("message");

        return WebClient.builder()
                .baseUrl(MESSAGE_BASE_URL)
                .clientConnector(clientHttpConnector)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(oauth)
                .filter(logRequest())
                .build();
    }
*/
    @Bean
    public ClientHttpConnector clientHttpConnector() {

        TcpClient tcpClient = TcpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,2000)
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
