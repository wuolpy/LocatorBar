package com.wuolp.updater;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wuolp.LocatorBar;
import com.wuolp.managers.ConfigManager;
import com.wuolp.utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class UpdateManager {

    private final LocatorBar plugin;
    private final ConfigManager configManager;
    private BukkitTask checkerTask;

    public UpdateManager(LocatorBar plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        if (!configManager.isUpdaterEnabled()) {
            return;
        }

        int intervalSeconds = configManager.getUpdaterCheckInterval();
        if (intervalSeconds > 0) {
            long ticks = intervalSeconds * 20L;
            this.checkerTask = new BukkitRunnable() {
                @Override
                public void run() {
                    checkForUpdates(result -> {
                        if (result.isUpdateAvailable()) {
                            plugin.getLogger().info("A new version of LocatorBar is available: v" + result.getLatestVersion() + " (current: v" + result.getCurrentVersion() + ")");
                            if (configManager.isUpdaterAutoDownload() && configManager.isUpdaterAutoReload()) {
                                plugin.getLogger().info("Auto-download and auto-reload enabled. Applying hot-update now...");
                                downloadAndApply(result, null);
                            } else {
                                notifyAdmins(result);
                            }
                        }
                    });
                }
            }.runTaskTimer(plugin, 100L, ticks); // Run initially after 5 seconds, then every interval
        }
    }

    public void shutdown() {
        if (checkerTask != null) {
            checkerTask.cancel();
            checkerTask = null;
        }
    }

    public void checkForUpdates(Consumer<UpdateResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String currentVersion = plugin.getDescription().getVersion();
            String mode = configManager.getUpdaterMode();

            try {
                if ("github".equalsIgnoreCase(mode)) {
                    String repo = configManager.getUpdaterGithubRepo();
                    if (repo == null || repo.trim().isEmpty() || repo.contains("your-username")) {
                        dispatch(callback, new UpdateResult(false, currentVersion, currentVersion, null, "GitHub repository not configured."));
                        return;
                    }

                    String apiUrl = "https://api.github.com/repos/" + repo.trim() + "/releases/latest";
                    String jsonResponse = sendHttpGet(apiUrl);

                    JsonObject json = JsonParser.parseString(jsonResponse).getAsJsonObject();
                    if (!json.has("tag_name")) {
                        dispatch(callback, new UpdateResult(false, currentVersion, currentVersion, null, "No releases found on GitHub."));
                        return;
                    }

                    String tagName = json.get("tag_name").getAsString();
                    String remoteVersion = tagName.replaceFirst("^[vV]", "").trim();

                    String downloadUrl = null;
                    if (json.has("assets") && json.get("assets").isJsonArray()) {
                        JsonArray assets = json.getAsJsonArray("assets");
                        for (JsonElement assetElem : assets) {
                            if (assetElem.isJsonObject()) {
                                JsonObject asset = assetElem.getAsJsonObject();
                                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                                if (name.endsWith(".jar") && asset.has("browser_download_url")) {
                                    downloadUrl = asset.get("browser_download_url").getAsString();
                                    break;
                                }
                            }
                        }
                    }

                    boolean isNewer = isNewerVersion(currentVersion, remoteVersion);
                    dispatch(callback, new UpdateResult(isNewer, currentVersion, remoteVersion, downloadUrl, isNewer ? "Update available!" : "Plugin is up to date."));

                } else if ("direct".equalsIgnoreCase(mode)) {
                    String directUrl = configManager.getUpdaterDirectUrl();
                    if (directUrl == null || directUrl.trim().isEmpty()) {
                        dispatch(callback, new UpdateResult(false, currentVersion, currentVersion, null, "Direct URL not configured."));
                        return;
                    }

                    if (directUrl.endsWith(".jar")) {
                        // Direct JAR link
                        dispatch(callback, new UpdateResult(true, currentVersion, "latest", directUrl, "Direct JAR update available."));
                    } else {
                        // Assume JSON manifest: {"version":"1.0.1", "download_url":"..."}
                        String response = sendHttpGet(directUrl);
                        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                        String remoteVersion = json.has("version") ? json.get("version").getAsString() : "unknown";
                        String downloadUrl = json.has("download_url") ? json.get("download_url").getAsString() : null;
                        boolean isNewer = isNewerVersion(currentVersion, remoteVersion);
                        dispatch(callback, new UpdateResult(isNewer, currentVersion, remoteVersion, downloadUrl, isNewer ? "Update available!" : "Plugin is up to date."));
                    }
                } else {
                    dispatch(callback, new UpdateResult(false, currentVersion, currentVersion, null, "Unknown updater mode: " + mode));
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
                dispatch(callback, new UpdateResult(false, currentVersion, currentVersion, null, "Check error: " + e.getMessage()));
            }
        });
    }

    public void downloadAndApply(UpdateResult result, CommandSender sender) {
        if (result.getDownloadUrl() == null || result.getDownloadUrl().isEmpty()) {
            if (sender != null) {
                Utils.sendMessage(sender, configManager.getPrefix() + "&cNo download URL available for this update.");
            }
            return;
        }

        if (sender != null) {
            Utils.sendMessage(sender, configManager.getPrefix() + "&eDownloading update &6v" + result.getLatestVersion() + "&e...");
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            File updateFile = new File(pluginsFolder, "LocatorBar-update.jar");

            try {
                downloadFileWithRedirects(result.getDownloadUrl(), updateFile);

                if (sender != null) {
                    Utils.sendMessage(sender, configManager.getPrefix() + "&aDownload complete! Applying hot-update without restarting the server...");
                }

                // Hot-reload into server on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> PluginReloader.reloadPlugin(plugin, updateFile, sender));

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to download update: " + e.getMessage());
                if (sender != null) {
                    Utils.sendMessage(sender, configManager.getPrefix() + "&cFailed to download update: &e" + e.getMessage());
                }
            }
        });
    }

    private void notifyAdmins(UpdateResult result) {
        String msg = configManager.getPrefix() + "&eA new update is available: &6v" + result.getLatestVersion() +
                     " &7(Current: &ev" + result.getCurrentVersion() + "&7). Run &b/lb update download &7to apply without restarting!";
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("locatorbar.admin")) {
                Utils.sendMessage(p, msg);
            }
        }
    }

    private void dispatch(Consumer<UpdateResult> callback, UpdateResult result) {
        if (callback != null) {
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
        }
    }

    private String sendHttpGet(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "LocatorBar-Plugin-Updater");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP error code: " + code);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private void downloadFileWithRedirects(String urlString, File destination) throws Exception {
        String currentUrl = urlString;
        int redirects = 0;

        while (redirects < 6) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "LocatorBar-Plugin-Updater");
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER || status == 307 || status == 308) {
                currentUrl = conn.getHeaderField("Location");
                redirects++;
                continue;
            }

            if (status != HttpURLConnection.HTTP_OK) {
                throw new Exception("Download failed with HTTP code " + status);
            }

            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return;
        }

        throw new Exception("Too many redirects while downloading update.");
    }

    public static boolean isNewerVersion(String current, String remote) {
        if (current == null || remote == null) {
            return false;
        }
        current = current.trim().replaceFirst("^[vV]", "");
        remote = remote.trim().replaceFirst("^[vV]", "");
        if (current.equalsIgnoreCase(remote)) {
            return false;
        }

        String[] curParts = current.split("[-._]");
        String[] remParts = remote.split("[-._]");
        int maxLen = Math.max(curParts.length, remParts.length);

        for (int i = 0; i < maxLen; i++) {
            int curNum = 0;
            int remNum = 0;
            if (i < curParts.length) {
                try {
                    curNum = Integer.parseInt(curParts[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (i < remParts.length) {
                try {
                    remNum = Integer.parseInt(remParts[i]);
                } catch (NumberFormatException ignored) {}
            }
            if (remNum > curNum) {
                return true;
            }
            if (remNum < curNum) {
                return false;
            }
        }
        return false;
    }

    public static class UpdateResult {
        private final boolean updateAvailable;
        private final String currentVersion;
        private final String latestVersion;
        private final String downloadUrl;
        private final String message;

        public UpdateResult(boolean updateAvailable, String currentVersion, String latestVersion, String downloadUrl, String message) {
            this.updateAvailable = updateAvailable;
            this.currentVersion = currentVersion;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
            this.message = message;
        }

        public boolean isUpdateAvailable() {
            return updateAvailable;
        }

        public String getCurrentVersion() {
            return currentVersion;
        }

        public String getLatestVersion() {
            return latestVersion;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public String getMessage() {
            return message;
        }
    }
}
