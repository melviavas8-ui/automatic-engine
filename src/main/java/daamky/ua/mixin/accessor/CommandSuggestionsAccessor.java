package daamky.ua.mixin.accessor;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ChatInputSuggestor.class})
public interface CommandSuggestionsAccessor {
   @Accessor("pendingSuggestions")
   CompletableFuture<Suggestions> getPendingSuggestions();

   @Accessor("pendingSuggestions")
   void setPendingSuggestions(CompletableFuture<Suggestions> var1);

   @Accessor("parse")
   ParseResults<?> getCurrentParse();

   @Accessor("parse")
   void setCurrentParse(ParseResults<?> var1);

   @Accessor("currentParseIsCommand")
   void setCurrentParseIsCommand(boolean var1);

   @Accessor("currentParseIsMessage")
   void setCurrentParseIsMessage(boolean var1);

   @Accessor("messages")
   List<OrderedText> getCommandUsage();

   @Invoker("updateUsageInfo")
   void invokeUpdateUsageInfo(ParseResults<?> var1, Suggestions var2);
}
