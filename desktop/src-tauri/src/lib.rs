use std::sync::Mutex;
use discord_rich_presence::{DiscordIpc, DiscordIpcClient, activity};
use serde::{Deserialize, Serialize};
use tauri::{Manager, State};

struct DiscordState {
    client: Option<DiscordIpcClient>,
}

#[derive(Serialize, Deserialize)]
struct DiscordPresence {
    state: String,
    details: String,
    large_image: Option<String>,
    large_text: Option<String>,
    small_image: Option<String>,
    small_text: Option<String>,
    start_timestamp: Option<i64>,
}

#[tauri::command]
fn start_discord_rpc(client_id: &str, state: State<'_, Mutex<DiscordState>>) -> Result<String, String> {
    let mut discord = state.lock().map_err(|e| e.to_string())?;

    if discord.client.is_some() {
        return Err("Discord RPC already running".to_string());
    }

    let mut client = DiscordIpcClient::new(client_id).map_err(|e| e.to_string())?;
    client.connect().map_err(|e| e.to_string())?;

    discord.client = Some(client);
    Ok("Discord RPC connected".to_string())
}

#[tauri::command]
fn stop_discord_rpc(state: State<'_, Mutex<DiscordState>>) -> Result<String, String> {
    let mut discord = state.lock().map_err(|e| e.to_string())?;

    if let Some(client) = &mut discord.client {
        client.close().map_err(|e| e.to_string())?;
    }

    discord.client = None;
    Ok("Discord RPC disconnected".to_string())
}

#[tauri::command]
fn update_discord_presence(
    presence: DiscordPresence,
    state: State<'_, Mutex<DiscordState>>,
) -> Result<String, String> {
    let mut discord = state.lock().map_err(|e| e.to_string())?;

    let client = discord.client.as_mut().ok_or("Discord RPC not connected")?;

    let mut builder = activity::Activity::new()
        .state(&presence.state)
        .details(&presence.details);

    if let Some(ts) = presence.start_timestamp {
        builder = builder.timestamps(activity::Timestamps::new().start(ts));
    }

    if let Some(img) = &presence.large_image {
        builder = builder.assets(
            activity::Assets::new()
                .large_image(img)
                .large_text(presence.large_text.as_deref().unwrap_or("")),
        );
    }

    client.set_activity(builder).map_err(|e| e.to_string())?;

    Ok("Presence updated".to_string())
}

#[tauri::command]
fn is_discord_running(state: State<'_, Mutex<DiscordState>>) -> Result<bool, String> {
    let discord = state.lock().map_err(|e| e.to_string())?;
    Ok(discord.client.is_some())
}

#[tauri::command]
fn app_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}

/// Native window fullscreen. The web fullscreen API is unreliable inside
/// webviews, so the player calls this first and only falls back to it.
#[tauri::command]
fn set_fullscreen(window: tauri::Window, fullscreen: bool) -> Result<(), String> {
    window.set_fullscreen(fullscreen).map_err(|e| e.to_string())
}

fn resolve_mpv(mpv_path: &str) -> Option<std::path::PathBuf> {
    let candidate = std::path::PathBuf::from(mpv_path);
    // Absolute/relative path with a separator: use as-is if executable.
    if mpv_path.contains('/') || mpv_path.contains('\\') {
        return candidate.is_file().then_some(candidate);
    }
    // Bare name: search PATH.
    std::env::var_os("PATH").and_then(|paths| {
        std::env::split_paths(&paths).map(|dir| dir.join(mpv_path)).find(|p| p.is_file())
    })
}

/// Check whether an mpv binary is usable for upscaled playback.
#[tauri::command]
fn mpv_available(mpv_path: String) -> bool {
    let path = mpv_path.trim();
    let path = if path.is_empty() { "mpv" } else { path };
    match resolve_mpv(path) {
        Some(bin) => std::process::Command::new(bin)
            .arg("--version")
            .output()
            .map(|out| out.status.success())
            .unwrap_or(false),
        None => false,
    }
}

