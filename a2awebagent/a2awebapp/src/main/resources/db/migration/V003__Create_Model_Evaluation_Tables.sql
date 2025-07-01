-- Migration: Create Model Evaluation Tables for Agentic AI Model Assessment
-- Version: V003
-- Description: Adds comprehensive model evaluation framework with screenshot capture
-- Author: Generated for a2aTravelAgent agentic evaluation system

-- =================================================================
-- Model Evaluations Table - Core evaluation tracking
-- =================================================================

CREATE TABLE IF NOT EXISTS model_evaluations (
    evaluation_id VARCHAR(36) PRIMARY KEY,
    
    -- Model Information
    model_name VARCHAR(100) NOT NULL,
    model_provider VARCHAR(50) NOT NULL,
    
    -- Benchmark Information
    benchmark_name VARCHAR(100) NOT NULL,
    benchmark_version VARCHAR(20),
    
    -- Evaluation Status and Progress
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    
    -- Scoring and Performance Metrics
    overall_score DOUBLE PRECISION,
    max_possible_score DOUBLE PRECISION,
    success_rate DOUBLE PRECISION,
    
    -- Task Tracking
    total_tasks INTEGER,
    completed_tasks INTEGER NOT NULL DEFAULT 0,
    successful_tasks INTEGER NOT NULL DEFAULT 0,
    failed_tasks INTEGER NOT NULL DEFAULT 0,
    
    -- Performance Metrics
    average_execution_time_seconds DOUBLE PRECISION,
    total_execution_time_seconds BIGINT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    
    -- Metadata
    error_message TEXT,
    configuration TEXT,
    environment_info TEXT,
    initiated_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT chk_evaluation_status CHECK (
        status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT chk_evaluation_progress CHECK (
        completed_tasks <= total_tasks
    ),
    CONSTRAINT chk_evaluation_success_tracking CHECK (
        successful_tasks + failed_tasks <= completed_tasks
    )
);

-- =================================================================
-- Evaluation Tasks Table - Individual task execution tracking
-- =================================================================

CREATE TABLE IF NOT EXISTS evaluation_tasks (
    task_id VARCHAR(36) PRIMARY KEY,
    
    -- Relationship to evaluation
    evaluation_id VARCHAR(36) NOT NULL,
    
    -- Task Information
    task_name VARCHAR(200) NOT NULL,
    task_description TEXT,
    prompt TEXT NOT NULL,
    
    -- Execution Information
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    execution_order INTEGER,
    
    -- Expected vs Actual Results
    expected_result TEXT,
    actual_result TEXT,
    
    -- Scoring
    success BOOLEAN DEFAULT FALSE,
    score DOUBLE PRECISION,
    max_score DOUBLE PRECISION,
    
    -- Performance Tracking
    execution_time_seconds BIGINT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Evaluation Criteria and Configuration
    evaluation_criteria TEXT,
    timeout_seconds INTEGER DEFAULT 300,  -- 5 minutes default
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 2,
    
    -- Categorization
    task_category VARCHAR(50),            -- 'navigation', 'form_filling', 'data_extraction'
    difficulty_level INTEGER,             -- 1-5 scale
    tags VARCHAR(500),                    -- Comma-separated tags
    
    -- Constraints
    CONSTRAINT fk_evaluation_task_evaluation FOREIGN KEY (evaluation_id)
        REFERENCES model_evaluations(evaluation_id) ON DELETE CASCADE,
    CONSTRAINT chk_task_status CHECK (
        status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'SKIPPED', 'TIMEOUT')
    ),
    CONSTRAINT chk_task_retry_logic CHECK (retry_count <= max_retries),
    CONSTRAINT chk_task_difficulty CHECK (difficulty_level >= 1 AND difficulty_level <= 5)
);

-- =================================================================
-- Evaluation Screenshots Table - Visual evidence capture
-- =================================================================

CREATE TABLE IF NOT EXISTS evaluation_screenshots (
    screenshot_id BIGSERIAL PRIMARY KEY,
    
    -- Relationship to evaluation task
    task_id VARCHAR(36) NOT NULL,
    
    -- Screenshot Information
    screenshot_path VARCHAR(500) NOT NULL,
    step_number INTEGER NOT NULL,
    step_description VARCHAR(500),
    
    -- Screenshot Metadata
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_taken VARCHAR(300),
    before_action BOOLEAN DEFAULT FALSE,  -- true if before action, false if after
    
    -- File Information
    file_size_bytes BIGINT,
    image_width INTEGER,
    image_height INTEGER,
    
    -- Analysis Flags
    success_indicator BOOLEAN DEFAULT FALSE,  -- Screenshot shows success
    error_indicator BOOLEAN DEFAULT FALSE,    -- Screenshot shows error
    
    -- Constraints
    CONSTRAINT fk_evaluation_screenshot_task FOREIGN KEY (task_id)
        REFERENCES evaluation_tasks(task_id) ON DELETE CASCADE,
    CONSTRAINT chk_screenshot_indicators CHECK (
        NOT (success_indicator = TRUE AND error_indicator = TRUE)
    )
);

