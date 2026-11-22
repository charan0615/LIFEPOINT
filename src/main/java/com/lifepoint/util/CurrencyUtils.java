package com.lifepoint.util;

import java.util.Map;

public class CurrencyUtils {
    private static final Map<String, String> SYMBOLS = Map.of(
        "INR", "₹",
        "USD", "$",
        "EUR", "€",
        "GBP", "£",
        "AED", "AED ",
        "CAD", "C$",
        "AUD", "A$",
        "JPY", "¥",
        "SGD", "S$"
    );

    public static String getSymbol(String currencyCode) {
        if (currencyCode == null) return "₹";
        return SYMBOLS.getOrDefault(currencyCode.toUpperCase(), currencyCode + " ");
    }
}
