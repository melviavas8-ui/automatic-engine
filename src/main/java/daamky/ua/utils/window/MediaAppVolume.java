package daamky.ua.utils.window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class MediaAppVolume {
   private static final long SCRIPT_TIMEOUT_MS = 4000L;
   private static final String AUDIO_BRIDGE = "Add-Type -Language CSharp -TypeDefinition @\"\nusing System;\nusing System.Collections.Generic;\nusing System.Diagnostics;\nusing System.Linq;\nusing System.Runtime.InteropServices;\n\n[ComImport]\n[Guid(\"BCDE0395-E52F-467C-8E3D-C4579291692E\")]\nclass MMDeviceEnumeratorComObject {}\n\nenum EDataFlow { eRender, eCapture, eAll, EDataFlow_enum_count }\nenum ERole { eConsole, eMultimedia, eCommunications, ERole_enum_count }\n\n[Guid(\"A95664D2-9614-4F35-A746-DE8DB63617E6\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IMMDeviceEnumerator {\n    int EnumAudioEndpoints(EDataFlow dataFlow, int stateMask, out IntPtr devices);\n    int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice endpoint);\n    int GetDevice(string id, out IMMDevice device);\n    int RegisterEndpointNotificationCallback(IntPtr client);\n    int UnregisterEndpointNotificationCallback(IntPtr client);\n}\n\n[Guid(\"D666063F-1587-4E43-81F1-B948E807363F\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IMMDevice {\n    int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object interfacePointer);\n    int OpenPropertyStore(int accessMode, out IntPtr properties);\n    int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);\n    int GetState(out int state);\n}\n\n[Guid(\"BFA971F1-4D5E-40BB-935E-967039BFBEE4\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionManager2 {\n    int GetAudioSessionControl(ref Guid sessionGuid, uint streamFlags, out IntPtr sessionControl);\n    int GetSimpleAudioVolume(ref Guid sessionGuid, uint streamFlags, out IntPtr audioVolume);\n    int GetSessionEnumerator(out IAudioSessionEnumerator sessionEnum);\n    int RegisterSessionNotification(IntPtr sessionNotification);\n    int UnregisterSessionNotification(IntPtr sessionNotification);\n    int RegisterDuckNotification(string sessionID, IntPtr duckNotification);\n    int UnregisterDuckNotification(IntPtr duckNotification);\n}\n\n[Guid(\"E2F5BB11-0570-40CA-ACDD-3AA01277DEE8\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionEnumerator {\n    int GetCount(out int sessionCount);\n    int GetSession(int sessionCount, out IAudioSessionControl session);\n}\n\n[Guid(\"F4B1A599-7266-4319-A8CA-E70ACB11E8CD\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionControl {\n    int GetState(out int state);\n    int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)] out string displayName);\n    int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetIconPath([MarshalAs(UnmanagedType.LPWStr)] out string iconPath);\n    int SetIconPath([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetGroupingParam(out Guid groupingParam);\n    int SetGroupingParam(ref Guid groupingParam, ref Guid eventContext);\n    int RegisterAudioSessionNotification(IntPtr client);\n    int UnregisterAudioSessionNotification(IntPtr client);\n}\n\n[Guid(\"bfb7ff88-7239-4fc9-8fa2-07c950be9c6d\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionControl2 {\n    int GetState(out int state);\n    int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)] out string displayName);\n    int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetIconPath([MarshalAs(UnmanagedType.LPWStr)] out string iconPath);\n    int SetIconPath([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetGroupingParam(out Guid groupingParam);\n    int SetGroupingParam(ref Guid groupingParam, ref Guid eventContext);\n    int RegisterAudioSessionNotification(IntPtr client);\n    int UnregisterAudioSessionNotification(IntPtr client);\n    int GetSessionIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string retVal);\n    int GetSessionInstanceIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string retVal);\n    int GetProcessId(out uint retv);\n    int IsSystemSoundsSession();\n    int SetDuckingPreference(bool optOut);\n}\n\n[Guid(\"87CE5498-68D6-44E5-9215-6DA47EF883D8\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface ISimpleAudioVolume {\n    int SetMasterVolume(float level, ref Guid eventContext);\n    int GetMasterVolume(out float level);\n    int SetMute(bool mute, ref Guid eventContext);\n    int GetMute(out bool mute);\n}\n\npublic static class AudioAppBridge {\n    public static List<ISimpleAudioVolume> FindVolumes(string owner) {\n        var result = new List<ISimpleAudioVolume>();\n        IMMDeviceEnumerator enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();\n        IMMDevice device;\n        Marshal.ThrowExceptionForHR(enumerator.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out device));\n\n        Guid managerGuid = typeof(IAudioSessionManager2).GUID;\n        object managerObject;\n        Marshal.ThrowExceptionForHR(device.Activate(ref managerGuid, 23, IntPtr.Zero, out managerObject));\n        IAudioSessionManager2 manager = (IAudioSessionManager2)managerObject;\n\n        IAudioSessionEnumerator sessions;\n        Marshal.ThrowExceptionForHR(manager.GetSessionEnumerator(out sessions));\n\n        int count;\n        Marshal.ThrowExceptionForHR(sessions.GetCount(out count));\n        for (int i = 0; i < count; i++) {\n            IAudioSessionControl control;\n            Marshal.ThrowExceptionForHR(sessions.GetSession(i, out control));\n            IAudioSessionControl2 control2 = control as IAudioSessionControl2;\n            ISimpleAudioVolume volume = control as ISimpleAudioVolume;\n            if (control2 == null || volume == null) {\n                continue;\n            }\n\n            uint pid;\n            if (control2.GetProcessId(out pid) != 0 || pid == 0) {\n                continue;\n            }\n\n            string processName;\n            try {\n                processName = Process.GetProcessById((int)pid).ProcessName + \".exe\";\n            } catch {\n                continue;\n            }\n\n            if (Matches(processName, owner)) {\n                result.Add(volume);\n            }\n        }\n        return result;\n    }\n\n    public static bool Matches(string processName, string owner) {\n        if (string.IsNullOrWhiteSpace(processName) || string.IsNullOrWhiteSpace(owner)) {\n            return false;\n        }\n\n        var processLower = processName.Trim().ToLowerInvariant();\n        var ownerLower = owner.Trim().ToLowerInvariant();\n        return processLower == ownerLower\n            || processLower.Contains(ownerLower)\n            || ownerLower.Contains(processLower)\n            || processLower == ownerLower + \".exe\"\n            || ownerLower == processLower + \".exe\";\n    }\n\n    public static float? GetVolume(string owner) {\n        var volumes = FindVolumes(owner);\n        if (volumes.Count == 0) {\n            return null;\n        }\n\n        float total = 0f;\n        foreach (var volume in volumes) {\n            float level;\n            Marshal.ThrowExceptionForHR(volume.GetMasterVolume(out level));\n            total += level;\n        }\n        return total / volumes.Count;\n    }\n\n    public static bool SetVolume(string owner, float level) {\n        var volumes = FindVolumes(owner);\n        if (volumes.Count == 0) {\n            return false;\n        }\n\n        var eventContext = Guid.Empty;\n        foreach (var volume in volumes) {\n            Marshal.ThrowExceptionForHR(volume.SetMasterVolume(level, ref eventContext));\n        }\n        return true;\n    }\n}\n\"@\n";

   private MediaAppVolume() {
   }

   public static boolean isSupported() {
      String osName = System.getProperty("os.name", "");
      return osName.toLowerCase(Locale.ROOT).contains("win");
   }

   public static Float getAppVolume(String owner) {
      return isSupported() && owner != null && !owner.isBlank()
         ? executeScript(
            owner,
            "$level = [AudioAppBridge]::GetVolume($owner)\nif ($null -eq $level) { exit 2 }\n[Console]::Write([string]::Format([System.Globalization.CultureInfo]::InvariantCulture, \"{0:0.###}\", $level))\n"
         )
         : null;
   }

   public static boolean setAppVolume(String owner, float value) {
      if (isSupported() && owner != null && !owner.isBlank()) {
         float clamped = Math.max(0.0F, Math.min(1.0F, value));
         String script = String.format(
            Locale.ROOT, "$changed = [AudioAppBridge]::SetVolume($owner, [float]%s)\nif (-not $changed) { exit 2 }\n[Console]::Write(\"ok\")\n", clamped
         );
         return executeScript(owner, script) != null;
      } else {
         return false;
      }
   }

   private static Float executeScript(String owner, String operationScript) {
      Process process = null;

      Float var6;
      try {
         String escapedOwner = owner.replace("'", "''");
         process = new ProcessBuilder(
               "powershell.exe",
               "-NoProfile",
               "-NonInteractive",
               "-ExecutionPolicy",
               "Bypass",
               "-Command",
               "Add-Type -Language CSharp -TypeDefinition @\"\nusing System;\nusing System.Collections.Generic;\nusing System.Diagnostics;\nusing System.Linq;\nusing System.Runtime.InteropServices;\n\n[ComImport]\n[Guid(\"BCDE0395-E52F-467C-8E3D-C4579291692E\")]\nclass MMDeviceEnumeratorComObject {}\n\nenum EDataFlow { eRender, eCapture, eAll, EDataFlow_enum_count }\nenum ERole { eConsole, eMultimedia, eCommunications, ERole_enum_count }\n\n[Guid(\"A95664D2-9614-4F35-A746-DE8DB63617E6\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IMMDeviceEnumerator {\n    int EnumAudioEndpoints(EDataFlow dataFlow, int stateMask, out IntPtr devices);\n    int GetDefaultAudioEndpoint(EDataFlow dataFlow, ERole role, out IMMDevice endpoint);\n    int GetDevice(string id, out IMMDevice device);\n    int RegisterEndpointNotificationCallback(IntPtr client);\n    int UnregisterEndpointNotificationCallback(IntPtr client);\n}\n\n[Guid(\"D666063F-1587-4E43-81F1-B948E807363F\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IMMDevice {\n    int Activate(ref Guid iid, int clsCtx, IntPtr activationParams, [MarshalAs(UnmanagedType.IUnknown)] out object interfacePointer);\n    int OpenPropertyStore(int accessMode, out IntPtr properties);\n    int GetId([MarshalAs(UnmanagedType.LPWStr)] out string id);\n    int GetState(out int state);\n}\n\n[Guid(\"BFA971F1-4D5E-40BB-935E-967039BFBEE4\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionManager2 {\n    int GetAudioSessionControl(ref Guid sessionGuid, uint streamFlags, out IntPtr sessionControl);\n    int GetSimpleAudioVolume(ref Guid sessionGuid, uint streamFlags, out IntPtr audioVolume);\n    int GetSessionEnumerator(out IAudioSessionEnumerator sessionEnum);\n    int RegisterSessionNotification(IntPtr sessionNotification);\n    int UnregisterSessionNotification(IntPtr sessionNotification);\n    int RegisterDuckNotification(string sessionID, IntPtr duckNotification);\n    int UnregisterDuckNotification(IntPtr duckNotification);\n}\n\n[Guid(\"E2F5BB11-0570-40CA-ACDD-3AA01277DEE8\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionEnumerator {\n    int GetCount(out int sessionCount);\n    int GetSession(int sessionCount, out IAudioSessionControl session);\n}\n\n[Guid(\"F4B1A599-7266-4319-A8CA-E70ACB11E8CD\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionControl {\n    int GetState(out int state);\n    int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)] out string displayName);\n    int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetIconPath([MarshalAs(UnmanagedType.LPWStr)] out string iconPath);\n    int SetIconPath([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetGroupingParam(out Guid groupingParam);\n    int SetGroupingParam(ref Guid groupingParam, ref Guid eventContext);\n    int RegisterAudioSessionNotification(IntPtr client);\n    int UnregisterAudioSessionNotification(IntPtr client);\n}\n\n[Guid(\"bfb7ff88-7239-4fc9-8fa2-07c950be9c6d\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface IAudioSessionControl2 {\n    int GetState(out int state);\n    int GetDisplayName([MarshalAs(UnmanagedType.LPWStr)] out string displayName);\n    int SetDisplayName([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetIconPath([MarshalAs(UnmanagedType.LPWStr)] out string iconPath);\n    int SetIconPath([MarshalAs(UnmanagedType.LPWStr)] string value, ref Guid eventContext);\n    int GetGroupingParam(out Guid groupingParam);\n    int SetGroupingParam(ref Guid groupingParam, ref Guid eventContext);\n    int RegisterAudioSessionNotification(IntPtr client);\n    int UnregisterAudioSessionNotification(IntPtr client);\n    int GetSessionIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string retVal);\n    int GetSessionInstanceIdentifier([MarshalAs(UnmanagedType.LPWStr)] out string retVal);\n    int GetProcessId(out uint retv);\n    int IsSystemSoundsSession();\n    int SetDuckingPreference(bool optOut);\n}\n\n[Guid(\"87CE5498-68D6-44E5-9215-6DA47EF883D8\"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]\ninterface ISimpleAudioVolume {\n    int SetMasterVolume(float level, ref Guid eventContext);\n    int GetMasterVolume(out float level);\n    int SetMute(bool mute, ref Guid eventContext);\n    int GetMute(out bool mute);\n}\n\npublic static class AudioAppBridge {\n    public static List<ISimpleAudioVolume> FindVolumes(string owner) {\n        var result = new List<ISimpleAudioVolume>();\n        IMMDeviceEnumerator enumerator = (IMMDeviceEnumerator)new MMDeviceEnumeratorComObject();\n        IMMDevice device;\n        Marshal.ThrowExceptionForHR(enumerator.GetDefaultAudioEndpoint(EDataFlow.eRender, ERole.eMultimedia, out device));\n\n        Guid managerGuid = typeof(IAudioSessionManager2).GUID;\n        object managerObject;\n        Marshal.ThrowExceptionForHR(device.Activate(ref managerGuid, 23, IntPtr.Zero, out managerObject));\n        IAudioSessionManager2 manager = (IAudioSessionManager2)managerObject;\n\n        IAudioSessionEnumerator sessions;\n        Marshal.ThrowExceptionForHR(manager.GetSessionEnumerator(out sessions));\n\n        int count;\n        Marshal.ThrowExceptionForHR(sessions.GetCount(out count));\n        for (int i = 0; i < count; i++) {\n            IAudioSessionControl control;\n            Marshal.ThrowExceptionForHR(sessions.GetSession(i, out control));\n            IAudioSessionControl2 control2 = control as IAudioSessionControl2;\n            ISimpleAudioVolume volume = control as ISimpleAudioVolume;\n            if (control2 == null || volume == null) {\n                continue;\n            }\n\n            uint pid;\n            if (control2.GetProcessId(out pid) != 0 || pid == 0) {\n                continue;\n            }\n\n            string processName;\n            try {\n                processName = Process.GetProcessById((int)pid).ProcessName + \".exe\";\n            } catch {\n                continue;\n            }\n\n            if (Matches(processName, owner)) {\n                result.Add(volume);\n            }\n        }\n        return result;\n    }\n\n    public static bool Matches(string processName, string owner) {\n        if (string.IsNullOrWhiteSpace(processName) || string.IsNullOrWhiteSpace(owner)) {\n            return false;\n        }\n\n        var processLower = processName.Trim().ToLowerInvariant();\n        var ownerLower = owner.Trim().ToLowerInvariant();\n        return processLower == ownerLower\n            || processLower.Contains(ownerLower)\n            || ownerLower.Contains(processLower)\n            || processLower == ownerLower + \".exe\"\n            || ownerLower == processLower + \".exe\";\n    }\n\n    public static float? GetVolume(string owner) {\n        var volumes = FindVolumes(owner);\n        if (volumes.Count == 0) {\n            return null;\n        }\n\n        float total = 0f;\n        foreach (var volume in volumes) {\n            float level;\n            Marshal.ThrowExceptionForHR(volume.GetMasterVolume(out level));\n            total += level;\n        }\n        return total / volumes.Count;\n    }\n\n    public static bool SetVolume(string owner, float level) {\n        var volumes = FindVolumes(owner);\n        if (volumes.Count == 0) {\n            return false;\n        }\n\n        var eventContext = Guid.Empty;\n        foreach (var volume in volumes) {\n            Marshal.ThrowExceptionForHR(volume.SetMasterVolume(level, ref eventContext));\n        }\n        return true;\n    }\n}\n\"@\n"
                  + System.lineSeparator()
                  + "$owner = '"
                  + escapedOwner
                  + "'"
                  + System.lineSeparator()
                  + operationScript
            )
            .redirectErrorStream(true)
            .start();
         boolean finished = process.waitFor(4000L, TimeUnit.MILLISECONDS);
         if (!finished) {
            process.destroyForcibly();
            return null;
         }

         String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
         if (process.exitValue() != 0 || output.isEmpty()) {
            return null;
         }

         if (!"ok".equalsIgnoreCase(output)) {
            return Float.parseFloat(output);
         }

         var6 = 1.0F;
      } catch (InterruptedException | NumberFormatException | IOException var10) {
         if (var10 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         return null;
      } finally {
         if (process != null) {
            process.destroy();
         }
      }

      return var6;
   }
}
