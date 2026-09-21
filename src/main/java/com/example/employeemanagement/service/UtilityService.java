package com.example.employeemanagement.service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class UtilityService {

    private static final String FALLBACK_INITIALS = "EMP";
    private static final int MAX_INITIALS = 3;

    private final Clock clock;

    public UtilityService(Clock clock) {
        this.clock = clock;
    }

    public String formatName(String name) {
        return name.strip().replaceAll("\\s+", " ");
    }

    public LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    /**
     * Builds a readable employee code from the initials of the name and a sequence
     * number, for example "Nguyễn Đức Minh" with sequence 7 becomes "NDM-0007".
     */
    public String generateEmployeeCode(String name, long sequence) {
        StringBuilder initials = new StringBuilder();
        for (String part : formatName(name).split(" ")) {
            String letters = toAsciiLetters(part);
            if (!letters.isEmpty()) {
                initials.append(letters.charAt(0));
            }
        }

        String prefix = initials.isEmpty() ? FALLBACK_INITIALS : initials.toString();
        if (prefix.length() > MAX_INITIALS) {
            prefix = prefix.substring(prefix.length() - MAX_INITIALS);
        }
        return "%s-%04d".formatted(prefix.toUpperCase(Locale.ROOT), sequence);
    }

    private static String toAsciiLetters(String value) {
        // Decompose first so accents become separate marks, then fold the Vietnamese
        // letters that have no canonical decomposition, then drop everything else.
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .replace('ư', 'u')
                .replace('Ư', 'U')
                .replace('ơ', 'o')
                .replace('Ơ', 'O')
                .replaceAll("[^A-Za-z]", "");
    }
}
