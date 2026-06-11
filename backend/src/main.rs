use std::collections::HashMap;
use axum::{
    routing::{get, post, put, delete},
    Json, Router,
    extract::{State, Path, Query},
    response::{IntoResponse, Response},
    body::Body,
};
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use tracing::{info, error, warn, debug};
use sqlx::sqlite::{SqlitePoolOptions};
use sqlx::{SqlitePool, Row};
use tower_http::cors::CorsLayer;
use regex::Regex;
use walkdir::WalkDir;
use std::time::Instant;
use std::sync::Arc;
use tokio::sync::Mutex;
use chrono;
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message};
use futures_util::{StreamExt, SinkExt};
use notify::{Watcher, RecursiveMode, Config};
use std::path::Path as StdPath;
use http::{StatusCode, header};
use tokio::io::AsyncReadExt;
use rust_embed::RustEmbed;
use std::fs::File as StdFile;
use std::io::Write;

#[derive(Serialize, Deserialize, Clone, Debug)]
struct DiscordActivity {
    name: String,
    #[serde(rename = "type")]
    activity_type: u8,
    details: Option<String>,
    state: Option<String>,
    assets: Option<DiscordAssets>,
    timestamps: Option<DiscordTimestamps>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
struct DiscordAssets {
    large_image: Option<String>,
    large_text: Option<String>,
    small_image: Option<String>,
    small_text: Option<String>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
struct DiscordTimestamps {
    start: Option<u64>,
    end: Option<u64>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
struct DiscordPresence {
    status: String,
    since: Option<u64>,
    activities: Vec<DiscordActivity>,
    afk: bool,
}

struct DiscordRpcSession {
    token: String,
    presence_tx: tokio::sync::mpsc::UnboundedSender<DiscordPresence>,
}

struct RpcManager {
    sessions: std::collections::HashMap<String, DiscordRpcSession>,
}

impl RpcManager {
    fn new() -> Self {
        Self { sessions: std::collections::HashMap::new() }
    }
}

const TMDB_API_KEY: &str = "fb7bb23f03b6994dafc674c074d01761";
const IMDB_API_KEY: &str = "4b447405";
const VERSION: &str = "v0.2.0";
const GITHUB_REPO: &str = "sudoloser/sunset";

fn platform_asset_name() -> Option<String> {
    let arch = std::env::consts::ARCH;
    let os = std::env::consts::OS;
    let os = if os == "android" { "linux" } else { os };
    match (arch, os) {
        ("aarch64", "linux") => Some("sunset-server-aarch64-unknown-linux-gnu".to_string()),
        ("x86_64", "linux") => Some("sunset-server-x86_64-unknown-linux-gnu".to_string()),
        ("x86_64", "windows") => Some("sunset-server-x86_64-pc-windows-msvc".to_string()),
        _ => None,
    }
}

fn check_and_update() {
    let asset_name = match platform_asset_name() {
        Some(n) => n,
        None => { eprintln!("[Updater] Unsupported platform: {}-{}", std::env::consts::ARCH, std::env::consts::OS); return; }
    };

    println!("\x1b[1;36m[SunSet Updater]\x1b[0m Checking for updates...");
    println!("  Current version: \x1b[1;33m{}\x1b[0m", VERSION);
    println!("  Repository: \x1b[1;34m{}\x1b[0m", GITHUB_REPO);

    let client = reqwest::blocking::Client::builder()
        .user_agent("SunSet-Updater/1.0")
        .timeout(std::time::Duration::from_secs(15))
        .build().unwrap();

    let release_url = format!("https://api.github.com/repos/{}/releases/latest", GITHUB_REPO);
    let resp = match client.get(&release_url).send() {
        Ok(r) => r,
        Err(e) => { eprintln!("[Updater] Failed to check for updates: {}", e); return; }
    };

    let json: serde_json::Value = match resp.json() {
        Ok(j) => j,
        Err(e) => { eprintln!("[Updater] Failed to parse release info: {}", e); return; }
    };

    let remote_tag = match json["tag_name"].as_str() {
        Some(t) => t.to_string(),
        None => { eprintln!("[Updater] Could not determine latest version."); return; }
    };

    println!("  Remote version: \x1b[1;33m{}\x1b[0m", remote_tag);

    if remote_tag == VERSION {
        println!("\x1b[1;32m[SunSet Updater] You're up to date!\x1b[0m");
        return;
    }

    println!();
    println!("\x1b[1;33mUpdate available: {} > {}\x1b[0m", VERSION, remote_tag);
    print!("Update now? [\x1b[1;32my\x1b[0m/\x1b[1;31mN\x1b[0m] ");
    std::io::stdout().flush().unwrap();

    let mut input = String::new();
    if std::io::stdin().read_line(&mut input).is_err() { return; }
    if input.trim().to_lowercase() != "y" {
        println!("[Updater] Update skipped.");
        return;
    }

    println!("[Updater] Downloading {}...", asset_name);

    let assets = json["assets"].as_array().unwrap();
    let asset = match assets.iter().find(|a| a["name"].as_str().unwrap_or("").contains(&asset_name)) {
        Some(a) => a,
        None => { eprintln!("[Updater] Could not find matching asset for platform."); return; }
    };

    let download_url = asset["browser_download_url"].as_str().unwrap();
    let home = dirs::home_dir().unwrap();
    let sunset_dir = home.join(".sunset");
    let tmp_dir = sunset_dir.join("tmp");
    let bin_dir = sunset_dir.join("bin");
    std::fs::create_dir_all(&tmp_dir).ok();
    std::fs::create_dir_all(&bin_dir).ok();

    let zip_path = tmp_dir.join("update.zip");
    let mut zip_file = match StdFile::create(&zip_path) {
        Ok(f) => f,
        Err(e) => { eprintln!("[Updater] Failed to create temp file: {}", e); return; }
    };

    let dl_resp = match client.get(download_url).send() {
        Ok(r) => r,
        Err(e) => { eprintln!("[Updater] Download failed: {}", e); return; }
    };

    let bytes = match dl_resp.bytes() {
        Ok(b) => b.to_vec(),
        Err(e) => { eprintln!("[Updater] Download failed: {}", e); return; }
    };

    if zip_file.write_all(&bytes).is_err() {
        eprintln!("[Updater] Failed to write download.");
        return;
    }
    drop(zip_file);

    println!("[Updater] Extracting...");
    let zip_file = match StdFile::open(&zip_path) {
        Ok(f) => f,
        Err(e) => { eprintln!("[Updater] Failed to open zip: {}", e); return; }
    };
    let mut archive = match zip::ZipArchive::new(zip_file) {
        Ok(a) => a,
        Err(e) => { eprintln!("[Updater] Invalid zip: {}", e); return; }
    };

    let extracted_dir = tmp_dir.join("extracted");
    std::fs::create_dir_all(&extracted_dir).ok();

    for i in 0..archive.len() {
        let mut file = archive.by_index(i).unwrap();
        let out_path = extracted_dir.join(file.name());
        if file.is_dir() {
            std::fs::create_dir_all(&out_path).ok();
        } else {
            if let Some(parent) = out_path.parent() {
                std::fs::create_dir_all(parent).ok();
            }
            let mut outfile = StdFile::create(&out_path).unwrap();
            std::io::copy(&mut file, &mut outfile).unwrap();
        }
    }

    // Find the binary in the extracted files
    let binary_name = if cfg!(windows) { format!("{}.exe", asset_name) } else { asset_name.clone() };
    let binary_in_zip = extracted_dir.join(&binary_name);

    // If not found at root, search subdirectories for any sunset-server binary
    let binary_path = if binary_in_zip.exists() {
        binary_in_zip
    } else {
        let mut found = None;
        for entry in walkdir::WalkDir::new(&extracted_dir).into_iter().filter_map(|e| e.ok()) {
            if entry.file_type().is_file() {
                let fname = entry.file_name().to_str().unwrap_or("");
                if fname.starts_with("sunset-server") {
                    found = Some(entry.path().to_path_buf());
                    break;
                }
            }
        }
        match found {
            Some(p) => p,
            None => { eprintln!("[Updater] Binary not found in archive."); return; }
        }
    };

    let install_target = bin_dir.join(binary_path.file_name().unwrap_or(std::ffi::OsStr::new(&binary_name)));
    println!("[Updater] Installing to {}", install_target.display());

    // Remove existing binary first (handles replacement cleanly)
    if install_target.exists() {
        #[cfg(unix)]
        {
            // On Unix, rename the old binary aside so the new one can take its place atomically
            let backup = install_target.with_extension("old");
            let _ = std::fs::rename(&install_target, &backup);
        }
        #[cfg(windows)]
        {
            // On Windows, if the binary is running we can't overwrite it.
            // Check if current exe is the target
            if let Ok(exe) = std::env::current_exe() {
                if exe == install_target {
                    eprintln!("[Updater] Cannot overwrite running binary on Windows. Restart from the new location manually.");
                    eprintln!("[Updater] Update saved to: {}", binary_path.display());
                    return;
                }
            }
            let _ = std::fs::remove_file(&install_target);
        }
    }

    if std::fs::copy(&binary_path, &install_target).is_err() {
        eprintln!("[Updater] Failed to install update.");
        return;
    }

    // On Unix, clean up backup
    #[cfg(unix)]
    {
        let backup = install_target.with_extension("old");
        if backup.exists() {
            let _ = std::fs::remove_file(&backup);
        }
    }

    // Make executable on Unix
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        std::fs::set_permissions(&install_target, std::fs::Permissions::from_mode(0o755)).ok();
    }

