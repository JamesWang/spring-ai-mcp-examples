package org.example.toolcall;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
public class TimeTool {

    @Tool(name = "getCurrentTime", description = "Get the current time in the specified time zone")
    public String getCurrentTime(String timeZone) {
        log.info("Getting the current time in {}", timeZone);
        var now = LocalDateTime.now(ZoneId.of(timeZone));
        return now.toString();
    }
}