/// Elements WebKit needs for in-app video: an MP4 demuxer, an H.264
/// decoder, and audio/video sinks. Reports which are missing so the UI
/// can tell the user exactly what to install.
#[cfg(not(windows))]
#[tauri::command]
fn gstreamer_check() -> Vec<String> {
    // At least one working element per role is enough.
    const REQUIRED: &[(&str, &[&str])] = &[
        ("MP4 demuxer (qtdemux/isomp4)", &["qtdemux", "isomp4"]),
        ("H.264 decoder (avdec_h264/openh264dec)", &["avdec_h264", "openh264dec"]),
        ("audio sink (autoaudiosink)", &["autoaudiosink"]),
        ("app sink (appsink)", &["appsink"]),
    ];
    REQUIRED
        .iter()
        .filter_map(|(label, candidates)| {
            let ok = candidates.iter().any(|el| {
                // Scrub the bundle's LD_LIBRARY_PATH: under it, even the
                // system gst-inspect loads mixed libs and fails, which would
                // report every element as missing (false positive).
                std::process::Command::new("gst-inspect-1.0")
                    .arg(el)
                    .env_remove("LD_LIBRARY_PATH")
                    .output()
                    .map(|out| out.status.success())
                    .unwrap_or(false)
            });
            if ok {
                None
            } else {
                Some(label.to_string())
            }
        })
        .collect()
}

/// GStreamer probing is a Linux/WebKit thing. On Windows the check would
/// just report everything missing (false positive), so skip it. Separate
/// cfg-gated definition (instead of an early return) so the Linux body
/// isn't flagged as unreachable code on Windows.
#[cfg(windows)]
#[tauri::command]
fn gstreamer_check() -> Vec<String> {
    Vec::new()
}

/// Launch mpv as a detached player for upscaled (Anime4K shader) playback.
/// Subtitles and shaders are optional; mpv handles MKV/ASS natively.
#[tauri::command]
fn mpv_play(
    mpv_path: String,
    url: String,
    title: String,
    sub_path: Option<String>,
    shaders: Vec<String>,
) -> Result<String, String> {
    let bin = resolve_mpv(mpv_path.trim()).ok_or("mpv binary not found")?;

    let mut cmd = std::process::Command::new(bin);
    cmd.arg(format!("--title={}", title)).arg(&url);
    if let Some(sub) = sub_path.filter(|s| !s.is_empty()) {
        cmd.arg(format!("--sub-file={}", sub));
    }
    for shader in shaders.iter().filter(|s| !s.is_empty()) {
        cmd.arg(format!("--glsl-shader={}", shader));
    }

    // Orphaned on purpose: closing the desktop app doesn't kill playback.
    cmd.spawn().map_err(|e| e.to_string())?;
    Ok("mpv launched".to_string())
}

struct MpvState {
    sessions: std::collections::HashMap<String, std::path::PathBuf>,
}

/// IPC endpoint mpv listens on: a unix socket file on Unix, a named pipe
/// on Windows (`tokio::net::UnixStream` doesn't exist there).
#[cfg(unix)]
fn mpv_socket_path(id: &str) -> std::path::PathBuf {
    std::env::temp_dir().join(format!("sunset-mpv-{}.sock", id))
}

#[cfg(windows)]
fn mpv_socket_path(id: &str) -> std::path::PathBuf {
    // mpv on Windows serves IPC over a named pipe, not a socket file.
    std::path::PathBuf::from(format!(r"\\.\pipe\sunset-mpv-{}", id))
}

