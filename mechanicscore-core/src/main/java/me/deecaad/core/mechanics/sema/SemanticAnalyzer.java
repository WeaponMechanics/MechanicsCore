package me.deecaad.core.mechanics.sema;

import me.deecaad.core.file.InlineSerializer;
import me.deecaad.core.file.MapConfigLike;
import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.ast.BlockNode;
import me.deecaad.core.mechanics.ast.ExprNode;
import me.deecaad.core.mechanics.ast.InlineCallNode;
import me.deecaad.core.mechanics.ast.ProgramNode;
import me.deecaad.core.mechanics.ast.StmtNode;
import me.deecaad.core.mechanics.ast.SubjectNode;
import me.deecaad.core.mechanics.expression.Expression;
import me.deecaad.core.mechanics.expression.ExpressionConsumer;
import me.deecaad.core.mechanics.parse.ExpressionParser;
import me.deecaad.core.mechanics.conditions.Condition;
import me.deecaad.core.mechanics.defaultmechanics.Mechanic;
import me.deecaad.core.diagnostic.Diagnostic;
import me.deecaad.core.diagnostic.DiagnosticKind;
import me.deecaad.core.diagnostic.DiagnosticReporter;
import me.deecaad.core.diagnostic.Severity;
import me.deecaad.core.diagnostic.Span;
import me.deecaad.core.file.verify.ConfigSchema;
import me.deecaad.core.file.verify.KeySpec;
import me.deecaad.core.file.verify.KeyType;
import me.deecaad.core.file.verify.SchemaValidator;
import me.deecaad.core.mechanics.parse.InlineScan;
import me.deecaad.core.mechanics.program.MechanicBlock;
import me.deecaad.core.mechanics.program.Program;
import me.deecaad.core.mechanics.program.Statement;
import me.deecaad.core.mechanics.program.Subject;
import me.deecaad.core.mechanics.scope.CastScope;
import me.deecaad.core.mechanics.targeters.Targeter;
import me.deecaad.core.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves a parsed {@link ProgramNode} into the executable instance IR
 * ({@link Program}), reusing the existing {@code serialize(SerializeData)}
 * methods (catch-convert on error) and collecting diagnostics. Returns a Program
 * regardless; callers only run it when the reporter has no errors.
 */
public final class SemanticAnalyzer {

    private final SymbolSource symbols;

    public SemanticAnalyzer() {
        this(new GlobalSymbolSource());
    }

    public SemanticAnalyzer(@NotNull SymbolSource symbols) {
        this.symbols = symbols;
    }

    public @NotNull Program analyze(@NotNull ProgramNode program, @NotNull File file, @NotNull DiagnosticReporter reporter) {
        Set<String> blockNames = program.blocks().keySet();
        Set<String> baseContexts = new LinkedHashSet<>();
        baseContexts.add(CastScope.SOURCE);
        baseContexts.add(CastScope.TARGET);
        baseContexts.addAll(symbols.providedContexts());
        // Variables are checked whole-program (not flow-sensitive): a '$x = ...' anywhere makes 'x'
        // known, so the block-return idiom (a block sets a var its caller reads) never false-positives.
        // A '$ref' that is neither provided nor assigned anywhere is a hard error, not a silent 0.
        Set<String> variables = new LinkedHashSet<>(symbols.providedVariables());
        for (BlockNode block : program.blocks().values())
            for (StmtNode node : block.statements())
                if (node instanceof StmtNode.Assign assign)
                    variables.add(assign.var());
        Map<String, MechanicBlock> blocks = new LinkedHashMap<>();

        for (Map.Entry<String, BlockNode> entry : program.blocks().entrySet()) {
            BlockNode block = entry.getValue();
            // Flow-sensitive within the block: a '@x = Targeter{}' bind makes 'x' available to later
            // lines, so use-before-bind is caught. Cross-block context passing is not tracked.
            Set<String> contexts = new LinkedHashSet<>(baseContexts);
            List<Statement> statements = new ArrayList<>();
            for (StmtNode node : block.statements()) {
                Statement statement = resolveStatement(node, blockNames, contexts, variables, file, reporter);
                if (statement != null)
                    statements.add(statement);
                if (node instanceof StmtNode.Bind bind)
                    contexts.add(bind.contextName());
            }
            blocks.put(entry.getKey(), new MechanicBlock(block.name(), statements));
        }

        return new Program(blocks, program.entry());
    }

