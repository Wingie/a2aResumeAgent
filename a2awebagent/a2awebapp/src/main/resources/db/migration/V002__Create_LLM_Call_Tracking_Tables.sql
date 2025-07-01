-- Migration: Create LLM Call Tracking Tables for Agentic-Harness System
-- Version: V002
-- Description: Adds comprehensive LLM call logging with cache integration
-- Author: Generated for a2aTravelAgent agentic-harness system

-- =================================================================
-- LLM Call Logs Table - Core tracking for all LLM API interactions
-- =================================================================

CREATE TABLE IF NOT EXISTS llm_call_logs (
    call_id VARCHAR(36) PRIMARY KEY,
    
    -- Cache Integration (links to tool_descriptions table)
    cache_key VARCHAR(255) NOT NULL,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- LLM Provider Information (only populated for cache misses)
    provider VARCHAR(50),              -- 'openai', 'gemini', 'anthropic', 'openrouter'
    model_name VARCHAR(100),           -- 'gpt-4o-mini', 'gemini-2.0-flash', etc.
    
    -- Request/Response Data (stored as TEXT for large payloads)
    request_payload TEXT,              -- JSON request sent to LLM API
    response_payload TEXT,             -- JSON response received from LLM API
    
    -- Performance Metrics
    response_time_ms BIGINT,           -- Total API call duration
    
    -- Token Usage and Cost Tracking
    input_tokens INTEGER,              -- Input tokens sent to LLM
    output_tokens INTEGER,             -- Output tokens received from LLM
    estimated_cost_usd DECIMAL(10,6),  -- Estimated cost in USD
    
    -- Error Handling and Retry Logic
    error_code VARCHAR(50),            -- Error classification
    error_message TEXT,                -- Detailed error description
    retry_attempt INTEGER NOT NULL DEFAULT 0,  -- Retry counter (0 = first attempt)
    
    -- Context and Relationships
    tool_name VARCHAR(100) NOT NULL,   -- Which MCP tool triggered this call
    task_execution_id VARCHAR(36),     -- Links to task_executions table for agent workflows
    session_id VARCHAR(100),           -- Groups related calls in a session
    user_id VARCHAR(100),              -- Track usage per user (optional)
    
    -- Request Context for Security and Analytics
    request_ip INET,                   -- Client IP address
    user_agent TEXT,                   -- Client user agent string
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,            -- When the LLM call completed (success or failure)
    
    -- Constraints
    CONSTRAINT chk_cache_hit_logic CHECK (
        -- If cache_hit = true, then LLM-specific fields should be minimal
        (cache_hit = true AND response_time_ms = 0 AND estimated_cost_usd = 0) OR
        (cache_hit = false)
    )
);

-- =================================================================
-- Agent Decision Steps Table - Tracks decision-making in agentic loops
-- =================================================================

