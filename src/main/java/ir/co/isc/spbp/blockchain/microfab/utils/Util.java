package ir.co.isc.spbp.blockchain.microfab.utils;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Util {

    public static <T> T toUnchecked(Callable<T> throwable) {
        try {
            return throwable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @UtilityClass
    public static class Time {

        private static final Pattern TIMEOUT_PATTERN = Pattern.compile("(\\d+)([smhSMH])");

        public static Duration parse(String raw) {
            Objects.requireNonNull(raw);
            Matcher m = TIMEOUT_PATTERN.matcher(raw.trim());

            if (!m.matches()) {
                throw new IllegalArgumentException("Invalid timeout: " + raw);
            }

            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase();

            return switch (unit) {
                case "s" -> Duration.ofSeconds(value);
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
            };
        }
    }
}
