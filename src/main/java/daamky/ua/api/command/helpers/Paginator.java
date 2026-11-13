package daamky.ua.api.command.helpers;

import java.util.List;
import java.util.function.Function;
import net.minecraft.text.Text;
import daamky.ua.api.command.CommandManager;

public final class Paginator<T> {
   private final List<T> items;
   private final int pageSize;
   private int page = 1;

   public Paginator(List<T> items) {
      this(items, 8);
   }

   public Paginator(List<T> items, int pageSize) {
      this.items = items == null ? List.of() : items;
      this.pageSize = Math.max(1, pageSize);
   }

   public void setPage(int page) {
      this.page = Math.max(1, page);
   }

   public void display(Runnable header, Function<T, Text> rowFactory, String baseCommand) {
      if (header != null) {
         header.run();
      }

      int pages = Math.max(1, (int)Math.ceil((double)this.items.size() / (double)this.pageSize));
      int safePage = Math.min(this.page, pages);
      int start = (safePage - 1) * this.pageSize;
      int end = Math.min(this.items.size(), start + this.pageSize);
      CommandManager manager = CommandManager.getInstance();

      for (int i = start; i < end; i++) {
         Text row = rowFactory.apply(this.items.get(i));
         if (manager != null) {
            manager.sendRaw(row);
         }
      }

      if (manager != null && pages > 1) {
         manager.sendMessage("Page " + safePage + "/" + pages + " | " + baseCommand + " <page>");
      }
   }
}
