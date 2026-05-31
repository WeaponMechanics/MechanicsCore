package me.deecaad.core.mechanics.parse;

import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.ast.InlineCallNode;
import me.deecaad.core.mechanics.ast.Loc;
import me.deecaad.core.mechanics.ast.SourceRef;
import me.deecaad.core.mechanics.ast.Span;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.ast.SubjectNode;
import me.deecaad.core.mechanics.diagnostic.DiagnosticReporter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses one config line into a {@link StmtNode} with source spans. Purely
 * syntactic: name resolution and arg validation happen in sema. Records
 * diagnostics and returns an {@link StmtNode.Error} on a malformed line so
 * sibling lines keep parsing.
 */
public final class StatementParser {

    // Splits a line into the mechanic segment, the @subject, and ?conditions
    // (handles escaped \@ and \?).
    private static final Pattern SEGMENTS = Pattern.compile(".+?(?=(?<!\\\\)(?:\\\\\\\\)*[?@]|$)");

    private StatementParser() {
    }

    public static @NotNull StmtNode parse(@NotNull String rawLine, int listIndex, File file,
                                          @NotNull String configPath, @NotNull DiagnosticReporter reporter) {
        SourceRef source = new SourceRef(file, configPath, listIndex, rawLine);
        String trimmed = rawLine.strip();
        if (trimmed.isEmpty()) {
            reporter.error(wholeLine(source, rawLine), "Empty mechanic line");
            return new StmtNode.Error(wholeLine(source, rawLine));
        }

        char first = trimmed.charAt(0);
        if (first == '$')
            return parseAssign(rawLine, source, reporter);
        if (first == '@')
            return parseBind(rawLine, source, reporter);
        return parseInvoke(rawLine, source, reporter);
    }

    private static StmtNode parseAssign(String line, SourceRef source, DiagnosticReporter reporter) {
        int dollar = line.indexOf('$');
        int eq = findAssignmentEquals(line);
        if (eq < 0) {
            reporter.error(wholeLine(source, line), "Expected '$name = <expression>'");
            return new StmtNode.Error(wholeLine(source, line));
        }
        String var = line.substring(dollar + 1, eq).trim();
        int exprCol = skipSpaces(line, eq + 1);
        String exprText = line.substring(exprCol);
        ExprNode value = ExpressionParser.parse(exprText, source, 0, exprCol, reporter);
        return new StmtNode.Assign(var, value, wholeLine(source, line));
    }

    private static StmtNode parseBind(String line, SourceRef source, DiagnosticReporter reporter) {
        int at = line.indexOf('@');
        int eq = findAssignmentEquals(line);
        if (eq < 0) {
            reporter.error(wholeLine(source, line), "Expected '@name = Targeter{...}'");
            return new StmtNode.Error(wholeLine(source, line));
        }
        String ctx = line.substring(at + 1, eq).trim();
        int targeterCol = skipSpaces(line, eq + 1);
        InlineCallNode targeter = InlineScan.scan(line.substring(targeterCol), targeterCol, 0, source, reporter);
        if (targeter == null)
            return new StmtNode.Error(wholeLine(source, line));
        return new StmtNode.Bind(ctx, targeter, wholeLine(source, line));
    }

    private static StmtNode parseInvoke(String line, SourceRef source, DiagnosticReporter reporter) {
        Matcher matcher = SEGMENTS.matcher(line);
        InlineCallNode mechanic = null;
        SubjectNode subject = null;
        List<InlineCallNode> conditions = new ArrayList<>();

        while (matcher.find()) {
            String group = matcher.group();
            if (group.isBlank())
                continue;
            int start = matcher.start();
            char kind = group.strip().charAt(0);

            if (kind == '@') {
                int markerIndex = group.indexOf('@');
                String body = group.substring(markerIndex + 1);
                int bodyCol = start + markerIndex + 1;
                String bodyStripped = body.strip();
                if (subject != null) {
                    reporter.error(wholeLine(source, line), "Found multiple subjects ('@') on one line");
                    continue;
                }
                if (bodyStripped.contains("{")) {
                    InlineCallNode targeter = InlineScan.scan(body, bodyCol, 0, source, reporter);
                    if (targeter != null)
                        subject = new SubjectNode.Inline(targeter, targeter.loc());
                } else {
                    int nameCol = bodyCol + leadingSpaces(body);
                    Loc loc = new Loc(source, Span.of(0, nameCol, nameCol + bodyStripped.length()));
                    subject = new SubjectNode.Ref(bodyStripped, loc);
                }
            } else if (kind == '?') {
                int markerIndex = group.indexOf('?');
                String body = group.substring(markerIndex + 1);
                int bodyCol = start + markerIndex + 1;
                InlineCallNode condition = InlineScan.scan(body, bodyCol, 0, source, reporter);
                if (condition != null)
                    conditions.add(condition);
            } else {
                if (mechanic != null) {
                    reporter.error(wholeLine(source, line), "Found multiple mechanics on one line",
                        "Use '@' for the subject and '?' for conditions");
                    continue;
                }
                mechanic = InlineScan.scan(group, start, 0, source, reporter);
            }
        }

        if (mechanic == null) {
            reporter.error(wholeLine(source, line), "Could not find a mechanic on the line");
            return new StmtNode.Error(wholeLine(source, line));
        }
        return new StmtNode.Invoke(mechanic, subject, conditions, wholeLine(source, line));
    }

    private static Loc wholeLine(SourceRef source, String line) {
        return new Loc(source, Span.of(0, 0, line.length()));
    }

    // Finds the assignment '=' (skips ==, !=, <=, >=).
    static int findAssignmentEquals(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != '=')
                continue;
            char prev = i > 0 ? s.charAt(i - 1) : ' ';
            char next = i + 1 < s.length() ? s.charAt(i + 1) : ' ';
            if (next != '=' && prev != '!' && prev != '<' && prev != '>' && prev != '=')
                return i;
        }
        return -1;
    }

    private static int skipSpaces(String s, int from) {
        int i = from;
        while (i < s.length() && s.charAt(i) == ' ')
            i++;
        return i;
    }

    private static int leadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ')
            i++;
        return i;
    }
}
