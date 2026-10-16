package com.plummy.vio.client.markdown;

import com.plummy.vio.client.VioTheme;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.function.UnaryOperator;

// Inline markdown to styled text: **bold**, *italic* or _italic_, ~~strikethrough~~, __underline__, `code`,
// [links](https://...) and bare https:// links. Styles nest; a backslash escapes a punctuation character.
// Anything that does not close stays as typed. Plain text keeps the color it is drawn with.
public final class InlineMarkdown {

    // Tags code runs, so the renderer can find them and draw their background. Insertion is otherwise unused here.
    private static final String CODE_MARKER = "vio:code";
    private static final String[] LINK_SCHEMES = {"https://", "http://"};
    // Left off the end of a bare link: usually the sentence's punctuation, not part of the address.
    private static final String TRAILING_PUNCTUATION = ".,;:!?'\"";

    private static final Delimiter[] DELIMITERS = {
            // Longer delimiters first, so ** is not read as two *.
            new Delimiter("**", style -> style.withBold(true), false),
            new Delimiter("__", style -> style.withUnderlined(true), true),
            new Delimiter("~~", style -> style.withStrikethrough(true), false),
            new Delimiter("*", style -> style.withItalic(true), false),
            new Delimiter("_", style -> style.withItalic(true), true)
    };

    private InlineMarkdown() {
    }

    public static boolean isCode(Style style) {
        return CODE_MARKER.equals(style.getInsertion());
    }

    public static Component parse(String text) {
        MutableComponent result = Component.empty();
        parse(text, 0, text.length(), Style.EMPTY, result);
        return result;
    }

    private static void parse(String text, int start, int end, Style style, MutableComponent out) {
        StringBuilder literal = new StringBuilder();
        int i = start;

        while (i < end) {
            char c = text.charAt(i);

            if (c == '\\' && i + 1 < end && isEscapable(text.charAt(i + 1))) {
                literal.append(text.charAt(i + 1));
                i += 2;
                continue;
            }

            if (c == '`') {
                int next = parseCode(text, i, end, style, literal, out);

                if (next > i) {
                    i = next;
                    continue;
                }
            }

            if (c == '[') {
                int next = parseLink(text, i, end, style, literal, out);

                if (next > i) {
                    i = next;
                    continue;
                }
            }

            if (isLinkStart(text, i)) {
                int next = parseBareLink(text, i, end, style, literal, out);

                if (next > i) {
                    i = next;
                    continue;
                }
            }

            Delimiter delimiter = openingAt(text, i, end);

            if (delimiter != null) {
                int close = findClose(text, delimiter, i + delimiter.marker().length(), end);

                if (close >= 0) {
                    flush(literal, style, out);
                    parse(text, i + delimiter.marker().length(), close, delimiter.apply().apply(style), out);
                    i = close + delimiter.marker().length();
                    continue;
                }
            }

            literal.append(c);
            i++;
        }

        flush(literal, style, out);
    }

    // `code`, opened and closed by runs of the same number of backticks, so ``a ` inside`` works too.
    // Nothing inside is parsed. Returns where parsing continues, or `start` when this is no code span.
    private static int parseCode(String text, int start, int end, Style style, StringBuilder literal, MutableComponent out) {
        int contentStart = start;

        while (contentStart < end && text.charAt(contentStart) == '`') {
            contentStart++;
        }

        int length = contentStart - start;
        int index = contentStart;

        while (index < end) {
            if (text.charAt(index) != '`') {
                index++;
                continue;
            }

            int runEnd = index;

            while (runEnd < end && text.charAt(runEnd) == '`') {
                runEnd++;
            }

            if (runEnd - index == length && index > contentStart) {
                String code = text.substring(contentStart, index);

                // One space on each side lets code start or end with a backtick; it is not part of the code.
                if (code.length() >= 2 && code.startsWith(" ") && code.endsWith(" ") && !code.isBlank()) {
                    code = code.substring(1, code.length() - 1);
                }

                flush(literal, style, out);
                out.append(Component.literal(code).withStyle(style
                        .withColor(TextColor.fromRgb(VioTheme.CODE_TEXT & 0xFFFFFF))
                        .withInsertion(CODE_MARKER)));
                return runEnd;
            }

            index = runEnd;
        }

        // Unclosed: the whole run of backticks stays as typed.
        literal.append(text, start, contentStart);
        return contentStart;
    }

    // [text](https://...), where the text may hold other markdown. Returns `start` when this is no link.
    private static int parseLink(String text, int start, int end, Style style, StringBuilder literal, MutableComponent out) {
        int textEnd = findUnescaped(text, ']', start + 1, end);

        if (textEnd < 0 || textEnd == start + 1 || textEnd + 1 >= end || text.charAt(textEnd + 1) != '(') {
            return start;
        }

        int urlEnd = findUnescaped(text, ')', textEnd + 2, end);

        if (urlEnd < 0) {
            return start;
        }

        String url = text.substring(textEnd + 2, urlEnd).strip();

        if (!isLinkStart(url, 0) || url.chars().anyMatch(Character::isWhitespace)) {
            return start;
        }

        flush(literal, style, out);
        parse(text, start + 1, textEnd, linkStyle(style, url), out);
        return urlEnd + 1;
    }

