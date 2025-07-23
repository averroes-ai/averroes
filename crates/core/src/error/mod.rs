pub mod api;
pub mod config;
pub mod core;

// Re-export anyhow for convenient error handling
pub use anyhow::Context;
pub use anyhow::Error;
pub use anyhow::Result;
pub use anyhow::anyhow;
// Re-export our error types
pub use api::{ApiError, ApiResult, ErrorResponse};

pub use self::core::AIError;
pub use self::core::ActorSystemError;
pub use self::core::FiqhAIError;
pub use self::core::VectorDatabaseError;

// For consistent error handling with location info
#[macro_export]
macro_rules! err_with_loc {
    ($err:expr) => {
        anyhow::anyhow!($err).context(format!("at {}:{}", file!(), line!()))
    };
}
