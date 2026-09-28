package org.example.toolcall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class GetTimeController {
    private static final String CURRENT_TIME_TEMPLATE = "What is the current time in {city}";

    private final ChatClient chatClient;

    public GetTimeController(ChatClient.Builder chatClientBuilder, TimeTool timeTool) {
        // Force write to class logger
        // The 3rd parameter: execution order
        SimpleLoggerAdvisor customLoggerAdvisor = new SimpleLoggerAdvisor(
                request -> {
                    String data = request != null ? request.toString() : "null";
                    log.info("TARGET LOG -> REQUEST: {}", data); // Force write to class logger
                    return data;
                },
                response -> {
                    String data = response != null ? response.toString() : "null";
                    log.info("TARGET LOG -> RESPONSE: {}", data); // Force write to class logger
                    return data;
                },
                0 // The 3rd parameter: execution order
        );
        this.chatClient = chatClientBuilder
                .defaultAdvisors(customLoggerAdvisor)
                .defaultTools(timeTool)
                .build();
    }

    @GetMapping(path = "/time", params = "city")
    public String getTime(@RequestParam("city") String city) {
        return chatClient.prompt()
                .user(promptUserSpec -> {
                    promptUserSpec.text(CURRENT_TIME_TEMPLATE)
                            .param("city", city);
                })
                .call()
                .content();
    }

}