/// Start a controllable mpv session (JSON IPC). Returns a session id the
/// frontend uses for commands, status polls and stop. The mpv window stays
/// separate — true in-window embedding isn't possible on Wayland — but the
/// app stays in charge: pause, seek, tracks, progress sync, autoplay-next.
#[tauri::command]
async fn mpv_start(
    state: State<'_, Mutex<MpvState>>,
    mpv_path: String,
    url: String,
    title: String,
    sub_path: Option<String>,
    shaders: Vec<String>,
) -> Result<String, String> {
    let bin = resolve_mpv(mpv_path.trim()).ok_or("mpv binary not found")?;
    let id = uuid::Uuid::new_v4().to_string();
    let sock = mpv_socket_path(&id);
    let _ = std::fs::remove_file(&sock);

    // Resolve each shader: as given, else by basename under the conventional
    // per-user shader dir (~/.config/mpv/shaders), so a standard Anime4K
    // install works even if the frontend sent a stale custom folder.
    let home = std::env::var("HOME")
        .or_else(|_| std::env::var("USERPROFILE"))
        .unwrap_or_else(|_| std::env::temp_dir().to_string_lossy().to_string());
    let fallback_dir = std::path::PathBuf::from(home).join(".config").join("mpv").join("shaders");
    let shaders: Vec<String> = shaders
        .into_iter()
        .filter(|s| !s.is_empty())
        .map(|s| {
            if std::path::Path::new(&s).is_file() {
                return s;
            }
            let base = std::path::Path::new(&s)
                .file_name()
                .map(|n| n.to_string_lossy().to_string())
                .unwrap_or(s.clone());
            let candidate = fallback_dir.join(&base);
            if candidate.is_file() {
                return candidate.to_string_lossy().to_string();
            }
            s
        })
        .collect();

    let mut cmd = std::process::Command::new(bin);
    cmd.arg(format!("--input-ipc-server={}", sock.display()))
        .arg("--no-terminal")
        .arg("--force-window")
        .arg(format!("--title={}", title))
        .arg(&url);
    if let Some(sub) = sub_path.filter(|s| !s.is_empty()) {
        cmd.arg(format!("--sub-file={}", sub));
    }
    for shader in shaders.iter().filter(|s| !s.is_empty()) {
        cmd.arg(format!("--glsl-shader={}", shader));
    }
    cmd.spawn().map_err(|e| e.to_string())?;

    // Wait for mpv to create the socket (it starts before loading media).
    #[cfg(unix)]
    {
        for _ in 0..30 {
            if sock.exists() {
                break;
            }
            tokio::time::sleep(std::time::Duration::from_millis(100)).await;
        }
        if !sock.exists() {
            return Err("mpv did not start its control socket".to_string());
        }
    }
    #[cfg(windows)]
    {
        // Named pipes don't appear as files, so poll by connecting.
        let mut ready = false;
        for _ in 0..30 {
            if tokio::net::windows::named_pipe::ClientOptions::new()
                .open(&sock)
                .is_ok()
            {
                ready = true;
                break;
            }
            tokio::time::sleep(std::time::Duration::from_millis(100)).await;
        }
        if !ready {
            return Err("mpv did not start its control socket".to_string());
        }
    }

    state
        .lock()
        .map_err(|e| e.to_string())?
        .sessions
        .insert(id.clone(), sock);
    Ok(id)
}

#[cfg(unix)]
async fn mpv_send(sock: &std::path::Path, command: serde_json::Value) -> Result<serde_json::Value, String> {
    use tokio::io::{AsyncBufReadExt, AsyncWriteExt};
    let mut stream = tokio::net::UnixStream::connect(sock)
        .await
        .map_err(|e| e.to_string())?;
    let line = serde_json::to_string(&serde_json::json!({ "command": command }))
        .map_err(|e| e.to_string())?;
    stream
        .write_all(line.as_bytes())
        .await
        .map_err(|e| e.to_string())?;
    stream.write_all(b"\n").await.map_err(|e| e.to_string())?;
    let mut reader = tokio::io::BufReader::new(stream);
    let mut resp = String::new();
    tokio::time::timeout(std::time::Duration::from_secs(5), reader.read_line(&mut resp))
        .await
        .map_err(|_| "mpv did not answer".to_string())?
        .map_err(|e| e.to_string())?;
    let json: serde_json::Value =
        serde_json::from_str(&resp).map_err(|e| e.to_string())?;
    if json.get("error").and_then(|e| e.as_str()) != Some("success") {
        return Err(json
            .get("error")
            .and_then(|e| e.as_str())
            .unwrap_or("mpv command failed")
            .to_string());
    }
    Ok(json.get("data").cloned().unwrap_or(serde_json::Value::Null))
}

#[cfg(windows)]
async fn mpv_send(sock: &std::path::Path, command: serde_json::Value) -> Result<serde_json::Value, String> {
    use tokio::io::{AsyncBufReadExt, AsyncWriteExt};
    let pipe = tokio::net::windows::named_pipe::ClientOptions::new()
        .open(sock)
        .map_err(|e| e.to_string())?;
    let mut reader = tokio::io::BufReader::new(pipe);
    let line = serde_json::to_string(&serde_json::json!({ "command": command }))
        .map_err(|e| e.to_string())?;
    reader
        .write_all(line.as_bytes())
        .await
        .map_err(|e| e.to_string())?;
    reader.write_all(b"\n").await.map_err(|e| e.to_string())?;
    reader.flush().await.map_err(|e| e.to_string())?;
    let mut resp = String::new();
    tokio::time::timeout(std::time::Duration::from_secs(5), reader.read_line(&mut resp))
        .await
        .map_err(|_| "mpv did not answer".to_string())?
        .map_err(|e| e.to_string())?;
    let json: serde_json::Value =
        serde_json::from_str(&resp).map_err(|e| e.to_string())?;
    if json.get("error").and_then(|e| e.as_str()) != Some("success") {
        return Err(json
            .get("error")
            .and_then(|e| e.as_str())
            .unwrap_or("mpv command failed")
            .to_string());
    }
    Ok(json.get("data").cloned().unwrap_or(serde_json::Value::Null))
}

