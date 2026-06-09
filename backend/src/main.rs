use axum::{
    routing::{get, post, put},
    Json, Router,
    extract::{State, Path, Query},
    response::{IntoResponse, Response},
    body::Body,
};
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use tracing::info;
use sqlx::sqlite::{SqlitePoolOptions};
use sqlx::{SqlitePool, Row};
use tower_http::cors::CorsLayer;
use regex::Regex;
use walkdir::WalkDir;
use std::time::Instant;
use std::sync::Arc;
use notify::{Watcher, RecursiveMode, Config};
use std::path::Path as StdPath;
use http::{StatusCode, header};
use tokio::io::AsyncReadExt;
use rust_embed::RustEmbed;
use std::fs::File as StdFile;
use std::io::Write;

const TMDB_API_KEY: &str = "fb7bb23f03b6994dafc674c074d01761";
const OMDB_API_KEY: &str = "4b447405";

#[derive(RustEmbed)]
#[folder = "dist/"]
struct Asset;

#[derive(Serialize, Deserialize)]
struct LoginResponse {
    user_id: String,
    username: String,
    is_admin: bool,
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

#[derive(Serialize, Deserialize, sqlx::FromRow)]
struct MediaItem {
    id: String,
    title: String,
    show_title: Option<String>,
    media_type: String,
    year: Option<i32>,
    season: Option<i32>,
    episode: Option<i32>,
    added_at: Option<chrono::NaiveDateTime>,
    file_path: String,
    description: Option<String>,
    cast: Option<String>,
}

struct AppState {
    pool: SqlitePool,
    start_time: Instant,
    client: reqwest::Client,
}

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
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
    sqlx::query("CREATE TABLE IF NOT EXISTS users (id TEXT PRIMARY KEY, username TEXT UNIQUE NOT NULL, password_hash TEXT NOT NULL, is_admin BOOLEAN DEFAULT 0)").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS libraries (id TEXT PRIMARY KEY, name TEXT NOT NULL, path TEXT NOT NULL, lib_type TEXT NOT NULL)").execute(&pool).await.unwrap();

    // Drop and recreate media_items to ensure it has all columns
    sqlx::query("DROP TABLE IF EXISTS media_items").execute(&pool).await.unwrap();
    sqlx::query("CREATE TABLE IF NOT EXISTS media_items (id TEXT PRIMARY KEY, library_id TEXT NOT NULL, title TEXT NOT NULL, show_title TEXT, file_path TEXT UNIQUE NOT NULL, media_type TEXT NOT NULL, year INTEGER, season INTEGER, episode INTEGER, added_at DATETIME DEFAULT CURRENT_TIMESTAMP, description TEXT, cast TEXT, FOREIGN KEY(library_id) REFERENCES libraries(id))").execute(&pool).await.unwrap();
    info!("Database schema verified.");

    info!("Initializing application state...");
    let state = Arc::new(AppState {
        pool: pool.clone(),
        start_time: Instant::now(),
        client: reqwest::Client::new(),
    });

    info!("Configuring API routes and serving embedded frontend...");
    let app = Router::new()
        .route("/api/status", get(get_status))
        .route("/api/uptime", get(get_uptime))
        .route("/api/onboard", post(onboard))
        .route("/api/login", post(login))
        .route("/api/users/:id", get(get_user_profile))
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
    let row = sqlx::query("SELECT id, username, password_hash, is_admin FROM users WHERE username = ?")
        .bind(&payload.username)
        .fetch_optional(&state.pool).await.unwrap();

    if let Some(user) = row {
        let hash: String = user.get("password_hash");
        if bcrypt::verify(payload.password_hash, &hash).unwrap() {
            return Json(Some(LoginResponse {
                user_id: user.get("id"),
                username: user.get("username"),
                is_admin: user.get("is_admin"),
            }));
        }
    }
    Json(None)
}

