# AI Engineering: A Practical Textbook

**Audience:** A developer new to modern AI who wants to become an AI engineer: someone who can design, evaluate, secure, and orchestrate AI tools in real software projects.

**How to use this book:** Read Part I once, then complete the labs in order. Keep prompts, test cases, evaluation results, and run notes in version control. AI engineering is learned by making behavior measurable, not by collecting clever prompts.

**Technology note:** Models, SDKs, product names, limits, and pricing change quickly. Learn the durable ideas here, then verify current syntax and capabilities in the official documentation for the provider you choose.

## Contents

1. [What an AI Engineer Does](#1-what-an-ai-engineer-does)
2. [The Mental Model](#2-the-mental-model)
3. [Your First Reliable Model Call](#3-your-first-reliable-model-call)
4. [Prompt Engineering](#4-prompt-engineering)
5. [Structured Outputs](#5-structured-outputs)
6. [Tools and Orchestration](#6-tools-and-orchestration)
7. [Agents](#7-agents)
8. [Retrieval-Augmented Generation](#8-retrieval-augmented-generation)
9. [AI-Assisted Software Development](#9-ai-assisted-software-development)
10. [Evaluation](#10-evaluation)
11. [Safety and Security](#11-safety-and-security)
12. [Production Architecture](#12-production-architecture)
13. [Android and Kotlin](#13-android-and-kotlin)
14. [Twelve-Week Learning Path](#14-twelve-week-learning-path)
15. [Projects and Checklists](#15-projects-and-checklists)
16. [Glossary](#16-glossary)

---

## 1. What an AI Engineer Does

An AI engineer turns a probabilistic model into a dependable product capability. The work includes translating a user problem into an AI task, choosing a model and context, designing prompts and tools, validating outputs, measuring quality/cost/latency, protecting data, and operating the system after release.

The central mindset is:

> A model is a powerful, fallible component. The surrounding software is responsible for control.

An AI feature might be “summarize a recipe.” An AI system includes authentication, a prompt version, retrieved sources, a model call, parsing, validation, retries, logging, feedback, and a fallback. For every result, ask:

1. What information was the model allowed to use?
2. What action was it allowed to take?
3. How did we decide whether the result was acceptable?
4. What happens when it is wrong, unavailable, or manipulated?

### The AI engineering loop

1. Define the task and success criterion.
2. Create a small representative test set.
3. Build the simplest baseline, usually one model call.
4. Inspect and classify failures.
5. Add the smallest control that addresses the dominant failure.
6. Evaluate normal and adversarial cases again.
7. Measure cost and latency.
8. Release behind a flag and monitor real use.

Do not begin with a multi-agent architecture. Complexity should be earned by evidence.

## 2. The Mental Model

### Models, tokens, and context

A language model predicts the next token from a sequence of tokens. It can classify, extract, summarize, plan, and generate because training gives it broad patterns. It does not guarantee truth, intent, or execution.

The **context** is everything supplied for one request: instructions, history, retrieved documents, tool results, and user input. A large context window is not a substitute for relevant context. Irrelevant information increases cost and can reduce accuracy.

Treat context as a budget. Prefer authoritative excerpts, explicit boundaries, recent state, structured data, and summaries of old conversation turns.

### Model families

- A **generative model** produces or transforms content.
- An **embedding model** maps text to vectors for semantic search.
- A **reranker** scores retrieved candidates more precisely.
- A **classifier or moderation model** detects categories or policy risks.

Choose the smallest model that passes your quality bar. Use a stronger model for difficult cases, not automatically for every request.

### Parameters

- **Temperature:** sampling variability; lower values are often useful for extraction, while higher values can help brainstorming.
- **Output limit:** protects latency and cost.
- **Reasoning effort:** where supported, trades latency and cost for harder problems.
- **Model version:** pin it when reproducibility matters and test migrations.

These controls do not make an answer true. They change behavior and must be evaluated.

## 3. Your First Reliable Model Call

Keep provider-specific code behind a small adapter. The rest of the application should not know whether the model is hosted by a cloud provider, a local runtime, or another vendor.

Every call should have a named use case, a versioned prompt, an input and output schema, a timeout, a retry policy, a request ID, usage telemetry, and a failure state.

```kotlin
suspend fun classifyIngredient(input: IngredientInput): ClassificationResult {
    val request = ModelRequest(
        model = modelPolicy.classifierModel,
        system = promptRegistry.load("ingredient-classifier", version = 3),
        user = jsonEncoder.encode(input),
        outputSchema = ClassificationResult.schema,
        temperature = 0.0,
        timeout = 10.seconds
    )
    val response = modelClient.complete(request)
    val result = jsonDecoder.decode<ClassificationResult>(response.text)
    require(result.confidence in 0.0..1.0)
    return result
}
```

The syntax varies by SDK; the design should remain recognizable.

Never put a provider secret in a mobile application. A mobile client should call your authenticated backend, and the backend should call the provider. Store secrets in a secret manager, rotate them, and restrict permissions.

## 4. Prompt Engineering

Prompt engineering is interface design for a probabilistic component. The goal is not a magical phrase. The goal is a clear contract with observable failure.

### A durable prompt structure

1. **Objective:** What job is being performed?
2. **Scope:** What is in and out?
3. **Inputs:** What does each field mean?
4. **Rules:** What constraints apply?
5. **Evidence policy:** When must the system abstain?
6. **Output contract:** What exact structure is required?
7. **Examples:** Include ambiguous and edge cases.

```text
You classify pantry items for a recipe application.

Rules:
- Use only the supplied item name and category list.
- Do not invent an item that is not present.
- If the name is ambiguous, return "unknown" and explain why briefly.
- Return valid JSON matching the schema. Do not include Markdown.

Input:
{item}

Allowed categories:
{categories}
```

### Prompt injection

Any untrusted text can contain instructions aimed at the model: a user message, web page, PDF, issue comment, or database field. Treat retrieved content as data, not authority. Use delimiters, separate trusted instructions from untrusted content, and validate tool arguments outside the model.

Prompt instructions are not a security boundary.

Store prompts like code, for example `prompts/ingredient-classifier.v3.txt`. Record the prompt version with every evaluation and production trace. A prompt change can alter behavior as significantly as a code change.

## 5. Structured Outputs

Free-form text is difficult for software to consume. Prefer a schema whenever the result crosses a program boundary.

```json
{
  "type": "object",
  "required": ["label", "confidence", "needs_review"],
  "properties": {
    "label": {"type": "string", "enum": ["ingredient", "tool", "unknown"]},
    "confidence": {"type": "number", "minimum": 0, "maximum": 1},
    "needs_review": {"type": "boolean"}
  },
  "additionalProperties": false
}
```

Use native structured output or tool-call schemas when available, but validate locally too. A schema guarantees shape, not correctness.

Validation layers:

1. Parse the response.
2. Validate types and schema.
3. Validate business rules.
4. Check authorization for requested actions.
5. Repair or retry only recoverable failures.
6. Fall back safely when a failure is not recoverable.

Never blindly execute generated SQL, shell commands, code, URLs, or API arguments. A model-generated confidence number is an opinion until calibrated against labeled data.

## 6. Tools and Orchestration

Tool calling lets a model request a typed function while the application remains responsible for execution:

```text
user request
  -> model proposes an allowed tool and arguments
  -> application validates name, arguments, and authorization
  -> application executes the tool
  -> tool result returns to the model
  -> model answers or requests another allowed tool
```

The model proposes. Your application disposes.

A good tool is narrow, typed, documented, observable, and easy to authorize.

```json
{
  "name": "search_recipes",
  "description": "Find recipes matching available ingredients.",
  "parameters": {
    "type": "object",
    "required": ["ingredients", "limit"],
    "properties": {
      "ingredients": {"type": "array", "items": {"type": "string"}},
      "limit": {"type": "integer", "minimum": 1, "maximum": 20}
    },
    "additionalProperties": false
  }
}
```

Separate read tools from side-effecting tools. Reads may run automatically. Writes, purchases, messages, deletions, and permission changes should require confirmation or a policy decision.

For known workflows, use ordinary application code:

```text
extract ingredients -> validate -> search -> rank -> format
```

Use an agent only where the sequence genuinely depends on intermediate results or open-ended investigation.

## 7. Agents

An agent is a model-driven loop that observes state, chooses an action, receives a result, and continues toward a goal. “Agent” is an architecture pattern, not a guarantee of autonomy.

Choose the least autonomous design:

- one call for transformation;
- a pipeline for a known sequence;
- a router for different request types;
- an agent only for open-ended, tool-centered work.

Keep state explicit:

```text
TaskState
  goal, user_id, trusted_facts, observations
  completed_actions, pending_approval
  step_count, budget_remaining
```

Every loop needs a maximum step count, time and cost budget, tool allowlist, per-tool timeouts, cancellation, duplicate-call detection, a fallback, and trace logging.

Multiple agents multiply latency, cost, coordination errors, and debugging difficulty. Start with one orchestrator and deterministic tools. Add specialization only when evaluation shows a benefit.

Human review is an engineering control. Insert approval before irreversible, expensive, sensitive, or externally visible actions, and show the user what will happen.

## 8. Retrieval-Augmented Generation

Retrieval-augmented generation, or RAG, supplies relevant external information at request time. It helps when facts change, answers must cite private documents, or the model should not rely on memory.

```text
documents -> parse -> clean -> chunk -> embed -> index
query -> embed -> retrieve -> optionally rerank -> answer with citations
```

During ingestion, preserve titles, headings, tables, source IDs, timestamps, and access-control metadata. Remove repeated headers and navigation noise. Chunk by meaning and store each chunk’s source location.

Measure retrieval separately from generation:

- Did the correct source appear in the top results?
- Was the answer supported by the retrieved text?
- Were permissions applied before retrieval?
- What happens when no relevant document exists?

Hybrid keyword/vector search is often stronger than either alone. A reranker can improve precision when candidates are noisy. Tell the model to answer only from supplied sources, cite source IDs, and say when evidence is missing. RAG reduces stale knowledge; it does not eliminate hallucination or authorization risk.

## 9. AI-Assisted Software Development

AI coding tools work best when you provide context, constraints, and a verification loop. Include the goal, non-goals, relevant files/symbols, local patterns, interfaces that must not change, tests, acceptance criteria, and validation commands.

Use this loop:

```text
understand -> plan -> edit -> focused test -> inspect diff
           -> broader checks -> security and edge-case review
```

Do not accept generated code because it looks plausible. Check error handling, nullability, concurrency, cleanup, authorization, dependency changes, and test quality. Ask the tool to identify assumptions and likely failure cases. Keep changes small enough to understand.

Maintain a short repository guide describing architecture, commands, conventions, test strategy, and forbidden changes. This helps people and AI tools.

## 10. Evaluation

Evaluation is the defining discipline of AI engineering. A demo shows possibility; an evaluation shows behavior.

Start with 30 to 100 cases covering common requests, ambiguity, empty/malformed/long inputs, relevant languages and accessibility needs, adversarial instructions, unavailable data, permission boundaries, and expected refusals. Store inputs, expected properties, and stable case IDs. Do not store secrets or unnecessary personal data.

Useful metrics include exact match or field accuracy, precision/recall/F1, groundedness, citation correctness, task completion, tool-call validity, policy violations, latency percentiles, error rate, cost, user correction, abandonment, and escalation.

For open-ended text, combine automated checks with human review. Model-based graders can scale review but should be calibrated against human judgments.

Run the same regression cases whenever you change the prompt, model, retrieval, tool schema, or post-processing. Keep failures as permanent test cases.

A useful trace records request ID, model and prompt versions, token counts, latency, retries, retrieved IDs, tool calls, validation failures, and redacted feedback. Never log sensitive content by default.

## 11. Safety and Security

AI applications face ordinary software risks plus prompt injection, sensitive data disclosure, insecure output handling, excessive agency, data poisoning, denial of service through huge inputs or loops, biased outcomes, and supply-chain risk.

Practical controls:

- minimize data sent to providers;
- classify data and define retention rules;
- encrypt data in transit and at rest;
- authorize before retrieval and before every tool call;
- allowlist tools, domains, paths, and operations;
- cap input size, output size, steps, time, and spend;
- require confirmation for side effects;
- redact logs and restrict trace access;
- provide fallbacks and correction paths;
- monitor different failure rates across relevant user groups.

Before sending data to a provider, understand processing location, retention, training use, subprocessors, deletion, contracts, and regulatory obligations. Involve security, privacy, and legal specialists for sensitive workloads.

For each capability, keep a short record of purpose, owner, data sources, model, prompt version, tools, risks, evaluation results, approvals, and rollback plan.

## 12. Production Architecture

```text
mobile/web client
  -> authenticated application API
  -> AI orchestration service
       -> prompt registry
       -> model gateway
       -> retrieval service
       -> approved tools
       -> policy and validation layer
       -> telemetry and evaluation store
```

The model gateway centralizes routing, quotas, retries, and usage accounting. The policy layer stays outside the model so it can enforce rules deterministically.

Use timeouts, bounded backoff, idempotency keys for side effects, circuit breakers, graceful degradation, asynchronous jobs for long tasks, and cancellation. Track cost per successful task. Reduce cost with smaller models for easy cases, context trimming, caching, batching, and stopping unnecessary loops.

Release with prompt/model versioning, feature flags, shadow traffic, canaries, regression evaluations, monitoring, and rollback. Treat provider changes as dependency upgrades.

## 13. Android and Kotlin

For Android, keep model access on a backend. The app should call an authenticated API and receive typed responses:

```text
Compose UI -> ViewModel -> domain use case -> repository -> backend API
```

Represent loading, success, and failure explicitly. Support cancellation, retry, offline behavior, and a non-AI fallback. Validate server responses before displaying or acting on them; do not make UI code parse model prose.

```kotlin
@Serializable
data class RecipeSuggestion(
    val title: String,
    val ingredients: List<String>,
    val confidence: Double,
    val sourceIds: List<String>
)
```

Android-specific concerns include keeping keys out of the APK, sending minimal device/account data, handling flaky networks, using WorkManager for retryable background work, showing progress for long generation, surviving configuration changes, supporting TalkBack and large text, and preventing sensitive AI data from screenshots, clipboard, or analytics.

A good first feature for a pastry or recipe application is ingredient normalization or recipe search. Both have clear evaluation cases and ordinary fallbacks. Delay autonomous shopping actions until authorization and confirmation are designed.

## 14. Twelve-Week Learning Path

### Weeks 1-2: Foundations

Learn tokens, context, model selection, HTTP, JSON, authentication, and basic Python or Kotlin scripting. Build a command-line summarizer with a fixed prompt.

### Weeks 3-4: Prompting and output

Build an extractor with a schema, validation, retries, and 50 test cases. Compare free-form and structured output.

### Weeks 5-6: Tools

Build an assistant with two read-only tools. Add argument validation, allowlisting, timeouts, step limits, and traces.

### Weeks 7-8: Retrieval

Index a small permission-safe corpus. Add citations and a “not found” response. Measure retrieval separately from answer quality.

### Weeks 9-10: Evaluation and security

Create a regression harness, adversarial cases, cost tracking, and redacted logs. Test prompt injection and unauthorized tool requests.

### Weeks 11-12: Production project

Ship one feature behind a flag with a backend boundary, monitoring, fallback behavior, human confirmation, and a system card documenting limitations.

At the end of each week, write: What did I expect? What failed? What evidence changed my design?

## 15. Projects and Checklists

### Portfolio projects

1. **Pantry classifier:** Normalize an ingredient and return a category, confidence, and review flag.
2. **Recipe retrieval assistant:** Retrieve from a fixed corpus, cite sources, and refuse unsupported claims.
3. **Tool-using meal planner:** Add read-only search and nutrition tools with typed arguments and step budgets.
4. **Human-approved shopping draft:** Generate a list but require confirmation before any external write.
5. **AI development workflow:** Add repository instructions, prompt/evaluation files, generated tests, checks, and diff review.

For each project publish an architecture diagram, threat model, evaluation set, results, limitations, and demo.

### Before building

- [ ] Is AI needed, or would search, rules, or a normal query be better?
- [ ] What is the success metric and acceptable failure rate?
- [ ] What data may enter the system?
- [ ] What is the fallback?
- [ ] Which actions require approval?

### Before shipping

- [ ] Inputs and outputs have explicit schemas.
- [ ] Model, prompt, and tool versions are recorded.
- [ ] Authorization happens outside the model.
- [ ] Tool arguments are validated locally.
- [ ] Time, token, step, and cost limits exist.
- [ ] Sensitive logs are redacted.
- [ ] Normal and adversarial evaluations pass.
- [ ] Monitoring and rollback are ready.
- [ ] Users can correct or retry important results.

### Debugging order

1. Reproduce with the exact model, prompt, input, and context.
2. Classify the failure: context, retrieval, instruction, parsing, tool, policy, or product design.
3. Fix the narrowest controlling layer.
4. Add the failure to regression tests.
5. Rerun the relevant evaluation.

### Design template

```text
Capability:
User and task:
Why AI is needed:
Inputs and data classification:
Model and version:
Prompt version:
Output schema:
Tools and permissions:
Human approval points:
Fallback:
Quality metrics:
Latency and cost budget:
Threats and mitigations:
Evaluation set:
Owner and rollback plan:
```

## 16. Glossary

**Agent:** A model-driven loop that observes state, chooses actions, and continues toward a goal.

**Embedding:** A vector representation used to compare semantic similarity.

**Evaluation set:** Representative inputs and expected properties used to measure behavior.

**Function/tool calling:** A structured request for an application-owned function to execute.

**Grounding:** Connecting an answer to supplied evidence or authoritative data.

**Hallucination:** An unsupported or false output presented as reliable.

**Inference:** Running a trained model to produce an output.

**Model gateway:** A service centralizing model routing, policy, usage, and provider integration.

**Prompt injection:** Untrusted content attempting to override instructions or manipulate behavior.

**RAG:** Retrieval-augmented generation; retrieving external context before generation.

**Reranker:** A model or algorithm that reorders retrieved candidates for relevance.

**Structured output:** A response constrained to a machine-readable schema.

**Tool:** An application function exposed to a model through a typed contract.

**Trace:** A record of model calls, tools, timings, and outcomes for one request.

## Closing Principle

The durable skill is not memorizing which model is fashionable. It is learning to turn uncertain model behavior into a controlled, testable, observable software system. Start with one useful capability, make failures visible, and add autonomy only where evidence supports it.