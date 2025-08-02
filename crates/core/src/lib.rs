// UniFFI exports
uniffi::setup_scaffolding!();

use std::sync::Arc;
use std::sync::Mutex;

use log;
use rig::client::CompletionClient;
use rig::completion::Prompt;
use rig::providers::groq;
use tokio::runtime::Runtime;

// ============================================================================
// SIMPLE FIQH AI DATA STRUCTURES
// ============================================================================

#[derive(uniffi::Record, Clone, Debug)]
pub struct FiqhAIConfig {
    pub groq_api_key: String,
    pub model_name: String,
    pub preferred_model: String, // "groq" or "mock"
}

#[derive(uniffi::Record, Clone, Debug)]
pub struct QueryResponse {
    pub query_id: String,
    pub response: String,
    pub confidence: f64,
    pub sources: Vec<String>,
    pub timestamp: u64,
}

// New: Streaming chunk for real-time responses
#[derive(uniffi::Record, Clone, Debug)]
pub struct StreamChunk {
    pub query_id: String,
    pub content: String,
    pub is_final: bool,
    pub chunk_index: u32,
}

#[derive(uniffi::Error, thiserror::Error, Debug)]
pub enum FiqhAIError {
    #[error("Initialization error: {0}")]
    InitializationError(String),

    #[error("AI error: {0}")]
    AIError(String),

    #[error("Invalid query: {0}")]
    InvalidQuery(String),
}

// Callback trait for streaming responses (UniFFI compatible)
#[uniffi::export(callback_interface)]
pub trait StreamCallback: Send + Sync {
    fn on_chunk(
        &self,
        chunk: StreamChunk,
    );
    fn on_error(
        &self,
        error: String,
    );
    fn on_complete(
        &self,
        final_response: QueryResponse,
    );
}

impl Default for FiqhAIConfig {
    fn default() -> Self {
        Self {
            groq_api_key: std::env::var("GROQ_API_KEY").unwrap_or_default(),
            model_name: groq::DEEPSEEK_R1_DISTILL_LLAMA_70B.to_owned(),
            preferred_model: "mock".to_owned(), // Default to mock, switch to "groq" when ready
        }
    }
}

// ============================================================================
// SIMPLE GROQ AI AGENT (FOLLOWING RIG EXAMPLE PATTERN)
// ============================================================================

#[derive(uniffi::Object)]
pub struct FiqhAISystem {
    // Use Mutex for interior mutability (UniFFI exports Arc<Self>)
    agent_type: Mutex<AgentType>,
    // Shared Tokio runtime for all async operations (UniFFI best practice)
    runtime: Runtime,
}

// Simple enum to handle different agent types
#[allow(clippy::large_enum_variant)]
enum AgentType {
    Groq(Arc<rig::agent::Agent<groq::CompletionModel>>),
    Mock,
}

#[uniffi::export]
impl FiqhAISystem {
    /// Simple synchronous constructor (following sprucekit-mobile pattern)
    #[uniffi::constructor]
    pub fn new_fiqh_ai() -> Result<Self, FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: new_fiqh_ai() called!");
        log::warn!("🔥 RUST DEBUG: Creating FiqhAI system (sync constructor)");

        // Create Tokio runtime for async operations (UniFFI best practice)
        let runtime = Runtime::new()
            .map_err(|e| FiqhAIError::InitializationError(format!("Failed to create Tokio runtime: {}", e)))?;

        // Start with mock, upgrade to Groq via async method
        let agent_type = Mutex::new(AgentType::Mock);

        log::warn!("🔥 RUST DEBUG: FiqhAI system created with Mock agent (ready for upgrade)");

