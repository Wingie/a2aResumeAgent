# Model Management System

## Overview

The a2awebagent now includes a comprehensive model management system that allows administrators to switch between different AI models and providers through an intuitive web interface.

## Features

### 🧠 Model Selection & Switching
- **6 Pre-configured Models**: Choose from OpenRouter, OpenAI, Anthropic Claude, Mistral, and Google Gemini models
- **Real-time Switching**: Switch models without restarting the application (runtime switching)
- **Provider Support**: OpenRouter (free models), OpenAI, Anthropic, Mistral, Google Cloud
- **Tier Indicators**: Clear badges showing free vs paid models

### 🔧 Admin Dashboard (/startup)
- **Model Statistics**: View current model, available models, and cache statistics
- **Configuration Validation**: Automatic API key validation for each provider
- **Quick Tool Testing**: Test tools immediately with the selected model
- **Cache Management**: Clear old model caches and view statistics

### 🛡️ Safety & Validation
- **API Key Validation**: Checks if required API keys are configured before switching
- **Error Handling**: Comprehensive error messages and fallback options
- **Cache Isolation**: Separate caches for each model to prevent conflicts

## Available Models

| Model | Provider | Type | Description |
|-------|----------|------|-------------|
| **Gemma 3B** | OpenRouter | Free | Google Gemma 3B - Fast and efficient |
| **DeepSeek R1** | OpenRouter | Free | Advanced reasoning model |
| **GPT-4o Mini** | OpenAI | Paid | OpenAI's optimized mini model |
| **Claude 3 Haiku** | Anthropic | Paid | Fast and lightweight Claude model |
| **Mistral Large** | Mistral | Paid | Advanced reasoning capabilities |
| **Gemini 2.0 Flash** | Google | Paid | Fast multimodal model |

## Quick Start

### 1. Access Admin Dashboard
Navigate to: `http://localhost:7860/startup`

### 2. Select a Model
- Click on any model card to select it
- View validation status (green checkmark = ready, red warning = configuration needed)
- Click "Switch to Selected Model" button

### 3. Test the Model
- Use the Quick Tool Test panel on the right
- Select any available tool from the dropdown
- Enter test input and click "Test Tool"
- View results and performance metrics

### 4. Monitor Performance
- Track tool call counts and cache hit rates
- View model-specific statistics
- Manage cache storage per model

## Configuration

### Required API Keys (in tools4ai.properties or environment)
```properties
# OpenRouter (for free models)
openrouterKey=your_openrouter_key

# OpenAI
openAiKey=your_openai_key

# Anthropic Claude
claudeKey=your_claude_key

# Mistral
mistralKey=your_mistral_key

# Google Cloud (for Gemini)
gemini.projectId=your_project_id
```

### Model Switching Behavior
- **Runtime Only**: Model switches are active immediately but require restart for persistence
- **Cache Isolation**: Each model maintains separate cache entries
- **Automatic Validation**: System checks API key availability before switching

## API Endpoints

### Get Available Models
```http
GET /startup/api/models
```

### Switch Model
```http
POST /startup/api/models/switch
Content-Type: application/json

{
  "modelKey": "gpt-4o-mini",
  "clearOldCache": true
}
```

### Validate Model
```http
GET /startup/api/models/validate/{modelKey}
```

### Get Model Statistics
```http
GET /startup/api/models/statistics
```

## Integration with Tool Testing

The model management system is fully integrated with the existing tool testing interface:

1. **Dynamic Tool Loading**: Available tools are loaded from the current model
2. **Real-time Testing**: Test tools immediately after switching models
3. **Performance Metrics**: Track execution times and success rates per model
4. **Error Handling**: Graceful handling of model-specific errors (e.g., Gemma token limits)

## Troubleshooting

### Model Switch Failed
- **Check API Keys**: Ensure the required API key is configured
- **Network Issues**: Verify internet connectivity for remote providers
- **Rate Limits**: Free tier models may have usage restrictions

### Tool Test Failures
- **Model Compatibility**: Some tools may work better with certain models
- **Input Format**: Ensure test input is appropriate for the selected tool
- **Token Limits**: Free models may have stricter token limitations

### Cache Issues
- **Clear Cache**: Use the "Clear Current Model" button to reset cached descriptions
- **Restart Required**: For persistent model changes, restart the application
- **Storage Space**: Monitor cache statistics to prevent excessive storage usage

## Best Practices

1. **Start with Free Models**: Test functionality with OpenRouter's free models first
2. **Validate Configuration**: Always check the green validation checkmarks before switching
3. **Monitor Performance**: Use the metrics dashboard to track model effectiveness
4. **Clear Old Caches**: Periodically clean up unused model caches
5. **Test Tools**: Always test critical tools after switching models

## Future Enhancements

- **Persistent Model Switching**: Write changes directly to configuration files
- **Model Performance Analytics**: Detailed metrics and comparison tools
- **Automatic Model Selection**: AI-driven model recommendation based on task type
- **Custom Model Addition**: Support for adding custom models and providers
- **Bulk Operations**: Batch tool testing across multiple models

## Support

For issues or questions about the model management system, please check:
1. Application logs: `./logs/a2awebagent.log`
2. Browser console for frontend errors
3. Cache dashboard at `/cache` for detailed cache information
4. Tool testing interface at `/tools-test` for comprehensive tool testing