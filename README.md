# spring-ai-mcp-examples

```text
+--------------+                 +---------------------+                 +-----------------------+
|              |    MCP/stdio    |  Weather MCP Server |    HTTP REST    |   External Weather    |
|  LLM Client  | --------------> |                     | --------------> |       Service         |
|   (Claude)   | <-------------- | (my_weather_server) | <-------------- |  (e.g., Open-Meteo)   |
|              |    or MCP/SSE   |      [ADAPTER]      |  (Raw Weather)  |                       |
+--------------+                 +---------------------+                 +-----------------------+
```

---
