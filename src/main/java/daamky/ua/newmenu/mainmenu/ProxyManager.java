package daamky.ua.newmenu.mainmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import daamky.ua.newmenu.multibot.BotProxyConfig;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class ProxyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ExecutorService PING_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Daamky-ProxyPingWorker");
        t.setDaemon(true);
        return t;
    });
    private static final ProxyManager INSTANCE = new ProxyManager();

    public static final class ProxyItem {
        private String id;
        private BotProxyConfig.Type type;
        private String host;
        private int port;
        private String username;
        private String password;
        private String label;
        private long dateAdded;

        // Runtime state (not saved)
        private transient int pingMs = -1; // -1: untested, -2: error/offline, > 0: latency ms
        private transient String testMessage = "";
        private transient boolean testing = false;

        public ProxyItem(BotProxyConfig.Type type, String host, int port, String username, String password, String label) {
            this.id = UUID.randomUUID().toString();
            this.type = type != null ? type : BotProxyConfig.Type.SOCKS5;
            this.host = host == null ? "" : host.trim();
            this.port = port;
            this.username = username == null ? "" : username.trim();
            this.password = password == null ? "" : password.trim();
            this.label = label == null ? "" : label.trim();
            this.dateAdded = System.currentTimeMillis();
        }

        public String getId() {
            return id;
        }

        public BotProxyConfig.Type getType() {
            return type;
        }

        public void setType(BotProxyConfig.Type type) {
            this.type = type;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public boolean hasAuth() {
            return !username.isBlank() || !password.isBlank();
        }

        public int getPingMs() {
            return pingMs;
        }

        public String getTestMessage() {
            return testMessage;
        }

        public boolean isTesting() {
            return testing;
        }

        public String getDisplayAddress() {
            return host + ":" + port;
        }

        public String getCombinedString() {
            if (hasAuth()) {
                return host + ":" + port + ":" + username + ":" + password;
            }
            return host + ":" + port;
        }

        public String getUriFormat() {
            String proto = type == BotProxyConfig.Type.HTTP ? "http" : "socks5";
            if (hasAuth()) {
                return proto + "://" + username + ":****@" + host + ":" + port;
            }
            return proto + "://" + host + ":" + port;
        }

        public BotProxyConfig toBotProxyConfig() {
            return new BotProxyConfig(type, host, port, username, password, getCombinedString());
        }

        public void testAsync(Consumer<ProxyItem> callback) {
            this.testing = true;
            this.testMessage = "Проверка...";
            PING_EXECUTOR.submit(() -> {
                long start = System.currentTimeMillis();
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 2500);
                    socket.setSoTimeout(2500);
                    long latency = System.currentTimeMillis() - start;
                    this.pingMs = (int) Math.max(1L, latency);
                    this.testMessage = "Доступен (" + this.pingMs + " мс)";
                } catch (Exception e) {
                    this.pingMs = -2;
                    this.testMessage = "Ошибка подключения";
                } finally {
                    this.testing = false;
                    if (callback != null) {
                        callback.accept(this);
                    }
                }
            });
        }
    }

    private boolean proxyEnabled = false;
    private ProxyItem activeProxy = null;
    private final List<ProxyItem> savedProxies = new ArrayList<>();
    private File configFile;

    private ProxyManager() {
        initFile();
        load();
        if (savedProxies.isEmpty()) {
            // Seed sample proxies as examples
            savedProxies.add(new ProxyItem(BotProxyConfig.Type.SOCKS5, "127.0.0.1", 1080, "", "", "Локальный SOCKS5"));
            savedProxies.add(new ProxyItem(BotProxyConfig.Type.HTTP, "127.0.0.1", 8080, "", "", "Локальный HTTP"));
            activeProxy = savedProxies.get(0);
            save();
        }
    }

    public static ProxyManager getInstance() {
        return INSTANCE;
    }

    private void initFile() {
        Minecraft mc = Minecraft.getInstance();
        File baseDir = (mc != null && mc.gameDirectory != null) ? mc.gameDirectory : new File(".");
        File configDir = new File(baseDir, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        configFile = new File(configDir, "daamky_proxies.json");
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public void setProxyEnabled(boolean enabled) {
        this.proxyEnabled = enabled;
        save();
    }

    public ProxyItem getActiveProxy() {
        return activeProxy;
    }

    public void setActiveProxy(ProxyItem proxy) {
        this.activeProxy = proxy;
        save();
    }

    public List<ProxyItem> getSavedProxies() {
        return new ArrayList<>(savedProxies);
    }

    public void addProxy(ProxyItem proxy) {
        if (proxy == null) return;
        savedProxies.removeIf(p -> p.getHost().equalsIgnoreCase(proxy.getHost()) && p.getPort() == proxy.getPort());
        savedProxies.add(0, proxy);
        if (activeProxy == null) {
            activeProxy = proxy;
        }
        save();
    }

    public void removeProxy(ProxyItem proxy) {
        if (proxy == null) return;
        savedProxies.removeIf(p -> p.getId().equals(proxy.getId()));
        if (activeProxy != null && activeProxy.getId().equals(proxy.getId())) {
            activeProxy = savedProxies.isEmpty() ? null : savedProxies.get(0);
        }
        save();
    }

    public ProxyItem parseCombined(String input, BotProxyConfig.Type fallbackType) {
        if (input == null || input.isBlank()) return null;
        String clean = input.trim();
        BotProxyConfig.Type type = fallbackType != null ? fallbackType : BotProxyConfig.Type.SOCKS5;

        if (clean.toLowerCase(Locale.ROOT).startsWith("socks5://")) {
            type = BotProxyConfig.Type.SOCKS5;
            clean = clean.substring(9);
        } else if (clean.toLowerCase(Locale.ROOT).startsWith("socks4://")) {
            type = BotProxyConfig.Type.SOCKS5;
            clean = clean.substring(9);
        } else if (clean.toLowerCase(Locale.ROOT).startsWith("http://")) {
            type = BotProxyConfig.Type.HTTP;
            clean = clean.substring(7);
        } else if (clean.toLowerCase(Locale.ROOT).startsWith("https://")) {
            type = BotProxyConfig.Type.HTTP;
            clean = clean.substring(8);
        }

        // Format 1: user:pass@host:port
        if (clean.contains("@")) {
            int atIndex = clean.lastIndexOf('@');
            String userPass = clean.substring(0, atIndex);
            String hostPort = clean.substring(atIndex + 1);

            String username = "";
            String password = "";
            int colonUser = userPass.indexOf(':');
            if (colonUser >= 0) {
                username = userPass.substring(0, colonUser);
                password = userPass.substring(colonUser + 1);
            } else {
                username = userPass;
            }

            int colonHost = hostPort.lastIndexOf(':');
            if (colonHost > 0) {
                String host = hostPort.substring(0, colonHost);
                try {
                    int port = Integer.parseInt(hostPort.substring(colonHost + 1));
                    return new ProxyItem(type, host, port, username, password, host + ":" + port);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Format 2: host:port:user:pass
        String[] parts = clean.split(":");
        if (parts.length == 4) {
            String host = parts[0];
            try {
                int port = Integer.parseInt(parts[1]);
                String username = parts[2];
                String password = parts[3];
                return new ProxyItem(type, host, port, username, password, host + ":" + port);
            } catch (NumberFormatException ignored) {}
        } else if (parts.length == 2) {
            // Format 3: host:port
            String host = parts[0];
            try {
                int port = Integer.parseInt(parts[1]);
                return new ProxyItem(type, host, port, "", "", host + ":" + port);
            } catch (NumberFormatException ignored) {}
        }

        return null;
    }

    public void load() {
        savedProxies.clear();
        if (configFile == null || !configFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("enabled")) {
                this.proxyEnabled = json.get("enabled").getAsBoolean();
            }

            String activeId = json.has("active_id") ? json.get("active_id").getAsString() : "";

            if (json.has("proxies") && json.get("proxies").isJsonArray()) {
                JsonArray arr = json.getAsJsonArray("proxies");
                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                        String typeStr = obj.has("type") ? obj.get("type").getAsString() : "SOCKS5";
                        BotProxyConfig.Type type = "HTTP".equalsIgnoreCase(typeStr) ? BotProxyConfig.Type.HTTP : BotProxyConfig.Type.SOCKS5;
                        String host = obj.has("host") ? obj.get("host").getAsString() : "";
                        int port = obj.has("port") ? obj.get("port").getAsInt() : 1080;
                        String user = obj.has("username") ? obj.get("username").getAsString() : "";
                        String pass = obj.has("password") ? obj.get("password").getAsString() : "";
                        String label = obj.has("label") ? obj.get("label").getAsString() : host + ":" + port;

                        if (!host.isBlank()) {
                            ProxyItem item = new ProxyItem(type, host, port, user, pass, label);
                            item.id = id;
                            savedProxies.add(item);
                            if (id.equals(activeId)) {
                                this.activeProxy = item;
                            }
                        }
                    }
                }
            }

            if (activeProxy == null && !savedProxies.isEmpty()) {
                activeProxy = savedProxies.get(0);
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        if (configFile == null) initFile();
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            JsonObject json = new JsonObject();
            json.addProperty("enabled", proxyEnabled);
            json.addProperty("active_id", activeProxy != null ? activeProxy.getId() : "");

            JsonArray arr = new JsonArray();
            for (ProxyItem item : savedProxies) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", item.getId());
                obj.addProperty("type", item.getType().name());
                obj.addProperty("host", item.getHost());
                obj.addProperty("port", item.getPort());
                obj.addProperty("username", item.getUsername());
                obj.addProperty("password", item.getPassword());
                obj.addProperty("label", item.getLabel());
                arr.add(obj);
            }
            json.add("proxies", arr);

            GSON.toJson(json, writer);
        } catch (Exception ignored) {
        }
    }
}
