package org.example.mcphost;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class McpConfig {
    final String pyToolPath;
    final String pyVenvPath;

    public McpConfig( @Value("${ai.mcp.tool.py.path}") String pyToolPath,
                      @Value("${ai.mcp.tool.py.venv}") String pyVenvPath ) {
        this.pyToolPath = pyToolPath;
        this.pyVenvPath = pyVenvPath;
    }
    //@Bean
    public CommandLineRunner runMcpStdioToolEngine(ChatModel chatModel) {
        return args -> {
            String venvPythonPath = this.pyVenvPath + File.separator + "bin" + File.separator + "python";

            log.info("[Spring Boot] Starting CommandLineRunner execution context...");
            // Wrap it inside the SDK's structural interface instance layer
            McpJsonMapper customJsonMapper = new JacksonMcpJsonMapper(new JsonMapper());

            // 1. Configure OS-level Process parameters (like Popen configuration)
            ServerParameters pythonParams = ServerParameters.builder(venvPythonPath)
                    .args(this.pyToolPath + File.separator + "my_weather_server.py") // Script must reside in the working directory folder
                    .build();

            // 2. Wrap parameters inside the standard input/output transport layer
            StdioClientTransport transport = new StdioClientTransport(pythonParams, customJsonMapper);

            // 3. Construct synchronous MCP Client configuration instance

            try (McpSyncClient mcpClient = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build()) {
                // 4. Perform network handshake to establish server process control
                log.info("[Spring Boot] Spawning Python subprocess and initializing protocol...");
                mcpClient.initialize();
                log.info("[Spring Boot] MCP Handshake successful.");

                McpSchema.ListToolsResult discoveredTools = mcpClient.listTools(null);
                log.info("[Agent] Discovering tools directly from python venv environment:");
                discoveredTools.tools().forEach(t -> log.info("  -> Found tool executable: {}", t.name()));

                // 5. Build prompt options telling the model it has access to the local tools framework
                List<ToolCallback> toolCallbacks = discoveredTools
                        .tools()
                        .stream()
                        .map(tool -> (ToolCallback)SyncMcpToolCallback.builder()
                                .mcpClient(mcpClient)
                                .tool(tool)
                                .build()
                        )
                        .toList();

                String userQuery = "Can you look at my tools and tell me what the current temperature is in Tokyo right now?";
                log.info("\n[User Query] -> {}", userQuery);
                // 4. Wrap your model inside a fluent ChatClient container configuration
                ChatClient client = ChatClient.create(chatModel);

                // 5. Fire query over standard HTTP port down to LM Studio
                String response = client.prompt()
                        .user(userQuery)
                        .tools(toolCallbacks) // Binds Python execution safely
                        .call()
                        .content();

                log.info("\n===== [LM Studio Final Consolidated Output] =====");
                log.info(response);
                log.info("========================================\n");
            } catch (Exception pipelineException) {
                log.info("[Spring Boot] Error encountered during routing execution: {}", pipelineException.getMessage());
                log.error("Error: ", pipelineException);
            } finally {
                // 8. Gracefully close streams and kill underlying child process
                log.info("[Spring Boot] Finalizing execution. Closing MCP client stream pipes...");
                log.info("[Spring Boot] Subprocess safely terminated.");
            }
        };
    }
}