        Ok(Self {
            agent_type,
            runtime,
        })
    }

    /// Async method to upgrade to Groq (following sprucekit-mobile pattern)
    pub async fn initialize_groq_agent(&self) -> Result<(), FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: initialize_groq_agent() called!");

        // Use the shared runtime to spawn async task (UniFFI best practice)
        let handle = self.runtime.spawn(async move {
            log::warn!("📡 Upgrading to Groq agent (async method)...");

            match Self::create_groq_agent().await {
                Ok(agent) => {
                    log::warn!("✅ Groq agent created successfully!");
                    Ok(Arc::new(agent))
                },
                Err(e) => {
                    log::warn!("⚠️ Failed to create Groq agent: {e}, keeping mock");
                    Err(FiqhAIError::InitializationError(e.to_string()))
                },
            }
        });

        // Await the spawned task
        match handle.await {
            Ok(Ok(agent)) => {
                // Use interior mutability to update agent
                let mut agent_type = self.agent_type.lock().unwrap();
                *agent_type = AgentType::Groq(agent);
                Ok(())
            },
            Ok(Err(e)) => Err(e),
            Err(join_error) => {
                log::error!("❌ Task join error: {}", join_error);
                Err(FiqhAIError::InitializationError(format!("Task execution failed: {}", join_error)))
            },
        }
    }

    /// Analyze if a cryptocurrency is halal or haram
    pub async fn analyze_token(
        &self,
        token: String,
    ) -> Result<QueryResponse, FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: analyze_token({}) called!", token);

        // Extract agent Arc or identify as Mock, outside the spawn
        let (groq_agent, is_groq) = {
            let agent_guard = self.agent_type.lock().unwrap();
            match &*agent_guard {
                AgentType::Groq(agent) => (Some(agent.clone()), true),
                AgentType::Mock => (None, false),
            }
        }; // Guard is dropped here

        // Use the shared runtime to spawn async task (UniFFI best practice)
        let handle = self.runtime.spawn(async move {
            log::info!("🔍 Analyzing token: {token}");

            let response = match groq_agent {
                Some(agent) => {
                    log::info!("🤖 Using Groq AI for analysis...");

                    let prompt = format!(
                        "Analyze the cryptocurrency '{token}' from an Islamic finance perspective. 
                         Consider factors like: speculation, utility, volatility, underlying technology.
                         Provide a clear halal/haram ruling with reasoning."
                    );

                    // Use direct agent prompting (following Rig examples)
                    match agent.prompt(&prompt).await {
                        Ok(response) => response,
                        Err(e) => {
                            log::error!("❌ Groq API error: {e}");
                            return Err(FiqhAIError::AIError(e.to_string()));
                        },
                    }
                },
                None => {
                    log::info!("🎭 Using Mock agent for analysis...");
                    format!(
                        "**{token} Analysis**\n\n🔴 **Ruling: Haram (Prohibited)**\n\n**Reasoning:** Excessive \
                         volatility and speculation make {token} problematic under Islamic finance principles. The \
                         lack of intrinsic value and speculative nature conflict with Sharia guidelines on risk and \
                         uncertainty (gharar).\n\n**Recommendation:** Consult with qualified Islamic scholars for \
                         personalized guidance."
                    )
                },
            };

            Ok(QueryResponse {
                query_id: format!("token_{}_{}", token, chrono::Utc::now().timestamp()),
                response,
                confidence: if is_groq {
                    0.9
                } else {
                    0.8
                },
                sources: vec![
                    "Islamic Finance Analysis".to_owned(),
                    if is_groq {
                        "Groq AI".to_owned()
                    } else {
                        "Mock Response".to_owned()
                    },
                ],
                timestamp: chrono::Utc::now().timestamp() as u64,
            })
        });

        // Await the spawned task and handle join errors
        match handle.await {
            Ok(result) => result,
            Err(join_error) => {
                log::error!("❌ Task join error: {}", join_error);
                Err(FiqhAIError::InitializationError(format!("Task execution failed: {}", join_error)))
            },
        }
    }

    /// Handle general queries about Islamic finance
    pub async fn query(
        &self,
        question: String,
    ) -> Result<QueryResponse, FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: query({}) called!", question.chars().take(30).collect::<String>());

        // Extract agent Arc or identify as Mock, outside the spawn
        let (groq_agent, is_groq) = {
            let agent_guard = self.agent_type.lock().unwrap();
            match &*agent_guard {
                AgentType::Groq(agent) => (Some(agent.clone()), true),
                AgentType::Mock => (None, false),
            }
        }; // Guard is dropped here

        // Use the shared runtime to spawn async task (UniFFI best practice)
        let handle = self.runtime.spawn(async move {
            log::info!("🤔 Processing general query: {}", question.chars().take(50).collect::<String>());

            let response = match groq_agent {
                Some(agent) => {
                    log::info!("🤖 Using Groq AI for query...");

                    let prompt = format!(
                        "From an Islamic finance and Fiqh perspective, please answer: {}
                         Provide clear guidance based on Sharia principles and recommend consulting scholars when \
                         appropriate.",
                        question
                    );

                    // Use direct agent prompting (following Rig examples)
                    match agent.prompt(&prompt).await {
                        Ok(response) => response,
                        Err(e) => {
                            log::error!("❌ Groq API error: {e}");
                            return Err(FiqhAIError::AIError(e.to_string()));
                        },
                    }
                },
                None => {
                    log::info!("🎭 Using Mock agent for query...");
                    "**Mock Response**\n\nThis is a simulated response for testing purposes. In the real \
                     implementation, this would provide Islamic finance guidance based on your question. Please \
                     consult qualified Islamic scholars for actual religious guidance."
                        .to_owned()
                },
            };

            Ok(QueryResponse {
                query_id: format!("query_{}", chrono::Utc::now().timestamp()),
                response,
                confidence: if is_groq {
                    0.9
                } else {
                    0.7
                },
                sources: vec![
                    "Islamic Finance Knowledge".to_owned(),
                    if is_groq {
                        "Groq AI".to_owned()
                    } else {
                        "Mock Response".to_owned()
                    },
                ],
                timestamp: chrono::Utc::now().timestamp() as u64,
            })
        });

        // Await the spawned task and handle join errors
        match handle.await {
            Ok(result) => result,
            Err(join_error) => {
                log::error!("❌ Task join error: {}", join_error);
                Err(FiqhAIError::InitializationError(format!("Task execution failed: {}", join_error)))
            },
        }
    }

    /// Analyze if a cryptocurrency is halal or haram (with streaming)
    pub async fn analyze_token_stream(
        &self,
        token: String,
        callback: Box<dyn StreamCallback>,
    ) -> Result<(), FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: analyze_token_stream({token}) called!");

        let query_id = format!("token_{token}_{}", chrono::Utc::now().timestamp());

        // Extract agent Arc or identify as Mock, outside the spawn
        let (groq_agent, _is_groq) = {
            let agent_guard = self.agent_type.lock().unwrap();
            match &*agent_guard {
                AgentType::Groq(agent) => (Some(agent.clone()), true),
                AgentType::Mock => (None, false),
            }
        }; // Guard is dropped here

        // Use the shared runtime to spawn async task (UniFFI best practice)
        // All callback usage must happen inside this task
        self.runtime
            .spawn(async move {
                log::info!("🔍 Analyzing token with streaming: {token}");

                let result = match groq_agent {
                    Some(agent) => {
                        log::info!("🤖 Using Groq AI for streaming analysis...");

                        let prompt = format!(
                            "Analyze the cryptocurrency '{token}' from an Islamic finance perspective. 
                         Consider factors like: speculation, utility, volatility, underlying technology.
                         Provide a clear halal/haram ruling with reasoning."
                        );

                        // Use regular completion with simulated streaming
                        match agent.prompt(&prompt).await {
                            Ok(response) => {
                                log::info!("✅ Got full response, simulating streaming...");
                                // Clean and format the response for better readability
                                let full_response = format_ai_response(&response);

                                // Simulate streaming by breaking response into words
                                let words: Vec<&str> = full_response.split_whitespace().collect();
                                let mut accumulated = String::new();

                                for (i, word) in words.iter().enumerate() {
                                    if i > 0 {
                                        accumulated.push(' ');
                                    }
                                    accumulated.push_str(word);

                                    // Send chunk to callback
                                    callback.on_chunk(StreamChunk {
                                        query_id: query_id.clone(),
                                        content: format!("{} ", word),
                                        is_final: false,
                                        chunk_index: i as u32,
                                    });

                                    // Small delay to simulate streaming
                                    tokio::time::sleep(tokio::time::Duration::from_millis(30)).await;
                                }

                                // Send final response
                                let final_response = QueryResponse {
                                    query_id: query_id.clone(),
                                    response: accumulated,
                                    confidence: 0.9,
                                    sources: vec![
                                        "Islamic Finance Analysis".to_owned(),
                                        "Groq AI (Streaming)".to_owned(),
                                    ],
                                    timestamp: chrono::Utc::now().timestamp() as u64,
                                };

                                callback.on_complete(final_response);
                                Ok(())
                            },
                            Err(e) => {
                                log::error!("❌ Groq API error: {e}");
                                callback.on_error(format!("Groq API error: {e}"));
                                Err(FiqhAIError::AIError(e.to_string()))
                            },
                        }
                    },
                    None => {
                        log::info!("🎭 Using Mock agent for streaming analysis...");

                        // Simulate streaming for mock response
                        let mock_response = format!(
                            "**{token} Analysis**\n\n🔴 **Ruling: Haram (Prohibited)**\n\n**Reasoning:** Excessive \
                             volatility and speculation make {token} problematic under Islamic finance principles. \
                             The lack of intrinsic value and speculative nature conflict with Sharia guidelines on \
                             risk and uncertainty (gharar).\n\n**Recommendation:** Consult with qualified Islamic \
                             scholars for personalized guidance."
                        );

                        // Simulate streaming by sending chunks with delays
                        let words: Vec<&str> = mock_response.split_whitespace().collect();
                        let mut accumulated = String::new();

                        for (i, word) in words.iter().enumerate() {
                            if i > 0 {
                                accumulated.push(' ');
                            }
                            accumulated.push_str(word);

                            callback.on_chunk(StreamChunk {
                                query_id: query_id.clone(),
                                content: format!("{} ", word),
                                is_final: false,
                                chunk_index: i as u32,
                            });

                            // Small delay to simulate streaming
                            tokio::time::sleep(tokio::time::Duration::from_millis(50)).await;
                        }

                        // Send final response
                        let final_response = QueryResponse {
                            query_id: query_id.clone(),
                            response: accumulated,
                            confidence: 0.8,
                            sources: vec![
                                "Islamic Finance Analysis".to_owned(),
                                "Mock Response (Streaming)".to_owned(),
                            ],
                            timestamp: chrono::Utc::now().timestamp() as u64,
                        };

                        callback.on_complete(final_response);
                        Ok(())
                    },
                };

                // Handle any errors from the task execution
                if let Err(e) = result {
                    log::error!("❌ Task execution error: {e}");
                }
            })
            .await
            .map_err(|join_error| {
                log::error!("❌ Task join error: {join_error}");
                FiqhAIError::InitializationError(format!("Task execution failed: {join_error}"))
            })?;

        Ok(())
    }

    /// Handle general queries about Islamic finance (with streaming)
    pub async fn query_stream(
        &self,
        question: String,
        callback: Box<dyn StreamCallback>,
    ) -> Result<(), FiqhAIError> {
        log::warn!("🔥 RUST DEBUG: query_stream({}) called!", question.chars().take(30).collect::<String>());

        let query_id = format!("query_{}", chrono::Utc::now().timestamp());

        // Extract agent Arc or identify as Mock, outside the spawn
        let (groq_agent, _is_groq) = {
            let agent_guard = self.agent_type.lock().unwrap();
            match &*agent_guard {
                AgentType::Groq(agent) => (Some(agent.clone()), true),
                AgentType::Mock => (None, false),
            }
        }; // Guard is dropped here

        // Use the shared runtime to spawn async task (UniFFI best practice)
        // All callback usage must happen inside this task
        self.runtime
            .spawn(async move {
                log::info!("🤔 Processing query with streaming: {}", question.chars().take(50).collect::<String>());

                let result = match groq_agent {
                    Some(agent) => {
                        log::info!("🤖 Using Groq AI for streaming query...");

                        let prompt = format!(
                            "From an Islamic finance and Fiqh perspective, please answer: {question}
                         Provide clear guidance based on Sharia principles and recommend consulting scholars when \
                             appropriate."
                        );

                        // Use regular completion with simulated streaming
                        match agent.prompt(&prompt).await {
                            Ok(response) => {
                                log::info!("✅ Got full response, simulating streaming...");
                                // Clean and format the response for better readability
                                let full_response = format_ai_response(&response);

                                // Simulate streaming by breaking response into words
                                let words: Vec<&str> = full_response.split_whitespace().collect();
                                let mut accumulated = String::new();

                                for (i, word) in words.iter().enumerate() {
                                    if i > 0 {
                                        accumulated.push(' ');
                                    }
                                    accumulated.push_str(word);

                                    // Send chunk to callback
                                    callback.on_chunk(StreamChunk {
                                        query_id: query_id.clone(),
                                        content: format!("{} ", word),
                                        is_final: false,
                                        chunk_index: i as u32,
                                    });

                                    // Small delay to simulate streaming
                                    tokio::time::sleep(tokio::time::Duration::from_millis(30)).await;
                                }

                                // Send final response
                                let final_response = QueryResponse {
                                    query_id: query_id.clone(),
                                    response: accumulated,
                                    confidence: 0.9,
                                    sources: vec![
                                        "Islamic Finance Knowledge".to_owned(),
                                        "Groq AI (Streaming)".to_owned(),
                                    ],
                                    timestamp: chrono::Utc::now().timestamp() as u64,
                                };

                                callback.on_complete(final_response);
                                Ok(())
                            },
                            Err(e) => {
                                log::error!("❌ Groq API error: {e}");
                                callback.on_error(format!("Groq API error: {e}"));
                                Err(FiqhAIError::AIError(e.to_string()))
                            },
                        }
                    },
                    None => {
                        log::info!("🎭 Using Mock agent for streaming query...");

                        // Mock streaming response
                        let mock_response = "**Mock Response**\n\nThis is a simulated streaming response for testing \
                                             purposes. In the real implementation, this would provide Islamic finance \
                                             guidance based on your question. Please consult qualified Islamic \
                                             scholars for actual religious guidance.";

                        // Simulate streaming by sending chunks with delays
                        let words: Vec<&str> = mock_response.split_whitespace().collect();
                        let mut accumulated = String::new();

                        for (i, word) in words.iter().enumerate() {
                            if i > 0 {
                                accumulated.push(' ');
                            }
                            accumulated.push_str(word);

                            callback.on_chunk(StreamChunk {
                                query_id: query_id.clone(),
                                content: format!("{} ", word),
                                is_final: false,
                                chunk_index: i as u32,
                            });

                            // Small delay to simulate streaming
                            tokio::time::sleep(tokio::time::Duration::from_millis(50)).await;
                        }

                        // Send final response
                        let final_response = QueryResponse {
                            query_id: query_id.clone(),
                            response: accumulated,
                            confidence: 0.7,
                            sources: vec![
                                "Islamic Finance Knowledge".to_owned(),
                                "Mock Response (Streaming)".to_owned(),
                            ],
                            timestamp: chrono::Utc::now().timestamp() as u64,
                        };

                        callback.on_complete(final_response);
                        Ok(())
                    },
                };

                // Handle any errors from the task execution
                if let Err(e) = result {
                    log::error!("❌ Task execution error: {e}");
                }
            })
            .await
            .map_err(|join_error| {
                log::error!("❌ Task join error: {join_error}");
                FiqhAIError::InitializationError(format!("Task execution failed: {join_error}"))
            })?;

        Ok(())
    }

    /// Check what type of agent is being used
    pub fn get_agent_info(&self) -> String {
        log::warn!("🔥 RUST DEBUG: get_agent_info() called!");

        let info = match &*self.agent_type.lock().unwrap() {
            AgentType::Groq(_) => "Groq AI".to_owned(),
            AgentType::Mock => "Mock Agent".to_owned(),
        };

        log::warn!("🔥 RUST DEBUG: Returning agent info: {info}");
        info
    }

    /// Check if real AI is active
    pub fn is_using_real_ai(&self) -> bool {
        log::warn!("🔥 RUST DEBUG: is_using_real_ai() called!");

        let is_real = matches!(*self.agent_type.lock().unwrap(), AgentType::Groq(_));

        log::warn!("🔥 RUST DEBUG: Returning is_real: {is_real}");
        is_real
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

impl FiqhAISystem {
    /// Create Groq agent following Rig example pattern
    async fn create_groq_agent()
    -> Result<rig::agent::Agent<groq::CompletionModel>, Box<dyn std::error::Error + Send + Sync>> {
        // Following the Rig example pattern
        let client = groq::Client::new("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");

        let agent = client
            .agent(groq::DEEPSEEK_R1_DISTILL_LLAMA_70B)
            .preamble(
                "You are an expert Islamic scholar specializing in Islamic finance and Fiqh. 
                      Provide clear, balanced analysis based on Sharia principles. 
                      Always include confidence levels and recommend consulting qualified scholars for important \
                 decisions.",
            )
            .build();

        Ok(agent)
    }
}

/// Format AI response with proper paragraphs and structure for better readability
fn format_ai_response(raw_response: &str) -> String {
    // Remove thinking tags and clean up
    let cleaned = raw_response.replace("<think>", "").replace("</think>", "").trim().to_string();

    // Add paragraph breaks for better readability
    let formatted = cleaned
        .replace(". In", ".\n\nIn")
        .replace(". Islamic", ".\n\nIslamic")
        .replace(". From", ".\n\nFrom")
        .replace(". However", ".\n\nHowever")
        .replace(". Therefore", ".\n\nTherefore")
        .replace(". Overall", ".\n\nOverall")
        .replace(". Key", ".\n\n• Key")
        .replace(". Important", ".\n\n• Important")
        .replace("1. ", "\n\n1. ")
        .replace("2. ", "\n2. ")
        .replace("3. ", "\n3. ")
        .replace("4. ", "\n4. ")
        .replace("5. ", "\n5. ");

    // Clean up multiple newlines
    let final_text = formatted
        .split('\n')
        .map(|line| line.trim())
        .filter(|line| !line.is_empty())
        .collect::<Vec<&str>>()
        .join("\n\n");

    final_text
}

// ============================================================================
// MOBILE SUPPORT COMPONENTS
// ============================================================================

#[derive(uniffi::Object)]
pub struct AudioProcessor;

impl Default for AudioProcessor {
    fn default() -> Self {
        Self::new()
    }
}

#[uniffi::export]
impl AudioProcessor {
    #[uniffi::constructor]
    pub fn new() -> Self {
        Self
    }

    pub async fn transcribe_audio(
        &self,
        audio_data: Vec<u8>,
        _language: Option<String>,
    ) -> Result<String, FiqhAIError> {
        if audio_data.is_empty() {
            return Err(FiqhAIError::InvalidQuery("Audio data cannot be empty".to_owned()));
        }

        // Mock STT based on audio data size
        let transcription = match audio_data.len() {
            0..=1000 => "Is Bitcoin halal?",
            1001..=5000 => "What is the Islamic ruling on Ethereum?",
            _ => "Please analyze this cryptocurrency from Sharia perspective",
        };

        Ok(transcription.to_owned())
    }
}

#[derive(uniffi::Object)]
pub struct SolanaConnector {
    rpc_url: String,
}

#[uniffi::export]
impl SolanaConnector {
    #[uniffi::constructor]
    pub fn new(rpc_url: String) -> Self {
        Self {
            rpc_url,
        }
    }

    pub async fn is_connected(&self) -> Result<bool, FiqhAIError> {
        Ok(true) // Mock connection
    }

    pub fn get_network_name(&self) -> String {
        if self.rpc_url.contains("devnet") {
            "Solana Devnet".to_owned()
        } else {
            "Solana Mainnet".to_owned()
        }
    }
}

#[derive(uniffi::Object)]
pub struct ChatbotSession {
    user_id: String,
    session_id: String,
}

#[uniffi::export]
impl ChatbotSession {
    #[uniffi::constructor]
    pub fn new(
        user_id: String,
        _language: String,
    ) -> Self {
        Self {
            user_id,
            session_id: uuid::Uuid::new_v4().to_string(),
        }
    }

    pub fn start_session(&self) -> String {
        self.session_id.clone()
    }

    pub async fn send_message(
        &self,
        message: String,
        _context: Option<String>,
    ) -> Result<QueryResponse, FiqhAIError> {
        let response_text =
            format!("Thank you for your question: '{message}'. I will analyze this based on Islamic principles.");

        Ok(QueryResponse {
            query_id: format!("chat_{}_{}", self.user_id.clone(), chrono::Utc::now().timestamp()),
            response: response_text,
            confidence: 0.8,
            sources: vec!["Islamic Chat Session".to_owned()],
            timestamp: chrono::Utc::now().timestamp() as u64,
        })
    }

    pub fn is_active(&self) -> bool {
        true
    }

    pub fn get_session_start_time(&self) -> u64 {
        chrono::Utc::now().timestamp() as u64
    }
}
