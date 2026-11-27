package daamky.ua.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.TabCompleteEvent;
import daamky.ua.manager.Manager;

@Mixin({CommandSuggestions.class})
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   EditBox input;
   @Shadow
   @Final
   private List<FormattedCharSequence> commandUsage;
   @Shadow
   private ParseResults<?> currentParse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private SuggestionsList suggestions;
   @Shadow
   boolean keepSuggestions;

   public ChatInputSuggestorMixin() {
   }

   @Shadow
   public abstract void showSuggestions(boolean var1);

   @Inject(
      method = {"updateCommandInfo"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$updateCommandInfo(CallbackInfo ci) {
      String text = this.input.getValue();
      int cursor = this.input.getCursorPosition();
      String prefix = text.substring(0, Math.min(text.length(), cursor));
      TabCompleteEvent event = Manager.postEvent(new TabCompleteEvent(prefix));
      if (event.isCancelled()) {
         ci.cancel();
      } else if (event.getCompletions() != null) {
         ci.cancel();
         this.currentParse = null;
         if (!this.keepSuggestions) {
            this.input.setSuggestion(null);
            this.suggestions = null;
            this.commandUsage.clear();
            String[] completions = event.getCompletions();
            if (completions.length == 0) {
               this.pendingSuggestions = Suggestions.empty();
            } else {
               int lastSpace = prefix.lastIndexOf(32);
               StringRange range = StringRange.between(lastSpace + 1, prefix.length());
               List<Suggestion> suggestionList = Stream.of(completions).map(value -> new Suggestion(range, value)).toList();
               Suggestions suggestions = new Suggestions(range, suggestionList);
               this.pendingSuggestions = CompletableFuture.completedFuture(suggestions);
               this.showSuggestions(true);
            }
         }
      }
   }
}