    private @Nullable Statement resolveStatement(@NotNull StmtNode node, @NotNull Set<String> blockNames,
                                                 @NotNull Set<String> contexts, @NotNull Set<String> variables,
                                                 @NotNull File file, @NotNull DiagnosticReporter reporter) {
        return switch (node) {
            case StmtNode.Assign assign -> new Statement.Assignment(assign.var(),
                ExprLower.lower(assign.value(), contexts, variables, reporter));
            case StmtNode.Bind bind -> {
                Targeter targeter = resolveTargeter(bind.targeter(), contexts, variables, file, reporter);
                yield targeter == null ? null : new Statement.Binding(bind.contextName(), targeter);
            }
            case StmtNode.Invoke invoke -> resolveInvoke(invoke, blockNames, contexts, variables, file, reporter);
            case StmtNode.Error ignored -> null;
        };
    }

    private @Nullable Statement resolveInvoke(@NotNull StmtNode.Invoke invoke, @NotNull Set<String> blockNames,
                                              @NotNull Set<String> contexts, @NotNull Set<String> variables,
                                              @NotNull File file, @NotNull DiagnosticReporter reporter) {
        InlineCallNode call = invoke.call();
        String name = call.name();
        Subject subject = toSubject(invoke.subject(), contexts, variables, file, reporter);
        List<Condition> conds = resolveConditions(invoke.conditions(), contexts, variables, file, reporter);

        Mechanic mechanicProto = symbols.mechanic(name);
        if (mechanicProto != null) {
            Mechanic mechanic = serializeOrNull(mechanicProto, call, contexts, variables, file, reporter);
            return mechanic == null ? null : new Statement.BuiltinInvocation(mechanic, subject, conds);
        }

        if (blockNames.contains(name) || symbols.blockNames().contains(name.toLowerCase(Locale.ROOT))) {
            if (hasArgs(call))
                reporter.error(call.nameLoc(), "Block '" + name + "' takes no arguments",
                    "Set the variables it reads before the call, e.g. '$dmg = 10' then '" + name + "{}'");
            return new Statement.BlockInvocation(name, subject, conds);
        }

        Set<String> options = new LinkedHashSet<>(symbols.mechanicNames());
        options.addAll(blockNames);
        options.addAll(symbols.blockNames());
        reporter.error(call.nameLoc(), "Unknown mechanic or block '" + name + "'", suggest(name, options));
        return null;
    }

    private @NotNull Subject toSubject(@Nullable SubjectNode node, @NotNull Set<String> contexts,
                                       @NotNull Set<String> variables, @NotNull File file,
                                       @NotNull DiagnosticReporter reporter) {
        if (node == null)
            return new Subject.Reference(CastScope.TARGET);
        if (node instanceof SubjectNode.Ref ref) {
            if (!contexts.contains(ref.contextName()))
                reporter.error(ref.loc(), "Unknown context '@" + ref.contextName() + "'", suggest(ref.contextName(), contexts));
            return new Subject.Reference(ref.contextName());
        }
        SubjectNode.Inline inline = (SubjectNode.Inline) node;
        Targeter targeter = resolveTargeter(inline.targeter(), contexts, variables, file, reporter);
        return targeter == null ? new Subject.Reference(CastScope.TARGET) : new Subject.Inline(targeter);
    }

    private @Nullable Targeter resolveTargeter(@NotNull InlineCallNode call, @NotNull Set<String> contexts,
                                               @NotNull Set<String> variables, @NotNull File file,
                                               @NotNull DiagnosticReporter reporter) {
        Targeter proto = symbols.targeter(call.name());
        if (proto == null) {
            reporter.error(call.nameLoc(), "Unknown targeter '" + call.name() + "'", suggest(call.name(), symbols.targeterNames()));
            return null;
        }
        return serializeOrNull(proto, call, contexts, variables, file, reporter);
    }

    private @NotNull List<Condition> resolveConditions(@NotNull List<InlineCallNode> nodes, @NotNull Set<String> contexts,
                                                       @NotNull Set<String> variables, @NotNull File file,
                                                       @NotNull DiagnosticReporter reporter) {
        List<Condition> result = new ArrayList<>(nodes.size());
        for (InlineCallNode node : nodes) {
            Condition proto = symbols.condition(node.name());
            if (proto == null) {
                reporter.error(node.nameLoc(), "Unknown condition '" + node.name() + "'", suggest(node.name(), symbols.conditionNames()));
                continue;
            }
            Condition condition = serializeOrNull(proto, node, contexts, variables, file, reporter);
            if (condition != null)
                result.add(condition);
        }
        return result;
    }

