# Migration Plan: Wingie Asynchronous Browsing Agent

## Project State Documentation

### Current Architecture
- **Main Project**: a2aTravelAgent (Playwright-based) on port 7860
- **Subproject**: a2awebagent (Playwright-based) on port 7860
- **Protocols**: A2A and MCP support
- **AI Integration**: OpenAI, Gemini, Claude providers
- **Web Automation**: Booking.com travel research specialization
- **Status**: Fully migrated to Playwright - Selenium dependencies removed

### Current Capabilities
1. Synchronous web automation via JSON-RPC
2. Natural language to web action decomposition
3. Screenshot capture and text extraction
4. Error recovery with AI assistance
5. MCP integration with Claude Desktop

## Migration Phases

### Phase 1: Project Preparation & Rebranding
**Goal**: Prepare codebase for async transformation and rebrand to Wingie

#### 1.1 Backup & Documentation
- [x] Create comprehensive project overview
- [x] Document current API endpoints
- [x] Create async API specification
- [ ] Backup current working state
- [ ] Tag current version in git

#### 1.2 Rebranding (vishalmysore → wingie)
- [ ] Update package names: `io.vishalmysore` → `io.wingie`
- [ ] Update groupId in pom.xml files
- [ ] Update import statements across all Java files
- [ ] Update documentation and README files
- [ ] Update web interface branding
- [ ] Update agent cards and configuration

#### 1.3 Code Organization
- [ ] Consolidate duplicated classes between projects
- [x] Establish clear separation - Selenium completely removed
- [ ] Create shared interfaces for common functionality

### Phase 2: Async Infrastructure Setup
**Goal**: Implement core async capabilities without breaking existing functionality

#### 2.1 Dependencies & Configuration
- [ ] Add Spring Boot Async support
- [ ] Configure thread pools for job execution
- [ ] Add Redis for job queue and caching
- [ ] Add PostgreSQL for job persistence
- [ ] Configure connection pooling

#### 2.2 Core Async Components
- [ ] Create Job model and repository
- [ ] Implement JobService with async methods
- [ ] Create JobQueue interface with Redis implementation
- [ ] Add JobStatus tracking and transitions
- [ ] Implement job scheduling and execution

#### 2.3 Database Schema
```sql
-- Jobs table
CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    input JSONB NOT NULL,
    result JSONB,
    error_details JSONB,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    priority INTEGER DEFAULT 5,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3
);

-- Screenshots table
CREATE TABLE screenshots (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id),
    file_path VARCHAR(255) NOT NULL,
    step_name VARCHAR(100),
    timestamp TIMESTAMP NOT NULL,
    file_size BIGINT,
    width INTEGER,
    height INTEGER
);

-- Job steps table for progress tracking
CREATE TABLE job_steps (
    id UUID PRIMARY KEY,
    job_id UUID REFERENCES jobs(id),
    step_number INTEGER NOT NULL,
    step_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    screenshot_id UUID REFERENCES screenshots(id)
);
```

### Phase 3: API Implementation
**Goal**: Implement the async API endpoints while maintaining backward compatibility

#### 3.1 New Async Endpoints
- [ ] POST /api/v1/jobs (submit job)
- [ ] GET /api/v1/jobs/{id} (get job status)
- [ ] GET /api/v1/jobs (list jobs)
- [ ] POST /api/v1/jobs/{id}/cancel (cancel job)
- [ ] WebSocket /api/v1/jobs/stream (real-time updates)

#### 3.2 Screenshot Management
- [ ] GET /api/v1/jobs/{id}/screenshots (list screenshots)
- [ ] GET /api/v1/screenshots/{id} (get screenshot)
- [ ] File storage service (local/S3-compatible)

#### 3.3 Legacy Compatibility
- [ ] Maintain existing synchronous endpoints
- [ ] Add async=true parameter to legacy endpoints
- [ ] Automatic fallback for timeout scenarios

### Phase 4: Enhanced Features
**Goal**: Add advanced async capabilities and monitoring

#### 4.1 Real-time Updates
- [ ] WebSocket implementation for job progress
- [ ] Server-Sent Events (SSE) alternative
- [ ] Progress tracking with step-by-step updates

#### 4.2 Batch Operations
- [ ] Batch job submission
- [ ] Parallel execution with concurrency limits
- [ ] Bulk status queries

#### 4.3 Monitoring & Analytics
- [ ] System status endpoint
- [ ] Job statistics and performance metrics
- [ ] Prometheus metrics integration
- [ ] Health checks and alerting

### Phase 5: Advanced Features
**Goal**: Add sophisticated job management and optimization

#### 5.1 Job Templates
- [ ] Parameterized job templates
- [ ] Template library for common tasks
- [ ] Template versioning

#### 5.2 Smart Scheduling
- [ ] Priority-based job scheduling
- [ ] Resource-aware execution
- [ ] Automatic retry with exponential backoff

#### 5.3 Performance Optimization
- [ ] Browser instance pooling
- [ ] Result caching
- [ ] Smart screenshot compression

## Technical Considerations

### Backward Compatibility Strategy
1. **Dual Mode Operation**: Support both sync and async modes
2. **API Versioning**: Use /api/v1/ for new async endpoints
3. **Legacy Wrapper**: Wrap async operations for sync compatibility
4. **Configuration Flag**: Allow toggling between modes

### Data Migration
1. **Schema Evolution**: Use Flyway for database migrations
2. **Job History**: Migrate existing job logs if any
3. **Configuration**: Update tools4ai.properties for async settings

### Testing Strategy
1. **Unit Tests**: Test individual async components
2. **Integration Tests**: Test full async workflows
3. **Load Testing**: Verify async performance under load
4. **Compatibility Tests**: Ensure legacy endpoints still work

### Deployment Strategy
1. **Blue-Green Deployment**: Zero-downtime migration
2. **Feature Flags**: Gradual rollout of async features
3. **Rollback Plan**: Quick revert to synchronous mode
4. **Monitoring**: Comprehensive monitoring during migration

## Risk Mitigation

### Technical Risks
- **Memory Leaks**: Proper cleanup of async resources
- **Deadlocks**: Careful thread pool configuration
- **Data Consistency**: Proper transaction management
- **Browser Instances**: Prevent resource exhaustion

### Operational Risks
- **Service Disruption**: Maintain backward compatibility
- **Data Loss**: Robust persistence and error handling
- **Performance Degradation**: Thorough testing and monitoring
- **Configuration Complexity**: Clear documentation and defaults

## Success Criteria

### Performance Metrics
- **Throughput**: Handle 10x more concurrent requests
- **Response Time**: Initial response under 200ms
- **Success Rate**: Maintain 98%+ success rate
- **Resource Usage**: Efficient memory and CPU utilization

### User Experience
- **Real-time Updates**: Progress visible within 1 second
- **Job Tracking**: Complete audit trail for all jobs
- **Error Recovery**: Automatic retry for transient failures
- **API Usability**: Intuitive async API design

## Timeline Estimate
- **Phase 1**: 1 week (Preparation & Rebranding)
- **Phase 2**: 2 weeks (Async Infrastructure)
- **Phase 3**: 2 weeks (API Implementation)
- **Phase 4**: 1 week (Enhanced Features)
- **Phase 5**: 1 week (Advanced Features)

**Total**: 7 weeks for complete migration

## Next Steps
1. Create git branch for async development
2. Begin Phase 1 with rebranding
3. Set up development environment with Redis/PostgreSQL
4. Implement core async infrastructure
5. Gradually migrate features while maintaining compatibility