CREATE TABLE IF NOT EXISTS agent_decision_steps (
    step_id VARCHAR(36) PRIMARY KEY,
    
    -- Relationship to main task execution
    task_execution_id VARCHAR(36) NOT NULL,
    step_number INTEGER NOT NULL,      -- Sequential step number in the agent workflow
    
    -- Decision Information
    tool_selected VARCHAR(100),        -- Which tool the agent decided to use
    reasoning_text TEXT,               -- LLM's reasoning for this decision
    confidence_score DECIMAL(3,2),     -- Agent's confidence in this decision (0.00-1.00)
    
    -- LLM Call Integration
    llm_call_id VARCHAR(36),           -- Links to the LLM call that made this decision
    
    -- Cost and Performance per Step
    tokens_used INTEGER,               -- Total tokens used for this decision step
    step_cost_usd DECIMAL(8,4),        -- Cost for this specific agent step
    
    -- Metadata
    alternatives_considered JSONB,      -- Other options the agent considered
    execution_context JSONB,           -- Additional context about the decision
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_agent_decision_task FOREIGN KEY (task_execution_id) 
        REFERENCES task_executions(task_id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_decision_llm_call FOREIGN KEY (llm_call_id) 
        REFERENCES llm_call_logs(call_id) ON DELETE SET NULL,
    CONSTRAINT chk_confidence_range CHECK (confidence_score >= 0 AND confidence_score <= 1),
    CONSTRAINT chk_step_number_positive CHECK (step_number > 0)
);

-- =================================================================
-- Provider Cost Configuration Table - Rate tracking for cost calculation
-- =================================================================

CREATE TABLE IF NOT EXISTS provider_costs (
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    input_cost_per_1k_tokens DECIMAL(10,8) NOT NULL,   -- Cost per 1000 input tokens
    output_cost_per_1k_tokens DECIMAL(10,8) NOT NULL,  -- Cost per 1000 output tokens
    effective_date DATE NOT NULL DEFAULT CURRENT_DATE,  -- When this pricing took effect
    is_active BOOLEAN NOT NULL DEFAULT TRUE,            -- Whether this pricing is current
    
    -- Metadata
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    notes TEXT,                                         -- Additional pricing notes
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (provider, model_name, effective_date)
);

-- =================================================================
-- Daily LLM Usage Aggregation Table - Pre-computed analytics
-- =================================================================

CREATE TABLE IF NOT EXISTS daily_llm_usage (
    usage_date DATE NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    
    -- Aggregated Metrics
    total_calls INTEGER NOT NULL DEFAULT 0,
    cache_hits INTEGER NOT NULL DEFAULT 0,
    cache_misses INTEGER NOT NULL DEFAULT 0,
    total_input_tokens BIGINT NOT NULL DEFAULT 0,
    total_output_tokens BIGINT NOT NULL DEFAULT 0,
    total_cost_usd DECIMAL(10,2) NOT NULL DEFAULT 0,
    
    -- Performance Metrics
    avg_response_time_ms INTEGER,
    success_rate DECIMAL(5,2),          -- Percentage of successful calls
    
    -- Error Tracking
    total_errors INTEGER NOT NULL DEFAULT 0,
    most_common_error VARCHAR(50),
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (usage_date, provider, model_name)
);

-- =================================================================
-- Indexes for Performance Optimization
-- =================================================================

-- LLM Call Logs Indexes
CREATE INDEX IF NOT EXISTS idx_llm_calls_cache_key ON llm_call_logs(cache_key);
CREATE INDEX IF NOT EXISTS idx_llm_calls_created_at ON llm_call_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_llm_calls_provider_model ON llm_call_logs(provider, model_name);
CREATE INDEX IF NOT EXISTS idx_llm_calls_task_execution ON llm_call_logs(task_execution_id);
CREATE INDEX IF NOT EXISTS idx_llm_calls_session_id ON llm_call_logs(session_id);
CREATE INDEX IF NOT EXISTS idx_llm_calls_tool_name ON llm_call_logs(tool_name);
CREATE INDEX IF NOT EXISTS idx_llm_calls_cache_hit ON llm_call_logs(cache_hit);
CREATE INDEX IF NOT EXISTS idx_llm_calls_cost_analysis ON llm_call_logs(created_at, provider, cache_hit) 
    WHERE estimated_cost_usd IS NOT NULL;

-- Agent Decision Steps Indexes
CREATE INDEX IF NOT EXISTS idx_agent_steps_task_execution ON agent_decision_steps(task_execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_steps_step_number ON agent_decision_steps(task_execution_id, step_number);
CREATE INDEX IF NOT EXISTS idx_agent_steps_llm_call ON agent_decision_steps(llm_call_id);
CREATE INDEX IF NOT EXISTS idx_agent_steps_created_at ON agent_decision_steps(created_at);

-- Daily Usage Indexes
CREATE INDEX IF NOT EXISTS idx_daily_usage_date_provider ON daily_llm_usage(usage_date, provider);
CREATE INDEX IF NOT EXISTS idx_daily_usage_cost_analysis ON daily_llm_usage(usage_date, total_cost_usd);

-- =================================================================
-- Insert Default Provider Cost Data
-- =================================================================

INSERT INTO provider_costs (provider, model_name, input_cost_per_1k_tokens, output_cost_per_1k_tokens, notes) VALUES
-- OpenAI Pricing (as of 2024)
('openai', 'gpt-4o-mini', 0.000150, 0.000600, 'OpenAI GPT-4o Mini pricing'),
('openai', 'gpt-4o', 0.003000, 0.006000, 'OpenAI GPT-4o pricing'),
('openai', 'gpt-3.5-turbo', 0.001500, 0.002000, 'OpenAI GPT-3.5 Turbo pricing'),

-- Google Gemini Pricing
('gemini', 'gemini-2.0-flash', 0.001000, 0.002000, 'Google Gemini 2.0 Flash pricing'),
('gemini', 'gemini-1.5-pro', 0.003500, 0.010500, 'Google Gemini 1.5 Pro pricing'),

-- Anthropic Claude Pricing
('anthropic', 'claude-3-haiku', 0.000250, 0.001250, 'Anthropic Claude 3 Haiku pricing'),
('anthropic', 'claude-3-sonnet', 0.003000, 0.015000, 'Anthropic Claude 3 Sonnet pricing'),
('anthropic', 'claude-3-opus', 0.015000, 0.075000, 'Anthropic Claude 3 Opus pricing'),

-- OpenRouter (aggregated pricing - may vary)
('openrouter', 'default', 0.001000, 0.002000, 'OpenRouter average pricing estimate')

ON CONFLICT (provider, model_name, effective_date) DO NOTHING;

-- =================================================================
-- Views for Common Analytics Queries
-- =================================================================

-- Cost Analysis View
CREATE OR REPLACE VIEW llm_cost_analysis AS
SELECT 
    DATE(created_at) as date,
    provider,
    model_name,
    tool_name,
    COUNT(*) as total_calls,
    SUM(CASE WHEN cache_hit THEN 1 ELSE 0 END) as cache_hits,
    SUM(CASE WHEN NOT cache_hit THEN 1 ELSE 0 END) as cache_misses,
    SUM(CASE WHEN NOT cache_hit THEN estimated_cost_usd ELSE 0 END) as total_cost_usd,
    AVG(CASE WHEN NOT cache_hit AND response_time_ms IS NOT NULL THEN response_time_ms END) as avg_response_time_ms
FROM llm_call_logs
GROUP BY DATE(created_at), provider, model_name, tool_name
ORDER BY date DESC, total_cost_usd DESC;

-- Cache Effectiveness View
CREATE OR REPLACE VIEW cache_effectiveness AS
SELECT 
    tool_name,
    COUNT(*) as total_requests,
    SUM(CASE WHEN cache_hit THEN 1 ELSE 0 END) as cache_hits,
    SUM(CASE WHEN NOT cache_hit THEN 1 ELSE 0 END) as cache_misses,
    ROUND((SUM(CASE WHEN cache_hit THEN 1 ELSE 0 END) * 100.0 / COUNT(*)), 2) as hit_rate_percent,
    SUM(CASE WHEN NOT cache_hit THEN estimated_cost_usd ELSE 0 END) as cost_without_cache_usd,
    SUM(CASE WHEN cache_hit THEN estimated_cost_usd ELSE 0 END) as cost_saved_usd
FROM llm_call_logs
GROUP BY tool_name
ORDER BY hit_rate_percent DESC;

-- Agent Performance View  
CREATE OR REPLACE VIEW agent_performance AS
SELECT 
    ads.task_execution_id,
    te.task_type,
    te.status as task_status,
    COUNT(ads.step_id) as total_steps,
    SUM(ads.tokens_used) as total_tokens,
    SUM(ads.step_cost_usd) as total_agent_cost_usd,
    AVG(ads.confidence_score) as avg_confidence,
    te.created_at as task_started,
    te.completed_at as task_completed
FROM agent_decision_steps ads
JOIN task_executions te ON ads.task_execution_id = te.task_id
GROUP BY ads.task_execution_id, te.task_type, te.status, te.created_at, te.completed_at
ORDER BY te.created_at DESC;

-- =================================================================
-- Comments and Documentation
-- =================================================================

COMMENT ON TABLE llm_call_logs IS 'Comprehensive tracking of all LLM API calls with cache integration for cost optimization and performance monitoring in the agentic-harness system';
COMMENT ON TABLE agent_decision_steps IS 'Tracks individual decision steps in agentic workflows, linking agent reasoning to LLM calls and costs';
COMMENT ON TABLE provider_costs IS 'Maintains current and historical pricing for LLM providers to enable accurate cost calculation';
COMMENT ON TABLE daily_llm_usage IS 'Pre-aggregated daily usage statistics for fast dashboard queries and trend analysis';

COMMENT ON COLUMN llm_call_logs.cache_key IS 'Links to tool_descriptions table and enables cache hit/miss correlation';
COMMENT ON COLUMN llm_call_logs.cache_hit IS 'TRUE if this request was served from cache (no actual LLM call), FALSE if LLM API was called';
COMMENT ON COLUMN llm_call_logs.estimated_cost_usd IS 'Calculated cost based on token usage and provider pricing at time of call';
COMMENT ON COLUMN agent_decision_steps.reasoning_text IS 'LLM-generated explanation of why this tool/action was selected';
COMMENT ON COLUMN agent_decision_steps.confidence_score IS 'Agent confidence in this decision (0.0-1.0), useful for quality assessment';

-- =================================================================
-- Migration Complete
-- =================================================================