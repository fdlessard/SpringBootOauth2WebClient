package io.fdlessard.codebites.oauth2.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RestController
public class WebClientMessageController {

    private WebClient messageWebClient;

    public WebClientMessageController(WebClient messageWebClient) {
        this.messageWebClient = messageWebClient;
    }

    @GetMapping(value = "/WebClientMessage")
    @ResponseBody
    public Mono<Message> getMessage() {

        log.info("WebClientMessageController.getMessage()");

        return messageWebClient.get()
                .uri("/message")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Message.class)
                .onErrorResume(e -> logException(e));
    }

    private Mono<Message> logException(Throwable e) {

        log.info("WebClientMessageController.logException({})", e);

        return Mono.just(new Message(e.getMessage()));
    }


    @GetMapping(value = "/WebClientMessages")
    @ResponseBody
    public Collection<Message> getAllMessages() {

        List<String> ids = IntStream.range(1, 500).boxed().map(i -> i.toString()).collect(Collectors.toList());


        return Flux.fromIterable(ids)
                .flatMap(id -> messageWebClient.get()
                                .uri("/message")
                                .accept(MediaType.APPLICATION_JSON)
                                .retrieve()
                                .bodyToMono(Message.class)
                                .flatMap(c -> buildResponse(id, c))
                                .onErrorResume(e -> buildResponse(id, e)),
                        256)
                .collectMap(id -> id.getT1(), id -> id.getT2())
                .block().values();
    }

    private Mono<Tuple2<String, Message>> buildResponse(String id, Message message) {
        return Mono.just(Tuples.of(id, message));
    }

    private Mono<Tuple2<String, Message>> buildResponse(String id, Throwable e) {

        if (e instanceof WebClientResponseException) {
            WebClientResponseException wce = (WebClientResponseException) e;
            return Mono.just(Tuples.of(id, new Message( wce.getResponseBodyAsString())));
        }

        return Mono.just(Tuples.of(id, new Message("not WebClientResponseException")));
    }
}