    println!("\x1b[1;32m[SunSet Updater] Update installed successfully!\x1b[0m");
    println!("[SunSet Updater] Restarting server...");

    // Cleanup tmp
    std::fs::remove_dir_all(&tmp_dir).ok();

    // Restart: exec the new binary
    #[cfg(unix)]
    {
        use std::os::unix::process::CommandExt;
        let err = std::process::Command::new(&install_target)
            .args(std::env::args().skip(1))
            .exec();
        eprintln!("[Updater] Restart failed: {}", err);
    }

    #[cfg(windows)]
    {
        // On Windows, spawn a detached process then exit
        let mut child = std::process::Command::new(&install_target)
            .args(std::env::args().skip(1))
            .spawn()
            .ok();
        std::process::exit(0);
    }
}

#[derive(RustEmbed)]
#[folder = "dist/"]
struct Asset;

#[derive(Serialize, Deserialize)]
struct LoginResponse {
    user_id: String,
    username: String,
    is_admin: bool,
    discord_token: Option<String>,
    discord_status: Option<String>,
    profile_picture: Option<String>,
}

#[derive(Deserialize)]
struct CreateUserPayload {
    username: String,
    password_hash: String,
    is_admin: bool,
}

#[derive(Deserialize)]
struct ChangePasswordPayload {
    current_password: String,
    new_password: String,
}

#[derive(Deserialize)]
struct ChangeUsernamePayload {
    new_username: String,
}

#[derive(Deserialize)]
struct ProfilePicturePayload {
    image: String, // base64
}

async fn static_handler(uri: axum::http::Uri) -> Response {
    let path = uri.path().trim_start_matches('/');

    if path.is_empty() || path == "index.html" {
        return index_handler().await;
    }

    match Asset::get(path) {
        Some(content) => {
            let mime = mime_guess::from_path(path).first_or_octet_stream();
            (
                [(header::CONTENT_TYPE, mime.as_ref())],
                content.data,
            ).into_response()
        }
        None => {
            // Fallback to index.html for SPA routing
            index_handler().await
        }
    }
}

async fn index_handler() -> Response {
    match Asset::get("index.html") {
        Some(content) => (
            [(header::CONTENT_TYPE, "text/html")],
            content.data,
        ).into_response(),
        None => StatusCode::NOT_FOUND.into_response(),
    }
}

#[derive(Serialize, Deserialize)]
struct SetupStatus {
    setup_complete: bool,
    server_name: Option<String>,
}

#[derive(Serialize, Deserialize, Clone)]
struct LibraryConfig {
    name: String,
    path: String,
    lib_type: String,
}

#[derive(Serialize, Deserialize, Clone, sqlx::FromRow)]
struct Library {
    id: String,
    name: String,
    path: String,
    lib_type: String,
}

#[derive(Serialize, Deserialize)]
struct UserConfig {
    username: String,
    password_hash: String,
}

#[derive(Serialize, Deserialize)]
struct OnboardRequest {
    server_name: String,
    admin_user: UserConfig,
    libraries: Vec<LibraryConfig>,
}

#[derive(Serialize, Deserialize)]
struct LoginRequest {
    username: String,
    password_hash: String,
}

#[derive(Serialize, Deserialize, sqlx::FromRow, Clone)]
struct MediaItem {
    id: String,
    title: String,
    show_title: Option<String>,
    collection_name: Option<String>,
    media_type: String,
    year: Option<i32>,
    season: Option<i32>,
    episode: Option<i32>,
    added_at: Option<chrono::NaiveDateTime>,
    file_path: String,
    description: Option<String>,
    cast: Option<String>,
    genres: Option<String>,
    rating: Option<f64>,
    tmdb_id: Option<String>,
    poster_path: Option<String>,
    backdrop_path: Option<String>,
}

#[derive(Serialize, Deserialize, sqlx::FromRow)]
struct PlaybackState {
    id: String,
    item_id: String,
    user_id: String,
    timestamp: f64,
    duration: Option<f64>,
    updated_at: Option<chrono::NaiveDateTime>,
}

#[derive(Serialize, Deserialize)]
struct StorageInfo {
    total_size: u64,
    item_count: i64,
    library_count: i64,
    user_count: i64,
}

async fn run_discord_rpc(token: String, mut rx: tokio::sync::mpsc::UnboundedReceiver<DiscordPresence>) {
    let url = "wss://gateway.discord.gg/?v=10&encoding=json";
    
    loop {
        info!("Connecting to Discord Gateway...");
        let ws_stream = tokio::select! {
            res = connect_async(url) => {
                match res {
                    Ok((v, _)) => v,
                    Err(e) => {
                        error!("Failed to connect to Discord Gateway: {}", e);
                        tokio::select! {
                            _ = tokio::time::sleep(std::time::Duration::from_secs(5)) => {
                                continue;
                            }
                            _ = rx.recv() => {
                                if rx.is_closed() { return; }
                                continue;
                            }
                        }
                    }
                }
            }
            _ = rx.recv() => {
                if rx.is_closed() { return; }
                continue;
            }
        };

        let (mut write, mut read) = ws_stream.split();
        let mut heartbeat_interval = 41250;
        let mut sequence: Option<u64> = None;

        // Discord HELLO
        tokio::select! {
            msg = read.next() => {
                if let Some(Ok(Message::Text(msg))) = msg {
                    if let Ok(val) = serde_json::from_str::<serde_json::Value>(&msg) {
                        heartbeat_interval = val["d"]["heartbeat_interval"].as_u64().unwrap_or(41250);
                        info!("Discord Gateway HELLO received, heartbeat interval: {}ms", heartbeat_interval);
                    } else {
                        warn!("Discord Gateway: failed to parse HELLO: {}", msg);
                    }
                } else {
                    warn!("Discord Gateway: no HELLO received");
                    continue;
                }
            }
            _ = rx.recv() => {
                if rx.is_closed() { return; }
                continue;
            }
        }

        // Identify
        let identify = serde_json::json!({
            "op": 2,
            "d": {
                "token": token.clone(),
                "properties": {
                    "os": "linux",
                    "browser": "SunSet Media Server",
                    "device": "SunSet"
                },
                "presence": {
                    "status": "online",
                    "afk": false,
                    "since": 0,
                    "activities": []
                },
                "intents": 0,
                "capabilities": 65
            }
        });
        if let Err(e) = write.send(Message::Text(identify.to_string())).await {
            error!("Failed to send identify to Discord: {}", e);
            continue;
        }

        let mut heartbeat_timer = tokio::time::interval(std::time::Duration::from_millis(heartbeat_interval));
        
        loop {
            tokio::select! {
                _ = heartbeat_timer.tick() => {
                    let hb = serde_json::json!({"op": 1, "d": sequence});
                    if let Err(e) = write.send(Message::Text(hb.to_string())).await {
                        warn!("Discord heartbeat send failed: {}", e);
                        break;
                    }
                }
                recv = rx.recv() => {
                    match recv {
                        Some(presence) => {
                            let update = serde_json::json!({
                                "op": 3,
                                "d": presence
                            });
                            if let Err(e) = write.send(Message::Text(update.to_string())).await {
                                warn!("Discord presence send failed: {}", e);
                                break;
                            }
                        }
                        None => {
                            debug!("Discord RPC channel closed, shutting down session");
                            return;
                        }
                    }
                }
                msg = read.next() => {
                    match msg {
                        Some(Ok(Message::Text(text))) => {
                            if let Ok(val) = serde_json::from_str::<serde_json::Value>(&text) {
                                let op = val["op"].as_u64().unwrap_or(999);
                                if let Some(s) = val["s"].as_u64() { sequence = Some(s); }
                                match op {
                                    0 => {
                                        let t = val["t"].as_str().unwrap_or("");
                                        if t == "READY" {
                                            info!("Discord RPC connected successfully (READY received)");
                                        }
                                    }
                                    7 => {
                                        warn!("Discord requested reconnect (op 7)");
                                        break;
                                    }
                                    9 => {
                                        warn!("Discord Invalid Session (op 9)");
                                        break;
                                    }
                                    _ => {}
                                }
                            }
                        }
                        Some(Err(e)) => {
                            warn!("Discord websocket error: {}", e);
                            break;
                        }
                        None => {
                            debug!("Discord websocket closed");
                            break;
                        }
                        _ => {}
                    }
                }
            }
        }
        
        info!("Discord RPC session ended, reconnecting in 5s...");
        tokio::select! {
            _ = tokio::time::sleep(std::time::Duration::from_secs(5)) => {}
            _ = rx.recv() => {
                if rx.is_closed() { return; }
            }
        }
    }
}

struct AppState {
    pool: SqlitePool,
    start_time: Instant,
    client: reqwest::Client,
    rpc_manager: Arc<Mutex<RpcManager>>,
}

use clap::Parser;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Enable detailed logs
    #[arg(short, long)]
    logs: bool,

