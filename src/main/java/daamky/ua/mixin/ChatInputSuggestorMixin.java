package daamky.ua.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.screen.ChatInputSuggestor.SuggestionWindow;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.TabCompleteEvent;
import daamky.ua.manager.Manager;

@Mixin({ChatInputSuggestor.class})
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   @Final
   private List<OrderedText> messages;
   @Shadow
   private ParseResults<?> parse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionWindow window;
   @Shadow
   boolean completingSuggestions;

   public ChatInputSuggestorMixin() {
   }

   @Shadow
   public abstract void show(boolean var1);

   @Inject(
      method = {"refresh"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$updateCommandInfo(CallbackInfo ci) {
      String text = this.textField.getText();
      int cursor = this.textField.getCursor();
      String prefix = text.substring(0, Math.min(text.length(), cursor));
      TabCompleteEvent event = Manager.postEvent(new TabCompleteEvent(prefix));
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.getCompletions() != null) {
         ci.cancel();
         this.parse = null;
         if (!this.completingSuggestions) {
            this.textField.setSuggestion(null);
            this.window = null;
            this.messages.clear();
            String[] completions = event.getCompletions();
            if (completions.length == 0) {
               this.pendingSuggestions = Suggestions.empty();
            } else {
               int lastSpace = prefix.lastIndexOf(32);
               StringRange range = StringRange.between(lastSpace + 1, prefix.length());
               List<Suggestion> suggestionList = Stream.of(completions).map(value -> new Suggestion(range, value)).toList();
               Suggestions suggestions = new Suggestions(range, suggestionList);
               this.pendingSuggestions = CompletableFuture.completedFuture(suggestions);
               this.show(true);
            }
         }
      }
   }
}