    // A bare https:// address, up to the next whitespace. Returns `start` when there is no address after the scheme.
    private static int parseBareLink(String text, int start, int end, Style style, StringBuilder literal, MutableComponent out) {
        int urlEnd = start;

        while (urlEnd < end && !Character.isWhitespace(text.charAt(urlEnd)) && text.charAt(urlEnd) != '<' && text.charAt(urlEnd) != '>') {
            urlEnd++;
        }

        while (urlEnd > start && shouldTrim(text, start, urlEnd)) {
            urlEnd--;
        }

        String url = text.substring(start, urlEnd);

        if (!hasAddress(url)) {
            return start;
        }

        flush(literal, style, out);
        out.append(Component.literal(url).withStyle(linkStyle(style, url)));
        return urlEnd;
    }

    // Trailing punctuation is dropped, and so is a closing parenthesis without an opening one in the address,
    // as in "(see https://example.com)"; balanced ones stay, as in wiki links.
    private static boolean shouldTrim(String text, int start, int end) {
        char last = text.charAt(end - 1);

        if (TRAILING_PUNCTUATION.indexOf(last) >= 0) {
            return true;
        }

        if (last != ')') {
            return false;
        }

        String url = text.substring(start, end);
        return url.chars().filter(c -> c == '(').count() < url.chars().filter(c -> c == ')').count();
    }

    // A scheme followed by at least something.
    private static boolean hasAddress(String url) {
        for (String scheme : LINK_SCHEMES) {
            if (url.startsWith(scheme) && url.length() > scheme.length()) {
                return true;
            }
        }

        return false;
    }

    // A link scheme at a word start, so "xhttps://" is not one.
    private static boolean isLinkStart(String text, int index) {
        if (index > 0 && Character.isLetterOrDigit(text.charAt(index - 1))) {
            return false;
        }

        for (String scheme : LINK_SCHEMES) {
            if (text.startsWith(scheme, index)) {
                return true;
            }
        }

        return false;
    }

    private static Style linkStyle(Style style, String url) {
        return style
                .withColor(TextColor.fromRgb(VioTheme.LINK_TEXT & 0xFFFFFF))
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    private static int findUnescaped(String text, char target, int from, int end) {
        for (int index = from; index < end; index++) {
            char c = text.charAt(index);

            if (c == '\\') {
                index++;
            } else if (c == target) {
                return index;
            }
        }

        return -1;
    }

    // The delimiter that can open a span here: followed by a non-space, and for underscores not inside a word.
    private static Delimiter openingAt(String text, int index, int end) {
        for (Delimiter delimiter : DELIMITERS) {
            int after = index + delimiter.marker().length();

            if (!text.startsWith(delimiter.marker(), index) || after >= end || Character.isWhitespace(text.charAt(after))) {
                continue;
            }

            if (delimiter.wordBound() && index > 0 && Character.isLetterOrDigit(text.charAt(index - 1))) {
                continue;
            }

            return delimiter;
        }

        return null;
    }

    // Where the span opened at `from` closes: the delimiter after a non-space, and for underscores not inside a word.
    // Returns -1 when it never closes.
    private static int findClose(String text, Delimiter delimiter, int from, int end) {
        String marker = delimiter.marker();
        char markerChar = marker.charAt(0);
        int index = from + 1;

        while (index < end) {
            if (text.charAt(index) == '\\') {
                index += 2;
                continue;
            }

            if (text.charAt(index) != markerChar) {
                index++;
                continue;
            }

            // A run of the marker character, like the *** that closes ***both***.
            int runEnd = index;

            while (runEnd < end && text.charAt(runEnd) == markerChar) {
                runEnd++;
            }

            int runLength = runEnd - index;

            // A single * or _ is not closed by half of a ** or __ run that belongs to a nested span.
            boolean fits = marker.length() == 1 ? runLength % 2 == 1 || runLength >= 3 : runLength >= 2;

            if (fits && !Character.isWhitespace(text.charAt(index - 1))) {
                // Close with the end of the run, so the inner span gets the rest of it.
                int close = runEnd - marker.length();
                boolean insideWord = delimiter.wordBound() && runEnd < end && Character.isLetterOrDigit(text.charAt(runEnd));

                if (!insideWord) {
                    return close;
                }
            }

            index = runEnd;
        }

        return -1;
    }

    private static void flush(StringBuilder literal, Style style, MutableComponent out) {
        if (!literal.isEmpty()) {
            out.append(Component.literal(literal.toString()).withStyle(style));
            literal.setLength(0);
        }
    }

    private static boolean isEscapable(char c) {
        return c < 128 && !Character.isLetterOrDigit(c) && !Character.isWhitespace(c);
    }

    // `wordBound`: only opens and closes at word edges, so snake_case names stay as typed.
    private record Delimiter(String marker, UnaryOperator<Style> apply, boolean wordBound) {
    }
}
