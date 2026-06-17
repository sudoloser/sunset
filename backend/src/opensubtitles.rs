use serde::{Deserialize, Serialize};
use reqwest::Client;
use std::path::Path;

const OPENSUBTITLES_API_URL: &str = "https://api.opensubtitles.com/api/v1";

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct OpenSubtitlesConfig {
    pub enabled: bool,
    pub api_key: Option<String>,
    pub user_agent: Option<String>,
}

#[derive(Serialize, Deserialize)]
struct LoginResponse {
    token: String,
    status: u16,
}

#[derive(Serialize)]
struct LoginRequest {
    #[serde(rename = "Api-Key")]
    api_key: String,
}

#[derive(Serialize)]
struct SubtitleSearchParams {
    tmdb_id: Option<u64>,
    query: Option<String>,
    languages: String,
    #[serde(rename = "type")]
    search_type: Option<String>,
    season_number: Option<i32>,
    episode_number: Option<i32>,
}

#[derive(Deserialize)]
struct SubtitleSearchResponse {
    total_count: Option<u32>,
    data: Vec<SubtitleData>,
}

#[derive(Deserialize)]
struct SubtitleData {
    id: String,
    #[serde(rename = "type")]
    sub_type: String,
    attributes: SubtitleAttributes,
}

#[derive(Deserialize)]
struct SubtitleAttributes {
    language: String,
    subtitle_id: u64,
    download_count: u64,
    files: Vec<SubtitleFile>,
    release: Option<String>,
}

#[derive(Deserialize)]
struct SubtitleFile {
    file_id: u64,
    file_name: Option<String>,
}

#[derive(Serialize)]
struct DownloadRequest {
    file_id: u64,
}

#[derive(Deserialize)]
struct DownloadResponse {
    link: String,
    file_name: Option<String>,
}

/// Check if OpenSubtitles is enabled in server settings
pub async fn is_enabled(pool: &sqlx::SqlitePool) -> bool {
    let row = sqlx::query_as::<_, (bool,)>(
        "SELECT open_subtitles_enabled FROM settings WHERE id = 1"
    )
    .fetch_optional(pool)
    .await
    .ok()
    .flatten();

    match row {
        Some((enabled,)) => enabled,
        None => false,
    }
}

/// Get the OpenSubtitles API config from settings
pub async fn get_config(pool: &sqlx::SqlitePool) -> OpenSubtitlesConfig {
    let row = sqlx::query_as::<_, (bool, Option<String>, Option<String>)>(
        "SELECT open_subtitles_enabled, opensubtitles_api_key, opensubtitles_user_agent FROM settings WHERE id = 1"
    )
    .fetch_optional(pool)
    .await
    .ok()
    .flatten();

    match row {
        Some((enabled, api_key, user_agent)) => OpenSubtitlesConfig {
            enabled,
            api_key,
            user_agent,
        },
        None => OpenSubtitlesConfig {
            enabled: false,
            api_key: None,
            user_agent: None,
        },
    }
}

/// Search for subtitles via OpenSubtitles API
async fn search_subtitles(
    client: &Client,
    api_key: &str,
    user_agent: &str,
    tmdb_id: Option<u64>,
    query: Option<&str>,
    media_type: &str,
    season: Option<i32>,
    episode: Option<i32>,
) -> Result<Vec<SubtitleData>, String> {
    let mut params = SubtitleSearchParams {
        tmdb_id,
        query: query.map(|s| s.to_string()),
        languages: "en".to_string(),
        search_type: Some(media_type.to_string()),
        season_number: season,
        episode_number: episode,
    };

    // If we have a tmdb_id, we don't need the query
    if tmdb_id.is_some() {
        params.query = None;
    }

    let url = format!("{}/subtitles", OPENSUBTITLES_API_URL);

    let response = client
        .post(&url)
        .header("Api-Key", api_key)
        .header("User-Agent", user_agent)
        .header("Content-Type", "application/json")
        .json(&params)
        .send()
        .await
        .map_err(|e| format!("OpenSubtitles search request failed: {}", e))?;

    let status = response.status();
    let body = response.text().await.unwrap_or_default();

    if !status.is_success() {
        return Err(format!("OpenSubtitles search returned {}: {}", status, body));
    }

    let search_response: SubtitleSearchResponse = serde_json::from_str(&body)
        .map_err(|e| format!("Failed to parse OpenSubtitles search response: {}", e))?;

    Ok(search_response.data)
}

