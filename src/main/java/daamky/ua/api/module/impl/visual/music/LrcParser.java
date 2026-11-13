package daamky.ua.api.module.impl.visual.music;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Small, allocation-light parser for timestamped .lrc files. */
public final class LrcParser {
    private static final Pattern TIME = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]");

    private LrcParser() { }

    public static List<LrcTrack> read(Path file) throws IOException {
        List<LrcTrack> result = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            Matcher matcher = TIME.matcher(line);
            List<Long> stamps = new ArrayList<>(2);
            int end = 0;
            while (matcher.find()) {
                long minutes = Long.parseLong(matcher.group(1));
                long seconds = Long.parseLong(matcher.group(2));
                String fraction = matcher.group(3);
                long millis = 0L;
                if (fraction != null) {
                    if (fraction.length() == 1) millis = Long.parseLong(fraction) * 100L;
                    else if (fraction.length() == 2) millis = Long.parseLong(fraction) * 10L;
                    else millis = Long.parseLong(fraction.substring(0, 3));
                }
                stamps.add((minutes * 60L + seconds) * 1000L + millis);
                end = matcher.end();
            }
            if (stamps.isEmpty()) continue;
            String lyric = line.substring(end).trim();
            if (lyric.isEmpty()) continue;
            for (long stamp : stamps) result.add(new LrcTrack(stamp, lyric));
        }
        result.sort(Comparator.comparingLong(LrcTrack::timeMillis));
        return result;
    }
}
