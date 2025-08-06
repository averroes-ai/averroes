// AI integration module - placeholder for future rig/langchain-rs integration
// This module will contain:
// - AI model clients (OpenAI, Anthropic, local models)
// - Langchain-rs integration for prompt chaining
// - Rig agent orchestration
// - Vector embedding generation
// - Fine-tuned Islamic finance models

pub mod chains;
pub mod embeddings;
pub mod grok_client;
pub mod groq_client;
pub mod models;
pub mod openai_client;
pub mod service;

pub use chains::*;
pub use embeddings::*;
pub use grok_client::*;
pub use groq_client::*;
pub use models::*;
pub use openai_client::*;
pub use service::*;
