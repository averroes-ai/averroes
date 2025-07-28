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

#[derive(uniffi::Error, thiserror::Error, Debug)]
pub enum FiqhAIError {
    #[error("Initialization error: {0}")]
    InitializationError(String),

    #[error("AI error: {0}")]
    AIError(String),

    #[error("Invalid query: {0}")]
    InvalidQuery(String),
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
