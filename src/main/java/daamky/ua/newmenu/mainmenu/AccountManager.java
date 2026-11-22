package daamky.ua.newmenu.mainmenu;

import daamky.ua.mixin.accessor.MinecraftAccessor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public final class AccountManager {
    private static final Random RANDOM = new Random();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM.dd.yy", Locale.US);

    public static String getCurrentDateString() {
        try {
            if (DATE_FORMAT != null) {
                return DATE_FORMAT.format(new Date());
            }
        } catch (Exception ignored) {}
        return "09.26.26";
    }

    private static final AccountManager INSTANCE = new AccountManager();

    public static final class AccountEntry {
        private final String nickname;
        private final String dateAdded;
        private boolean favorite;
        private boolean microsoft;

        public AccountEntry(String nickname, String dateAdded, boolean favorite, boolean microsoft) {
            this.nickname = nickname;
            this.dateAdded = (dateAdded == null || dateAdded.isBlank()) ? getCurrentDateString() : dateAdded;
            this.favorite = favorite;
            this.microsoft = microsoft;
        }

        public String getNickname() {
            return nickname;
        }

        public String getDateAdded() {
            return dateAdded;
        }

        public boolean isFavorite() {
            return favorite;
        }

        public void setFavorite(boolean favorite) {
            this.favorite = favorite;
        }

        public boolean isMicrosoft() {
            return microsoft;
        }

        public void setMicrosoft(boolean microsoft) {
            this.microsoft = microsoft;
        }
    }

    // Extensive lists for diverse, cool, stylish nicknames
    private static final String[] ADJECTIVES = {
        "Dark", "Shadow", "Night", "Ghost", "Cyber", "Frost", "Nova", "Apex", "Toxic", "Silent",
        "Ultra", "Phantom", "Blood", "Storm", "Neon", "Lunar", "Solar", "Astral", "Chaos", "Vortex",
        "Blaze", "Iron", "Steel", "Venom", "Hyper", "Fatal", "Grim", "Savage", "Fallen", "Eternal",
        "Abyssal", "Cosmic", "Rogue", "Crimson", "Arcane", "Lethal", "Vicious", "Ethereal", "Zenith", "Infernal",
        "Brave", "Cold", "Furious", "Swift", "Mystic", "Grave", "Immortal", "Deadly", "Zero", "Prime"
    };

    private static final String[] NOUNS = {
        "Daamky", "Dream", "Demon", "Dragon", "Viper", "Koshka", "Angel", "Killer", "Player", "Hunter",
        "Slayer", "Warrior", "Knight", "Reaper", "Wolf", "Beast", "Titan", "Samurai", "Ninja", "Wizard",
        "Specter", "Ghoul", "Falcon", "Raven", "Cobra", "Phoenix", "Sniper", "Glitch", "Crusher", "Striker",
        "Master", "Legend", "Walker", "Runner", "Blade", "Rider", "Seeker", "Warden", "Lord", "Emperor",
        "Champion", "Phantom", "Overlord", "Godfather", "Soul", "Spirit", "Drifter", "Shadow", "Nemesis", "Valkyrie"
    };

    private static final String[] STYLISH_SHORT = {
        "zxcursed", "shadowfiend", "ghoul", "akuma", "kira", "neox", "k1ra", "v1per",
        "dr4in", "bl4ze", "qu1ck", "fl1ck", "crybaby", "voidwalker", "deadinside", "sadboy",
        "hyp3r", "f0x", "sn0w", "d3mon", "fr0st", "n1ght", "sh4dow", "c0ld", "zer0", "ap3x"
    };

    private static final String[] SUFFIXES = {
        "PvP", "God", "Dev", "Pro", "King", "Lord", "X", "YT", "TOP", "Elite",
        "Boss", "Hero", "Master", "Star", "Prime", "1337", "777", "99", "666", "Squad",
        "Main", "Ez", "WTF", "GG", "FPS", "Godlike", "Sensei", "King", "Official"
    };

    private final List<AccountEntry> accountEntries = new ArrayList<>();
    private String activeAccount = "";
    private File accountsFile;

    private AccountManager() {
        initFile();
        load();
    }

    public static AccountManager getInstance() {
        return INSTANCE;
    }

    private void initFile() {
        MinecraftClient mc = MinecraftClient.getInstance();
        File baseDir = (mc != null && mc.runDirectory != null) ? mc.runDirectory : new File(".");
        File configDir = new File(baseDir, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        accountsFile = new File(configDir, "daamky_accounts.json");
    }

    public List<String> getAccounts() {
        sortAccounts();
        List<String> list = new ArrayList<>();
        for (AccountEntry e : accountEntries) {
            list.add(e.getNickname());
        }
        return list;
    }

    public List<AccountEntry> getAccountEntries() {
        sortAccounts();
        return new ArrayList<>(accountEntries);
    }

    public boolean isFavorite(String nickname) {
        if (nickname == null) return false;
        String clean = nickname.trim();
        for (AccountEntry e : accountEntries) {
            if (e.getNickname().equalsIgnoreCase(clean)) {
                return e.isFavorite();
            }
        }
        return false;
    }

    public void toggleFavorite(String nickname) {
        if (nickname == null || nickname.trim().isBlank()) return;
        String clean = nickname.trim();
        for (AccountEntry e : accountEntries) {
            if (e.getNickname().equalsIgnoreCase(clean)) {
                e.setFavorite(!e.isFavorite());
                break;
            }
        }
        sortAccounts();
        save();
    }

    private void sortAccounts() {
        accountEntries.sort((first, second) -> {
            if (first.isFavorite() != second.isFavorite()) {
                return first.isFavorite() ? -1 : 1;
            }
            return first.getNickname().compareToIgnoreCase(second.getNickname());
        });
    }

    public String getActiveAccount() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getSession() != null) {
            String current = mc.getSession().getUsername();
            if (current != null && !current.isBlank()) {
                activeAccount = current;
                boolean found = false;
                for (AccountEntry e : accountEntries) {
                    if (e.getNickname().equalsIgnoreCase(current)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    accountEntries.add(0, new AccountEntry(current, DATE_FORMAT.format(new Date()), false, false));
                    sortAccounts();
                    save();
                }
            }
        }
        return activeAccount.isBlank() ? "Player" : activeAccount;
    }

    public boolean addAccount(String nickname) {
        return addAccount(nickname, false);
    }

    public boolean addAccount(String nickname, boolean isMicrosoft) {
        if (nickname == null || nickname.trim().isBlank()) return false;
        String clean = nickname.trim();
        if (clean.length() > 16) clean = clean.substring(0, 16);
        for (AccountEntry e : accountEntries) {
            if (e.getNickname().equalsIgnoreCase(clean)) {
                return false;
            }
        }
        accountEntries.add(0, new AccountEntry(clean, DATE_FORMAT.format(new Date()), false, isMicrosoft));
        sortAccounts();
        save();
        return true;
    }

    public void removeAccount(String nickname) {
        if (nickname == null) return;
        String clean = nickname.trim();
        accountEntries.removeIf(e -> e.getNickname().equalsIgnoreCase(clean));
        if (activeAccount.equalsIgnoreCase(clean)) {
            if (!accountEntries.isEmpty()) {
                switchAccount(accountEntries.get(0).getNickname());
            }
        }
        sortAccounts();
        save();
    }

    public void removeAccounts(Collection<String> nicknames) {
        if (nicknames == null || nicknames.isEmpty()) return;
        accountEntries.removeIf(e -> nicknames.contains(e.getNickname()));
        if (nicknames.contains(activeAccount)) {
            if (!accountEntries.isEmpty()) {
                switchAccount(accountEntries.get(0).getNickname());
            }
        }
        sortAccounts();
        save();
    }

    public boolean switchAccount(String nickname) {
        if (nickname == null || nickname.trim().isBlank()) return false;
        String clean = nickname.trim();
        if (clean.length() > 16) clean = clean.substring(0, 16);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return false;

        try {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + clean).getBytes(StandardCharsets.UTF_8));
            Session newSession = new Session(
                clean,
                uuid,
                "",
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.MOJANG
            );
            ((MinecraftAccessor) mc).daamky$setUser(newSession);
            activeAccount = clean;

            boolean found = false;
            for (AccountEntry e : accountEntries) {
                if (e.getNickname().equalsIgnoreCase(clean)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                accountEntries.add(0, new AccountEntry(clean, DATE_FORMAT.format(new Date()), false, false));
            }
            sortAccounts();
            save();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public String generateRandomNickname() {
        int style = RANDOM.nextInt(9);
        String name;

        String adj = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        String suff = SUFFIXES[RANDOM.nextInt(SUFFIXES.length)];

        switch (style) {
            case 0 -> name = adj + "_" + noun;
            case 1 -> name = adj + noun;
            case 2 -> name = "x_" + noun + "_x";
            case 3 -> name = noun + "_" + suff;
            case 4 -> name = adj + "_" + (RANDOM.nextInt(900) + 100);
            case 5 -> name = noun + (RANDOM.nextInt(900) + 100);
            case 6 -> name = STYLISH_SHORT[RANDOM.nextInt(STYLISH_SHORT.length)] + (RANDOM.nextBoolean() ? "_" + suff : "" + (RANDOM.nextInt(90) + 10));
            case 7 -> name = "i_" + noun;
            default -> name = adj + "_" + suff;
        }

        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        return name;
    }

    public void load() {
        accountEntries.clear();
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getSession() != null) {
                activeAccount = mc.getSession().getUsername();
                if (activeAccount != null && !activeAccount.isBlank()) {
                    accountEntries.add(new AccountEntry(activeAccount, getCurrentDateString(), false, false));
                }
            }
        } catch (Throwable ignored) {}

        if (accountsFile == null || !accountsFile.exists()) {
            return;
        }

        try (FileReader reader = new FileReader(accountsFile, StandardCharsets.UTF_8)) {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("active")) {
                String savedActive = json.get("active").getAsString();
                if (savedActive != null && !savedActive.isBlank()) {
                    switchAccount(savedActive);
                }
            }

            Set<String> favorites = new HashSet<>();
            if (json.has("favorites") && json.get("favorites").isJsonArray()) {
                for (com.google.gson.JsonElement el : json.getAsJsonArray("favorites")) {
                    favorites.add(el.getAsString());
                }
            }

            if (json.has("detailed_accounts") && json.get("detailed_accounts").isJsonArray()) {
                com.google.gson.JsonArray arr = json.getAsJsonArray("detailed_accounts");
                for (com.google.gson.JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        String name = obj.has("name") ? obj.get("name").getAsString() : "";
                        String date = obj.has("date") ? obj.get("date").getAsString() : getCurrentDateString();
                        boolean fav = obj.has("favorite") ? obj.get("favorite").getAsBoolean() : favorites.contains(name);
                        boolean ms = obj.has("microsoft") && obj.get("microsoft").getAsBoolean();
                        if (!name.isBlank()) {
                            accountEntries.removeIf(e -> e.getNickname().equalsIgnoreCase(name));
                            accountEntries.add(new AccountEntry(name, date, fav, ms));
                        }
                    }
                }
            } else if (json.has("accounts") && json.get("accounts").isJsonArray()) {
                com.google.gson.JsonArray arr = json.getAsJsonArray("accounts");
                for (com.google.gson.JsonElement el : arr) {
                    String name = el.getAsString();
                    if (name != null && !name.isBlank()) {
                        boolean fav = favorites.contains(name);
                        boolean exists = false;
                        for (AccountEntry e : accountEntries) {
                            if (e.getNickname().equalsIgnoreCase(name)) {
                                exists = true;
                                if (fav) e.setFavorite(true);
                                break;
                            }
                        }
                        if (!exists) {
                            accountEntries.add(new AccountEntry(name, getCurrentDateString(), fav, false));
                        }
                    }
                }
            }
            sortAccounts();
        } catch (Exception ignored) {
        }
    }

    public void save() {
        if (accountsFile == null) initFile();
        try (FileWriter writer = new FileWriter(accountsFile, StandardCharsets.UTF_8)) {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("active", activeAccount);

            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            com.google.gson.JsonArray detailedArr = new com.google.gson.JsonArray();
            com.google.gson.JsonArray favoritesArr = new com.google.gson.JsonArray();

            for (AccountEntry e : accountEntries) {
                arr.add(e.getNickname());
                if (e.isFavorite()) {
                    favoritesArr.add(e.getNickname());
                }

                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("name", e.getNickname());
                obj.addProperty("date", e.getDateAdded());
                obj.addProperty("favorite", e.isFavorite());
                obj.addProperty("microsoft", e.isMicrosoft());
                detailedArr.add(obj);
            }

            json.add("accounts", arr);
            json.add("detailed_accounts", detailedArr);
            json.add("favorites", favoritesArr);

            new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
        } catch (Exception ignored) {
        }
    }
}
