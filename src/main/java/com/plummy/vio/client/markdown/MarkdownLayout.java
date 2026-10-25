package com.plummy.vio.client.markdown;

import com.plummy.vio.client.TextLine;
import com.plummy.vio.client.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownLayout {

    public static final int LINE_HEIGHT = 9;
    private static final int BLOCK_GAP = 5;
    private static final int MIN_WRAP_WIDTH = 24;
    private static final int TAB_WIDTH = 4;
    private static final int CODE_PADDING_X = 3;
    private static final int CODE_PADDING_Y = 2;
    private static final int CODE_MARGIN = 2;
    private static final String[] BULLETS = {"•", "◦"};

    private static final Pattern HEADING = Pattern.compile("^ {0,3}#{1,6}\\s+(.*?)(?:\\s+#+)?\\s*$");
    private static final Pattern FENCE = Pattern.compile("^( *)(`{3,}|~{3,}).*$");
    private static final Pattern ITEM = Pattern.compile("^( *)([-*+]|\\d{1,9}[.)])\\s+(.*)$");

    private MarkdownLayout() {
    }

    public static Layout layout(Font font, String text, int width) {
        List<Block> blocks = parse(text);
        Map<Item, Integer> markerWidths = measureMarkers(font, blocks);
        List<TextLine> lines = new ArrayList<>();
        List<Panel> panels = new ArrayList<>();
        List<Integer> contentXByDepth = new ArrayList<>();
        int y = 0;
        boolean gap = false;

        for (Block block : blocks) {
            if (block instanceof Blank) {
                gap = !lines.isEmpty() || !panels.isEmpty();
                continue;
            }

            if (gap) {
                y += BLOCK_GAP;
                gap = false;
            }

            switch (block) {
                case Paragraph paragraph -> y = addWrapped(font, lines, InlineMarkdown.parse(paragraph.text()), 0, y, width);
                case Heading heading -> {
                    Component title = Component.empty()
                            .withStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(Theme.HEADING_TEXT & 0xFFFFFF)))
                            .append(InlineMarkdown.parse(heading.text()));

                    y = addWrapped(font, lines, title, 0, y, width);
                }
                case Item item -> {
                    int x = item.depth() == 0 ? 0 : contentXByDepth.get(item.depth() - 1);
                    int markerWidth = markerWidths.get(item);
                    int contentX = x + markerWidth + font.width(" ");

                    while (contentXByDepth.size() <= item.depth()) {
                        contentXByDepth.add(0);
                    }

                    contentXByDepth.set(item.depth(), contentX);

                    lines.add(new TextLine(FormattedCharSequence.forward(item.marker(), Style.EMPTY), x + markerWidth - font.width(item.marker()), y));
                    y = addWrapped(font, lines, InlineMarkdown.parse(item.text()), contentX, y, width);
                }
                case Code code -> {
                    int x = code.depth() < 0 ? 0 : contentXByDepth.get(code.depth());

                    if (!lines.isEmpty() || !panels.isEmpty()) {
                        y += CODE_MARGIN;
                    }

                    Panel panel = addCode(font, lines, code.lines(), x, y, width);
                    panels.add(panel);
                    y = panel.y() + panel.height() + CODE_MARGIN;
                }
                default -> throw new IllegalStateException();
            }
        }

        return new Layout(lines, panels);
    }

    private static Panel addCode(Font font, List<TextLine> lines, List<String> code, int x, int y, int width) {
        Style style = Style.EMPTY.withColor(TextColor.fromRgb(Theme.CODE_TEXT & 0xFFFFFF));
        int textX = x + CODE_PADDING_X;
        int textY = y + CODE_PADDING_Y;
        int textWidth = 0;

        for (String codeLine : code.isEmpty() ? List.of("") : code) {
            if (codeLine.isEmpty()) {
                textY += LINE_HEIGHT;
                continue;
            }

            for (FormattedCharSequence row : font.split(Component.literal(codeLine).withStyle(style), Math.max(MIN_WRAP_WIDTH, width - textX - CODE_PADDING_X))) {
                lines.add(new TextLine(row, textX, textY));
                textWidth = Math.max(textWidth, font.width(row));
                textY += LINE_HEIGHT;
            }
        }

        return new Panel(x, y, Math.max(0, textWidth - 1) + CODE_PADDING_X * 2, textY - y - 1 + CODE_PADDING_Y);
    }

    private static int addWrapped(Font font, List<TextLine> lines, FormattedText text, int x, int y, int width) {
        for (FormattedCharSequence line : font.split(text, Math.max(MIN_WRAP_WIDTH, width - x))) {
            lines.add(new TextLine(line, x, y));
            y += LINE_HEIGHT;
        }

        return y;
    }

    private static List<Block> parse(String text) {
        List<Block> blocks = new ArrayList<>();
        List<Integer> indents = new ArrayList<>();

        String[] rawLines = text.split("\\r?\\n", -1);

        for (int i = 0; i < rawLines.length; i++) {
            String line = expandTabs(rawLines[i]);
            Matcher fence = FENCE.matcher(line);

            if (fence.matches()) {
                int indent = fence.group(1).length();
                String marker = fence.group(2);
                List<String> code = new ArrayList<>();

                for (i++; i < rawLines.length; i++) {
                    String codeLine = expandTabs(rawLines[i]);

                    if (isClosingFence(codeLine, marker)) {
                        break;
                    }

                    code.add(removeIndent(codeLine, indent));
                }

                int depth = indent > 0 && !indents.isEmpty() ? indents.size() - 1 : -1;

                if (depth < 0) {
                    indents.clear();
                }

                blocks.add(new Code(depth, code));
                continue;
            }

            if (line.isBlank()) {
                blocks.add(new Blank());
                continue;
            }

            Matcher heading = HEADING.matcher(line);

            if (heading.matches()) {
                indents.clear();
                blocks.add(new Heading(heading.group(1)));
                continue;
            }

            Matcher item = ITEM.matcher(line);

            if (item.matches()) {
                int indent = item.group(1).length();

                while (!indents.isEmpty() && indent < indents.getLast()) {
                    indents.removeLast();
                }

                if (indents.isEmpty() || indent > indents.getLast()) {
                    indents.add(indent);
                }

                int depth = indents.size() - 1;
                String marker = item.group(2);
                boolean numbered = Character.isDigit(marker.charAt(0));

                blocks.add(new Item(depth, numbered ? marker : BULLETS[depth % BULLETS.length], numbered, item.group(3)));
                continue;
            }

            if (line.startsWith(" ") && !blocks.isEmpty() && blocks.getLast() instanceof Item last) {
                blocks.set(blocks.size() - 1, new Item(last.depth(), last.marker(), last.numbered(), last.text() + "\n" + line.strip()));
                continue;
            }

            indents.clear();
            blocks.add(new Paragraph(line));
        }

        return blocks;
    }

    private static Map<Item, Integer> measureMarkers(Font font, List<Block> blocks) {
        Map<Item, Integer> widths = new IdentityHashMap<>();
        List<Item> list = new ArrayList<>();

        for (Block block : blocks) {
            if (block instanceof Item item) {
                list.add(item);
            } else if (!(block instanceof Blank)) {
                measureList(font, list, widths);
                list.clear();
            }
        }

        measureList(font, list, widths);
        return widths;
    }

    private static void measureList(Font font, List<Item> list, Map<Item, Integer> widths) {
        Map<Integer, Integer> numberWidths = new HashMap<>();

        for (Item item : list) {
            if (item.numbered()) {
                numberWidths.merge(item.depth(), font.width(item.marker()), Math::max);
            }
        }

        for (Item item : list) {
            widths.put(item, item.numbered() ? numberWidths.get(item.depth()) : font.width(item.marker()));
        }
    }

    private static boolean isClosingFence(String line, String marker) {
        String stripped = line.strip();

        return stripped.length() >= marker.length() && stripped.chars().allMatch(c -> c == marker.charAt(0));
    }

    private static String removeIndent(String line, int indent) {
        int start = 0;

        while (start < indent && start < line.length() && line.charAt(start) == ' ') {
            start++;
        }

        return line.substring(start);
    }

    private static String expandTabs(String line) {
        StringBuilder expanded = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\t') {
                expanded.repeat(" ", TAB_WIDTH - expanded.length() % TAB_WIDTH);
            } else {
                expanded.append(c);
            }
        }

        return expanded.toString();
    }

    public record Layout(List<TextLine> lines, List<Panel> panels) {
    }

    public record Panel(int x, int y, int width, int height) {
    }

    private sealed interface Block permits Blank, Paragraph, Heading, Item, Code {
    }

    private record Blank() implements Block {
    }

    private record Paragraph(String text) implements Block {
    }

    private record Heading(String text) implements Block {
    }

    private record Item(int depth, String marker, boolean numbered, String text) implements Block {
    }

    private record Code(int depth, List<String> lines) implements Block {
    }
}