/// Get download link for a subtitle file
async fn get_download_link(
    client: &Client,
    api_key: &str,
    user_agent: &str,
    file_id: u64,
) -> Result<(String, Option<String>), String> {
    let url = format!("{}/download", OPENSUBTITLES_API_URL);
    let params = DownloadRequest { file_id };

    let response = client
        .post(&url)
        .header("Api-Key", api_key)
        .header("User-Agent", user_agent)
        .header("Content-Type", "application/json")
        .json(&params)
        .send()
        .await
        .map_err(|e| format!("OpenSubtitles download request failed: {}", e))?;

    let status = response.status();
    let body = response.text().await.unwrap_or_default();

    if !status.is_success() {
        return Err(format!("OpenSubtitles download returned {}: {}", status, body));
    }

    let download_resp: DownloadResponse = serde_json::from_str(&body)
        .map_err(|e| format!("Failed to parse download response: {}", e))?;

    Ok((download_resp.link, download_resp.file_name))
}

/// Download subtitles for a media item
pub async fn download_subtitles_for_item(
    client: &Client,
    pool: &sqlx::SqlitePool,
    item_id: &str,
    title: &str,
    show_title: Option<&str>,
    media_type: &str,
    season: Option<i32>,
    episode: Option<i32>,
    tmdb_id: Option<&str>,
    file_path: &str,
) {
    // Check if OpenSubtitles is enabled
    let config = get_config(pool).await;
    if !config.enabled {
        return;
    }

    let api_key = match config.api_key {
        Some(k) if !k.is_empty() => k,
        _ => return, // No API key configured
    };

    let user_agent = config.user_agent.unwrap_or_else(|| "SunSet v0.2.0".to_string());

    // Parse TMDB ID
    let tmdb_id_num = tmdb_id.and_then(|id| id.parse::<u64>().ok());

    let search_type = if media_type == "movie" { "movie" } else { "episode" };

    // Search for English subtitles
    let subtitles = search_subtitles(
        client,
        &api_key,
        &user_agent,
        tmdb_id_num,
        Some(title),
        search_type,
        season,
        episode,
    )
    .await;

    let subtitles = match subtitles {
        Ok(s) => s,
        Err(e) => {
            tracing::warn!("Failed to search OpenSubtitles for '{}': {}", title, e);
            return;
        }
    };

    if subtitles.is_empty() {
        tracing::debug!("No OpenSubtitles found for '{}'", title);
        return;
    }

    // Download the first subtitle (best match - sorted by relevance)
    if let Some(sub) = subtitles.first() {
        if let Some(file) = sub.attributes.files.first() {
            let download_result = get_download_link(
                client,
                &api_key,
                &user_agent,
                file.file_id,
            )
            .await;

            let (download_url, _) = match download_result {
                Ok(d) => d,
                Err(e) => {
                    tracing::warn!("Failed to get download link for '{}': {}", title, e);
                    return;
                }
            };

            // Download the actual subtitle file
            let subtitle_content = match client.get(&download_url).send().await {
                Ok(resp) => match resp.bytes().await {
                    Ok(bytes) => bytes,
                    Err(e) => {
                        tracing::warn!("Failed to read subtitle content for '{}': {}", title, e);
                        return;
                    }
                },
                Err(e) => {
                    tracing::warn!("Failed to download subtitle for '{}': {}", title, e);
                    return;
                }
            };

            // Determine where to save the subtitle file
            let source_path = Path::new(file_path);
            let parent = match source_path.parent() {
                Some(p) => p,
                None => return,
            };

            let lang = "eng";
            let subtitle_path = if media_type == "movie" {
                // Movies: save next to the video file
                let stem = source_path.file_stem().and_then(|s| s.to_str()).unwrap_or("subtitle");
                parent.join(format!("{}.{}.srt", stem, lang))
            } else {
                // TV Shows: save in Subtitles/Season XX/
                let show = show_title.unwrap_or("Unknown");
                let season_num = season.unwrap_or(1);
                let episode_num = episode.unwrap_or(1);
                let subs_dir = parent.join("Subtitles").join(format!("Season {:02}", season_num));
                if let Err(e) = std::fs::create_dir_all(&subs_dir) {
                    tracing::warn!("Failed to create subtitles directory: {}", e);
                    return;
                }
                subs_dir.join(format!("{} S{:02}E{:02}.{}.srt", show, season_num, episode_num, lang))
            };

            // Write the subtitle file
            match std::fs::write(&subtitle_path, &subtitle_content) {
                Ok(_) => tracing::info!("Downloaded OpenSubtitle for '{}' to {:?}", title, subtitle_path),
                Err(e) => tracing::warn!("Failed to save subtitle for '{}': {}", title, e),
            }
        }
    }
}