fn mpv_session_socket(
    state: &State<'_, Mutex<MpvState>>,
    id: &str,
) -> Result<std::path::PathBuf, String> {
    state
        .lock()
        .map_err(|e| e.to_string())?
        .sessions
        .get(id)
        .cloned()
        .ok_or_else(|| "mpv session ended".to_string())
}

/// Run one raw mpv command, e.g. ["cycle","pause"] or ["seek",10].
#[tauri::command]
async fn mpv_command(
    state: State<'_, Mutex<MpvState>>,
    id: String,
    command: Vec<serde_json::Value>,
) -> Result<serde_json::Value, String> {
    let sock = mpv_session_socket(&state, &id)?;
    mpv_send(&sock, serde_json::Value::Array(command)).await
}

#[derive(Serialize)]
struct MpvStatus {
    time_pos: Option<f64>,
    duration: Option<f64>,
    paused: bool,
    eof: bool,
}

/// Poll current playback state. Errors only when mpv itself is gone (user
/// quit it). Individual properties may be unavailable while media loads —
/// those come back as None, not errors, so slow streams don't kill the
/// session during startup buffering.
#[tauri::command]
async fn mpv_status(
    state: State<'_, Mutex<MpvState>>,
    id: String,
) -> Result<MpvStatus, String> {
    async fn prop(sock: &std::path::Path, name: &str) -> Option<serde_json::Value> {
        mpv_send(sock, serde_json::json!(["get_property", name]))
            .await
            .ok()
    }

    let sock = mpv_session_socket(&state, &id)?;
    // `pause` always answers while mpv lives: it is the liveness probe.
    let paused = prop(&sock, "pause")
        .await
        .and_then(|v| v.as_bool())
        .ok_or_else(|| "mpv session ended".to_string())?;
    let time_pos = prop(&sock, "time-pos").await.and_then(|v| v.as_f64());
    let duration = prop(&sock, "duration").await.and_then(|v| v.as_f64());
    let eof = prop(&sock, "eof-reached")
        .await
        .and_then(|v| v.as_bool())
        .unwrap_or(false);
    Ok(MpvStatus {
        time_pos,
        duration,
        paused,
        eof,
    })
}

/// Tell mpv to quit and forget the session. Best-effort: also succeeds when
/// mpv already exited on its own.
#[tauri::command]
async fn mpv_stop(state: State<'_, Mutex<MpvState>>, id: String) -> Result<(), String> {
    let sock = mpv_session_socket(&state, &id)?;
    let _ = mpv_send(&sock, serde_json::json!(["quit"])).await;
    state
        .lock()
        .map_err(|e| e.to_string())?
        .sessions
        .remove(&id);
    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| {
            // Focus the existing window when a second copy is launched.
            if let Some(window) = app.get_webview_window("main") {
                let _ = window.set_focus();
            }
        }))
        .plugin(tauri_plugin_store::Builder::new().build())
        .plugin(tauri_plugin_updater::Builder::new().build())
        // Window starts hidden (visible: false) to avoid a white flash on boot;
        // show it as soon as the frontend has actually loaded its page.
        .on_page_load(|webview, _payload| {
            let _ = webview.window().show();
        })
        .manage(Mutex::new(DiscordState { client: None }))
        .manage(Mutex::new(MpvState {
            sessions: std::collections::HashMap::new(),
        }))
        .invoke_handler(tauri::generate_handler![
            start_discord_rpc,
            stop_discord_rpc,
            update_discord_presence,
            is_discord_running,
            app_version,
            mpv_available,
            mpv_play,
            mpv_start,
            mpv_command,
            mpv_status,
            mpv_stop,
            gstreamer_check,
            set_fullscreen,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
