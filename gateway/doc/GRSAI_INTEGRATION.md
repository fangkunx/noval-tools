# Grsai API Integration Guide

## Overview

Grsai is an AI API proxy service that provides OpenAI-compatible endpoints for various AI models, including the Nano Banana image generation series.

**API Documentation**: https://qmy27nhsd9.apifox.cn/

## Base URLs

| Region | URL | Notes |
|--------|-----|-------|
| Global | `https://grsaiapi.com` | Default, worldwide access |
| China | `https://grsai.dakka.com.cn` | Optimized for China mainland |

## Authentication

All requests require a Bearer token in the Authorization header:

```
Authorization: Bearer sk-xxxxxxxxxxx
```

## Supported Models

### Nano Banana Series (Image Generation)

| Model | Description | Notes |
|-------|-------------|-------|
| `nano-banana` | Basic version | Standard quality |
| `nano-banana-fast` | Fast version | Optimized for speed |
| `nano-banana-2` | Generation 2 | Improved quality |
| `nano-banana-2-cl` | Generation 2 CL | |
| `nano-banana-2-2k-cl` | Generation 2 2K CL | 2K resolution |
| `nano-banana-2-4k-cl` | Generation 2 4K CL | 4K resolution |
| `nano-banana-2.1` | Generation 2.1 | Latest version |
| `nano-banana-pro` | Pro version | Professional quality |
| `nano-banana-pro-vt` | Pro VT | |
| `nano-banana-pro-cl` | Pro CL | |
| `nano-banana-pro-vip` | Pro VIP | Premium tier |
| `nano-banana-pro-4k-vip` | Pro 4K VIP | 4K premium tier |

**Nano Banana 2 Additional Aspect Ratios:**
- Standard: 1:1, 16:9, 9:16
- Extended: 1:4, 4:1, 1:8, 8:1

### Chat Models (OpenAI Compatible)

Grsai also provides proxy access to standard chat models:
- `gpt-4o`
- `gpt-4o-mini`
- `claude-sonnet-5-5`
- `gemini-2.0-flash`

## API Endpoints

### Chat Completions

```
POST /v1/chat/completions
```

**Request Example:**
```bash
curl -X POST https://grsaiapi.com/v1/chat/completions \
  -H "Authorization: Bearer sk-xxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [
      {"role": "system", "content": "You are a helpful assistant."},
      {"role": "user", "content": "Hello!"}
    ],
    "temperature": 0.7
  }'
```

**Response:**
```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1700000000,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "Hello! How can I help you today?"
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 20,
    "completion_tokens": 15,
    "total_tokens": 35
  }
}
```

### Image Generation

```
POST /v1/images/generations
```

**Request Example:**
```bash
curl -X POST https://grsaiapi.com/v1/images/generations \
  -H "Authorization: Bearer sk-xxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "nano-banana-2",
    "prompt": "A cute orange cat playing with yarn ball, digital art",
    "n": 1,
    "size": "1024x1024"
  }'
```

**Response:**
```json
{
  "created": 1700000000,
  "data": [
    {
      "url": "https://example.com/generated-image.png",
      "revised_prompt": null
    }
  ]
}
```

### Image Edits

```
POST /v1/images/edits
```

For image editing capabilities, refer to the [API Documentation](https://qmy27nhsd9.apifox.cn/).

## Configuration

Add to `application.yml` or `application-dev.yml`:

```yaml
gateway:
  providers:
    grsai:
      api-key: ${GRSAI_API_KEY:sk-xxxxxxxxxxx}
      base-url: https://grsaiapi.com  # Or https://grsai.dakka.com.cn for China
```

## Usage via Gateway

Once configured, use the gateway's OpenAI-compatible endpoints:

```bash
# Chat
curl -X POST http://localhost:8082/v1/chat/completions \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-4o",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'

# Image Generation
curl -X POST http://localhost:8082/v1/images/generations \
  -H "Authorization: Bearer gw-default-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "nano-banana-2",
    "prompt": "A cute orange cat",
    "n": 1,
    "size": "1024x1024"
  }'
```

## Implementation Details

- **Provider Type**: `GRSATI`
- **Provider Class**: `GrsaiProvider`
- **Capabilities**: CHAT, VISION, IMAGE_GEN

The implementation follows the OpenAI-compatible format, making it easy to switch between OpenAI and Grsai endpoints.

## Error Handling

Errors follow the OpenAI format:

```json
{
  "error": {
    "message": "Error description",
    "type": "error_type",
    "code": "error_code"
  }
}
```

## Rate Limits

Refer to the [Grsai API Documentation](https://qmy27nhsd9.apifox.cn/) for current rate limits.

---

*Document Version: 1.0.0*  
*Last Updated: 2026-11-15*
