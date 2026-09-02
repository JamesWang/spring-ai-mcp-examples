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
## A Rag With Semantica

[Java App] ──> Calls LLM (HyDE text generation)
[Java App] ──> Calls Semantica HTTP API (Passes raw HyDE text string)
│
└───> [Inside Semantica Python Engine]:
1. Automatically calls Torch_Engine to embed the text
2. Automatically queries Chroma internally
3. Automatically queries the Knowledge Graph
4. Merges and formats everything into a clean text packet
│
[Java App] <── Returns Enriched Context String
[Java App] ──> Calls LLM (Feeds context to build final response)

```
my-python-ai-service/
├── main.py                 <-- The FastAPI app exposed to Java
├── requirements.txt        <-- Contains fastapi, semantica, google-genai, torch
└── src/
├── __init__.py
├── llm_wrapper.py   <-- Your existing LLM API/HyDE prompt logic
└── semantica_graph.py  <-- Semantica GraphRAG & Chroma definitions
```

```python
from fastapi import FastAPI, Query
from src.llm_wrapper import generate_hyde_document, synthesize_final_answer
from src.semantica_graph import retrieve_graph_context

app = FastAPI(title="Java Companion Context Engine")

@app.post("/ask-graph-hyde")
async def ask_graph_hyde(user_query: str = Query(..., description="Raw question from Java GUI")):
    # 1. Use your existing Python LLM Wrapper to execute the HyDE logic
    #    (Turns a short question into an ideal factual paragraph)
    hypothetical_doc = generate_hyde_document(user_query)
    
    # 2. Pass that hypothetical text straight into Semantica in the same codebase.
    #    Semantica will use torch_engine + Chroma internally to weave the context.
    enriched_graph_context = retrieve_graph_context(hypothetical_doc)
    
    # 3. Optional Choice: You can choose where to generate the text.
    #    A) Return the raw graph text back to Java and let Java call LLM.
    #    B) Let Python call LLM right here and return the finished response to Java.
    return {
        "raw_query": user_query,
        "hyde_document": hypothetical_doc,
        "grounded_context": enriched_graph_context
    }

```

```diagram
[ Java GUI / Spring AI ]
│
│ 1. Passes the raw, short question over HTTP
▼
┌────────────────────────────────────────────────────────┐
│  UNIFIED PYTHON SERVICE (Your Consolidated Codebase)   │
│                                                        │
│  2. Your LLM Wrapper generates the HyDE text.       │
│            │                                           │
│            ▼ (Direct in-memory variable pass)          │
│  3. Semantica instantly reads the HyDE text variable.  │
│            │                                           │
│            ▼                                           │
│  4. Semantica runs torch_engine, Chroma, and the Graph.│
└────────────────────────┬───────────────────────────────┘
│
│ 5. Returns the fully enriched knowledge graph context
▼
[ Java GUI / Spring AI ] ───► Passes context to LLM for the final answer
```