-- =================================================================
-- Benchmark Definitions Table - Reusable evaluation templates
-- =================================================================

CREATE TABLE IF NOT EXISTS benchmark_definitions (
    benchmark_id VARCHAR(36) PRIMARY KEY,
    
    -- Benchmark Information
    benchmark_name VARCHAR(100) NOT NULL UNIQUE,
    benchmark_version VARCHAR(20) NOT NULL DEFAULT '1.0',
    description TEXT,
    
    -- Configuration
    total_tasks INTEGER NOT NULL DEFAULT 0,
    expected_duration_seconds INTEGER,
    difficulty_rating INTEGER,  -- 1-5 overall difficulty
    
    -- Metadata
    category VARCHAR(50),        -- 'web_automation', 'data_extraction', 'reasoning'
    tags VARCHAR(500),           -- Comma-separated tags
    author VARCHAR(100),
    
    -- Status
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_benchmark_difficulty CHECK (difficulty_rating >= 1 AND difficulty_rating <= 5)
);

-- =================================================================
-- Benchmark Tasks Table - Template tasks for evaluations
-- =================================================================

CREATE TABLE IF NOT EXISTS benchmark_tasks (
    benchmark_task_id VARCHAR(36) PRIMARY KEY,
    
    -- Relationship to benchmark
    benchmark_id VARCHAR(36) NOT NULL,
    
    -- Task Template Information
    task_name VARCHAR(200) NOT NULL,
    task_description TEXT,
    prompt_template TEXT NOT NULL,
    
    -- Scoring Configuration
    max_score DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    evaluation_criteria TEXT,
    
    -- Execution Configuration
    execution_order INTEGER NOT NULL,
    timeout_seconds INTEGER DEFAULT 300,
    max_retries INTEGER DEFAULT 2,
    
    -- Categorization
    task_category VARCHAR(50),
    difficulty_level INTEGER,
    tags VARCHAR(500),
    
    -- Expected Result (optional)
    expected_result_pattern TEXT,  -- Regex or exact match pattern
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_benchmark_task_definition FOREIGN KEY (benchmark_id)
        REFERENCES benchmark_definitions(benchmark_id) ON DELETE CASCADE,
    CONSTRAINT chk_benchmark_task_difficulty CHECK (difficulty_level >= 1 AND difficulty_level <= 5),
    CONSTRAINT chk_benchmark_task_max_score CHECK (max_score > 0)
);

-- =================================================================
-- Indexes for Performance Optimization
-- =================================================================

-- Model Evaluations Indexes
CREATE INDEX IF NOT EXISTS idx_evaluation_status ON model_evaluations(status);
CREATE INDEX IF NOT EXISTS idx_evaluation_model ON model_evaluations(model_name);
CREATE INDEX IF NOT EXISTS idx_evaluation_benchmark ON model_evaluations(benchmark_name);
CREATE INDEX IF NOT EXISTS idx_evaluation_created ON model_evaluations(created_at);
CREATE INDEX IF NOT EXISTS idx_evaluation_performance ON model_evaluations(model_provider, overall_score, success_rate);

-- Evaluation Tasks Indexes
CREATE INDEX IF NOT EXISTS idx_eval_task_status ON evaluation_tasks(status);
CREATE INDEX IF NOT EXISTS idx_eval_task_name ON evaluation_tasks(task_name);
CREATE INDEX IF NOT EXISTS idx_eval_task_evaluation ON evaluation_tasks(evaluation_id);
CREATE INDEX IF NOT EXISTS idx_eval_task_order ON evaluation_tasks(execution_order);
CREATE INDEX IF NOT EXISTS idx_eval_task_category ON evaluation_tasks(task_category);
CREATE INDEX IF NOT EXISTS idx_eval_task_performance ON evaluation_tasks(evaluation_id, status, score);

-- Evaluation Screenshots Indexes
CREATE INDEX IF NOT EXISTS idx_eval_screenshot_task ON evaluation_screenshots(task_id);
CREATE INDEX IF NOT EXISTS idx_eval_screenshot_step ON evaluation_screenshots(step_number);
CREATE INDEX IF NOT EXISTS idx_eval_screenshot_timestamp ON evaluation_screenshots(timestamp);
CREATE INDEX IF NOT EXISTS idx_eval_screenshot_indicators ON evaluation_screenshots(success_indicator, error_indicator);