async fn get_user_profile(Path(id): Path<String>, State(state): State<Arc<AppState>>) -> Json<Option<LoginResponse>> {
    let row = sqlx::query("SELECT id, username, is_admin FROM users WHERE id = ?")
        .bind(id)
        .fetch_optional(&state.pool).await.unwrap();

    if let Some(user) = row {
        return Json(Some(LoginResponse {
            user_id: user.get("id"),
            username: user.get("username"),
            is_admin: user.get("is_admin"),
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

async fn fetch_metadata(state: &AppState, title: &str, year: Option<i32>, media_type: &str, folder_path: &StdPath) -> (Option<String>, Option<String>) {
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

    if let Ok(resp) = state.client.get(&url).send().await {
        if let Ok(json) = resp.json::<serde_json::Value>().await {
            if let Some(result) = json["results"].get(0) {
                let id = result["id"].as_i64().unwrap_or(0);
                overview = result["overview"].as_str().map(|s| s.to_string());
                let poster_path = result["poster_path"].as_str().unwrap_or("");
                let backdrop_path = result["backdrop_path"].as_str().unwrap_or("");

                // Download basic art
                if !poster_path.is_empty() {
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/w500{}", poster_path), &folder_path.join("folder.jpg")).await;
                }
                if !backdrop_path.is_empty() {
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/original{}", backdrop_path), &folder_path.join("backdrop.jpg")).await;
                    let _ = download_image(&state.client, &format!("https://image.tmdb.org/t/p/w1280{}", backdrop_path), &folder_path.join("landscape.jpg")).await;
                }

                // Fetch extra assets (logo) and credits
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

                let credits_url = format!("https://api.themoviedb.org/3/{}/{}/credits?api_key={}", search_type, id, TMDB_API_KEY);
                if let Ok(cred_resp) = state.client.get(&credits_url).send().await {
                    if let Ok(cred_json) = cred_resp.json::<serde_json::Value>().await {
                        if let Some(cast_array) = cred_json[\"cast\"].as_array() {
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
    (overview, cast)
}

async fn manual_scan(State(state): State<Arc<AppState>>) -> Json<bool> {
    let state_clone = state.clone();
    tokio::spawn(async move { scan_all_libraries(state_clone).await; });
    Json(true)
}

async fn get_recently_added(State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, media_type, year, season, episode, added_at, file_path, description, \"cast\" FROM media_items ORDER BY added_at DESC LIMIT 15")
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
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, media_type, year, season, episode, added_at, file_path, description, \"cast\" FROM media_items WHERE library_id = ? ORDER BY title ASC")
        .bind(id)
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn get_show_episodes(Path(show_title): Path<String>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, media_type, year, season, episode, added_at, file_path, description, \"cast\" FROM media_items WHERE show_title = ? ORDER BY season ASC, episode ASC")
        .bind(show_title)
        .fetch_all(&state.pool).await.unwrap();
    Json(items)
}

async fn search_media(Query(params): Query<std::collections::HashMap<String, String>>, State(state): State<Arc<AppState>>) -> Json<Vec<MediaItem>> {
    let query = params.get("q").cloned().unwrap_or_default();
    let search_pattern = format!("%{}%", query);
    let items = sqlx::query_as::<_, MediaItem>("SELECT id, title, show_title, media_type, year, season, episode, added_at, file_path, description, \"cast\" FROM media_items WHERE title LIKE ? LIMIT 20")
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
        let sub_path = folder.join(name);
        if sub_path.exists() {
            let mut file = tokio::fs::File::open(sub_path).await.unwrap();
            let mut contents = Vec::new();
            file.read_to_end(&mut contents).await.unwrap();
            return (
                [(header::CONTENT_TYPE, "text/vtt")], // Usually better for browsers
                contents,
            ).into_response();
        }
    }
    StatusCode::NOT_FOUND.into_response()
}

async fn stream_media(Path(id): Path<String>, State(state): State<Arc<AppState>>, req: axum::http::Request<Body>) -> Response {
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

async fn scan_all_libraries(state: Arc<AppState>) {
    info!("Starting full media scan...");
    let libraries = sqlx::query_as::<_, Library>("SELECT id, name, path, lib_type FROM libraries").fetch_all(&state.pool).await.unwrap();
    for lib in libraries { scan_library(state.clone(), lib).await; }
}

async fn scan_library(state: Arc<AppState>, lib: Library) {
    info!("Scanning library '{}' at {}...", lib.name, lib.path);
    let movie_regex = Regex::new(r"^(.*)\s\((\d{4})\)$").unwrap();
    let show_regex = Regex::new(r"(?i)^(.*?)\s*S(\d{2})E(\d{2})").unwrap();
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
                let (overview, cast) = fetch_metadata(&state, &title, year, "movie", folder_path).await;

                if let Ok(_) = sqlx::query("INSERT OR IGNORE INTO media_items (id, library_id, title, show_title, file_path, media_type, year, description, \"cast\") VALUES (?, ?, ?, NULL, ?, 'movie', ?, ?, ?)")
                    .bind(uuid::Uuid::new_v4().to_string()).bind(&lib.id).bind(title).bind(&file_path).bind(year).bind(overview).bind(cast).execute(&state.pool).await {
                        count += 1;
                    }
            } else {
                if let Some(caps) = show_regex.captures(file_name) {
                    let show_title = caps[1].trim().to_string();
                    let season = caps[2].parse::<i32>().unwrap_or(0);
                    let episode = caps[3].parse::<i32>().unwrap_or(0);

                    // Fetch metadata and assets for the show if not already done
                    let (overview, cast) = fetch_metadata(&state, &show_title, None, "tv", folder_path).await;

                    if let Ok(_) = sqlx::query("INSERT OR IGNORE INTO media_items (id, library_id, title, show_title, file_path, media_type, season, episode, description, \"cast\") VALUES (?, ?, ?, ?, ?, 'episode', ?, ?, ?, ?)")
                        .bind(uuid::Uuid::new_v4().to_string()).bind(&lib.id).bind(file_name).bind(show_title).bind(&file_path).bind(season).bind(episode).bind(overview).bind(cast).execute(&state.pool).await {
                            count += 1;
                        }
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
