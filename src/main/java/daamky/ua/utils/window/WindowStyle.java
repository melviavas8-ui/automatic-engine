package daamky.ua.utils.window;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.win32.StdCallLibrary;
import java.util.Locale;
import org.lwjgl.glfw.GLFWNativeWin32;

public final class WindowStyle {
   private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

   private WindowStyle() {
   }

   public static void setDarkMode(long windowHandle, boolean enabled) {
      if (isWindows() && windowHandle != 0L) {
         long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
         if (hwnd != 0L) {
            Memory darkModeEnabled = new Memory(4L);
            darkModeEnabled.setInt(0L, enabled ? 1 : 0);
            WindowStyle.DwmApi.INSTANCE.DwmSetWindowAttribute(new HWND(new Pointer(hwnd)), 20, darkModeEnabled, 4);
         }
      }
   }

   private static boolean isWindows() {
      String osName = System.getProperty("os.name", "");
      return osName.toLowerCase(Locale.ROOT).contains("win");
   }

   public interface DwmApi extends StdCallLibrary {
      WindowStyle.DwmApi INSTANCE = (WindowStyle.DwmApi)Native.load("dwmapi", WindowStyle.DwmApi.class);

      HRESULT DwmSetWindowAttribute(HWND var1, int var2, Pointer var3, int var4);
   }
}