-- Benchmark Definitions Indexes
CREATE INDEX IF NOT EXISTS idx_benchmark_name ON benchmark_definitions(benchmark_name);
CREATE INDEX IF NOT EXISTS idx_benchmark_active ON benchmark_definitions(is_active);
CREATE INDEX IF NOT EXISTS idx_benchmark_category ON benchmark_definitions(category);

-- Benchmark Tasks Indexes
CREATE INDEX IF NOT EXISTS idx_benchmark_task_benchmark ON benchmark_tasks(benchmark_id);
CREATE INDEX IF NOT EXISTS idx_benchmark_task_order ON benchmark_tasks(benchmark_id, execution_order);
CREATE INDEX IF NOT EXISTS idx_benchmark_task_category ON benchmark_tasks(task_category);

-- =================================================================
-- Views for Model Evaluation Analytics
-- =================================================================

-- Model Performance Comparison View
CREATE OR REPLACE VIEW model_performance_comparison AS
SELECT 
    me.model_provider,
    me.model_name,
    me.benchmark_name,
    COUNT(me.evaluation_id) as total_evaluations,
    AVG(me.overall_score) as avg_overall_score,
    AVG(me.success_rate) as avg_success_rate,
    AVG(me.total_execution_time_seconds) as avg_execution_time_seconds,
    MAX(me.created_at) as latest_evaluation_date,
    STDDEV(me.overall_score) as score_std_dev
FROM model_evaluations me
WHERE me.status = 'COMPLETED'
GROUP BY me.model_provider, me.model_name, me.benchmark_name
ORDER BY avg_overall_score DESC, avg_success_rate DESC;

-- Task Success Analysis View
CREATE OR REPLACE VIEW task_success_analysis AS
SELECT 
    et.task_category,
    et.difficulty_level,
    COUNT(et.task_id) as total_executions,
    SUM(CASE WHEN et.success THEN 1 ELSE 0 END) as successful_executions,
    ROUND((SUM(CASE WHEN et.success THEN 1 ELSE 0 END) * 100.0 / COUNT(et.task_id)), 2) as success_rate_percent,
    AVG(et.execution_time_seconds) as avg_execution_time_seconds,
    AVG(et.score) as avg_score,
    MAX(et.max_score) as max_possible_score
FROM evaluation_tasks et
WHERE et.status IN ('COMPLETED', 'FAILED')
GROUP BY et.task_category, et.difficulty_level
ORDER BY et.task_category, et.difficulty_level;

-- Evaluation Progress Summary View
CREATE OR REPLACE VIEW evaluation_progress_summary AS
SELECT 
    me.evaluation_id,
    me.model_name,
    me.benchmark_name,
    me.status,
    me.total_tasks,
    me.completed_tasks,
    me.successful_tasks,
    me.failed_tasks,
    ROUND((me.completed_tasks * 100.0 / NULLIF(me.total_tasks, 0)), 2) as progress_percent,
    ROUND((me.successful_tasks * 100.0 / NULLIF(me.completed_tasks, 0)), 2) as success_percent,
    me.created_at,
    me.started_at,
    CASE 
        WHEN me.status = 'RUNNING' AND me.started_at IS NOT NULL THEN
            EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - me.started_at))
        WHEN me.status IN ('COMPLETED', 'FAILED') AND me.total_execution_time_seconds IS NOT NULL THEN
            me.total_execution_time_seconds
        ELSE NULL
    END as current_duration_seconds
FROM model_evaluations me
ORDER BY me.created_at DESC;

-- Screenshot Analysis View
CREATE OR REPLACE VIEW screenshot_analysis AS
SELECT 
    es.task_id,
    et.task_name,
    et.task_category,
    COUNT(es.screenshot_id) as total_screenshots,
    SUM(CASE WHEN es.success_indicator THEN 1 ELSE 0 END) as success_screenshots,
    SUM(CASE WHEN es.error_indicator THEN 1 ELSE 0 END) as error_screenshots,
    SUM(CASE WHEN es.before_action THEN 1 ELSE 0 END) as before_action_screenshots,
    SUM(CASE WHEN NOT es.before_action THEN 1 ELSE 0 END) as after_action_screenshots,
    AVG(es.file_size_bytes) as avg_file_size_bytes,
    MIN(es.timestamp) as first_screenshot,
    MAX(es.timestamp) as last_screenshot
FROM evaluation_screenshots es
JOIN evaluation_tasks et ON es.task_id = et.task_id
GROUP BY es.task_id, et.task_name, et.task_category
ORDER BY total_screenshots DESC;

-- =================================================================
-- Insert Sample Benchmark Definitions
-- =================================================================

