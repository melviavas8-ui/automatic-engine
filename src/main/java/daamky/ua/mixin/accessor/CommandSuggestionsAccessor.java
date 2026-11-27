package daamky.ua.mixin.accessor;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CommandSuggestions.class})
public interface CommandSuggestionsAccessor {
   @Accessor("pendingSuggestions")
   CompletableFuture<Suggestions> getPendingSuggestions();

   @Accessor("pendingSuggestions")
   void setPendingSuggestions(CompletableFuture<Suggestions> var1);

   @Accessor("currentParse")
   ParseResults<?> getCurrentParse();

   @Accessor("currentParse")
   void setCurrentParse(ParseResults<?> var1);

   @Accessor("currentParseIsCommand")
   void setCurrentParseIsCommand(boolean var1);

   @Accessor("currentParseIsMessage")
   void setCurrentParseIsMessage(boolean var1);

   @Accessor("commandUsage")
   List<FormattedCharSequence> getCommandUsage();

   @Invoker("updateUsageInfo")
   void invokeUpdateUsageInfo(ParseResults<?> var1, Suggestions var2);
}
