package io.fdlessard.codebites.oauth2.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RestController

public class WebClientMessageController {

    private WebClient messageWebClient;

    public WebClientMessageController(WebClient messageWebClient) {
        this.messageWebClient = messageWebClient;
    }

    @GetMapping(value = "/WebClientMessage")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public Mono<Message> getMessage() {

        log.info("WebClientMessageController.getMessage()");

        return messageWebClient.get()
                .uri("/message")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Message.class);
    }
}
