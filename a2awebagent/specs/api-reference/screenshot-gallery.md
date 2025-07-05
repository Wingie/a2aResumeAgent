# Enhanced Screenshot Gallery API Documentation

## Overview

The Enhanced Screenshot Gallery API provides comprehensive screenshot management functionality including gallery views, similarity search, analytics, and manual processing capabilities. All endpoints are prefixed with `/api/screenshots`.

## Base URL
```
http://localhost:7860/api/screenshots
```

## Endpoints

### 1. Gallery Data Retrieval

#### GET `/gallery`
Retrieves gallery data with filtering, pagination, and sorting options.

**Parameters:**
- `limit` (optional, default: 50) - Maximum number of screenshots to return
- `filter` (optional, default: "all") - Filter criteria for screenshots
- `sortBy` (optional, default: "recent") - Sort order for results

**Response:**
```json
{
  "screenshots": [
    {
      "id": "screenshot-id",
      "taskId": "task-id",
      "timestamp": "2025-07-04T10:30:00Z",
      "url": "https://example.com",
      "isSuccess": true,
      "actionContext": "Navigation completed",
      "thumbnailPath": "/thumbnails/screenshot-id_small.png",
      "path": "/screenshots/screenshot-id.png"
    }
  ],
  "totalCount": 150,
  "hasMore": true,
  "filter": "all",
  "sortBy": "recent"
}
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/gallery?limit=20&filter=success&sortBy=recent"
```

---

### 2. Task Screenshots

#### GET `/task/{taskId}`
Retrieves all screenshots associated with a specific task.

**Path Parameters:**
- `taskId` (required) - The ID of the task

**Response:**
```json
[
  {
    "id": "screenshot-id",
    "taskId": "task-id",
    "timestamp": "2025-07-04T10:30:00Z",
    "url": "https://example.com",
    "isSuccess": true,
    "actionContext": "Page loaded successfully",
    "path": "/screenshots/screenshot-id.png"
  }
]
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/task/abc-123-def"
```

---

### 3. Task Screenshot Timeline

#### GET `/task/{taskId}/timeline`
Retrieves screenshots for a task ordered chronologically to show progression.

**Path Parameters:**
- `taskId` (required) - The ID of the task

**Response:**
```json
[
  {
    "id": "screenshot-1",
    "timestamp": "2025-07-04T10:30:00Z",
    "actionContext": "Initial page load",
    "isSuccess": true
  },
  {
    "id": "screenshot-2", 
    "timestamp": "2025-07-04T10:30:15Z",
    "actionContext": "Form filled",
    "isSuccess": true
  }
]
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/task/abc-123-def/timeline"
```

---

### 4. Similar Screenshots

#### GET `/{screenshotId}/similar`
Finds screenshots similar to the specified screenshot using image similarity algorithms.

**Path Parameters:**
- `screenshotId` (required) - The ID of the reference screenshot

**Query Parameters:**
- `limit` (optional, default: 10) - Maximum number of similar screenshots to return

**Response:**
```json
[
  {
    "id": "similar-screenshot-1",
    "similarity": 0.85,
    "taskId": "task-id",
    "timestamp": "2025-07-04T09:15:00Z",
    "url": "https://example.com/similar-page"
  }
]
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/screenshot-123/similar?limit=5"
```

---

### 5. URL Pattern Search

#### GET `/search/url`
Finds screenshots by URL pattern matching.

**Query Parameters:**
- `urlPattern` (required) - URL pattern to search for (supports wildcards)

**Response:**
```json
[
  {
    "id": "screenshot-id",
    "url": "https://example.com/matching-page",
    "taskId": "task-id",
    "timestamp": "2025-07-04T10:30:00Z"
  }
]
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/search/url?urlPattern=https://example.com/*"
```

---

### 6. Screenshot Analytics

#### GET `/analytics`
Retrieves comprehensive analytics about the screenshot collection.

**Response:**
```json
{
  "totalScreenshots": 1250,
  "successRate": 0.87,
  "failureRate": 0.13,
  "screenshotsByDay": {
    "2025-07-04": 45,
    "2025-07-03": 38,
    "2025-07-02": 42
  },
  "topUrls": [
    {
      "url": "https://example.com",
      "count": 25
    }
  ],
  "averageTaskDuration": "00:02:30",
  "mostActiveHours": [14, 15, 16],
  "taskTypeDistribution": {
    "navigation": 40,
    "form_fill": 30,
    "search": 20,
    "other": 10
  }
}
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/analytics"
```

