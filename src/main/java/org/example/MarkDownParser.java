package org.example;

public class MarkDownParser {
    public static String heading1(String heading) {
        return "# " + heading;
    }

    public static String heading2(String heading) {
        return "## " + heading;
    }

    public static String heading3(String heading) {
        return "### " + heading;
    }

    public static String bulletPoint(String point) {
        return "- " + point;
    }

    public static String codeBlock(String codeBlock) {
        return "```" + codeBlock + "```";
    }

    public static String boldText(String text) {
        return "**" + text + "**";
    }

    public static String quoteText(String text) {
        return "> " + text;
    }

    public static String newLine() {
        return "\\n";
    }

    public static String italicText(String text) {
        return "* *" + text + "* *";
    }

    public static String boldAndItalicText(String text) {
        return "***" + text + "***";
    }

    public static String task(String text, boolean checked) {
        return checked ? "- [ ]" + text : "- [x]" + text;
    }
}
