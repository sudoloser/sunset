use std::sync::Mutex;
use discord_rich_presence::{DiscordIpc, DiscordIpcClient, activity};
use serde::{Deserialize, Serialize};
use tauri::State;

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

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_shell::init())
        // Window starts hidden (visible: false) to avoid a white flash on boot;
        // show it as soon as the frontend has actually loaded its page.
        .on_page_load(|webview, _payload| {
            let _ = webview.window().show();
        })
        .manage(Mutex::new(DiscordState { client: None }))
        .invoke_handler(tauri::generate_handler![
            start_discord_rpc,
            stop_discord_rpc,
            update_discord_presence,
            is_discord_running,
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