    /// Print version information
    #[arg(short, long)]
    version: bool,

    /// Skip update check on startup
    #[arg(long)]
    no_update_check: bool,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();

    if args.version {
        println!("SunSet Server {}", VERSION);
        return;
    }

    let filter = if args.logs {
        "info"
    } else {
        "warn,sunset_backend=info"
    };

    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| tracing_subscriber::EnvFilter::new(filter))
        )
        .init();

    // ASCII Banner
    println!("\x1b[38;5;208m   _____             _____      _   \x1b[0m");
    println!("\x1b[38;5;208m  / ____|           / ____|    | |  \x1b[0m");
    println!("\x1b[38;5;214m | (___  _   _ _ __| (___   ___| |_ \x1b[0m");
    println!("\x1b[38;5;214m  \\___ \\| | | | '_ \\\\___ \\ / _ \\ __|\x1b[0m");
    println!("\x1b[38;5;220m  ____) | |_| | | | |___) |  __/ |_ \x1b[0m");
    println!("\x1b[38;5;220m |_____/ \\__,_|_| |_|_____/ \\___|\\__|\x1b[0m");
    println!();
    println!("\x1b[1;32mStarting server . . .\x1b[0m");

    // Check for updates (blocking, before server starts)
    if !args.no_update_check {
        check_and_update();
    }

    info!("Initializing storage and database...");
    let home_dir = dirs::home_dir().expect("Could not find home directory");
    let sunset_dir = home_dir.join(".sunset");
    if !sunset_dir.exists() {
        info!("Creating data directory at {:?}", sunset_dir);
        std::fs::create_dir_all(&sunset_dir).expect("Failed to create ~/.sunset directory");
    }

    let db_path = sunset_dir.join("sunset.db");
    let db_url = format!("sqlite://{}", db_path.to_str().unwrap());

    if !db_path.exists() {
        info!("Initializing new database file...");
        std::fs::File::create(&db_path).expect("Failed to create database file");
    }

    info!("Connecting to database...");
    let pool = SqlitePoolOptions::new()
        .max_connections(5)
        .connect(&db_url)
        .await
        .expect("Failed to connect to SQLite database");
    info!("Database connection established.");

    // Table Creation
    info!("Verifying database schema...");
    sqlx::query("CREATE TABLE IF NOT EXISTS settings (id INTEGER PRIMARY KEY CHECK (id = 1), server_name TEXT, setup_complete BOOLEAN DEFAULT 0)").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, is_admin BOOLEAN DEFAULT 0, discord_token TEXT, discord_status TEXT DEFAULT 'online', profile_picture TEXT)").execute(&pool).await.unwrap();
    // Migrations for existing databases
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN discord_token TEXT").execute(&pool).await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN discord_status TEXT DEFAULT 'online'").execute(&pool).await;
    let _ = sqlx::query("ALTER TABLE users ADD COLUMN profile_picture TEXT").execute(&pool).await;
    // Migrations for media_items table
    let _ = sqlx::query("ALTER TABLE media_items ADD COLUMN collection_name TEXT").execute(&pool).await;
    let _ = sqlx::query("ALTER TABLE media_items ADD COLUMN poster_path TEXT").execute(&pool).await;
    let _ = sqlx::query("ALTER TABLE media_items ADD COLUMN backdrop_path TEXT").execute(&pool).await;
    sqlx::query("CREATE TABLE IF NOT EXISTS libraries (id TEXT PRIMARY KEY, name TEXT NOT NULL, path TEXT NOT NULL, lib_type TEXT NOT NULL)").execute(&pool).await.unwrap();

    sqlx::query("CREATE TABLE IF NOT EXISTS media_items (id TEXT PRIMARY KEY, library_id TEXT NOT NULL, title TEXT NOT NULL, show_title TEXT, collection_name TEXT, file_path TEXT UNIQUE NOT NULL, media_type TEXT NOT NULL, year INTEGER, season INTEGER, episode INTEGER, added_at DATETIME DEFAULT CURRENT_TIMESTAMP, description TEXT, \"cast\" TEXT, genres TEXT, rating REAL, tmdb_id TEXT, poster_path TEXT, backdrop_path TEXT, FOREIGN KEY(library_id) REFERENCES libraries(id))").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS playback_state (id TEXT PRIMARY KEY, item_id TEXT NOT NULL, user_id TEXT DEFAULT 'default', timestamp REAL NOT NULL, duration REAL, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE(item_id, user_id))").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS invite_codes (code TEXT PRIMARY KEY, used BOOLEAN DEFAULT 0, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS temp_tokens (id TEXT PRIMARY KEY, media_id TEXT NOT NULL, created_at DATETIME DEFAULT CURRENT_TIMESTAMP, expires_at DATETIME NOT NULL)").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS user_items (user_id TEXT NOT NULL, item_id TEXT NOT NULL, added_at DATETIME DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(user_id, item_id))").execute(&pool).await.unwrap();
    info!("Database schema verified.");

    info!("Initializing application state...");
    let state = Arc::new(AppState {
        pool: pool.clone(),
        start_time: Instant::now(),
        client: reqwest::Client::new(),
        rpc_manager: Arc::new(Mutex::new(RpcManager::new())),
    });

    info!("Configuring API routes and serving embedded frontend...");
    let app = Router::new()
        .route("/api/status", get(get_status))
        .route("/api/uptime", get(get_uptime))
        .route("/api/onboard", post(onboard))
        .route("/api/login", post(login))
        .route("/api/users/:id", get(get_user_profile))
        .route("/api/users/:id/discord-config", put(update_discord_config))
        .route("/api/recently-added", get(get_recently_added))
        .route("/api/libraries", get(get_libraries).post(add_library))
        .route("/api/libraries/:id", put(update_library).delete(delete_library))
        .route("/api/libraries/:id/items", get(get_library_items))
        .route("/api/shows/:show_title/episodes", get(get_show_episodes))
        .route("/api/search", get(search_media))
        .route("/api/scan", post(manual_scan))
        .route("/api/stream/:id", get(stream_media))
        .route("/api/media/:id/asset/:name", get(get_media_asset))
        .route("/api/media/:id/subtitles", get(get_media_subtitles))
        .route("/api/media/:id/subtitle/:name", get(get_media_subtitle_file))
        .route("/api/media/:id", put(update_media_item))
        .route("/api/media/:id/refresh", post(refresh_media_item))
        .route("/api/playback", post(save_playback))
        .route("/api/playback/:item_id", get(get_playback))
        .route("/api/continue-watching/:user_id", get(get_continue_watching))
        .route("/api/user-items/:user_id", get(get_user_items).post(add_user_item))
        .route("/api/user-items/:user_id/:item_id", delete(remove_user_item))
        .route("/api/storage", get(get_storage))
        .route("/api/genres", get(get_genres))
        .route("/api/genre/:genre", get(get_genre_items))
        .route("/api/invite", post(create_invite))
        .route("/api/invite/redeem", post(redeem_invite))
        .route("/api/media/:id/token", post(generate_media_token))
        .route("/api/media/:id/download", get(download_media))
        .route("/api/media/:id/download-zip", get(download_media_zip))
        .route("/api/users", get(list_users).post(create_user))
        .route("/api/users/:id/password", put(change_password))
        .route("/api/users/:id/username", put(change_username))
        .route("/api/users/:id/profile-picture", get(get_profile_picture).post(upload_profile_picture))
        .fallback(static_handler)
        .layer(CorsLayer::permissive())
        .with_state(state.clone());

    let addr = SocketAddr::from(([0, 0, 0, 0], 7867));
    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    println!("\x1b[1;32mServer started on localhost:7867\x1b[0m");
    info!("SunSet Media Server is ready!");

    let pool_clone = pool.clone();
    let state_clone = state.clone();
    tokio::spawn(async move {
        info!("Checking setup status...");
        let row = sqlx::query("SELECT setup_complete FROM settings WHERE id = 1").fetch_optional(&pool_clone).await.unwrap();
        if let Some(r) = row {
            if r.get::<bool, _>("setup_complete") {
                info!("Setup is complete. Starting background services...");
                scan_all_libraries(state_clone.clone()).await;
                start_watchers(state_clone).await;
            } else {
                info!("Server is awaiting onboarding setup.");
            }
        } else {
            info!("Server is awaiting onboarding setup.");
        }
    });

    // Periodic cleanup of temporary downloads (every hour, delete files older than 3 hours)
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(std::time::Duration::from_secs(3600));
        loop {
            interval.tick().await;
            let home_dir = dirs::home_dir().expect("Could not find home directory");
            let dl_dir = home_dir.join(".sunset").join("tmp").join("downloads");
            if dl_dir.exists() {
                if let Ok(entries) = std::fs::read_dir(dl_dir) {
                    for entry in entries.filter_map(|e| e.ok()) {
                        if let Ok(meta) = entry.metadata() {
                            if let Ok(modified) = meta.modified() {
                                if let Ok(elapsed) = modified.elapsed() {
                                    if elapsed.as_secs() > 3 * 3600 {
                                        info!("Cleaning up expired download: {:?}", entry.path());
                                        let _ = std::fs::remove_file(entry.path());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    });

    axum::serve(listener, app).await.unwrap();
}

async fn get_status(State(state): State<Arc<AppState>>) -> Json<SetupStatus> {
    let row = sqlx::query("SELECT server_name, setup_complete FROM settings WHERE id = 1").fetch_optional(&state.pool).await.unwrap();
    match row {
        Some(r) => Json(SetupStatus { 
            setup_complete: r.get("setup_complete"), 
            server_name: r.get("server_name") 
        }),
        None => Json(SetupStatus { setup_complete: false, server_name: None }),
    }
}

async fn get_uptime(State(state): State<Arc<AppState>>) -> Json<u64> {
    Json(state.start_time.elapsed().as_secs())
}

async fn onboard(State(state): State<Arc<AppState>>, Json(payload): Json<OnboardRequest>) -> Json<bool> {
    info!("Starting server onboarding for '{}'...", payload.server_name);
    sqlx::query("INSERT OR REPLACE INTO settings (id, server_name, setup_complete) VALUES (1, ?, 1)")
        .bind(&payload.server_name)
        .execute(&state.pool).await.unwrap();
    
    let hashed_password = bcrypt::hash(payload.admin_user.password_hash, bcrypt::DEFAULT_COST).unwrap();
    info!("Creating administrator account: {}...", payload.admin_user.username);
    sqlx::query("INSERT INTO users (id, username, password_hash, is_admin) VALUES (?, ?, ?, 1)")
        .bind(uuid::Uuid::new_v4().to_string())
        .bind(&payload.admin_user.username)
        .bind(hashed_password)
        .execute(&state.pool).await.unwrap();

    for lib in payload.libraries {
        info!("Registering library: {} ({}) at {}...", lib.name, lib.lib_type, lib.path);
        sqlx::query("INSERT INTO libraries (id, name, path, lib_type) VALUES (?, ?, ?, ?)")
            .bind(uuid::Uuid::new_v4().to_string())
            .bind(lib.name)
            .bind(lib.path)
            .bind(lib.lib_type)
            .execute(&state.pool).await.unwrap();
    }
    
    let state_clone = state.clone();
    tokio::spawn(async move { 
        info!("Setup complete. Triggering initial background scan...");
        scan_all_libraries(state_clone.clone()).await;
        start_watchers(state_clone).await;
    });
    Json(true)
}

async fn login(State(state): State<Arc<AppState>>, Json(payload): Json<LoginRequest>) -> Json<Option<LoginResponse>> {
    let row = sqlx::query("SELECT id, username, password_hash, is_admin, discord_token, discord_status, profile_picture FROM users WHERE username = ?")
        .bind(&payload.username)
        .fetch_optional(&state.pool).await.unwrap();

    if let Some(user) = row {
        let hash: String = user.get("password_hash");
        if bcrypt::verify(payload.password_hash, &hash).unwrap() {
            return Json(Some(LoginResponse {
                user_id: user.get("id"),
                username: user.get("username"),
                is_admin: user.get("is_admin"),
                discord_token: user.get("discord_token"),
                discord_status: user.get("discord_status"),
                profile_picture: user.get("profile_picture"),
            }));
        }
    }
    Json(None)
}

async fn get_user_profile(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<Option<LoginResponse>> {
    let row = sqlx::query("SELECT id, username, is_admin, discord_token, discord_status, profile_picture FROM users WHERE id = ?")
        .bind(id)
        .fetch_optional(&state.pool).await.unwrap();

    if let Some(user) = row {
        return Json(Some(LoginResponse {
            user_id: user.get("id"),
            username: user.get("username"),
            is_admin: user.get("is_admin"),
            discord_token: user.get("discord_token"),
            discord_status: user.get("discord_status"),
            profile_picture: user.get("profile_picture"),
        }));
    }
    Json(None)
}

async fn download_image(client: &reqwest::Client, url: &str, path: &StdPath) -> Result<(), Box<dyn std::error::Error>> {
    if path.exists() { return Ok(()); }
    let response = client.get(url).send().await?;
    let mut file = StdFile::create(path)?;
    let content = response.bytes().await?;
    file.write_all(&content)?;
    Ok(())
}

async fn fetch_metadata(state: &AppState, title: &str, year: Option<i32>, media_type: &str, folder_path: &StdPath) -> (Option<String>, Option<String>, Option<String>, Option<f64>, Option<String>, Option<String>, Option<String>, Option<String>) {
    let search_type = if media_type == "movie" { "movie" } else { "tv" };
    let url = format!(
        "https://api.themoviedb.org/3/search/{}?api_key={}&query={}{}",
        search_type,
        TMDB_API_KEY,
        urlencoding::encode(title),
        year.map(|y| format!("&year={}", y)).unwrap_or_default()
    );

    let mut overview = None;
    let mut cast = None;
    let mut genres = None;
    let mut rating = None;
    let mut tmdb_id = None;
    let mut collection_name = None;
    let mut poster_path_out = None;
    let mut backdrop_path_out = None;

    if let Ok(resp) = state.client.get(&url).send().await {
        if let Ok(json) = resp.json::<serde_json::Value>().await {
            if let Some(result) = json["results"].get(0) {
                let id = result["id"].as_i64().unwrap_or(0);
                tmdb_id = Some(id.to_string());
                overview = result["overview"].as_str().map(|s| s.to_string());
                rating = result["vote_average"].as_f64();
                let poster_path = result["poster_path"].as_str().unwrap_or("");
                let backdrop_path = result["backdrop_path"].as_str().unwrap_or("");
                poster_path_out = if !poster_path.is_empty() { Some(poster_path.to_string()) } else { None };
                backdrop_path_out = if !backdrop_path.is_empty() { Some(backdrop_path.to_string()) } else { None };

                // Download basic art
                if !poster_path.is_empty() {
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/w500{}", poster_path), &folder_path.join("folder.jpg")).await;
                }
                if !backdrop_path.is_empty() {
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/original{}", backdrop_path), &folder_path.join("backdrop.jpg")).await;
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/w1280{}", backdrop_path), &folder_path.join("landscape.jpg")).await;
                }

                // Fetch extra assets (logo)
                let images_url = format!("https://api.themoviedb.org/3/{}/{}/images?api_key={}", search_type, id, TMDB_API_KEY);
                if let Ok(img_resp) = state.client.get(&images_url).send().await {
                    if let Ok(img_json) = img_resp.json::<serde_json::Value>().await {
                        if let Some(logos) = img_json["logos"].as_array() {
                            if let Some(logo) = logos.get(0) {
                                if let Some(path) = logo["file_path"].as_str() {
                                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/original{}", path), &folder_path.join("logo.png")).await;
                                }
                            }
                        }
                    }
                }

                // Fetch details for genres and collection
                let details_url = format!("https://api.themoviedb.org/3/{}/{}?api_key={}", search_type, id, TMDB_API_KEY);
                if let Ok(detail_resp) = state.client.get(&details_url).send().await {
                    if let Ok(detail_json) = detail_resp.json::<serde_json::Value>().await {
                        if let Some(genres_array) = detail_json["genres"].as_array() {
                            let genre_names: Vec<&str> = genres_array.iter()
                                .filter_map(|g| g["name"].as_str())
                                .collect();
                            if !genre_names.is_empty() {
                                genres = Some(genre_names.join(", "));
                            }
                        }
                        if let Some(collection) = detail_json["belongs_to_collection"].as_object() {
                            collection_name = collection["name"].as_str().map(|s| s.to_string());
                        }
                    }
                }

                let credits_url = format!("https://api.themoviedb.org/3/{}/{}/credits?api_key={}", search_type, id, TMDB_API_KEY);
                if let Ok(cred_resp) = state.client.get(&credits_url).send().await {
                    if let Ok(cred_json) = cred_resp.json::<serde_json::Value>().await {
                        if let Some(cast_array) = cred_json["cast"].as_array() {
                            let cast_names: Vec<&str> = cast_array.iter()
                                .take(10)
                                .filter_map(|c| c["name"].as_str())
                                .collect();
                            cast = Some(cast_names.join(", "));
                        }
                    }
                }
            }
        }
    }
    (overview, cast, genres, rating, tmdb_id, collection_name, poster_path_out, backdrop_path_out)
}

async fn manual_scan(State(state): State<Arc<AppState>>) -> Json<bool> {
    let state_clone = state.clone();
    tokio::spawn(async move { scan_all_libraries(state_clone).await; });
    Json(true)
}

async fn get_recently_added(State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, collection_name, media_type, year, season, episode, added_at, file_path, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path FROM media_items ORDER BY added_at DESC LIMIT 15")
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn get_libraries(State(state): State<Arc<AppState>>) -> Json<Vec<Library>> {
    let libs = sqlx::query_as::<_, Library>("SELECT id, name, path, lib_type FROM libraries")
        .fetch_all(&state.pool).await.unwrap();
    Json(libs)
}

async fn add_library(State(state): State<Arc<AppState>>, Json(payload): Json<LibraryConfig>) -> Json<bool> {
    let id = uuid::Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO libraries (id, name, path, lib_type) VALUES (?, ?, ?, ?)")
        .bind(&id)
        .bind(&payload.name)
        .bind(&payload.path)
        .bind(&payload.lib_type)
        .execute(&state.pool).await.unwrap();
    
    let state_clone = state.clone();
    tokio::spawn(async move { scan_all_libraries(state_clone).await; });
    Json(true)
}

async fn update_library(Path(id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<LibraryConfig>) -> Json<bool> {
    sqlx::query("UPDATE libraries SET name = ?, path = ?, lib_type = ? WHERE id = ?")
        .bind(payload.name)
        .bind(payload.path)
        .bind(payload.lib_type)
        .bind(id)
        .execute(&state.pool).await.unwrap();
    Json(true)
}

async fn delete_library(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<bool> {
    sqlx::query("DELETE FROM media_items WHERE library_id = ?").bind(&id).execute(&state.pool).await.unwrap();
    sqlx::query("DELETE FROM libraries WHERE id = ?").bind(id).execute(&state.pool).await.unwrap();
    Json(true)
}

async fn get_library_items(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, collection_name, media_type, year, season, episode, added_at, file_path, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path FROM media_items WHERE library_id = ? ORDER BY title ASC")
        .bind(id)
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn get_show_episodes(Path(show_title): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, collection_name, media_type, year, season, episode, added_at, file_path, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path FROM media_items WHERE show_title = ? ORDER BY season ASC, episode ASC")
        .bind(show_title)
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn search_media(Query(params): Query<std::collections::HashMap<String, String>>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let query = params.get("q").cloned().unwrap_or_default();
    let search_pattern = format!("%{}%", query);
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, collection_name, media_type, year, season, episode, added_at, file_path, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path FROM media_items WHERE title LIKE ? LIMIT 20")
        .bind(search_pattern)
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn get_media_asset(
    Path((id, asset_name)): Path<(String, String)>,
    State(state): State<Arc<AppState>>,
) -> Response {
    let row = sqlx::query("SELECT file_path FROM media_items WHERE id = ?").bind(id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let path: String = r.get("file_path");
        let folder = std::path::Path::new(&path).parent().unwrap();
        let asset_path = folder.join(&asset_name);
        if asset_path.exists() {
            let mut file = tokio::fs::File::open(asset_path).await.unwrap();
            let mut contents = Vec::new();
            file.read_to_end(&mut contents).await.unwrap();
            let mime = mime_guess::from_path(asset_name).first_or_octet_stream();
            return (
                [(header::CONTENT_TYPE, mime.as_ref())],
                contents,
            ).into_response();
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

async fn get_media_subtitles(
    Path(id): Path<String>,
    State(state): State<Arc<AppState>>,
) -> Json<Vec<String>> {
    let row = sqlx::query("SELECT file_path FROM media_items WHERE id = ?").bind(id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let path: String = r.get("file_path");
        let path = std::path::Path::new(&path);
        if let Some(folder) = path.parent() {
            if let Some(stem) = path.file_stem().and_then(|s| s.to_str()) {
                let mut subs = Vec::new();
                if let Ok(entries) = std::fs::read_dir(folder) {
                    for entry in entries.filter_map(|e| e.ok()) {
                        let p = entry.path();
                        if p.is_file() {
                            if let Some(name) = p.file_name().and_then(|s| s.to_str()) {
                                if name.starts_with(stem) && (name.ends_with(".srt") || name.ends_with(".vtt")) {
                                    subs.push(name.to_string());
                                }
                            }
                        }
                    }
                }
                return Json(subs);
            }
        }
    }
    Json(Vec::new())
}

async fn get_media_subtitle_file(
    Path((id, name)): Path<(String, String)>,
    State(state): State<Arc<AppState>>,
) -> Response {
    let row = sqlx::query("SELECT file_path FROM media_items WHERE id = ?").bind(id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let path: String = r.get("file_path");
        let folder = std::path::Path::new(&path).parent().unwrap();
        let sub_path = folder.join(&name);
        if sub_path.exists() {
            let mut file = tokio::fs::File::open(sub_path).await.unwrap();
            let mut contents = String::new();
            if let Ok(_) = file.read_to_string(&mut contents).await {
                // If it's an SRT, do a simple conversion to VTT
                if name.ends_with(".srt") {
                    let vtt_content = format!("WEBVTT\n\n{}", contents.replace(",", "."));
                    return (
                        [(header::CONTENT_TYPE, "text/vtt")],
                        vtt_content,
                    ).into_response();
                }
                return (
                    [(header::CONTENT_TYPE, "text/vtt")],
                    contents,
                ).into_response();
            }
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

async fn stream_media(Path(id): Path<String>, State(state): State<Arc<AppState>>, req: axum::http::Request<Body>) -> Response {
    let token = req.uri().query().and_then(|q| {
        q.split('&').find_map(|pair| {
            let mut parts = pair.splitn(2, '=');
            if parts.next()? == "token" { parts.next() } else { None }
        })
    });

    if let Some(token) = token {
        let valid = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM temp_tokens WHERE id = ? AND media_id = ? AND expires_at > datetime('now')"
        )
        .bind(token)
        .bind(&id)
        .fetch_one(&state.pool)
        .await.unwrap_or(0);
        if valid == 0 {
            return StatusCode::UNAUTHORIZED.into_response();
        }
    }

    let row = sqlx::query("SELECT file_path FROM media_items WHERE id = ?").bind(id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let path: String = r.get("file_path");
        let path = std::path::Path::new(&path);
        if path.exists() {
            let file = tokio::fs::File::open(path).await.unwrap();
            let metadata = file.metadata().await.unwrap();
            let size = metadata.len();
            
            // Basic range support
            let range = req.headers().get(header::RANGE).and_then(|h| h.to_str().ok());
            if let Some(range) = range {
                if let Some(cap) = Regex::new(r"bytes=(\d+)-(\d+)?").unwrap().captures(range) {
                    let start = cap[1].parse::<u64>().unwrap();
                    let end = cap.get(2).map(|m| m.as_str().parse::<u64>().unwrap()).unwrap_or(size - 1);
                    let chunk_size = end - start + 1;
                    
                    use tokio::io::AsyncSeekExt;
                    use tokio_util::io::ReaderStream;
                    let mut file = file;
                    file.seek(std::io::SeekFrom::Start(start)).await.unwrap();
                    let stream = ReaderStream::with_capacity(file.take(chunk_size), 64 * 1024);
                    
                    return Response::builder()
                        .status(StatusCode::PARTIAL_CONTENT)
                        .header(header::CONTENT_TYPE, "video/mp4")
                        .header(header::CONTENT_RANGE, format!("bytes {}-{}/{}", start, end, size))
                        .header(header::ACCEPT_RANGES, "bytes")
                        .header(header::CONTENT_LENGTH, chunk_size)
                        .body(Body::from_stream(stream))
                        .unwrap();
                }
            }
            
            use tokio_util::io::ReaderStream;
            let stream = ReaderStream::new(file);
            return Response::builder()
                .header(header::CONTENT_TYPE, "video/mp4")
                .header(header::CONTENT_LENGTH, size)
                .header(header::ACCEPT_RANGES, "bytes")
                .body(Body::from_stream(stream))
                .unwrap();
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

async fn generate_media_token(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<String> {
    let token = uuid::Uuid::new_v4().to_string();
    let _ = sqlx::query("INSERT INTO temp_tokens (id, media_id, expires_at) VALUES (?, ?, datetime('now', '+1 day'))")
        .bind(&token)
        .bind(&id)
        .execute(&state.pool)
        .await;
    Json(token)
}

async fn download_media(Path(id): Path<String>, State(state): State<Arc<AppState>>, req: axum::http::Request<Body>) -> Response {
    let token = req.uri().query().and_then(|q| {
        q.split('&').find_map(|pair| {
            let mut parts = pair.splitn(2, '=');
            if parts.next()? == "token" { parts.next() } else { None }
        })
    });

    match token {
        Some(token) => {
            let valid = sqlx::query_scalar::<_, i64>(
                "SELECT COUNT(*) FROM temp_tokens WHERE id = ? AND media_id = ? AND expires_at > datetime('now')"
            )
            .bind(token)
            .bind(&id)
            .fetch_one(&state.pool)
            .await.unwrap_or(0);
            if valid == 0 {
                return StatusCode::UNAUTHORIZED.into_response();
            }
        }
        None => return StatusCode::UNAUTHORIZED.into_response(),
    }

    let row = sqlx::query("SELECT file_path FROM media_items WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let path: String = r.get("file_path");
        let path = std::path::Path::new(&path);
        if path.exists() {
            let file = tokio::fs::File::open(path).await.unwrap();
            let metadata = file.metadata().await.unwrap();
            let size = metadata.len();

            use tokio_util::io::ReaderStream;
            let stream = ReaderStream::new(file);
            let filename = path.file_name().and_then(|n| n.to_str()).unwrap_or("download");

            return Response::builder()
                .header(header::CONTENT_TYPE, "application/octet-stream")
                .header(header::CONTENT_DISPOSITION, format!("attachment; filename=\"{}\"", filename))
                .header(header::CONTENT_LENGTH, size)
                .body(Body::from_stream(stream))
                .unwrap();
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

async fn download_media_zip(Path(id): Path<String>, State(state): State<Arc<AppState>>, req: axum::http::Request<Body>) -> Response {
    let token = req.uri().query().and_then(|q| {
        q.split('&').find_map(|pair| {
            let mut parts = pair.splitn(2, '=');
            if parts.next()? == "token" { parts.next() } else { None }
        })
    });

    match token {
        Some(token) => {
            let valid = sqlx::query_scalar::<_, i64>(
                "SELECT COUNT(*) FROM temp_tokens WHERE id = ? AND media_id = ? AND expires_at > datetime('now')"
            )
            .bind(token)
            .bind(&id)
            .fetch_one(&state.pool)
            .await.unwrap_or(0);
            if valid == 0 {
                return StatusCode::UNAUTHORIZED.into_response();
            }
        }
        None => return StatusCode::UNAUTHORIZED.into_response(),
    }

    let row = sqlx::query("SELECT file_path, title FROM media_items WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let file_path: String = r.get("file_path");
        let title: String = r.get("title");
        let path = std::path::Path::new(&file_path);
        if let Some(parent) = path.parent() {
            if parent.exists() {
                let home_dir = dirs::home_dir().expect("Could not find home directory");
                let dl_dir = home_dir.join(".sunset").join("tmp").join("downloads");
                if !dl_dir.exists() {
                    let _ = std::fs::create_dir_all(&dl_dir);
                }
                
                let zip_name = format!("{}_{}.zip", id, uuid::Uuid::new_v4().to_string()[..8].to_string());
                let zip_path = dl_dir.join(&zip_name);
                
                // Perform zipping (blocking in spawn_blocking)
                let parent_buf = parent.to_path_buf();
                let zip_path_buf = zip_path.clone();
                let zip_res = tokio::task::spawn_blocking(move || {
                    zip_folder(&parent_buf, &zip_path_buf)
                }).await.unwrap();

                if zip_res.is_ok() {
                    let file = tokio::fs::File::open(&zip_path).await.unwrap();
                    let metadata = file.metadata().await.unwrap();
                    let size = metadata.len();

                    use tokio_util::io::ReaderStream;
                    let stream = ReaderStream::new(file);
                    let filename = format!("{}.zip", title.replace(" ", "_"));

                    return Response::builder()
                        .header(header::CONTENT_TYPE, "application/zip")
                        .header(header::CONTENT_DISPOSITION, format!("attachment; filename=\"{}\"", filename))
                        .header(header::CONTENT_LENGTH, size)
                        .body(Body::from_stream(stream))
                        .unwrap();
                } else {
                    error!("Failed to create ZIP: {:?}", zip_res.err());
                    return StatusCode::INTERNAL_SERVER_ERROR.into_response();
                }
            }
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

fn zip_folder(source: &std::path::Path, target: &std::path::Path) -> zip::result::ZipResult<()> {
    let file = std::fs::File::create(target)?;
    let mut zip = zip::ZipWriter::new(file);
    let options = zip::write::FileOptions::default()
        .compression_method(zip::CompressionMethod::Stored)
        .unix_permissions(0o755);

    for entry in walkdir::WalkDir::new(source).into_iter().filter_map(|e| e.ok()) {
        let path = entry.path();
        let name = path.strip_prefix(source).unwrap();

        if path.is_file() {
            zip.start_file(name.to_string_lossy(), options)?;
            let mut f = std::fs::File::open(path)?;
            std::io::copy(&mut f, &mut zip)?;
        } else if !name.as_os_str().is_empty() {
            zip.add_directory(name.to_string_lossy(), options)?;
        }
    }
    zip.finish()?;
    Ok(())
}

#[derive(Deserialize)]
struct UpdateMediaPayload {
    title: Option<String>,
    description: Option<String>,
    year: Option<i32>,
    genres: Option<String>,
    cast: Option<String>,
    rating: Option<f64>,
}

#[derive(Serialize, Deserialize)]
struct PlaybackPayload {
    item_id: String,
    timestamp: f64,
    duration: Option<f64>,
    user_id: Option<String>,
    is_playing: Option<bool>,
}

#[derive(Serialize, Deserialize)]
struct InviteRequest {
    code: String,
}

#[derive(Deserialize)]
struct DiscordConfigPayload {
    token: String,
    status: String,
}

async fn update_discord_config(
    Path(id): Path<String>,
    State(state): State<Arc<AppState>>,
    Json(payload): Json<DiscordConfigPayload>,
) -> Json<bool> {
    let masked = payload.token.chars().map(|c| if c.len_utf8() == 1 && c.is_ascii_graphic() { '*' } else { c }).collect::<String>();
    info!("Updating Discord config for user {} (token: {}, status: {})", id, masked, payload.status);
    sqlx::query("UPDATE users SET discord_token = ?, discord_status = ? WHERE id = ?")
        .bind(&payload.token)
        .bind(&payload.status)
        .bind(&id)
        .execute(&state.pool)
        .await
        .unwrap();
    
    // Close existing session to force reconnect with new token
    let mut manager = state.rpc_manager.lock().await;
    if manager.sessions.remove(&id).is_some() {
        info!("Closed existing Discord RPC session for user {}", id);
    }
    
    Json(true)
}

async fn list_users(State(state): State<Arc<AppState>>) -> Json<Vec<LoginResponse>> {
    let rows = sqlx::query("SELECT id, username, is_admin, discord_token, discord_status, profile_picture FROM users ORDER BY username")
        .fetch_all(&state.pool).await.unwrap();
    Json(rows.iter().map(|r| LoginResponse {
        user_id: r.get("id"),
        username: r.get("username"),
        is_admin: r.get("is_admin"),
        discord_token: r.get("discord_token"),
        discord_status: r.get("discord_status"),
        profile_picture: r.get("profile_picture"),
    }).collect())
}

async fn create_user(State(state): State<Arc<AppState>>, Json(payload): Json<CreateUserPayload>) -> Json<bool> {
    let hashed = bcrypt::hash(&payload.password_hash, bcrypt::DEFAULT_COST).unwrap();
    let id = uuid::Uuid::new_v4().to_string();
    sqlx::query("INSERT INTO users (id, username, password_hash, is_admin) VALUES (?, ?, ?, ?)")
        .bind(&id).bind(&payload.username).bind(hashed).bind(payload.is_admin)
        .execute(&state.pool).await.unwrap();
    Json(true)
}

async fn change_password(Path(id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<ChangePasswordPayload>) -> Json<bool> {
    let row = sqlx::query("SELECT password_hash FROM users WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap();
    match row {
        Some(r) => {
            let hash: String = r.get("password_hash");
            if bcrypt::verify(&payload.current_password, &hash).unwrap() {
                let new_hash = bcrypt::hash(&payload.new_password, bcrypt::DEFAULT_COST).unwrap();
                sqlx::query("UPDATE users SET password_hash = ? WHERE id = ?").bind(new_hash).bind(&id).execute(&state.pool).await.unwrap();
                Json(true)
            } else {
                Json(false)
            }
        }
        None => Json(false),
    }
}

async fn change_username(Path(id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<ChangeUsernamePayload>) -> Json<bool> {
    sqlx::query("UPDATE users SET username = ? WHERE id = ?").bind(&payload.new_username).bind(&id).execute(&state.pool).await.unwrap();
    Json(true)
}

async fn upload_profile_picture(Path(id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<ProfilePicturePayload>) -> Json<bool> {
    let sunset_dir = dirs::home_dir().unwrap().join(".sunset");
    let avatars_dir = sunset_dir.join("avatars");
    let _ = std::fs::create_dir_all(&avatars_dir);
    let avatar_path = avatars_dir.join(format!("{}.jpg", id));

    use base64::Engine;
    let parts: Vec<&str> = payload.image.split(',').collect();
    let data = if parts.len() > 1 {
        base64::engine::general_purpose::STANDARD.decode(parts[1]).unwrap_or_default()
    } else {
        base64::engine::general_purpose::STANDARD.decode(parts[0]).unwrap_or_default()
    };

    if data.is_empty() { return Json(false); }
    let _ = std::fs::write(&avatar_path, &data);
    let path_str = avatar_path.to_str().unwrap_or("").to_string();
    sqlx::query("UPDATE users SET profile_picture = ? WHERE id = ?").bind(&path_str).bind(&id).execute(&state.pool).await.unwrap();
    Json(true)
}

async fn get_profile_picture(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Response {
    let row = sqlx::query("SELECT profile_picture FROM users WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        if let Some(path) = r.get::<Option<String>, _>("profile_picture") {
            let p = std::path::Path::new(&path);
            if p.exists() {
                let data = std::fs::read(p).unwrap_or_default();
                return Response::builder()
                    .header(header::CONTENT_TYPE, "image/jpeg")
                    .body(Body::from(data))
                    .unwrap();
            }
        }
    }
    // Fallback: generate a simple SVG avatar with initial
    let name = sqlx::query_scalar::<_, String>("SELECT username FROM users WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap_or(None);
    let initial = name.as_deref().and_then(|n| n.chars().next()).unwrap_or('?').to_uppercase().to_string();
    let svg = format!(
        "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'><rect width='100' height='100' rx='50' fill='#333'/><text x='50' y='50' text-anchor='middle' dy='.35em' fill='white' font-size='40' font-family='sans-serif'>{}</text></svg>",
        initial
    );
    Response::builder()
        .header(header::CONTENT_TYPE, "image/svg+xml")
        .body(Body::from(svg))
        .unwrap()
}

async fn save_playback(State(state): State<Arc<AppState>>, Json(payload): Json<PlaybackPayload>) -> Json<bool> {
    let user_id = payload.user_id.clone().unwrap_or_else(|| "default".to_string());
    debug!("Saving playback for user {}: item={}, ts={}, playing={:?}", user_id, payload.item_id, payload.timestamp, payload.is_playing);
    let id = format!("{}_{}", user_id, payload.item_id);
    
    // Refresh updated_at on every save so it jumps to top of Continue Watching
    sqlx::query("INSERT INTO playback_state (id, item_id, user_id, timestamp, duration, updated_at) VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) ON CONFLICT(id) DO UPDATE SET timestamp = excluded.timestamp, duration = excluded.duration, updated_at = CURRENT_TIMESTAMP")
        .bind(&id).bind(&payload.item_id).bind(&user_id).bind(payload.timestamp).bind(payload.duration)
        .execute(&state.pool).await.unwrap();

    // Trigger Discord RPC update if user has a token
    let user_row = sqlx::query("SELECT discord_token, discord_status FROM users WHERE id = ?").bind(&user_id).fetch_optional(&state.pool).await.unwrap();
    if let Some(row) = user_row {
        if let Some(token) = row.get::<Option<String>, _>("discord_token").filter(|t| !t.is_empty()) {
            let status = row.get::<Option<String>, _>("discord_status").unwrap_or_else(|| "online".to_string());
            let mut manager = state.rpc_manager.lock().await;
            let session = if let Some(s) = manager.sessions.get(&user_id) {
                s
            } else {
                info!("Creating new Discord RPC session for user {}", user_id);
                let (tx, rx) = tokio::sync::mpsc::unbounded_channel();
                let token_clone = token.clone();
                tokio::spawn(async move { run_discord_rpc(token_clone, rx).await; });
                manager.sessions.insert(user_id.clone(), DiscordRpcSession { token, presence_tx: tx });
                manager.sessions.get(&user_id).unwrap()
            };

            if payload.is_playing == Some(false) {
                manager.sessions.remove(&user_id);
                info!("Closed Discord RPC session for user {} (playback stopped)", user_id);
                return Json(true);
            }

            // Fetch item details for presence
            if let Ok(item) = sqlx::query("SELECT title, show_title, media_type, poster_path, season, episode FROM media_items WHERE id = ?").bind(&payload.item_id).fetch_one(&state.pool).await {
                let title: String = item.get("title");
                let show_title: Option<String> = item.get("show_title");
                let media_type: String = item.get("media_type");
                let poster_path: Option<String> = item.get("poster_path");

                let name = if media_type == "episode" {
                    show_title.unwrap_or(title.clone())
                } else {
                    title.clone()
                };

                let progress = if let Some(dur) = payload.duration {
                    if dur > 0.0 { (payload.timestamp / dur * 100.0) as i32 } else { 0 }
                } else { 0 };

                debug!("Sending Discord presence: {} ({}%)", name, progress);

                let image_url = if let Some(path) = poster_path {
                    format!("https://image.tmdb.org/t/p/original{}", path)
                } else {
                    format!("https://sunset.sudoloser.com/api/media/{}/asset/folder.jpg", payload.item_id)
                };

                let state_text = if media_type == "episode" {
                    let ep = item.get::<Option<i32>, _>("episode").unwrap_or(1);
                    let season = item.get::<Option<i32>, _>("season").unwrap_or(1);
                    format!("Season {} Episode {} ({}%)", season, ep, progress)
                } else {
                    format!("Progress: {}%", progress)
                };

                let presence = DiscordPresence {
                    status: status,
                    since: None,
                    activities: vec![DiscordActivity {
                        name: "SunSet".to_string(),
                        activity_type: 3, // Watching
                        details: Some(name),
                        state: Some(state_text),
                        assets: Some(DiscordAssets {
                            large_image: Some(image_url),
                            large_text: Some(title),
                            small_image: None,
                            small_text: None,
                        }),
                        timestamps: Some(DiscordTimestamps {
                            // Discord expects timestamps in milliseconds for activities
                            start: Some((chrono::Utc::now().timestamp() as u64 - payload.timestamp as u64) * 1000),
                            end: None,
                        }),
                    }],
                    afk: false,
                };
                if session.presence_tx.send(presence).is_err() {
                    warn!("Failed to send presence to Discord RPC session for user {} (channel closed)", user_id);
                }
            } else {
                warn!("Could not fetch media item {} for Discord presence", payload.item_id);
            }
        } else {
            debug!("User {} has no discord_token set, skipping RPC", user_id);
        }
    }

    Json(true)
}

async fn get_playback(Path(item_id): Path<String>, State(state): State<Arc<AppState>>, Query(params): Query<HashMap<String, String>>) -> Json<Option<PlaybackState>> {
    let user_id = params.get("user_id").map(|s| s.as_str());
    let state_row = if let Some(uid) = user_id {
        sqlx::query_as::<_, PlaybackState>("SELECT id, item_id, user_id, timestamp, duration, updated_at FROM playback_state WHERE item_id = ? AND user_id = ? ORDER BY updated_at DESC LIMIT 1")
            .bind(&item_id).bind(uid).fetch_optional(&state.pool).await.unwrap()
    } else {
        sqlx::query_as::<_, PlaybackState>("SELECT id, item_id, user_id, timestamp, duration, updated_at FROM playback_state WHERE item_id = ? ORDER BY updated_at DESC LIMIT 1")
            .bind(&item_id).fetch_optional(&state.pool).await.unwrap()
    };
    Json(state_row)
}

async fn get_continue_watching(Path(user_id): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>(
        "SELECT mi.id, mi.title, mi.show_title, mi.collection_name, mi.media_type, mi.year, mi.season, mi.episode, mi.added_at, mi.file_path, mi.description, mi.\"cast\", mi.genres, mi.rating, mi.tmdb_id, mi.poster_path, mi.backdrop_path FROM media_items mi INNER JOIN playback_state ps ON ps.item_id = mi.id WHERE ps.user_id = ? ORDER BY ps.updated_at DESC"
    )
    .bind(user_id)
    .fetch_all(&state.pool)
    .await
    .unwrap_or_default();
    Json(items)
}

async fn get_user_items(Path(user_id): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>(
        "SELECT mi.id, mi.title, mi.show_title, mi.collection_name, mi.media_type, mi.year, mi.season, mi.episode, mi.added_at, mi.file_path, mi.description, mi.\"cast\", mi.genres, mi.rating, mi.tmdb_id, mi.poster_path, mi.backdrop_path FROM media_items mi INNER JOIN user_items ui ON ui.item_id = mi.id WHERE ui.user_id = ? ORDER BY ui.added_at DESC"
    )
    .bind(user_id)
    .fetch_all(&state.pool)
    .await
    .unwrap_or_default();
    Json(items)
}

async fn add_user_item(Path(user_id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<serde_json::Value>) -> Json<bool> {
    let item_id = payload["item_id"].as_str().unwrap_or("");
    if item_id.is_empty() { return Json(false); }
    let _ = sqlx::query("INSERT OR IGNORE INTO user_items (user_id, item_id, added_at) VALUES (?, ?, CURRENT_TIMESTAMP)")
        .bind(&user_id)
        .bind(item_id)
        .execute(&state.pool)
        .await;
    Json(true)
}

async fn remove_user_item(Path((user_id, item_id)): Path<(String, String)>, State(state): State<Arc<AppState>>) -> Json<bool> {
    let _ = sqlx::query("DELETE FROM user_items WHERE user_id = ? AND item_id = ?")
        .bind(&user_id)
        .bind(&item_id)
        .execute(&state.pool)
        .await;
    Json(true)
}

async fn get_storage(State(state): State<Arc<AppState>>) -> Json<StorageInfo> {
    let item_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM media_items").fetch_one(&state.pool).await.unwrap_or(0);
    let library_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM libraries").fetch_one(&state.pool).await.unwrap_or(0);
    let user_count: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM users").fetch_one(&state.pool).await.unwrap_or(0);
    // Rough total size: sum file sizes from filesystem
    let mut total_size: u64 = 0;
    if let Ok(rows) = sqlx::query("SELECT file_path FROM media_items").fetch_all(&state.pool).await {
        for row in rows {
            let p: String = row.get("file_path");
            if let Ok(meta) = std::fs::metadata(&p) {
                total_size += meta.len();
            }
        }
    }
    Json(StorageInfo { total_size, item_count, library_count, user_count })
}

async fn update_media_item(Path(id): Path<String>, State(state): State<Arc<AppState>>, Json(payload): Json<UpdateMediaPayload>) -> Json<bool> {
    if let Some(title) = &payload.title {
        sqlx::query("UPDATE media_items SET title = ? WHERE id = ?").bind(title).bind(&id).execute(&state.pool).await.unwrap();
    }
    if let Some(desc) = &payload.description {
        sqlx::query("UPDATE media_items SET description = ? WHERE id = ?").bind(desc).bind(&id).execute(&state.pool).await.unwrap();
    }
    if let Some(year) = payload.year {
        sqlx::query("UPDATE media_items SET year = ? WHERE id = ?").bind(year).bind(&id).execute(&state.pool).await.unwrap();
    }
    if let Some(genres) = &payload.genres {
        sqlx::query("UPDATE media_items SET genres = ? WHERE id = ?").bind(genres).bind(&id).execute(&state.pool).await.unwrap();
    }
    if let Some(cast) = &payload.cast {
        sqlx::query("UPDATE media_items SET \"cast\" = ? WHERE id = ?").bind(cast).bind(&id).execute(&state.pool).await.unwrap();
    }
    if let Some(rating) = payload.rating {
        sqlx::query("UPDATE media_items SET rating = ? WHERE id = ?").bind(rating).bind(&id).execute(&state.pool).await.unwrap();
    }
    Json(true)
}

async fn refresh_media_item(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<bool> {
    let row = sqlx::query("SELECT file_path, title, year, media_type FROM media_items WHERE id = ?").bind(&id).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let file_path: String = r.get("file_path");
        let title: String = r.get("title");
        let year: Option<i32> = r.get("year");
        let media_type: String = r.get("media_type");
        let folder_path = std::path::Path::new(&file_path).parent().unwrap().to_path_buf();
        let search_type = if media_type == "movie" { "movie" } else { "tv" };
        let (overview, cast, genres, rating, tmdb_id, collection, poster, backdrop) = fetch_metadata(&state, &title, year, search_type, &folder_path).await;
        sqlx::query("UPDATE media_items SET description = ?, \"cast\" = ?, genres = ?, rating = ?, tmdb_id = ?, collection_name = ?, poster_path = ?, backdrop_path = ? WHERE id = ?")
            .bind(overview).bind(cast).bind(genres).bind(rating).bind(tmdb_id).bind(collection).bind(poster).bind(backdrop).bind(&id).execute(&state.pool).await.unwrap();
        return Json(true);
    }
    Json(false)
}

async fn create_invite(State(state): State<Arc<AppState>>) -> Json<String> {
    let code = uuid::Uuid::new_v4().to_string()[..8].to_string().to_uppercase();
    sqlx::query("INSERT INTO invite_codes (code) VALUES (?)").bind(&code).execute(&state.pool).await.unwrap();
    Json(code)
}

async fn redeem_invite(State(state): State<Arc<AppState>>, Json(payload): Json<InviteRequest>) -> Json<bool> {
    let row = sqlx::query("SELECT used FROM invite_codes WHERE code = ?").bind(&payload.code).fetch_optional(&state.pool).await.unwrap();
    if let Some(r) = row {
        let used: bool = r.get("used");
        if !used {
            sqlx::query("UPDATE invite_codes SET used = 1 WHERE code = ?").bind(&payload.code).execute(&state.pool).await.unwrap();
            return Json(true);
        }
    }
    Json(false)
}

async fn get_genres(State(state): State<Arc<AppState>>) -> Json<Vec<String>> {
    let rows = sqlx::query("SELECT DISTINCT genres FROM media_items WHERE genres IS NOT NULL AND genres != ''").fetch_all(&state.pool).await.unwrap();
    let mut all_genres = Vec::new();
    for row in rows {
        let g: String = row.get("genres");
        for genre in g.split(',').map(|s| s.trim()) {
            if !genre.is_empty() && !all_genres.contains(&genre.to_string()) {
                all_genres.push(genre.to_string());
            }
        }
    }
    all_genres.sort();
    Json(all_genres)
}

async fn get_genre_items(Path(genre): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let pattern = format!("%{}%", genre);
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, collection_name, media_type, year, season, episode, added_at, file_path, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path FROM media_items WHERE genres LIKE ? LIMIT 30")
        .bind(pattern).fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn scan_all_libraries(state: Arc<AppState>) {
    info!("Starting full media scan...");
    let libraries = sqlx::query_as::<_, Library>("SELECT id, name, path, lib_type FROM libraries").fetch_all(&state.pool).await.unwrap();
    for lib in libraries { scan_library(state.clone(), lib).await; }
}

async fn scan_library(state: Arc<AppState>, lib: Library) {
    info!("Scanning library '{}' at {}...", lib.name, lib.path);
    let movie_regex = Regex::new(r"^(.*)\s\((\d{4})\)$").unwrap();
    // More flexible show regex: S01E01, 1x01, S1E1, etc.
    let show_regex = Regex::new(r"(?i)^(.*?)\s*(?:S(\d{1,2})E(\d{1,2})|(\d{1,2})x(\d{1,2}))").unwrap();
    let mut count = 0;

    for entry in WalkDir::new(&lib.path).into_iter().filter_map(|e| e.ok()) {
        if entry.file_type().is_file() {
            let path = entry.path();
            let ext = path.extension().and_then(|s| s.to_str()).unwrap_or("");
            if !["mp4", "mkv", "avi", "mov"].contains(&ext.to_lowercase().as_str()) { continue; }
            let file_name = path.file_stem().and_then(|s| s.to_str()).unwrap_or("");
            let file_path = path.to_str().unwrap().to_string();
            let folder_path = path.parent().unwrap();

            if lib.lib_type == "movies" {
                let (title, year) = match movie_regex.captures(file_name) {
                    Some(caps) => (caps[1].to_string(), Some(caps[2].parse::<i32>().unwrap_or(0))),
                    None => (file_name.to_string(), None),
                };

                // Fetch metadata and assets
                let (overview, cast, genres, rating, tmdb_id, collection, poster, backdrop) = fetch_metadata(&state, &title, year, "movie", folder_path).await;

                if let Ok(_) = sqlx::query("INSERT OR IGNORE INTO media_items (id, library_id, title, show_title, collection_name, file_path, media_type, year, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path) VALUES (?, ?, ?, NULL, ?, ?, 'movie', ?, ?, ?, ?, ?, ?, ?, ?)")
                    .bind(uuid::Uuid::new_v4().to_string()).bind(&lib.id).bind(title).bind(collection).bind(&file_path).bind(year).bind(overview).bind(cast).bind(genres).bind(rating).bind(tmdb_id).bind(poster).bind(backdrop).execute(&state.pool).await {
                        count += 1;
                    }
            } else {
                let (show_title, season, episode) = match show_regex.captures(file_name) {
                    Some(caps) => {
                        let title = caps[1].trim().to_string();
                        let s = caps.get(2).or(caps.get(4)).map(|m| m.as_str().parse::<i32>().unwrap_or(0)).unwrap_or(0);
                        let e = caps.get(3).or(caps.get(5)).map(|m| m.as_str().parse::<i32>().unwrap_or(0)).unwrap_or(0);
                        (title, s, e)
                    },
                    None => {
                        // Fallback: use parent directory as show title so files in the same
                        // folder are grouped together, instead of each filename becoming its own show.
                        let dir = folder_path.file_name()
                            .and_then(|s| s.to_str())
                            .unwrap_or(file_name)
                            .to_string();
                        (dir, 1, 1)
                    }
                };

                // Fetch metadata and assets for the show if not already done
                let (overview, cast, genres, rating, tmdb_id, _, poster, backdrop) = fetch_metadata(&state, &show_title, None, "tv", folder_path).await;

                if let Ok(_) = sqlx::query("INSERT OR IGNORE INTO media_items (id, library_id, title, show_title, collection_name, file_path, media_type, season, episode, description, \"cast\", genres, rating, tmdb_id, poster_path, backdrop_path) VALUES (?, ?, ?, ?, NULL, ?, 'episode', ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                    .bind(uuid::Uuid::new_v4().to_string()).bind(&lib.id).bind(file_name).bind(show_title).bind(&file_path).bind(season).bind(episode).bind(overview).bind(cast).bind(genres).bind(rating).bind(tmdb_id).bind(poster).bind(backdrop).execute(&state.pool).await {
                        count += 1;
                    }
            }
        }
    }
    info!("Finished scanning '{}'. {} new items indexed.", lib.name, count);
}

async fn start_watchers(state: Arc<AppState>) {
    let (tx, mut rx) = tokio::sync::mpsc::channel(1);
    let mut watcher = notify::RecommendedWatcher::new(move |res: notify::Result<notify::Event>| {
        if res.is_ok() { let _ = tx.blocking_send(()); }
    }, Config::default()).unwrap();

    let libs = sqlx::query_as::<_, Library>("SELECT id, name, path, lib_type FROM libraries").fetch_all(&state.pool).await.unwrap();
    for lib in libs { let _ = watcher.watch(StdPath::new(&lib.path), RecursiveMode::Recursive); }

    tokio::spawn(async move {
        let _watcher = watcher;
        let state_clone = state.clone();
        while let Some(_) = rx.recv().await { scan_all_libraries(state_clone.clone()).await; }
    });
}
