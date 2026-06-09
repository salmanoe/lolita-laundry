package id.co.lolita.laundry.billing.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Indonesian-locale formatting for billing documents and helpers for document numbering.
 * Money and dates are turned into display strings here so the PDF templates only print text.
 */
final class BillingFormats {

    private BillingFormats() {
    }

    /**
     * Fixed payment terms printed on the monthly invoice.
     */
    static final String PAYMENT_TERMS = "2 Days";

    private static final String[] MONTHS_ID = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private static final String[] MONTHS_EN = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };

    private static final DecimalFormatSymbols ID_SYMBOLS = idSymbols();

    private static DecimalFormatSymbols idSymbols() {
        var s = new DecimalFormatSymbols();
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return s;
    }

    /**
     * "Rp 1.234.567" or "Rp 1.234.567,50" — up to 2 decimals, no trailing zeros.
     */
    static String money(BigDecimal value) {
        return "Rp " + new DecimalFormat("#,##0.##", ID_SYMBOLS).format(value == null ? BigDecimal.ZERO : value);
    }

    /**
     * Quantity with up to 3 decimals, no trailing zeros (e.g. "12" or "2,5").
     */
    static String quantity(BigDecimal value) {
        return new DecimalFormat("#,##0.###", ID_SYMBOLS).format(value == null ? BigDecimal.ZERO : value);
    }

    /**
     * "6 Juni 2026".
     */
    static String longDate(LocalDate date) {
        return "%d %s %d".formatted(date.getDayOfMonth(), MONTHS_ID[date.getMonthValue() - 1], date.getYear());
    }

    /**
     * "06/06/2026".
     */
    static String shortDate(LocalDate date) {
        return "%02d/%02d/%d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    /**
     * "Juni 2026".
     */
    static String periodLabel(int year, int month) {
        return "%s %d".formatted(MONTHS_ID[month - 1], year);
    }

    /**
     * "01/06/26" — dd/mm/yy, matching the invoice template's Date field.
     */
    static String shortDateYy(LocalDate date) {
        return "%02d/%02d/%02d".formatted(date.getDayOfMonth(), date.getMonthValue(), date.getYear() % 100);
    }

    /**
     * The single description line on the monthly invoice — the full billed month, first to last
     * day, e.g. "Laundry Periode 1 June - 30 June 2026". English month names match the template.
     */
    static String periodDescription(int year, int month) {
        var ym = YearMonth.of(year, month);
        String m = MONTHS_EN[month - 1];
        return "Laundry Periode 1 %s - %d %s %d".formatted(m, ym.lengthOfMonth(), m, year);
    }

    // ── Terbilang (Indonesian amount in words) ──

    private static final String[] TERBILANG_ONES = {
            "", "satu", "dua", "tiga", "empat", "lima", "enam", "tujuh", "delapan", "sembilan",
            "sepuluh", "sebelas"
    };

    /**
     * Indonesian amount-in-words for the invoice's Terbilang line, e.g.
     * 3_079_000 → "Tiga Juta Tujuh Puluh Sembilan Ribu Rupiah". Rounded to whole rupiah.
     */
    static String terbilang(BigDecimal amount) {
        long n = (amount == null ? BigDecimal.ZERO : amount).setScale(0, RoundingMode.HALF_UP).longValueExact();
        // A net-negative total can occur on a credit-carrying DRAFT (KI-11). spell() indexes its
        // word tables with the value, so a negative n would throw — spell the magnitude and prefix
        // "Minus" so the ledger draft still renders.
        if (n < 0) {
            return titleCase("minus " + spell(-n).trim()) + " Rupiah";
        }
        String words = n == 0 ? "nol" : spell(n).trim();
        return titleCase(words) + " Rupiah";
    }

    private static String spell(long n) {
        if (n < 12) return TERBILANG_ONES[(int) n];
        if (n < 20) return TERBILANG_ONES[(int) (n - 10)] + " belas";
        if (n < 100) return TERBILANG_ONES[(int) (n / 10)] + " puluh" + tail(n % 10);
        if (n < 200) return "seratus" + tail(n % 100);
        if (n < 1_000) return TERBILANG_ONES[(int) (n / 100)] + " ratus" + tail(n % 100);
        if (n < 2_000) return "seribu" + tail(n % 1_000);
        if (n < 1_000_000) return spell(n / 1_000) + " ribu" + tail(n % 1_000);
        if (n < 1_000_000_000) return spell(n / 1_000_000) + " juta" + tail(n % 1_000_000);
        if (n < 1_000_000_000_000L) return spell(n / 1_000_000_000) + " miliar" + tail(n % 1_000_000_000);
        return spell(n / 1_000_000_000_000L) + " triliun" + tail(n % 1_000_000_000_000L);
    }

    private static String tail(long n) {
        return n == 0 ? "" : " " + spell(n);
    }

    private static String titleCase(String s) {
        var sb = new StringBuilder(s.length());
        boolean cap = true;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                cap = true;
                sb.append(c);
            } else {
                sb.append(cap ? Character.toUpperCase(c) : c);
                cap = false;
            }
        }
        return sb.toString();
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