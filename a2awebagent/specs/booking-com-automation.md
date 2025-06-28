# Booking.com Travel Research Automation

## Overview

The `a2awebagent` provides a domain-specific implementation for automating travel research on Booking.com. This specialization includes pre-configured workflows for flights, hotels, and attractions.

## Tool Naming Recommendation

The current generic tool names should be renamed to reflect their Booking.com-specific nature:

- `browseWebAndReturnText` → `browseBookingComAndReturnText`
- `browseWebAndReturnImage` → `browseBookingComAndReturnImage`

This makes it clear that:
1. The tool contains Booking.com-specific logic and selectors
2. The web.action file is optimized for Booking.com's UI structure
3. Other domains would require different configurations

## Web.action Configuration

The `src/main/resources/web.action` file contains a comprehensive Booking.com research program with:

### Parameterized Variables
- `{origin_city}` - Starting location for flights
- `{destination_city}` - Travel destination
- `{travel_date}` - Departure date
- `{travel_month}` - Month for calendar navigation
- `{travel_year}` - Year for calendar navigation
- `{return_date}` - Return flight date (if applicable)
- `{checkout_date}` - Hotel checkout date

### Workflow Steps
1. **Flight Search** - Searches one-way flights with proper dropdown handling
2. **Hotel Search** - Filters by rating and price with multiple sort options
3. **Attractions** - Discovers activities, museums, and tours
4. **Data Compilation** - Creates structured travel report

## Testing the Implementation

### Method 1: Direct Web.action Execution
Since a2awebagent is running, it should automatically execute the web.action file:

```bash
# The application loads and executes web.action on startup
# Check logs for execution progress
```

### Method 2: JSON-RPC API Call
Send requests to the running a2awebagent:

```bash
# Test text extraction
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "browseWebAndReturnText",
  "params": {
    "arg0": "Navigate to booking.com and search for flights from Amsterdam to Palma on July 6th 2025"
  },
  "id": 1
}'

# Test screenshot capture
curl -X POST http://localhost:7860 \
-H "Content-Type: application/json" \
-d '{
  "jsonrpc": "2.0",
  "method": "browseWebAndReturnImage",
  "params": {
    "arg0": "Go to booking.com and take a screenshot of the homepage"
  },
  "id": 2
}'
```

### Method 3: Browser UI
Navigate to http://localhost:7860 to access the web interface (if available).

## Expected Behavior

When properly configured, the system should:

1. **Parse Natural Language** - Convert queries like "find flights from Amsterdam to Palma" into web actions
2. **Execute Web Automation** - Navigate Booking.com using Selenium WebDriver
3. **Handle Dynamic Elements** - Wait for dropdowns, calendars, and AJAX content
4. **Extract Data** - Capture prices, times, ratings from search results
5. **Generate Reports** - Compile findings into structured output

## Troubleshooting

### Common Issues
1. **CDP Version Warning** - Update Selenium or use compatible Chrome version
2. **Element Not Found** - Booking.com may have changed their UI selectors
3. **Timeout Errors** - Increase wait times for slow-loading pages

### Debugging Steps
1. Check application logs for detailed error messages
2. Verify Chrome/Chromium is installed and accessible
3. Test with simpler actions first (just navigation)
4. Use screenshot capture to see what the automation sees

## Integration with a2aTravelAgent

The two projects can work together:
- **a2awebagent** - Executes the Booking.com-specific automation
- **a2aTravelAgent** - Provides the AI layer for natural language processing

This separation allows for:
- Domain-specific optimizations in a2awebagent
- Generic AI capabilities in a2aTravelAgent
- Easy addition of other travel sites (Expedia, Kayak, etc.)