# ADR-001 — Layered Architecture

### Status

Accepted

---

### Context

Kairos is a knowledge system that combines:
- semantic search (pgvector + embeddings via ONNX)
    
- relational graph traversal (Neo4j)
    

The system interacts with multiple external concerns:
- HTTP APIs (current entry point)
- embedding pipeline (ONNX)
    
- vector and graph databases
    

Without clear boundaries, there is a risk of:

- business logic leaking into controllers
    
- domain coupling to frameworks (Spring, JPA, JWT)
    
- tight coupling to specific infrastructure (pgvector, Neo4j)
    

---

### Decision

Adopt a layered architecture with strict dependency direction:

```
Presentation → Application → Domain
                      ↑
                Infrastructure
```

#### Responsibilities

**Presentation**

- Handles HTTP
    
- Validates requests
    
- Maps Request DTO → Command
    

**Application**

- Implements use cases
    
- Orchestrates workflows (chunking, embedding, storage, fusion)
    
- Defines Commands and Ports
    
- Accesses RequestContext (e.g., user)
    

**Domain**

- Contains core models and rules
    
- No dependency on frameworks or infrastructure
    

**Infrastructure**

- Implements Ports (ONNX, pgvector, Neo4j, Redis, auth)
    
- Handles external integrations
    

---

### Key Rules

- Controllers must not contain business logic
    
- Request DTOs must not be used in Application layer
    
- Commands represent use case input
    
- Contextual data (e.g., user) is accessed via `RequestContext`
    
- Infrastructure implements interfaces defined in Application
    

---

### Consequences

**Pros**

- Clear separation of concerns
    
- Domain remains framework-independent
    
- Easier testing and evolution
    
- Supports multiple input channels
    

**Cons**

- Additional boilerplate (mapping, interfaces)
    
- Slightly higher complexity upfront
    

---

### Alternatives

**Use Request DTO directly in use case**

- Simpler, but couples Application to HTTP
    

**Handle auth in controllers**

- Leads to duplication and weaker encapsulation
    

---

### Decision Summary

Use a layered architecture with:

- Request → Command mapping
    
- RequestContext for implicit data
    
- Ports & Adapters for infrastructure
    

Prioritizing long-term maintainability over short-term simplicity.

## Related
- [[Kairos]]
- [[Definição de projeto]]
