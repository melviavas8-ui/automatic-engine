package daamky.ua.api.settings.bind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Locale;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;

public final class KeyBind {
   public static final KeyBind NONE = new KeyBind(InputType.NONE, -1);
   private final InputType type;
   private final int code;

   private KeyBind(InputType type, int code) {
      this.type = type == null ? InputType.NONE : type;
      this.code = code;
   }

   public static KeyBind keyboard(int key) {
      return key < 0 ? NONE : new KeyBind(InputType.KEYBOARD, key);
   }

   public static KeyBind mouse(int button) {
      return button < 0 ? NONE : new KeyBind(InputType.MOUSE, button);
   }

   public static KeyBind of(InputType type, int code) {
      return switch (type == null ? InputType.NONE : type) {
         case KEYBOARD -> keyboard(code);
         case MOUSE -> mouse(code);
         case NONE -> NONE;
      };
   }

   public InputType getType() {
      return this.type;
   }

   public int getCode() {
      return this.code;
   }

   public boolean isBound() {
      return this.type != InputType.NONE && this.code >= 0;
   }

   public boolean isDown(long windowHandle) {
      return switch (this.type) {
         case KEYBOARD -> GLFW.glfwGetKey(windowHandle, this.code) == 1;
         case MOUSE -> GLFW.glfwGetMouseButton(windowHandle, this.code) == 1;
         case NONE -> false;
      };
   }

   public String getDisplayName() {
      if (!this.isBound()) {
         return "None";
      } else if (this.type == InputType.MOUSE) {
         return switch (this.code) {
            case 0 -> "MOUSE 1";
            case 1 -> "MOUSE 2";
            case 2 -> "MOUSE 3";
            default -> "MOUSE " + (this.code + 1);
         };
      } else {
         return switch (this.code) {
            case 32 -> "SPACE";
            case 257, 335 -> "ENTER";
            case 258 -> "TAB";
            case 260 -> "INSERT";
            case 261 -> "DELETE";
            case 262 -> "RIGHT";
            case 263 -> "LEFT";
            case 264 -> "DOWN";
            case 265 -> "UP";
            case 266 -> "PAGE UP";
            case 267 -> "PAGE DOWN";
            case 268 -> "HOME";
            case 269 -> "END";
            case 280 -> "CAPS";
            case 340 -> "L SHIFT";
            case 341 -> "L CTRL";
            case 342 -> "L ALT";
            case 344 -> "R SHIFT";
            case 345 -> "R CTRL";
            case 346 -> "R ALT";
            default -> keyboardName(this.code);
         };
      }
   }

   public JsonObject toJson() {
      JsonObject object = new JsonObject();
      object.addProperty("type", this.type.name().toLowerCase(Locale.ROOT));
      object.addProperty("code", this.code);
      object.addProperty("name", this.getDisplayName());
      return object;
   }

   public static KeyBind fromJson(JsonElement element) {
      if (element != null && element.isJsonObject()) {
         JsonObject object = element.getAsJsonObject();
         String typeName = object.has("type") ? object.get("type").getAsString() : "none";
         int code = object.has("code") ? object.get("code").getAsInt() : -1;

         InputType type;
         try {
            type = InputType.valueOf(typeName.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException var6) {
            type = InputType.NONE;
         }

         return of(type, code);
      } else {
         return NONE;
      }
   }

   private static String keyboardName(int key) {
      String name = GLFW.glfwGetKeyName(key, -1);
      if (name != null && !name.isBlank()) {
         return name.toUpperCase(Locale.ROOT);
      } else {
         return key >= 290 && key <= 314 ? "F" + (key - 290 + 1) : "KEY " + key;
      }
   }

   @Override
   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else {
         return !(object instanceof KeyBind keyBind) ? false : this.code == keyBind.code && this.type == keyBind.type;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.type, this.code);
   }
}
