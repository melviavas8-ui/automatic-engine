package daamky.ua.utils.repository.way;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.config.ConfigManager;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.module.impl.visual.esp.EspGeometry;
import daamky.ua.utils.repository.RepositoryStorage;

public final class WayRepository {
   private static final WayRepository INSTANCE = new WayRepository();
   private final List<Way> wayList = new ArrayList<>();
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private final WayRenderer renderer = new WayRenderer();

   private WayRepository() {
   }

   public static WayRepository getInstance() {
      return INSTANCE;
   }

   public WayRenderer renderer() {
      return this.renderer;
   }

   public void load() {
      this.wayList.clear();

      for (JsonElement element : this.readWayArray()) {
         if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String name = object.has("name") ? object.get("name").getAsString() : "";
            String server = object.has("server") ? object.get("server").getAsString() : "";
            int x = object.has("x") ? object.get("x").getAsInt() : 0;
            int y = object.has("y") ? object.get("y").getAsInt() : 0;
            int z = object.has("z") ? object.get("z").getAsInt() : 0;
            if (!name.isBlank()) {
               this.wayList.add(new Way(name, new BlockPos(x, y, z), normalizeServer(server)));
            }
         }
      }
   }

   public void save() {
      JsonObject root = new JsonObject();
      JsonArray array = new JsonArray();

      for (Way way : this.wayList) {
         JsonObject object = new JsonObject();
         object.addProperty("name", way.name());
         object.addProperty("server", way.server());
         object.addProperty("x", way.pos().getX());
         object.addProperty("y", way.pos().getY());
         object.addProperty("z", way.pos().getZ());
         array.add(object);
      }

      root.add("ways", array);
      RepositoryStorage.write("ways.daamky", root);
   }

   private JsonArray readWayArray() {
      JsonObject root = RepositoryStorage.readObject("ways.daamky");
      if (root.has("ways") && root.get("ways").isJsonArray()) {
         return root.getAsJsonArray("ways");
      } else {
         JsonArray legacy = this.readLegacyWayArray();
         return legacy == null ? new JsonArray() : legacy;
      }
   }

   private JsonArray readLegacyWayArray() {
      for (Path file : this.legacyWayFiles()) {
         if (file != null && Files.exists(file)) {
            try {
               JsonArray var10;
               try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                  JsonElement element = JsonParser.parseReader(reader);
                  if (element == null || !element.isJsonArray()) {
                     if (element != null && element.isJsonObject()) {
                        JsonObject object = element.getAsJsonObject();
                        if (object.has("ways") && object.get("ways").isJsonArray()) {
                           return object.getAsJsonArray("ways");
                        }
                     }
                     continue;
                  }

                  var10 = element.getAsJsonArray();
               }

               return var10;
            } catch (Exception var9) {
            }
         }
      }

      return null;
   }

   private List<Path> legacyWayFiles() {
      List<Path> files = new ArrayList<>();
      files.add(RepositoryStorage.root().resolve("waypoints.daamky"));
      files.add(RepositoryStorage.root().resolve("ways.daamky"));
      files.add(ConfigManager.legacyDirectory().resolve("waypoints.daamky"));
      Path gameDir = ConfigManager.rootDirectory().getParent();
      if (gameDir != null) {
         files.add(gameDir.resolve("daamky").resolve("configs").resolve("defaults").resolve("waypoints.daamky"));
      }

      return files;
   }

   public boolean isEmpty() {
      return this.wayList.isEmpty();
   }

   public void addWay(String name, BlockPos pos, String server) {
      this.wayList.add(new Way(name, pos, normalizeServer(server)));
   }

   public void addWayAndSave(String name, BlockPos pos, String server) {
      this.addWay(name, pos, server);
      this.save();
   }

   public boolean hasWay(String name) {
      return this.hasWay(name, this.getCurrentServer());
   }

   public boolean hasWay(String name, String server) {
      return this.wayList.stream().anyMatch(way -> this.isSameWay(way, name, server));
   }

   public Optional<Way> getWay(String name) {
      return this.getWay(name, this.getCurrentServer());
   }

   public Optional<Way> getWay(String name, String server) {
      return this.wayList.stream().filter(way -> this.isSameWay(way, name, server)).findFirst();
   }

   public void deleteWay(String name) {
      this.deleteWay(name, this.getCurrentServer());
   }

   public void deleteWay(String name, String server) {
      this.wayList.removeIf(way -> this.isSameWay(way, name, server));
   }

   public void deleteWayAndSave(String name) {
      this.deleteWay(name);
      this.save();
   }

   public void deleteWayAndSave(String name, String server) {
      this.deleteWay(name, server);
      this.save();
   }

   public void clearList() {
      this.wayList.clear();
   }

   public void clearListAndSave() {
      this.clearList();
      this.save();
   }

   public int size() {
      return this.wayList.size();
   }

   public List<Way> getWayList() {
      return this.wayList;
   }

   public List<String> getWayNames() {
      return this.wayList.stream().map(Way::name).collect(Collectors.toList());
   }

   public List<String> getWayNamesForServer(String server) {
      return this.wayList.stream().filter(way -> this.isSameServer(way, server)).map(Way::name).collect(Collectors.toList());
   }

   public List<Way> getWaysForServer(String server) {
      return this.wayList.stream().filter(way -> this.isSameServer(way, server)).collect(Collectors.toList());
   }

   public int clearServer(String server) {
      int before = this.wayList.size();
      this.wayList.removeIf(way -> this.isSameServer(way, server));
      return before - this.wayList.size();
   }

   public int clearServerAndSave(String server) {
      int removed = this.clearServer(server);
      this.save();
      return removed;
   }

   public void setWays(List<Way> ways) {
      this.wayList.clear();
      if (ways != null) {
         this.wayList.addAll(ways);
      }
   }

   public String getCurrentServer() {
      return this.mc.getNetworkHandler() != null && this.mc.getNetworkHandler().getServerInfo() != null
         ? normalizeServer(this.mc.getNetworkHandler().getServerInfo().address)
         : "";
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (this.mc.player != null && this.mc.world != null && this.mc.gameRenderer != null && this.mc.getWindow() != null) {
         String server = this.getCurrentServer();
         if (!server.isBlank() && !this.wayList.isEmpty()) {
            EspGeometry.ProjectionContext context = EspGeometry.createProjectionContext(this.mc, event.getPartialTicks());
            if (context != null) {
               Vec3d playerPos = this.mc.player.getLerpedPos(event.getPartialTicks());
               this.renderer.beginFrame();

               for (Way way : this.getWaysForServer(server)) {
                  Vec3d worldPos = way.pos().toCenterPos();
                  if (this.isInFrontOfCamera(worldPos)) {
                     EspGeometry.ScreenPoint screen = EspGeometry.projectWorldToScreenSpace(worldPos, context);
                     if (screen != null) {
                        this.renderer.queue(this.mc, way, screen, playerPos.distanceTo(worldPos));
                     }
                  }
               }

               this.renderer.renderQueued(event.getGraphics());
            }
         }
      }
   }

   private boolean isInFrontOfCamera(Vec3d worldPos) {
      Camera camera = this.mc.gameRenderer.getCamera();
      if (camera != null && camera.isReady()) {
         Vec3d cameraPos = camera.getCameraPos();
         Vec3d toPoint = worldPos.subtract(cameraPos);
         double yaw = Math.toRadians((double)camera.getYaw());
         double pitch = Math.toRadians((double)camera.getPitch());
         double cosPitch = Math.cos(pitch);
         Vec3d look = new Vec3d(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
         return look.dotProduct(toPoint) > 0.0;
      } else {
         return false;
      }
   }

   private boolean isSameWay(Way way, String name, String server) {
      return way.name().equalsIgnoreCase(name) && this.isSameServer(way, server);
   }

   private boolean isSameServer(Way way, String server) {
      return normalizeServer(way.server()).equals(normalizeServer(server));
   }

   private static String normalizeServer(String server) {
      return server == null ? "" : server.trim().toLowerCase(Locale.ROOT);
   }
}