-- Web Automation Benchmark
INSERT INTO benchmark_definitions (
    benchmark_id, benchmark_name, benchmark_version, description, 
    total_tasks, expected_duration_seconds, difficulty_rating, 
    category, author, tags
) VALUES (
    'web-automation-basic-v1', 
    'Basic Web Automation', 
    '1.0',
    'Fundamental web automation tasks including navigation, form filling, and data extraction',
    10, 
    1800,  -- 30 minutes
    3, 
    'web_automation', 
    'a2aTravelAgent System',
    'navigation,forms,extraction,basic'
) ON CONFLICT (benchmark_name) DO NOTHING;

-- Travel Research Benchmark
INSERT INTO benchmark_definitions (
    benchmark_id, benchmark_name, benchmark_version, description,
    total_tasks, expected_duration_seconds, difficulty_rating,
    category, author, tags
) VALUES (
    'travel-research-v1',
    'Travel Research Automation',
    '1.0', 
    'Complex travel research tasks including flight search, hotel booking, and itinerary planning',
    15,
    3600,  -- 60 minutes
    4,
    'travel_automation',
    'a2aTravelAgent System',
    'travel,booking,research,complex'
) ON CONFLICT (benchmark_name) DO NOTHING;

-- =================================================================
-- Insert Sample Benchmark Tasks
-- =================================================================

-- Basic Web Automation Tasks
INSERT INTO benchmark_tasks (
    benchmark_task_id, benchmark_id, task_name, task_description, 
    prompt_template, max_score, execution_order, task_category, difficulty_level
) VALUES 
(
    'task-navigate-google', 
    'web-automation-basic-v1',
    'Navigate to Google',
    'Successfully navigate to Google.com and verify page load',
    'Navigate to https://google.com and take a screenshot to confirm the page loaded correctly',
    1.0, 1, 'navigation', 1
),
(
    'task-search-query',
    'web-automation-basic-v1', 
    'Perform Search Query',
    'Execute a search query and capture results',
    'Search for "a2a travel agent" on Google and take a screenshot of the results page',
    1.0, 2, 'search', 2
),
(
    'task-form-interaction',
    'web-automation-basic-v1',
    'Form Interaction',
    'Fill out a basic web form',
    'Navigate to a contact form and fill out basic information fields',
    1.0, 3, 'forms', 3
) ON CONFLICT (benchmark_task_id) DO NOTHING;

-- Travel Research Tasks
INSERT INTO benchmark_tasks (
    benchmark_task_id, benchmark_id, task_name, task_description,
    prompt_template, max_score, execution_order, task_category, difficulty_level
) VALUES
(
    'task-flight-search',
    'travel-research-v1',
    'Flight Search',
    'Search for flights between two destinations',
    'Search for flights from {origin} to {destination} on {date} and capture pricing information',
    2.0, 1, 'travel_search', 4
),
(
    'task-hotel-research', 
    'travel-research-v1',
    'Hotel Research',
    'Research hotels in destination city',
    'Find hotels in {destination} for {checkin_date} to {checkout_date} and compare prices',
    2.0, 2, 'travel_search', 4
),
(
    'task-itinerary-planning',
    'travel-research-v1',
    'Itinerary Planning',
    'Create a complete travel itinerary',
    'Create a 3-day itinerary for {destination} including activities, restaurants, and transportation',
    3.0, 3, 'planning', 5
) ON CONFLICT (benchmark_task_id) DO NOTHING;

-- =================================================================
-- Update Benchmark Task Counts
-- =================================================================

UPDATE benchmark_definitions 
SET total_tasks = (
    SELECT COUNT(*) 
    FROM benchmark_tasks 
    WHERE benchmark_tasks.benchmark_id = benchmark_definitions.benchmark_id
);

-- =================================================================
-- Comments and Documentation
-- =================================================================

COMMENT ON TABLE model_evaluations IS 'Tracks complete model evaluation runs with performance metrics and scoring';
COMMENT ON TABLE evaluation_tasks IS 'Individual task executions within model evaluations with detailed results';
COMMENT ON TABLE evaluation_screenshots IS 'Screenshot evidence captured during evaluation task execution';
COMMENT ON TABLE benchmark_definitions IS 'Reusable evaluation benchmark templates for consistent model testing';
COMMENT ON TABLE benchmark_tasks IS 'Template tasks that can be instantiated for model evaluations';

COMMENT ON COLUMN model_evaluations.overall_score IS 'Aggregated score across all tasks in this evaluation';
COMMENT ON COLUMN model_evaluations.success_rate IS 'Percentage of tasks completed successfully (0-100)';
COMMENT ON COLUMN evaluation_tasks.expected_result IS 'Expected outcome pattern for automated scoring';
COMMENT ON COLUMN evaluation_tasks.actual_result IS 'Actual result captured during task execution';
COMMENT ON COLUMN evaluation_screenshots.success_indicator IS 'True if screenshot shows successful task completion';
COMMENT ON COLUMN evaluation_screenshots.error_indicator IS 'True if screenshot shows error or failure state';

-- =================================================================
-- Migration Complete
-- =================================================================