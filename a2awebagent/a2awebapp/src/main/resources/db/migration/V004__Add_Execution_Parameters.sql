-- Add execution parameter fields to evaluation_tasks table
-- This migration adds user-controlled execution parameters to support
-- fine-grained control over task execution behavior

-- Add execution parameters columns
ALTER TABLE evaluation_tasks ADD COLUMN execution_parameters TEXT;
ALTER TABLE evaluation_tasks ADD COLUMN max_steps INTEGER DEFAULT 10;
ALTER TABLE evaluation_tasks ADD COLUMN execution_mode VARCHAR(20) DEFAULT 'MULTI_STEP';
ALTER TABLE evaluation_tasks ADD COLUMN early_completion_allowed BOOLEAN DEFAULT true;
ALTER TABLE evaluation_tasks ADD COLUMN steps_completed INTEGER;
ALTER TABLE evaluation_tasks ADD COLUMN early_completion_triggered BOOLEAN;

-- Add comments for documentation
COMMENT ON COLUMN evaluation_tasks.execution_parameters IS 'JSON string containing complete ExecutionParameters object';
COMMENT ON COLUMN evaluation_tasks.max_steps IS 'Maximum number of automation steps allowed (quick access field)';
COMMENT ON COLUMN evaluation_tasks.execution_mode IS 'Execution mode: ONE_SHOT, MULTI_STEP, or AUTO';
COMMENT ON COLUMN evaluation_tasks.early_completion_allowed IS 'Whether task can complete early if objectives are met';
COMMENT ON COLUMN evaluation_tasks.steps_completed IS 'Actual number of steps executed';
COMMENT ON COLUMN evaluation_tasks.early_completion_triggered IS 'Whether task completed early due to confidence threshold';

-- Add indexes for performance on common queries
CREATE INDEX idx_eval_task_execution_mode ON evaluation_tasks(execution_mode);
CREATE INDEX idx_eval_task_max_steps ON evaluation_tasks(max_steps);
CREATE INDEX idx_eval_task_early_completion ON evaluation_tasks(early_completion_allowed);

-- Add constraint to ensure execution_mode is valid
ALTER TABLE evaluation_tasks ADD CONSTRAINT chk_execution_mode 
    CHECK (execution_mode IN ('ONE_SHOT', 'MULTI_STEP', 'AUTO'));

-- Add constraint to ensure max_steps is positive
ALTER TABLE evaluation_tasks ADD CONSTRAINT chk_max_steps_positive 
    CHECK (max_steps > 0);

-- Add constraint to ensure steps_completed is not negative
ALTER TABLE evaluation_tasks ADD CONSTRAINT chk_steps_completed_non_negative 
    CHECK (steps_completed >= 0);

-- Update any existing tasks to have default values
UPDATE evaluation_tasks 
SET 
    max_steps = 10,
    execution_mode = 'MULTI_STEP',
    early_completion_allowed = true
WHERE 
    max_steps IS NULL 
    OR execution_mode IS NULL 
    OR early_completion_allowed IS NULL;

-- Create a partial index for active tasks with execution parameters
CREATE INDEX idx_eval_task_active_execution ON evaluation_tasks(status, execution_mode, max_steps) 
WHERE status IN ('PENDING', 'RUNNING');

-- Add execution efficiency view for analytics
CREATE OR REPLACE VIEW evaluation_task_efficiency AS
SELECT 
    task_id,
    task_name,
    execution_mode,
    max_steps,
    steps_completed,
    early_completion_triggered,
    CASE 
        WHEN steps_completed IS NULL OR max_steps IS NULL OR max_steps = 0 THEN 0.0
        ELSE (steps_completed::float / max_steps::float) * 100.0
    END as efficiency_percentage,
    CASE 
        WHEN early_completion_triggered = true THEN 'Early Completion'
        WHEN steps_completed = max_steps THEN 'Full Execution'
        WHEN steps_completed < max_steps THEN 'Partial Execution'
        ELSE 'Unknown'
    END as execution_type,
    status,
    success,
    score,
    execution_time_seconds,
    created_at,
    completed_at
FROM evaluation_tasks
WHERE status IN ('COMPLETED', 'FAILED', 'TIMEOUT')
  AND steps_completed IS NOT NULL;

-- Grant appropriate permissions on the view
GRANT SELECT ON evaluation_task_efficiency TO PUBLIC;

-- Add execution parameter statistics view
CREATE OR REPLACE VIEW execution_parameter_stats AS
SELECT 
    execution_mode,
    COUNT(*) as total_tasks,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_tasks,
    COUNT(CASE WHEN early_completion_triggered = true THEN 1 END) as early_completion_count,
    AVG(CASE WHEN steps_completed IS NOT NULL AND max_steps IS NOT NULL AND max_steps > 0 
             THEN (steps_completed::float / max_steps::float) * 100.0 END) as avg_efficiency,
    AVG(max_steps) as avg_max_steps,
    AVG(steps_completed) as avg_steps_completed,
    AVG(execution_time_seconds) as avg_execution_time_seconds
FROM evaluation_tasks
WHERE execution_mode IS NOT NULL
GROUP BY execution_mode;

-- Grant permissions on stats view
GRANT SELECT ON execution_parameter_stats TO PUBLIC;

-- Create sample execution parameter configurations for testing
INSERT INTO evaluation_tasks (
    task_id, 
    evaluation_id, 
    task_name, 
    task_description, 
    prompt,
    status,
    max_steps,
    execution_mode,
    early_completion_allowed,
    execution_parameters,
    created_at,
    updated_at
) 
SELECT 
    'sample-' || generate_random_uuid(),
    'sample-eval-' || generate_random_uuid(),
    'Sample ' || mode.execution_mode || ' Task',
    'Sample task for testing ' || mode.execution_mode || ' execution mode',
    'Perform a sample web automation task with ' || mode.execution_mode || ' parameters',
    'PENDING',
    mode.max_steps,
    mode.execution_mode,
    mode.early_completion,
    mode.json_params,
    NOW(),
    NOW()
FROM (
    VALUES 
        ('ONE_SHOT', 1, false, '{"maxSteps": 1, "executionMode": "ONE_SHOT", "allowEarlyCompletion": false}'),
        ('MULTI_STEP', 5, true, '{"maxSteps": 5, "executionMode": "MULTI_STEP", "allowEarlyCompletion": true}'),
        ('AUTO', 10, true, '{"maxSteps": 10, "executionMode": "AUTO", "allowEarlyCompletion": true, "earlyCompletionThreshold": 0.8}')
) AS mode(execution_mode, max_steps, early_completion, json_params)
WHERE NOT EXISTS (
    SELECT 1 FROM evaluation_tasks 
    WHERE task_name LIKE 'Sample % Task' 
    AND execution_mode = mode.execution_mode
);

-- Log migration completion
INSERT INTO schema_migrations_log (migration_version, description, applied_at) 
VALUES ('V004', 'Added execution parameters support to evaluation_tasks', NOW())
ON CONFLICT (migration_version) DO NOTHING;