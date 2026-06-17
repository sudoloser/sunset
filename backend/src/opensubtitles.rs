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
    data: Vec<SubtitleData>,
}

#[derive(Deserialize)]
struct SubtitleData {
    attributes: SubtitleAttributes,
}

#[derive(Deserialize)]
struct SubtitleAttributes {
    download_count: u64,
    files: Vec<SubtitleFile>,
}

#[derive(Deserialize)]
struct SubtitleFile {
    file_id: u64,
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

pub async fn update_config(pool: &sqlx::SqlitePool, config: &OpenSubtitlesConfig) -> Result<(), sqlx::Error> {
    sqlx::query(
        "UPDATE settings SET open_subtitles_enabled = ?, opensubtitles_api_key = ?, opensubtitles_user_agent = ? WHERE id = 1"
    )
    .bind(config.enabled)
    .bind(&config.api_key)
    .bind(&config.user_agent)
    .execute(pool)
    .await?;
    Ok(())
}

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
    let params = SubtitleSearchParams {
        tmdb_id,
        query: if tmdb_id.is_some() { None } else { query.map(|s| s.to_string()) },
        languages: "en".to_string(),
        search_type: Some(media_type.to_string()),
        season_number: season,
        episode_number: episode,
    };

    let response = client
        .post(format!("{}/subtitles", OPENSUBTITLES_API_URL))
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

    // Sort by download_count descending and return
    let mut data = search_response.data;
    data.sort_by(|a, b| b.attributes.download_count.cmp(&a.attributes.download_count));
    Ok(data)
}

async fn get_download_link(
    client: &Client,
    api_key: &str,
    user_agent: &str,
    file_id: u64,
) -> Result<(String, Option<String>), String> {
    let params = DownloadRequest { file_id };

    let response = client
        .post(format!("{}/download", OPENSUBTITLES_API_URL))
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

    serde_json::from_str::<DownloadResponse>(&body)
        .map(|r| (r.link, r.file_name))
        .map_err(|e| format!("Failed to parse download response: {}", e))
}

pub async fn download_subtitles_for_item(
    client: &Client,
    pool: &sqlx::SqlitePool,
    _item_id: &str,
    title: &str,
    show_title: Option<&str>,
    media_type: &str,
    season: Option<i32>,
    episode: Option<i32>,
    tmdb_id: Option<&str>,
    file_path: &str,
) {
    let config = get_config(pool).await;
    if !config.enabled {
        return;
    }

    let api_key = match config.api_key {
        Some(ref k) if !k.is_empty() => k.clone(),
        _ => return,
    };

    let user_agent = config.user_agent.unwrap_or_else(|| "SunSet v0.2.0".to_string());

    let tmdb_id_num = tmdb_id.and_then(|id| id.parse::<u64>().ok());
    let search_type = if media_type == "movie" { "movie" } else { "episode" };

    let subtitles = match search_subtitles(
        client, &api_key, &user_agent, tmdb_id_num, Some(title), search_type, season, episode,
    )
    .await
    {
        Ok(s) => s,
        Err(e) => {
            tracing::warn!("Failed to search OpenSubtitles for '{}': {}", title, e);
            return;
        }
    };

    let sub = match subtitles.first() {
        Some(s) => s,
        None => {
            tracing::debug!("No OpenSubtitles found for '{}'", title);
            return;
        }
    };

    let file = match sub.attributes.files.first() {
        Some(f) => f,
        None => return,
    };

    let (download_url, _) = match get_download_link(client, &api_key, &user_agent, file.file_id).await {
        Ok(d) => d,
        Err(e) => {
            tracing::warn!("Failed to get download link for '{}': {}", title, e);
            return;
        }
    };

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

    let source_path = Path::new(file_path);
    let parent = match source_path.parent() {
        Some(p) => p,
        None => return,
    };
    let stem = source_path.file_stem().and_then(|s| s.to_str()).unwrap_or("subtitle");

    let subtitle_path = if media_type == "movie" {
        parent.join(format!("{}.eng.srt", stem))
    } else {
        let show = show_title.unwrap_or("Unknown");
        let season_num = season.unwrap_or(1);
        let episode_num = episode.unwrap_or(1);
        let subs_dir = parent.join("Subtitles").join(format!("Season {:02}", season_num));
        if let Err(e) = std::fs::create_dir_all(&subs_dir) {
            tracing::warn!("Failed to create subtitles directory: {}", e);
            return;
        }
        subs_dir.join(format!("{} S{:02}E{:02}.eng.srt", show, season_num, episode_num))
    };

    match std::fs::write(&subtitle_path, &subtitle_content) {
        Ok(_) => tracing::info!("Downloaded OpenSubtitle for '{}' to {:?}", title, subtitle_path),
        Err(e) => tracing::warn!("Failed to save subtitle for '{}': {}", title, e),
    }
}
