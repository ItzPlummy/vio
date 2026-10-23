package com.plummy.vio.voice;

import java.util.regex.Pattern;

// Turns a markdown reply into text worth reading aloud: formatting goes, link text stays, code blocks are skipped.
public final class SpokenText {

    private static final Pattern CODE_BLOCK = Pattern.compile("```.*?(```|$)", Pattern.DOTALL);
    private static final Pattern IMAGE = Pattern.compile("!\\[([^]]*)]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^]]*)]\\([^)]*\\)");
    // Not including punctuation right after it, which belongs to the sentence.
    private static final Pattern URL = Pattern.compile("<?https?://[^\\s<>]*[^\\s<>.,;:!?)]>?");
    // Struck-through text is what the reply took back, so it isn't read at all.
    private static final Pattern STRIKETHROUGH = Pattern.compile("~~(?=\\S)(.+?)(?<=\\S)~~");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern TABLE_RULE = Pattern.compile("(?m)^\\s*\\|?\\s*:?-{3,}.*$");
    private static final Pattern LINE_MARKER = Pattern.compile("(?m)^\\s*(#{1,6}\\s+|>\\s?|[-*+]\\s+)");
    private static final Pattern STARS = Pattern.compile("(\\*{1,3})(?=\\S)(.+?)(?<=\\S)\\1");
    // Only around whole words, so identifiers like oak_log keep their underscores.
    private static final Pattern UNDERSCORES = Pattern.compile("(?<!\\w)(_{1,3})(?=\\S)(.+?)(?<=\\S)\\1(?!\\w)");
    private static final Pattern TABLE_EDGE = Pattern.compile("(?m)^[ \\t]*\\|[ \\t]*|[ \\t]*\\|[ \\t]*$");
    private static final Pattern TABLE_PIPE = Pattern.compile("[ \\t]*\\|[ \\t]*");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n+");

    private SpokenText() {
    }

    public static String fromMarkdown(String markdown) {
        String text = CODE_BLOCK.matcher(markdown).replaceAll("");

        text = IMAGE.matcher(text).replaceAll("$1");
        text = LINK.matcher(text).replaceAll("$1");
        text = URL.matcher(text).replaceAll("");
        text = INLINE_CODE.matcher(text).replaceAll("$1");
        text = TABLE_RULE.matcher(text).replaceAll("");
        text = LINE_MARKER.matcher(text).replaceAll("");
        text = STRIKETHROUGH.matcher(text).replaceAll("");
        text = STARS.matcher(text).replaceAll("$2");
        text = UNDERSCORES.matcher(text).replaceAll("$2");
        text = TABLE_EDGE.matcher(text).replaceAll("");
        text = TABLE_PIPE.matcher(text).replaceAll(", ");
        text = BLANK_LINES.matcher(text).replaceAll("\n");

        return text.strip();
    }
}
