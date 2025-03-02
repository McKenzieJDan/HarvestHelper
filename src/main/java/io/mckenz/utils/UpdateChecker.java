package io.mckenz.utils;

import io.mckenz.HarvestHelper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

/**
 * Utility class for checking for plugin updates.
 */
public class UpdateChecker implements Listener {

    private final HarvestHelper plugin;
    private final int resourceId;
    private final boolean notifyAdmins;
    private boolean updateAvailable = false;
    private String latestVersion = null;

    /**
     * Create a new update checker
     * @param plugin The plugin instance
     * @param resourceId The SpigotMC resource ID
     * @param notifyAdmins Whether to notify admins when they join
     */
    public UpdateChecker(HarvestHelper plugin, int resourceId, boolean notifyAdmins) {
        this.plugin = plugin;
        this.resourceId = resourceId;
        this.notifyAdmins = notifyAdmins;
        
        // Register the join event listener if notifications are enabled
        if (notifyAdmins) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    /**
     * Check for updates
     */
    public void checkForUpdates() {
        if (resourceId == 0) {
            plugin.getLogger().warning("Resource ID is not set. Update checking is disabled.");
            return;
        }
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String currentVersion = plugin.getDescription().getVersion();
                latestVersion = fetchLatestVersion();
                
                if (latestVersion == null) {
                    plugin.getLogger().warning("Failed to check for updates.");
                    return;
                }
                
                // Normalize versions for comparison
                String normalizedCurrent = normalizeVersion(currentVersion);
                String normalizedLatest = normalizeVersion(latestVersion);
                
                // Compare versions using semantic versioning
                if (!versionsEqual(normalizedCurrent, normalizedLatest)) {
                    // Check if the latest version is actually newer
                    String[] currentParts = normalizedCurrent.split("\\.");
                    String[] latestParts = normalizedLatest.split("\\.");
                    
                    boolean isNewer = false;
                    for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
                        int currentPart = Integer.parseInt(currentParts[i]);
                        int latestPart = Integer.parseInt(latestParts[i]);
                        
                        if (latestPart > currentPart) {
                            isNewer = true;
                            break;
                        } else if (latestPart < currentPart) {
                            // Current version is actually newer than "latest"
                            break;
                        }
                    }
                    
                    if (isNewer) {
                        updateAvailable = true;
                        plugin.getLogger().info("A new update is available: v" + latestVersion);
                        plugin.getLogger().info("You are currently running: v" + currentVersion);
                        plugin.getLogger().info("Download the latest version from: https://www.spigotmc.org/resources/" + resourceId);
                    } else {
                        plugin.getLogger().info("You are running the latest version: v" + currentVersion);
                    }
                } else {
                    plugin.getLogger().info("You are running the latest version: v" + currentVersion);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    /**
     * Fetch the latest version from SpigotMC API
     * @return The latest version string or null if the check failed
     */
    private String fetchLatestVersion() throws IOException {
        URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
        
        try (InputStream inputStream = url.openStream();
             Scanner scanner = new Scanner(inputStream)) {
            if (scanner.hasNext()) {
                return scanner.next();
            }
        }
        
        return null;
    }
    
    /**
     * Compare two version strings for equality
     * 
     * @param version1 The first version string
     * @param version2 The second version string
     * @return True if the versions are equal, false otherwise
     */
    private boolean versionsEqual(String version1, String version2) {
        // Simple string comparison after normalization
        return version1.equals(version2);
    }
    
    /**
     * Normalize a version string for comparison
     * @param version The version string to normalize
     * @return The normalized version string
     */
    private String normalizeVersion(String version) {
        // Remove all 'v' prefixes (handles cases like 'vv1.1.0')
        while (version.startsWith("v")) {
            version = version.substring(1);
        }
        
        // Remove any suffixes like -RELEASE, -SNAPSHOT, etc.
        int dashIndex = version.indexOf('-');
        if (dashIndex > 0) {
            version = version.substring(0, dashIndex);
        }
        
        // Trim any whitespace
        version = version.trim();
        
        // Ensure consistent format for comparison
        // For example, convert "1.1" to "1.1.0" if needed
        String[] parts = version.split("\\.");
        if (parts.length == 2) {
            version = version + ".0";
        }
        
        return version;
    }

    /**
     * Check if an update is available
     * @return True if an update is available, false otherwise
     */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
     * Get the latest version
     * @return The latest version string
     */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
     * Notify admins when they join if an update is available
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Only notify players with permission if notifications are enabled
        if (updateAvailable && notifyAdmins && player.hasPermission("harvesthelper.admin")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7[&aHarvestHelper&7] &7A new update is available: &bv" + latestVersion));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7[&aHarvestHelper&7] &7You are currently running: &bv" + plugin.getDescription().getVersion()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    "&7[&aHarvestHelper&7] &7Download it at: &bhttps://www.spigotmc.org/resources/" + resourceId));
            }, 40L); // Delay for 2 seconds after join
        }
    }
} 