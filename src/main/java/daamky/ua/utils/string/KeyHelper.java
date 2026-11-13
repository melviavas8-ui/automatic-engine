package daamky.ua.utils.string;

import java.util.Locale;
import org.lwjgl.glfw.GLFW;

public final class KeyHelper {
   private KeyHelper() {
   }

   public static int getKeyCode(String name) {
      if (name != null && !name.isBlank()) {
         String value = name.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
         if (value.startsWith("MOUSE") || value.startsWith("M")) {
            String digits = value.replace("MOUSE", "").replace("M", "");

            try {
               int mouse = Integer.parseInt(digits);
               return 0 + Math.max(0, mouse - 1);
            } catch (NumberFormatException var4) {
            }
         }
         return switch (value) {
            case "LMB" -> 0;
            case "RMB" -> 1;
            case "MMB" -> 2;
            case "SPACE" -> 32;
            case "TAB" -> 258;
            case "ENTER" -> 257;
            case "SHIFT", "L_SHIFT", "LEFT_SHIFT" -> 340;
            case "R_SHIFT", "RIGHT_SHIFT" -> 344;
            case "CTRL", "CONTROL", "L_CTRL", "LEFT_CONTROL" -> 341;
            case "R_CTRL", "RIGHT_CONTROL" -> 345;
            case "ALT", "L_ALT", "LEFT_ALT" -> 342;
            case "R_ALT", "RIGHT_ALT" -> 346;
            case "NONE" -> -1;
            default -> lookupKeyboard(value);
         };
      } else {
         return -1;
      }
   }

   public static String getKeyName(int key) {
      if (key == -1 || key < 0) {
         return "None";
      } else if (key >= 0 && key <= 7) {
         return "M" + (key - 0 + 1);
      } else {
         String name = GLFW.glfwGetKeyName(key, -1);
         if (name != null && !name.isBlank()) {
            return name.toUpperCase(Locale.ROOT);
         } else {
            return key >= 290 && key <= 314 ? "F" + (key - 290 + 1) : String.valueOf(key);
         }
      }
   }

   public static String[] getAllKeyNames() {
      String[] names = new String[69];
      int index = 0;

      for (char c = 'A'; c <= 'Z'; c++) {
         names[index++] = String.valueOf(c);
      }

      for (int i = 0; i <= 9; i++) {
         names[index++] = String.valueOf(i);
      }

      for (int i = 1; i <= 25; i++) {
         names[index++] = "F" + i;
      }

      names[index++] = "LMB";
      names[index++] = "RMB";
      names[index++] = "MMB";
      names[index++] = "M4";
      names[index++] = "M5";
      names[index++] = "SPACE";
      names[index++] = "SHIFT";
      names[index] = "CTRL";
      return names;
   }

   private static int lookupKeyboard(String value) {
      if (value.length() == 1) {
         char c = value.charAt(0);
         if (c >= 'A' && c <= 'Z') {
            return 65 + (c - 65);
         }

         if (c >= '0' && c <= '9') {
            return 48 + (c - 48);
         }
      }

      if (value.startsWith("F")) {
         try {
            int f = Integer.parseInt(value.substring(1));
            if (f >= 1 && f <= 25) {
               return 290 + f - 1;
            }
         } catch (NumberFormatException var2) {
         }
      }

      return -1;
   }
}
