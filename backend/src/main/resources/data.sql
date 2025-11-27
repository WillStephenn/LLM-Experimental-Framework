-- =============================================================================
-- LocalLab - Seed Data
-- =============================================================================
-- This script populates the database with sample data on application start.
-- Data is only inserted if the tables are empty (conditional inserts).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Task Templates
-- -----------------------------------------------------------------------------
-- Sample templates demonstrating various use cases and the {{variable}} syntax.

INSERT INTO task_templates (id, name, description, prompt_template, tags, evaluation_notes, created_at)
SELECT 1, 'Code Review Task',
       'Analyse code for quality, best practices, and potential issues. Suitable for reviewing pull requests or code snippets.',
       'Review the following code and provide detailed feedback on:
1. Code quality and readability
2. Potential bugs or issues
3. Performance considerations
4. Best practice adherence

Code to review:
```
{{code}}
```

Programming language: {{language}}

Please structure your response with clear sections for each area of feedback.',
       'code,review,quality',
       'Evaluate based on: accuracy of identified issues, actionability of suggestions, coverage of different aspects (style, logic, performance)',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM task_templates WHERE id = 1);

INSERT INTO task_templates (id, name, description, prompt_template, tags, evaluation_notes, created_at)
SELECT 2, 'Text Summarisation Task',
       'Summarise lengthy documents or articles whilst preserving key information and context.',
       'Summarise the following text in a clear and concise manner. Preserve the key points and main arguments.

Text to summarise:
{{text}}

Target length: {{target_length}}
Focus area (optional): {{focus_area}}

Provide a structured summary with:
- Main thesis/argument
- Key supporting points
- Important conclusions',
       'summarisation,nlp,content',
       'Assess: information retention, conciseness, coherence, and whether the summary captures the essential meaning',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM task_templates WHERE id = 2);

INSERT INTO task_templates (id, name, description, prompt_template, tags, evaluation_notes, created_at)
SELECT 3, 'Question Answering (RAG)',
       'Answer questions based on provided context. Designed for retrieval-augmented generation experiments.',
       'Based on the following context, answer the question accurately and comprehensively.

Context:
{{context}}

Question: {{question}}

Instructions:
- Only use information from the provided context
- If the context does not contain sufficient information, state this clearly
- Cite relevant parts of the context in your answer
- Be concise but thorough',
       'qa,rag,retrieval',
       'Evaluate: answer accuracy, proper use of context, handling of insufficient information, citation quality',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM task_templates WHERE id = 3);

INSERT INTO task_templates (id, name, description, prompt_template, tags, evaluation_notes, created_at)
SELECT 4, 'JSON Data Extraction',
       'Extract structured data from unstructured text and output in JSON format.',
       'Extract the following information from the text and return it as valid JSON.

Text:
{{input_text}}

Required fields to extract:
{{required_fields}}

Output format: JSON object with the specified fields. Use null for missing values.

Important: Return ONLY valid JSON, no additional explanation.',
       'extraction,json,structured',
       'Check: JSON validity, extraction accuracy, handling of missing data, adherence to specified schema',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM task_templates WHERE id = 4);

INSERT INTO task_templates (id, name, description, prompt_template, tags, evaluation_notes, created_at)
SELECT 5, 'Translation Task',
       'Translate text between languages whilst maintaining meaning and natural phrasing.',
       'Translate the following text from {{source_language}} to {{target_language}}.

Original text:
{{text}}

Requirements:
- Maintain the original meaning and tone
- Use natural phrasing in the target language
- Preserve any technical terms or proper nouns appropriately
- Handle idioms and cultural references appropriately',
       'translation,multilingual,nlp',
       'Assess: semantic accuracy, grammatical correctness, natural fluency, preservation of tone and style',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM task_templates WHERE id = 5);

-- -----------------------------------------------------------------------------
-- System Prompts
-- -----------------------------------------------------------------------------
-- Reusable system prompts for different assistant personas and behaviours.

INSERT INTO system_prompts (id, alias, content, created_at)
SELECT 1, 'code-assistant',
       'You are an expert software engineer with deep knowledge of multiple programming languages, design patterns, and best practices. You provide clear, accurate, and actionable technical advice. When reviewing code, you focus on correctness, maintainability, and performance. You explain your reasoning and provide examples where helpful.',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_prompts WHERE id = 1);

INSERT INTO system_prompts (id, alias, content, created_at)
SELECT 2, 'technical-writer',
       'You are a professional technical writer skilled at explaining complex concepts in clear, accessible language. You structure information logically, use appropriate terminology for your audience, and ensure completeness without unnecessary verbosity. You excel at creating documentation, tutorials, and explanatory content.',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_prompts WHERE id = 2);

INSERT INTO system_prompts (id, alias, content, created_at)
SELECT 3, 'json-formatter',
       'You are a precise data extraction assistant. Your responses must be valid JSON only, with no additional text, explanations, or markdown formatting. Follow the exact schema requested. Use null for missing values and empty arrays for missing list fields.',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_prompts WHERE id = 3);

INSERT INTO system_prompts (id, alias, content, created_at)
SELECT 4, 'research-analyst',
       'You are a thorough research analyst who provides well-reasoned, evidence-based answers. You clearly distinguish between facts and interpretations, acknowledge limitations in available information, and present balanced perspectives on complex topics. You cite sources when applicable and structure your analysis logically.',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_prompts WHERE id = 4);

INSERT INTO system_prompts (id, alias, content, created_at)
SELECT 5, 'concise-responder',
       'You are a helpful assistant who provides brief, direct answers. Avoid unnecessary preamble, caveats, or repetition. Get straight to the point whilst remaining accurate and helpful. If asked a question, answer it immediately without restating the question.',
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM system_prompts WHERE id = 5);

-- -----------------------------------------------------------------------------
-- Embedding Models
-- -----------------------------------------------------------------------------
-- Common embedding models available in Ollama for RAG operations.

INSERT INTO embedding_models (id, name, ollama_model_name, dimensions, created_at)
SELECT 1, 'Nomic Embed Text', 'nomic-embed-text', 768, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM embedding_models WHERE id = 1);

INSERT INTO embedding_models (id, name, ollama_model_name, dimensions, created_at)
SELECT 2, 'MXBai Embed Large', 'mxbai-embed-large', 1024, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM embedding_models WHERE id = 2);

INSERT INTO embedding_models (id, name, ollama_model_name, dimensions, created_at)
SELECT 3, 'All MiniLM L6 v2', 'all-minilm', 384, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM embedding_models WHERE id = 3);

INSERT INTO embedding_models (id, name, ollama_model_name, dimensions, created_at)
SELECT 4, 'Snowflake Arctic Embed', 'snowflake-arctic-embed', 1024, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM embedding_models WHERE id = 4);

-- -----------------------------------------------------------------------------
-- Reset Identity Sequences
-- -----------------------------------------------------------------------------
-- Ensure auto-increment starts after seeded IDs to prevent conflicts.

ALTER TABLE task_templates ALTER COLUMN id RESTART WITH 6;
ALTER TABLE system_prompts ALTER COLUMN id RESTART WITH 6;
ALTER TABLE embedding_models ALTER COLUMN id RESTART WITH 5;
