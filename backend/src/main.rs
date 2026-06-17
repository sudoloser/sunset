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

mod opensubtitles;

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