---

### 7. Manual Screenshot Processing

#### POST `/process`
Manually processes a screenshot for testing purposes.

**Parameters:**
- `taskId` (required) - The task ID to associate with the screenshot
- `screenshotPath` (required) - File path to the screenshot
- `url` (optional) - URL where the screenshot was taken
- `isSuccess` (optional, default: true) - Whether the action was successful
- `actionContext` (optional) - Description of the action that was performed

**Response:**
```
Screenshot processed successfully
```

**Example:**
```bash
curl -X POST "http://localhost:7860/api/screenshots/process" \
  -d "taskId=test-task-123" \
  -d "screenshotPath=/screenshots/test.png" \
  -d "url=https://example.com" \
  -d "isSuccess=true" \
  -d "actionContext=Manual test"
```

---

### 8. Base64 Screenshot Processing

#### POST `/process/base64`
Processes a screenshot from base64 data for testing purposes.

**Parameters:**
- `taskId` (required) - The task ID to associate with the screenshot
- `base64Data` (required, body) - Base64 encoded screenshot data
- `url` (optional) - URL where the screenshot was taken
- `isSuccess` (optional, default: true) - Whether the action was successful
- `actionContext` (optional) - Description of the action that was performed

**Request Body:**
```
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==
```

**Example:**
```bash
curl -X POST "http://localhost:7860/api/screenshots/process/base64?taskId=test-task-123" \
  -H "Content-Type: text/plain" \
  -d "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="
```

---

### 9. Gallery Page

#### GET `/gallery-page`
Returns the screenshot gallery HTML page.

**Response:**
HTML page for the screenshot gallery interface.

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/gallery-page"
```

---

### 10. Cleanup Old Screenshots

#### POST `/cleanup`
Removes old screenshots to free up storage space.

**Parameters:**
- `daysToKeep` (optional, default: 30) - Number of days worth of screenshots to retain

**Response:**
```
Screenshots cleaned up successfully
```

**Example:**
```bash
curl -X POST "http://localhost:7860/api/screenshots/cleanup?daysToKeep=14"
```

---

### 11. Health Check

#### GET `/health`
Provides health status and basic statistics for the screenshot gallery service.

**Response:**
```json
{
  "status": "healthy",
  "totalScreenshots": 1250,
  "successRate": 0.87,
  "timestamp": 1720087800000
}
```

**Example:**
```bash
curl "http://localhost:7860/api/screenshots/health"
```

---

## Error Responses

All endpoints return consistent error responses:

**500 Internal Server Error:**
```json
{
  "status": "unhealthy",
  "error": "Error message details",
  "timestamp": 1720087800000
}
```

**400 Bad Request:**
```json
{
  "error": "Invalid parameter",
  "message": "Detailed error description"
}
```

## Data Models

### ScreenshotNode
```json
{
  "id": "string",
  "taskId": "string", 
  "timestamp": "ISO 8601 datetime",
  "url": "string",
  "isSuccess": "boolean",
  "actionContext": "string",
  "path": "string",
  "thumbnailPath": "string (optional)",
  "similarity": "number (for similar screenshot results)"
}
```

### GalleryData
```json
{
  "screenshots": "ScreenshotNode[]",
  "totalCount": "number",
  "hasMore": "boolean", 
  "filter": "string",
  "sortBy": "string"
}
```

## Authentication

Currently, the API does not require authentication. All endpoints are publicly accessible on the local development environment.

## Rate Limiting

No rate limiting is currently implemented. For production use, consider implementing rate limiting on the cleanup and processing endpoints.

## Integration with Neo4j

The Enhanced Screenshot Gallery leverages Neo4j graph database for:
- **Similarity Detection**: Using graph algorithms to find visually similar screenshots
- **Relationship Mapping**: Connecting screenshots to tasks, URLs, and action contexts  
- **Analytics Queries**: Complex queries for usage patterns and trends
- **Timeline Analysis**: Understanding screenshot sequences and user journeys

## Thumbnail Integration

The API integrates with the ThumbnailService to provide:
- **Automatic Thumbnail Generation**: Creates multiple sizes (small, medium, large)
- **Lazy Loading Support**: Thumbnails for gallery performance
- **Responsive Images**: Different sizes for different display contexts