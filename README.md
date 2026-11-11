# Vio

An AI assistant mod for Minecraft servers (NeoForge 1.21.1). Every player gets a personal assistant in a custom chat window: ask Vio to find something, give items, build structures or write a script, and it does it through server-side tools.

## Features

- **LLM agent** with OpenAI, Anthropic and Ollama providers, configurable per server.
- **~100 tools**: world editing (fill shapes, clone, retexture), entities, players and inventories, NBT, registries, recipes and loot tables, datapacks, web requests.
- **Live scripting**: Vio writes Groovy scripts that run on the server in real time: one-shot, periodic, command and event-listener scripts, each with its own time budget and a timeout guard.
- **Access levels** per player (`none`, `basic`, `full`): basic players get limited, game-balanced help; dangerous operator commands are always blocked.
- **Prompt-injection guards**: player text, tool results and script descriptions are treated as data, never as instructions.
- **Voice chat** with speech-to-text and text-to-speech (ElevenLabs or VoiceStudio), through the optional Simple Voice Chat API.
- **Client UI**: a custom conversation screen with Markdown rendering and HUD status for running tools.

## Stack

Java 21 · NeoForge · Gradle · Groovy · Jackson · OpenAI / Anthropic / Ollama APIs · ElevenLabs

## Building

```bash
./gradlew build
```

The mod JAR is written to `build/libs/`. API keys and model settings go into the server config.
