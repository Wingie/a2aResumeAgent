# Asynchronous Browsing Agent API Specification

## Overview

This document outlines the API endpoints for the asynchronous browsing agent system that will enable job tracking, screenshot management, and state monitoring for web automation tasks.

## Core Concepts

### Job Lifecycle
1. **Submitted** - Job received and queued
2. **Processing** - Job actively being executed
3. **Completed** - Job finished successfully
4. **Failed** - Job encountered an error
5. **Cancelled** - Job was cancelled by user

### Data Models

```json
// Job Request
{
  "id": "job-uuid",
  "type": "browseWebAndReturnText" | "browseWebAndReturnImage",
  "input": {
    "provideAllValuesInPlainEnglish": "string"
  },
  "priority": "high" | "normal" | "low",
  "createdAt": "2025-06-27T19:00:00Z"
}

// Job Status
{
  "id": "job-uuid",
  "status": "submitted" | "processing" | "completed" | "failed" | "cancelled",
  "progress": {
    "currentStep": "Navigating to booking.com",
    "stepsCompleted": 3,
    "totalSteps": 10,
    "percentage": 30
  },
  "createdAt": "2025-06-27T19:00:00Z",
  "startedAt": "2025-06-27T19:00:05Z",
  "completedAt": "2025-06-27T19:01:30Z",
  "duration": 85000, // milliseconds
  "result": {
    "type": "text" | "image" | "mixed",
    "data": "string or base64",
    "screenshots": ["screenshot-id-1", "screenshot-id-2"],
    "metadata": {}
  },
  "error": {
    "code": "NAVIGATION_FAILED",
    "message": "Failed to navigate to URL",
    "details": {}
  }
}
```

## API Endpoints

### 1. Job Management

#### Submit Job
```
POST /api/v1/jobs
Content-Type: application/json

{
  "type": "browseWebAndReturnText",
  "input": {
    "provideAllValuesInPlainEnglish": "Search for flights from Amsterdam to Palma"
  },
  "priority": "normal",
  "options": {
    "timeout": 300000, // 5 minutes
    "retryOnFailure": true,
    "maxRetries": 3
  }
}

Response: 202 Accepted
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "submitted",
  "estimatedDuration": 120000,
  "queuePosition": 3
}
```

#### Get Job Status
```
GET /api/v1/jobs/{jobId}

Response: 200 OK
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "processing",
  "progress": {
    "currentStep": "Filling search form",
    "stepsCompleted": 5,
    "totalSteps": 12,
    "percentage": 42
  }
}
```

#### List Jobs
```
GET /api/v1/jobs?status=processing&limit=20&offset=0

Response: 200 OK
{
  "jobs": [...],
  "total": 150,
  "hasMore": true
}
```

#### Cancel Job
```
POST /api/v1/jobs/{jobId}/cancel

Response: 200 OK
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "cancelled",
  "message": "Job cancelled successfully"
}
```

#### Retry Failed Job
```
POST /api/v1/jobs/{jobId}/retry

Response: 202 Accepted
{
  "newJobId": "660e8400-e29b-41d4-a716-446655440001",
  "originalJobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "submitted"
}
```

### 2. Screenshot Management

#### List Screenshots for Job
```
GET /api/v1/jobs/{jobId}/screenshots

Response: 200 OK
{
  "screenshots": [
    {
      "id": "screenshot-001",
      "name": "amsterdam_to_palma_flights.png",
      "step": "Search results page",
      "timestamp": "2025-06-27T19:00:45Z",
      "size": 245678,
      "dimensions": {
        "width": 1920,
        "height": 1080
      }
    }
  ],
  "total": 5
}
```

#### Get Screenshot
```
GET /api/v1/screenshots/{screenshotId}

Response: 200 OK
Content-Type: image/png
[Binary image data]

// Or with metadata
GET /api/v1/screenshots/{screenshotId}?include=metadata

Response: 200 OK
{
  "id": "screenshot-001",
  "metadata": {...},
  "url": "/api/v1/screenshots/screenshot-001/download",
  "base64": "data:image/png;base64,..."
}
```

#### Delete Screenshot
```
DELETE /api/v1/screenshots/{screenshotId}

Response: 204 No Content
```

### 3. Real-time Updates

#### WebSocket Connection
```
WS /api/v1/jobs/stream

// Client subscribes to job updates
{
  "action": "subscribe",
  "jobIds": ["job-001", "job-002"]
}

// Server sends updates
{
  "type": "progress",
  "jobId": "job-001",
  "data": {
    "status": "processing",
    "progress": {
      "currentStep": "Extracting prices",
      "percentage": 85
    }
  }
}
```

