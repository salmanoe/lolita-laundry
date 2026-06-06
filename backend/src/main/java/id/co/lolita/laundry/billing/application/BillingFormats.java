package id.co.lolita.laundry.billing.application;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;

/**
 * Indonesian-locale formatting for billing documents and helpers for document numbering.
 * Money and dates are turned into display strings here so the PDF templates only print text.
 */
final class BillingFormats {

    private BillingFormats() {
    }

    private static final String[] MONTHS_ID = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private static final DecimalFormatSymbols ID_SYMBOLS = idSymbols();

    private static DecimalFormatSymbols idSymbols() {
        var s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return s;
    }

    /** "Rp 1.234.567" or "Rp 1.234.567,50" — up to 2 decimals, no trailing zeros. */
    static String money(BigDecimal value) {
        return "Rp " + new DecimalFormat("#,##0.##", ID_SYMBOLS).format(value == null ? BigDecimal.ZERO : value);
    }

    /** Quantity with up to 3 decimals, no trailing zeros (e.g. "12" or "2,5"). */
    static String quantity(BigDecimal value) {
        return new DecimalFormat("#,##0.###", ID_SYMBOLS).format(value == null ? BigDecimal.ZERO : value);
    }

    /** "6 Juni 2026". */
    static String longDate(LocalDate date) {
        return "%d %s %d".formatted(date.getDayOfMonth(), MONTHS_ID[date.getMonthValue() - 1], date.getYear());
    }

    /** "06/06/2026". */
    static String shortDate(LocalDate date) {
        return "%02d/%02d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    /** "Juni 2026". */
    static String periodLabel(int year, int month) {
        return "%s %d".formatted(MONTHS_ID[month - 1], year);
    }

    /**
     * Short uppercase abbreviation of a department name for the billing number suffix
     * (e.g. "Room Linen" → "RL", "F&B Linen" → "FBL"). Splits on non-letters, takes initials,
     * caps at 4 chars. Falls back to the department id when no letters are present.
     */
    static String departmentAbbrev(String departmentName, Long departmentId) {
        if (departmentName != null) {
            var sb = new StringBuilder();
            for (String word : departmentName.split("[^A-Za-z]+")) {
                if (!word.isBlank()) {
                    sb.append(Character.toUpperCase(word.charAt(0)));
                }
            }
            if (!sb.isEmpty()) {
                return sb.length() > 4 ? sb.substring(0, 4) : sb.toString();
            }
        }
        return "D" + departmentId;
    }
}