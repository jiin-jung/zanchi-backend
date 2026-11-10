package com.zanchi.zanchi_backend.config;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.ai.openai.api.ResponseFormat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class AiConfig {


    /** 기본 ChatClient (JSON 모드 + 모델/온도 기본값) */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        // OpenAI JSON 모드
        ResponseFormat rf = new ResponseFormat();
        rf.setType(ResponseFormat.Type.JSON_OBJECT);

        return builder
                .defaultSystem("""
                너는 데이트 코스 플래너다.
                반드시 JSON만 출력한다.
                (설명이 필요하면 JSON 필드에 포함하고, 바깥 텍스트는 쓰지 않는다)
            """)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.2)
                        .responseFormat(rf)
                        .build())
                .build();
    }

    /** (선택) 설명 전용 ChatClient – 문장 자연스러움 필요시 온도만 다르게 */
    @Bean(name = "explainChat")
    public ChatClient explainChat(ChatClient.Builder builder) {
        return builder
                .defaultSystem("너는 데이트 코스 설명가다. 간결하고 따뜻하게 한국어로 2~3문장 작성.")
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4o-mini")
                        .temperature(0.7)
                        .build())
                .build();
    }

    // Kakao Local API 호출용 WebClient
    @Bean(name = "kakaoWebClient")
    public WebClient kakaoWebClient(
            @Value("${app.maps.kakao.host}") String host,
            @Value("${app.maps.kakao.rest-api-key}") String restKey
    ) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(8))
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(8, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(8, TimeUnit.SECONDS));
                });

        return WebClient.builder()
                .baseUrl(host)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + restKey) // 헤더 고정
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