    private static boolean hasArgs(@NotNull InlineCallNode call) {
        for (String key : call.args().keySet())
            if (!key.equals(InlineSerializer.UNIQUE_IDENTIFIER))
                return true;
        return false;
    }

    private <T extends Serializer<T>> @Nullable T serializeOrNull(@NotNull T proto, @NotNull InlineCallNode call,
                                                                  @NotNull Set<String> contexts, @NotNull Set<String> variables,
                                                                  @NotNull File file, @NotNull DiagnosticReporter reporter) {
        MapConfigLike config = new MapConfigLike(call.args())
            .setDebugInfo(file, call.loc().source().configPath(), call.loc().source().rawLine());
        SerializeData data = new SerializeData(file, null, config);

        // Schema validation first: catches hallucinated args + type/range errors that serialize()
        // would silently ignore or report less precisely. On a blocking error we stop here so the
        // error is not reported twice (once by the schema, once by serialize()).
        ConfigSchema schema = proto.schema();
        if (schema != null) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            SchemaValidator.validate(schema, data, contexts, diagnostics);
            boolean blocking = false;
            for (Diagnostic diagnostic : diagnostics) {
                reporter.report(reanchor(diagnostic, call));
                if (diagnostic.severity() == Severity.ERROR)
                    blocking = true;
            }
            if (blocking)
                return null;
        }

        T result;
        try {
            result = proto.serialize(data);
        } catch (SerializerException ex) {
            String message = ex.getMessages().isEmpty() ? "Invalid arguments" : String.join(" | ", ex.getMessages());
            reporter.report(new Diagnostic(Severity.ERROR, DiagnosticKind.OTHER, message, call.loc().source(), call.nameLoc().span(), List.of(), null));
            return null;
        }

        // Expression args (exprKey): the compiler parses and lowers them through the AST pipeline so
        // they get the same spans/checks as '$x = <expr>', then hands them to the serializer.
        if (schema != null && result instanceof ExpressionConsumer consumer)
            consumer.acceptExpressions(compileExpressions(schema, call, contexts, variables, reporter));
        return result;
    }

    private @NotNull Map<String, Expression> compileExpressions(@NotNull ConfigSchema schema, @NotNull InlineCallNode call,
                                                                @NotNull Set<String> contexts, @NotNull Set<String> variables,
                                                                @NotNull DiagnosticReporter reporter) {
        Map<String, Expression> compiled = new LinkedHashMap<>();
        for (KeySpec spec : schema.keys()) {
            if (spec.type() != KeyType.EXPRESSION)
                continue;
            MapConfigLike.Holder holder = findHolder(call, spec.name());
            if (holder == null)
                continue;
            int column = InlineScan.argColumn(call, holder);
            ExprNode node = ExpressionParser.parse(String.valueOf(holder.value()), call.loc().source(),
                call.nameLoc().span().line(), column, reporter);
            compiled.put(spec.name(), ExprLower.lower(node, contexts, variables, reporter));
        }
        return compiled;
    }

    /**
     * Re-anchors a schema diagnostic (which has no column info) to the offending inline argument's
     * source span, so the caret points at the actual key. Falls back to the call name when the key
     * cannot be located (e.g. a missing-required key or a deeply nested diagnostic).
     */
    private @NotNull Diagnostic reanchor(@NotNull Diagnostic diagnostic, @NotNull InlineCallNode call) {
        String key = diagnostic.source().configPath();
        MapConfigLike.Holder holder = findHolder(call, key);
        Span span;
        if (holder != null) {
            int col = InlineScan.argColumn(call, holder);
            span = Span.of(call.nameLoc().span().line(), col, col + key.length());
        } else {
            span = call.nameLoc().span();
        }
        return new Diagnostic(diagnostic.severity(), diagnostic.kind(), diagnostic.message(),
            call.loc().source(), span, List.of(), diagnostic.hint());
    }

    private static @Nullable MapConfigLike.Holder findHolder(@NotNull InlineCallNode call, @NotNull String key) {
        String norm = MapConfigLike.normalizeKey(key);
        for (Map.Entry<String, MapConfigLike.Holder> entry : call.args().entrySet())
            if (MapConfigLike.normalizeKey(entry.getKey()).equals(norm))
                return entry.getValue();
        return null;
    }

    private static @Nullable String suggest(@NotNull String actual, @NotNull Iterable<String> options) {
        String best = StringUtil.didYouMean(actual, options, actual.length() + 2);
        return best == null ? null : "Did you mean '" + best + "'?";
    }
}
