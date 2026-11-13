package daamky.ua.api.module.impl.visual.esp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import daamky.ua.api.module.impl.player.NameProtect;
import daamky.ua.utils.network.Network;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class PlayerTagResolver {
   public PlayerTagResolver() {
   }

   PlayerTag resolve(PlayerEntity player, MinecraftClient mc) {
      Team team = this.resolvePlayerTeam(player);
      PlayerListEntry playerInfo = this.resolvePlayerInfo(player, mc);
      if (Network.isReallyWorld()) {
         PlayerTag tag = this.resolveReallyWorldPlayerTag(player, team, playerInfo);
         return this.stripDuplicateVoiceStatus(tag);
      } else {
         PlayerTag tag = this.resolveStyledPlayerTag(player, playerInfo, team);
         return this.stripVoiceStatus(tag);
      }
   }

   private PlayerTag resolveReallyWorldPlayerTag(PlayerEntity player, Team team, PlayerListEntry playerInfo) {
      String donation = this.decodeReallyWorldTag(team == null ? null : team.getPrefix().getString(), true);
      String suffix = this.decodeReallyWorldTag(team == null ? null : team.getSuffix().getString(), true);
      String name = this.decodeReallyWorldTag(player.getName().getString(), false);
      if (name.isBlank()) {
         name = this.decodeReallyWorldTag(player.getNameForScoreboard(), false);
      }

      if (name.isBlank()) {
         name = "Player";
      }

      name = NameProtect.protectPlayerName(player, name);
      return new PlayerTag(
         this.resolveVoiceStatusParts(player, playerInfo),
         TagParts.list(FontType.BOLD, donation, EspColors.TAG_MUTED),
         Collections.emptyList(),
         TagParts.list(FontType.SEMIBOLD, name, EspColors.TAG_TEXT),
         TagParts.list(FontType.SEMIBOLD, suffix, EspColors.TAG_MUTED)
      );
   }

   private PlayerListEntry resolvePlayerInfo(PlayerEntity player, MinecraftClient mc) {
      return player != null && mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
   }

   private PlayerTag resolveStyledPlayerTag(PlayerEntity player, PlayerListEntry playerInfo, Team team) {
      Text tabName = playerInfo == null ? null : playerInfo.getDisplayName();
      PlayerTag tabTag = this.resolvePlayerTagFromDisplay(player, playerInfo, tabName);
      if (tabTag != null) {
         return tabTag;
      } else {
         Text displayName = player == null ? null : player.getDisplayName();
         PlayerTag displayTag = this.resolvePlayerTagFromDisplay(player, playerInfo, displayName);
         if (displayTag != null) {
            return displayTag;
         } else {
            Text teamName = team == null ? null : Team.decorateName(team, Text.literal(player.getNameForScoreboard()));
            PlayerTag teamTag = this.resolvePlayerTagFromDisplay(player, playerInfo, teamName);
            if (teamTag != null) {
               return teamTag;
            } else {
               PlayerTagResolver.VoiceDonationPrefix prefix = this.splitVoiceDonationPrefix(
                  this.componentToTagParts(team == null ? null : team.getPrefix(), FontType.BOLD, EspColors.TAG_MUTED)
               );
               List<TagPart> suffix = this.componentToTagParts(team == null ? null : team.getSuffix(), FontType.SEMIBOLD, EspColors.TAG_MUTED);
               return new PlayerTag(
                  this.resolveVoiceStatusParts(player, playerInfo),
                  TagParts.retagFont(TagParts.stripOuterDecorators(prefix.donation()), FontType.BOLD),
                  TagParts.retagFont(prefix.prefix(), FontType.SEMIBOLD),
                  TagParts.list(FontType.SEMIBOLD, this.resolvePlainPlayerName(player, playerInfo), EspColors.TAG_TEXT),
                  TagParts.retagFont(TagParts.trim(suffix), FontType.SEMIBOLD)
               );
            }
         }
      }
   }

   private PlayerTag resolvePlayerTagFromDisplay(PlayerEntity player, PlayerListEntry playerInfo, Text displayName) {
      List<TagPart> displayParts = TagParts.trim(this.componentToTagParts(displayName, FontType.SEMIBOLD, EspColors.TAG_TEXT));
      if (displayParts.isEmpty()) {
         return null;
      } else {
         String plain = TagParts.plainText(displayParts);
         PlayerTagResolver.NameRange nameRange = this.findNameRange(plain, this.playerNameCandidates(player, playerInfo));
         if (nameRange == null) {
            return null;
         } else {
            PlayerTagResolver.VoiceDonationPrefix prefix = this.splitVoiceDonationPrefix(TagParts.slice(displayParts, 0, nameRange.start()));
            List<TagPart> name = this.protectNameParts(player, TagParts.trim(TagParts.slice(displayParts, nameRange.start(), nameRange.end())));
            List<TagPart> suffix = TagParts.trim(TagParts.slice(displayParts, nameRange.end(), TagParts.textLength(displayParts)));
            if (name.isEmpty()) {
               name = TagParts.list(FontType.SEMIBOLD, this.resolvePlainPlayerName(player, playerInfo), EspColors.TAG_TEXT);
            }

            return new PlayerTag(
               prefix.voice(),
               TagParts.retagFont(TagParts.stripOuterDecorators(prefix.donation()), FontType.BOLD),
               TagParts.retagFont(prefix.prefix(), FontType.SEMIBOLD),
               TagParts.retagFont(name, FontType.SEMIBOLD),
               TagParts.retagFont(suffix, FontType.SEMIBOLD)
            );
         }
      }
   }

   private PlayerTag withFriendPrefix(PlayerEntity player, PlayerTag tag) {
      if (tag == null) {
         tag = new PlayerTag(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            TagParts.list(FontType.SEMIBOLD, this.resolvePlainPlayerName(player, null), EspColors.TAG_TEXT),
            Collections.emptyList()
         );
      }

      return !FriendUtils.isFriend(player)
         ? tag
         : new PlayerTag(
            tag.voice(), tag.donation(), TagParts.join(TagParts.list(FontType.BOLD, "Friend", EspColors.TAG_MUTED), tag.prefix()), tag.name(), tag.suffix()
         );
   }

   private PlayerTag stripDuplicateVoiceStatus(PlayerTag tag) {
      if (tag == null) {
         return null;
      } else {
         PlayerTagResolver.VoiceStatusSplit donation = this.splitExplicitVoiceStatus(tag.donation());
         PlayerTagResolver.VoiceStatusSplit prefix = this.splitExplicitVoiceStatus(tag.prefix());
         PlayerTagResolver.VoiceStatusSplit name = this.splitExplicitVoiceStatus(tag.name());
         PlayerTagResolver.VoiceStatusSplit suffix = this.splitExplicitVoiceStatus(tag.suffix());
         List<TagPart> voice = tag.voice().isEmpty() ? this.firstVoice(donation.voice(), prefix.voice(), name.voice(), suffix.voice()) : tag.voice();
         return new PlayerTag(voice, donation.remaining(), prefix.remaining(), name.remaining(), suffix.remaining());
      }
   }

   private PlayerTag stripVoiceStatus(PlayerTag tag) {
      return tag == null
         ? null
         : new PlayerTag(
            Collections.emptyList(),
            this.splitExplicitVoiceStatus(tag.donation()).remaining(),
            this.splitExplicitVoiceStatus(tag.prefix()).remaining(),
            this.splitExplicitVoiceStatus(tag.name()).remaining(),
            this.splitExplicitVoiceStatus(tag.suffix()).remaining()
         );
   }

   @SafeVarargs
   private final List<TagPart> firstVoice(List<TagPart>... candidates) {
      for (List<TagPart> candidate : candidates) {
         if (candidate != null && !candidate.isEmpty()) {
            return candidate;
         }
      }

      return Collections.emptyList();
   }

   private String resolvePlainPlayerName(PlayerEntity player, PlayerListEntry playerInfo) {
      String name = player == null ? "" : TagParts.clean(player.getName().getString());
      if (name.isBlank() && player != null) {
         name = TagParts.clean(player.getNameForScoreboard());
      }

      if (name.isBlank() && playerInfo != null && playerInfo.getProfile() != null) {
         name = TagParts.clean(playerInfo.getProfile().name());
      }

      name = name.isBlank() ? "Player" : name;
      return NameProtect.protectPlayerName(player, name);
   }

   private List<TagPart> protectNameParts(PlayerEntity player, List<TagPart> name) {
      String protectedName = NameProtect.protectPlayerName(player, TagParts.plainText(name));
      if (protectedName != null && !protectedName.isBlank() && !protectedName.equals(TagParts.plainText(name))) {
         int color = name != null && !name.isEmpty() ? name.get(0).color() : EspColors.TAG_TEXT;
         float yOffset = name != null && !name.isEmpty() ? name.get(0).yOffset() : 0.0F;
         return List.of(new TagPart(FontType.SEMIBOLD, protectedName, color, yOffset));
      } else {
         return name;
      }
   }

   private List<String> playerNameCandidates(PlayerEntity player, PlayerListEntry playerInfo) {
      List<String> candidates = new ArrayList<>(3);
      if (player != null) {
         this.addNameCandidate(candidates, TagParts.clean(player.getNameForScoreboard()));
         this.addNameCandidate(candidates, TagParts.clean(player.getName().getString()));
      }

      if (playerInfo != null && playerInfo.getProfile() != null) {
         this.addNameCandidate(candidates, TagParts.clean(playerInfo.getProfile().name()));
      }

      candidates.sort((left, right) -> Integer.compare(right.length(), left.length()));
      return candidates;
   }

   private void addNameCandidate(List<String> candidates, String name) {
      if (name != null && !name.isBlank()) {
         for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(name)) {
               return;
            }
         }

         candidates.add(name);
      }
   }

   private PlayerTagResolver.NameRange findNameRange(String plain, List<String> candidates) {
      if (plain != null && !plain.isBlank() && candidates != null && !candidates.isEmpty()) {
         String lowerPlain = plain.toLowerCase(Locale.ROOT);

         for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
               String lowerCandidate = candidate.toLowerCase(Locale.ROOT);
               int index = lowerPlain.indexOf(lowerCandidate);
               if (index >= 0) {
                  return new PlayerTagResolver.NameRange(index, index + candidate.length());
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private List<TagPart> resolveVoiceStatusParts(PlayerEntity player, PlayerListEntry playerInfo) {
      if (playerInfo == null) {
         return Collections.emptyList();
      } else {
         List<TagPart> displayParts = TagParts.trim(this.componentToTagParts(playerInfo.getDisplayName(), FontType.SEMIBOLD, EspColors.TAG_MUTED));
         if (displayParts.isEmpty()) {
            return Collections.emptyList();
         } else {
            String plain = TagParts.plainText(displayParts);
            PlayerTagResolver.NameRange nameRange = this.findNameRange(plain, this.playerNameCandidates(player, playerInfo));
            List<TagPart> beforeName = nameRange == null ? displayParts : TagParts.slice(displayParts, 0, nameRange.start());
            return this.splitVoiceStatus(beforeName).voice();
         }
      }
   }

   private PlayerTagResolver.VoiceDonationPrefix splitVoiceDonationPrefix(List<TagPart> source) {
      PlayerTagResolver.VoiceStatusSplit voice = this.splitVoiceStatus(source);
      PlayerTagResolver.DonationPrefix prefix = this.splitDonationPrefix(voice.remaining());
      return new PlayerTagResolver.VoiceDonationPrefix(voice.voice(), prefix.donation(), prefix.prefix());
   }

   private PlayerTagResolver.VoiceStatusSplit splitVoiceStatus(List<TagPart> source) {
      return this.splitVoiceStatus(source, true);
   }

   private PlayerTagResolver.VoiceStatusSplit splitExplicitVoiceStatus(List<TagPart> source) {
      return this.splitVoiceStatus(source, false);
   }

   private PlayerTagResolver.VoiceStatusSplit splitVoiceStatus(List<TagPart> source, boolean allowColorFallback) {
      List<TagPart> parts = TagParts.trim(source);
      int index = this.findVoiceStatusIndex(parts, allowColorFallback);
      if (index < 0) {
         return new PlayerTagResolver.VoiceStatusSplit(Collections.emptyList(), parts);
      } else {
         int color = TagParts.colorAt(parts, index, EspColors.TAG_MUTED);
         List<TagPart> voice = List.of(new TagPart(FontType.SEMIBOLD, "●", color));
         List<TagPart> remaining = TagParts.concat(TagParts.slice(parts, 0, index), TagParts.slice(parts, index + 1, TagParts.textLength(parts)));
         return new PlayerTagResolver.VoiceStatusSplit(voice, TagParts.trim(remaining));
      }
   }

   private int findVoiceStatusIndex(List<TagPart> parts, boolean allowColorFallback) {
      String plain = TagParts.plainText(parts);

      for (int i = 0; i < plain.length(); i++) {
         char c = plain.charAt(i);
         if (!Character.isWhitespace(c) && !TagParts.isLooseDecorator(c)) {
            if (this.getIconLabel(c) != null) {
               return -1;
            }

            if (this.isVoiceStatusGlyph(c)) {
               return i;
            }

            if (allowColorFallback && !Character.isLetterOrDigit(c) && c != '_' && TagParts.colorAt(parts, i, EspColors.TAG_MUTED) != EspColors.TAG_MUTED) {
               return i;
            }

            return -1;
         }
      }

      return -1;
   }

   private boolean isVoiceStatusGlyph(char c) {
      return c == 9679 || c == 8226 || c == 9899 || c == 9673 || c == 9675 || c == 9632 || c == 9642 || c == 9670 || c == 9830 || c == 10010 || c == '+';
   }

   private PlayerTagResolver.DonationPrefix splitDonationPrefix(List<TagPart> source) {
      List<TagPart> parts = TagParts.trim(source);
      if (parts.isEmpty()) {
         return new PlayerTagResolver.DonationPrefix(Collections.emptyList(), Collections.emptyList());
      } else {
         List<TagPart> compactParts = TagParts.compactToSingleTextPart(TagParts.stripOuterDecorators(parts), FontType.BOLD, EspColors.TAG_MUTED);
         if (!TagParts.plainText(compactParts).equals(TagParts.plainText(TagParts.stripOuterDecorators(parts)))) {
            return new PlayerTagResolver.DonationPrefix(compactParts, Collections.emptyList());
         } else {
            String plain = TagParts.plainText(parts);
            int donationEnd = this.findDonationEnd(plain);
            if (donationEnd > 0 && donationEnd < plain.length()) {
               List<TagPart> donation = TagParts.compactToSingleTextPart(
                  TagParts.stripOuterDecorators(TagParts.slice(parts, 0, donationEnd)), FontType.BOLD, EspColors.TAG_MUTED
               );
               List<TagPart> prefix = TagParts.compactToSingleTextPart(
                  TagParts.stripOuterDecorators(TagParts.trim(TagParts.slice(parts, donationEnd, TagParts.textLength(parts)))),
                  FontType.SEMIBOLD,
                  EspColors.TAG_MUTED
               );
               return new PlayerTagResolver.DonationPrefix(donation, prefix);
            } else {
               return new PlayerTagResolver.DonationPrefix(compactParts, Collections.emptyList());
            }
         }
      }
   }

   private int findDonationEnd(String plain) {
      if (plain != null && !plain.isBlank()) {
         int start = 0;
         int length = plain.length();

         while (start < length && Character.isWhitespace(plain.charAt(start))) {
            start++;
         }

         if (start >= length) {
            return 0;
         } else {
            char first = plain.charAt(start);
            if (TagParts.isOpeningDecorator(first)) {
               char closing = this.matchingClosingDecorator(first);
               int closingIndex = closing == 0 ? -1 : plain.indexOf(closing, start + 1);
               if (closingIndex >= 0) {
                  return closingIndex + 1;
               }
            }

            if (first == 171) {
               int closingIndex = plain.indexOf(187, start + 1);
               if (closingIndex >= 0) {
                  return closingIndex + 1;
               }
            }

            int letterSpacedEnd = this.findLeadingLetterSpacedDonationEnd(plain, start);
            if (letterSpacedEnd > start) {
               return letterSpacedEnd;
            } else {
               int end;
               for (end = start; end < length; end++) {
                  char c = plain.charAt(end);
                  if (Character.isWhitespace(c) || c == '|' || c == ':') {
                     break;
                  }
               }

               return Math.max(start, end);
            }
         }
      } else {
         return 0;
      }
   }

   private int findLeadingLetterSpacedDonationEnd(String plain, int start) {
      int index = start;
      int end = start;
      int tokenCount = 0;

      for (int length = plain.length(); index < length; end = index) {
         int spaceStart = index;

         while (index < length && Character.isWhitespace(plain.charAt(index))) {
            index++;
         }

         if (tokenCount > 0 && index == spaceStart || index >= length) {
            break;
         }

         int tokenStart = index;

         while (index < length && !Character.isWhitespace(plain.charAt(index)) && plain.charAt(index) != '|' && plain.charAt(index) != ':') {
            index++;
         }

         if (index - tokenStart != 1 || !TagParts.isCompactPrefixChar(plain.charAt(tokenStart))) {
            break;
         }

         tokenCount++;
      }

      return tokenCount >= 2 ? end : -1;
   }

   private char matchingClosingDecorator(char c) {
      return switch (c) {
         case '(' -> ')';
         case '<' -> '>';
         case '[' -> ']';
         case '{' -> '}';
         default -> '\u0000';
      };
   }

   private List<TagPart> componentToTagParts(Text component, FontType font, int fallbackColor) {
      if (component == null) {
         return Collections.emptyList();
      } else {
         List<TagPart> parts = new ArrayList<>();
         component.visit((style, text) -> {
            this.appendComponentText(parts, font, text, this.colorFromStyle(style, fallbackColor));
            return Optional.empty();
         }, Style.EMPTY);
         if (parts.isEmpty()) {
            this.appendComponentText(parts, font, component.getString(), this.colorFromStyle(component.getStyle(), fallbackColor));
         }

         return TagParts.trim(parts);
      }
   }

   private void appendComponentText(List<TagPart> parts, FontType font, String text, int fallbackColor) {
      if (text != null && !text.isEmpty()) {
         int currentColor = fallbackColor;

         for (int i = 0; i < text.length(); i++) {
            PlayerTagResolver.LegacyFormatResult legacy = this.consumeLegacyFormat(text, i, fallbackColor, currentColor);
            if (legacy != null) {
               currentColor = legacy.color();
               i = legacy.nextIndex();
            } else {
               String token = this.normalizeTagChar(text.charAt(i));
               if (token != null && !token.isEmpty()) {
                  TagParts.appendRaw(parts, font, token, currentColor);
               }
            }
         }
      }
   }

   private PlayerTagResolver.LegacyFormatResult consumeLegacyFormat(String text, int index, int fallbackColor, int currentColor) {
      if (text != null && index >= 0 && index < text.length()) {
         char current = text.charAt(index);
         if (current == 194 && index + 1 < text.length() && text.charAt(index + 1) == 167) {
            index++;
            current = 167;
         }

         if (current == 167 && index + 1 < text.length()) {
            char code = Character.toLowerCase(text.charAt(index + 1));
            if (code == 'x' && index + 13 < text.length()) {
               StringBuilder hex = new StringBuilder(6);

               for (int i = index + 2; i <= index + 12; i += 2) {
                  if (text.charAt(i) != 167) {
                     return new PlayerTagResolver.LegacyFormatResult(currentColor, index + 1);
                  }

                  hex.append(text.charAt(i + 1));
               }

               try {
                  return new PlayerTagResolver.LegacyFormatResult(0xFF000000 | Integer.parseInt(hex.toString(), 16), index + 13);
               } catch (NumberFormatException var9) {
                  return new PlayerTagResolver.LegacyFormatResult(currentColor, index + 1);
               }
            } else if (code == '#' && index + 7 < text.length()) {
               try {
                  return new PlayerTagResolver.LegacyFormatResult(0xFF000000 | Integer.parseInt(text.substring(index + 2, index + 8), 16), index + 7);
               } catch (NumberFormatException var10) {
                  return new PlayerTagResolver.LegacyFormatResult(currentColor, index + 1);
               }
            } else {
               Integer legacyColor = switch (code) {
                  case '0' -> -16777216;
                  case '1' -> -16777046;
                  case '2' -> -16733696;
                  case '3' -> -16733526;
                  case '4' -> -5636096;
                  case '5' -> -5635926;
                  case '6' -> -22016;
                  case '7' -> -5592406;
                  case '8' -> -11184811;
                  case '9' -> -11184641;
                  default -> null;
                  case 'a' -> -11141291;
                  case 'b' -> -11141121;
                  case 'c' -> -43691;
                  case 'd' -> -43521;
                  case 'e' -> -171;
                  case 'f' -> -1;
                  case 'r' -> fallbackColor;
               };
               return new PlayerTagResolver.LegacyFormatResult(legacyColor == null ? currentColor : legacyColor, index + 1);
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private String normalizeTagChar(char value) {
      if (value == 0 || Character.isISOControl(value)) {
         return "";
      } else {
         return value != 194 && value != 160 && value != 8203 && value != '\ufeff' && !Character.isWhitespace(value) ? String.valueOf(value) : " ";
      }
   }

   private int colorFromStyle(Style style, int fallbackColor) {
      return style != null && style.getColor() != null ? 0xFF000000 | style.getColor().getRgb() : fallbackColor;
   }

   private Team resolvePlayerTeam(PlayerEntity player) {
      if (player != null && player.getEntityWorld() != null) {
         Scoreboard scoreboard = player.getEntityWorld().getScoreboard();
         Team team = scoreboard.getScoreHolderTeam(player.getNameForScoreboard());
         if (team == null) {
            team = scoreboard.getScoreHolderTeam(player.getName().getString());
         }

         return team;
      } else {
         return null;
      }
   }

   private String decodeReallyWorldTag(String text, boolean stripDecorators) {
      if (text != null && !text.isBlank()) {
         StringBuilder builder = new StringBuilder(text.length());
         boolean skipFormatting = false;
         boolean pendingSpace = false;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipFormatting) {
               skipFormatting = false;
            } else if (c == 167) {
               skipFormatting = true;
            } else if (c != 194 && c != 160 && c != 8203 && c != '\ufeff' && !Character.isWhitespace(c)) {
               String token = this.getIconLabel(c);
               if (token == null) {
                  token = this.normalizeReallyWorldChar(c);
               }

               if (token != null && !token.isBlank()) {
                  if (pendingSpace && builder.length() > 0 && !this.startsWithDecorator(token)) {
                     builder.append(' ');
                  }

                  builder.append(token);
                  pendingSpace = false;
               }
            } else {
               pendingSpace = builder.length() > 0;
            }
         }

         String result = TagParts.clean(builder.toString());
         return stripDecorators ? this.stripTagDecorators(result) : result;
      } else {
         return "";
      }
   }

   private boolean startsWithDecorator(String token) {
      if (token.isEmpty()) {
         return false;
      } else {
         char c = token.charAt(0);
         return c == ']' || c == ')' || c == '}' || c == '>' || c == ':' || c == '|';
      }
   }

   private String stripTagDecorators(String text) {
      if (text != null && !text.isBlank()) {
         String result = text.trim();

         boolean changed;
         do {
            changed = false;
            if (result.length() >= 2 && TagParts.isOpeningDecorator(result.charAt(0)) && TagParts.isClosingDecorator(result.charAt(result.length() - 1))) {
               result = result.substring(1, result.length() - 1).trim();
               changed = true;
            }

            while (!result.isEmpty() && TagParts.isLooseDecorator(result.charAt(0))) {
               result = result.substring(1).trim();
               changed = true;
            }

            while (!result.isEmpty() && TagParts.isLooseDecorator(result.charAt(result.length() - 1))) {
               result = result.substring(0, result.length() - 1).trim();
               changed = true;
            }
         } while (changed);

         return result;
      } else {
         return "";
      }
   }

   private String normalizeReallyWorldChar(char value) {
      if (Character.isISOControl(value)) {
         return "";
      } else {
         return switch (value) {
            case 'ǫ' -> "Q";
            case 'ɢ' -> "G";
            case 'ɪ' -> "I";
            case 'ɴ' -> "N";
            case 'ʀ' -> "R";
            case 'ʏ' -> "Y";
            case 'ʙ' -> "B";
            case 'ʜ' -> "H";
            case 'ʟ' -> "L";
            case 'ғ' -> "F";
            case 'ᴀ' -> "A";
            case 'ᴄ' -> "C";
            case 'ᴅ' -> "D";
            case 'ᴇ' -> "E";
            case 'ᴊ' -> "J";
            case 'ᴋ' -> "K";
            case 'ᴍ' -> "M";
            case 'ᴏ' -> "O";
            case 'ᴘ' -> "P";
            case 'ᴛ' -> "T";
            case 'ᴜ' -> "U";
            case 'ᴠ' -> "V";
            case 'ᴡ' -> "W";
            case 'ꜱ' -> "S";
            default -> String.valueOf(value);
         };
      }
   }

   private String getIconLabel(char c) {
      return switch (c) {
         case 'ꔀ' -> "PLAYER";
         case 'ꔁ' -> "MEDIA";
         default -> null;
         case 'ꔄ' -> "HERO";
         case 'ꔅ' -> "YT";
         case 'ꔈ' -> "TITAN";
         case 'ꔉ' -> "HELPER";
         case 'ꔒ' -> "AVENGER";
         case 'ꔓ' -> "ML.MODER";
         case 'ꔖ' -> "OVERLORD";
         case 'ꔗ' -> "MODER";
         case 'ꔠ' -> "MAGISTER";
         case 'ꔡ' -> "MODER+";
         case 'ꔤ' -> "IMPERATOR";
         case 'ꔥ' -> "ST.MODER";
         case 'ꔨ' -> "DRAGON";
         case 'ꔩ' -> "GL.MODER";
         case 'ꔲ' -> "BULL";
         case 'ꔳ' -> "ML.ADMIN";
         case 'ꔶ' -> "TIGER";
         case 'ꔷ' -> "ADMIN";
         case 'ꕀ' -> "HYDRA";
         case 'ꕄ' -> "DRACULA";
         case 'ꕅ' -> "VAMPIRE";
         case 'ꕈ' -> "COBRA";
         case 'ꕉ' -> "PEGAS";
         case 'ꕒ' -> "RABBIT";
         case 'ꕖ' -> "BUNNY";
         case 'ꕠ' -> "D.HELPER";
      };
   }

   private static record DonationPrefix(List<TagPart> donation, List<TagPart> prefix) {
   }

   private static record LegacyFormatResult(int color, int nextIndex) {
   }

   private static record NameRange(int start, int end) {
   }

   private static record VoiceDonationPrefix(List<TagPart> voice, List<TagPart> donation, List<TagPart> prefix) {
   }

   private static record VoiceStatusSplit(List<TagPart> voice, List<TagPart> remaining) {
   }
}
