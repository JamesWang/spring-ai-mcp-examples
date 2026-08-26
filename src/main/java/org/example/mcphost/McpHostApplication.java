package org.example.mcphost;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@Slf4j
@SpringBootApplication
public class McpHostApplication implements CommandLineRunner {
    static void main(String[] args) {
        SpringApplication.run(McpHostApplication.class, args);
    }

    final ApplicationContext context;
    final McpConfig mcpConfig;
    final private ChatModel chatModel;

    McpHostApplication(ApplicationContext context, McpConfig mcpConfig, ChatModel chatModel){
        this.context = context;
        this.mcpConfig = mcpConfig;
        this.chatModel = chatModel;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            mcpConfig.runMcpStdioToolEngine(chatModel).run(args);
        } finally {
            try {
                SpringApplication.exit(context, () -> 0);
            } catch (Exception e) {
                System.err.println("Context exit encountered an anomaly, forcing hard top.");
            }
        }
        System.exit(0);
    }
}