#### Server-Sent Events (SSE)
```
GET /api/v1/jobs/{jobId}/events

Response: 200 OK
Content-Type: text/event-stream

event: progress
data: {"step": "Navigating to website", "percentage": 10}

event: screenshot
data: {"screenshotId": "screenshot-001", "name": "search_form.png"}

event: complete
data: {"status": "completed", "result": {...}}
```

### 4. Batch Operations

#### Submit Multiple Jobs
```
POST /api/v1/jobs/batch

{
  "jobs": [
    {
      "type": "browseWebAndReturnText",
      "input": {...},
      "priority": "high"
    },
    {
      "type": "browseWebAndReturnImage",
      "input": {...},
      "priority": "normal"
    }
  ],
  "options": {
    "parallel": true,
    "maxConcurrent": 3
  }
}

Response: 202 Accepted
{
  "batchId": "batch-001",
  "jobIds": ["job-001", "job-002"],
  "status": "submitted"
}
```

### 5. Analytics & Monitoring

#### Get System Status
```
GET /api/v1/system/status

Response: 200 OK
{
  "status": "healthy",
  "queue": {
    "pending": 15,
    "processing": 3,
    "completed": 1250,
    "failed": 23
  },
  "workers": {
    "total": 5,
    "active": 3,
    "idle": 2
  },
  "performance": {
    "averageJobDuration": 45000,
    "successRate": 0.98,
    "throughput": "120 jobs/hour"
  }
}
```

#### Get Job Statistics
```
GET /api/v1/analytics/jobs?from=2025-06-01&to=2025-06-27

Response: 200 OK
{
  "period": {
    "from": "2025-06-01T00:00:00Z",
    "to": "2025-06-27T23:59:59Z"
  },
  "statistics": {
    "totalJobs": 5420,
    "successfulJobs": 5312,
    "failedJobs": 108,
    "averageDuration": 52000,
    "peakHour": "14:00-15:00",
    "mostCommonErrors": [
      {
        "code": "TIMEOUT",
        "count": 45,
        "percentage": 41.7
      }
    ]
  }
}
```

### 6. Configuration & Templates

#### Create Job Template
```
POST /api/v1/templates

{
  "name": "Booking.com Flight Search",
  "description": "Template for searching flights on Booking.com",
  "type": "browseWebAndReturnText",
  "defaultInput": {
    "provideAllValuesInPlainEnglish": "Search for flights from {origin} to {destination} on {date}"
  },
  "variables": ["origin", "destination", "date"]
}

Response: 201 Created
{
  "templateId": "template-001",
  "name": "Booking.com Flight Search"
}
```

#### Execute Template
```
POST /api/v1/templates/{templateId}/execute

{
  "variables": {
    "origin": "Amsterdam",
    "destination": "Palma",
    "date": "July 6th, 2025"
  }
}

Response: 202 Accepted
{
  "jobId": "job-003",
  "templateId": "template-001"
}
```

## Error Handling

### Standard Error Response
```json
{
  "error": {
    "code": "JOB_NOT_FOUND",
    "message": "Job with ID 'job-999' not found",
    "timestamp": "2025-06-27T19:00:00Z",
    "path": "/api/v1/jobs/job-999",
    "details": {
      "jobId": "job-999"
    }
  }
}
```

### Error Codes
- `JOB_NOT_FOUND` - Requested job does not exist
- `JOB_ALREADY_PROCESSING` - Job is already being processed
- `QUEUE_FULL` - Job queue has reached maximum capacity
- `INVALID_INPUT` - Input validation failed
- `TIMEOUT` - Job execution timed out
- `BROWSER_ERROR` - Browser automation error
- `NETWORK_ERROR` - Network connectivity issue
- `RATE_LIMITED` - API rate limit exceeded

## Rate Limiting

```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1719514800
```

## Authentication

```
Authorization: Bearer <api-token>
X-API-Key: <api-key>
```

## Pagination

All list endpoints support pagination:
```
?page=1&size=20
?limit=20&offset=40
```

## Filtering & Sorting

```
GET /api/v1/jobs?status=completed&priority=high&sort=-createdAt&from=2025-06-01
```

## WebHooks

Register webhooks for job events:
```
POST /api/v1/webhooks

{
  "url": "https://example.com/webhook",
  "events": ["job.completed", "job.failed"],
  "secret": "webhook-secret"
}
```

## Implementation Notes

1. **Job Queue**: Use Redis or RabbitMQ for job queuing
2. **Storage**: Store screenshots in S3-compatible storage
3. **Database**: PostgreSQL for job metadata and state
4. **Caching**: Redis for caching job status
5. **Monitoring**: Prometheus metrics endpoint at `/metrics`
6. **API Gateway**: Consider Kong or Spring Cloud Gateway
7. **Documentation**: OpenAPI/Swagger at `/api/docs`

This API design provides comprehensive job tracking, screenshot management, and monitoring capabilities for the asynchronous browsing agent system.