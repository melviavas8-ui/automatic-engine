package daamky.ua.api.settings;

@FunctionalInterface
public interface SettingChangeListener<T> {
   void onChanged(Setting<T> var1, T var2, T var3);
}
