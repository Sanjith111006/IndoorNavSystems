package com.indoornav.backend.service;

/** Number formats for route output: one-decimal metres on the wire, whole metres in text. */
final class RouteFormatting {

    private static final double TENTHS = 10.0;

    private RouteFormatting() {
    }

    static double oneDecimal(double metres) {
        return Math.round(metres * TENTHS) / TENTHS;
    }

    static long wholeSeconds(double seconds) {
        return Math.round(seconds);
    }

    /** Text such as "145 m". A segment shorter than half a metre still reads "1 m", never "0 m". */
    static String wholeMetres(double metres) {
        return Math.max(1L, Math.round(metres)) + " m";
    }
}
