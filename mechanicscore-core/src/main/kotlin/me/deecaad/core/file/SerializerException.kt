package me.deecaad.core.file

import me.deecaad.core.diagnostic.Diagnostic
import me.deecaad.core.diagnostic.DiagnosticKind
import me.deecaad.core.diagnostic.Severity
import me.deecaad.core.diagnostic.SourceRef
import me.deecaad.core.diagnostic.Span
import me.deecaad.core.utils.EnumUtil
import me.deecaad.core.utils.StringUtil
import java.io.File
import kotlin.math.abs

/**
 * An exception that is thrown when a mistake is found in the config file.
 * These mistakes are typically caused by the user, and not the plugin itself.
 *
 * Serializer exceptions carry a structured source ([file], [path], [listIndex]) and are converted to
 * a [Diagnostic] via [toDiagnostic], then rendered through the one shared
 * [me.deecaad.core.diagnostic.DiagnosticRenderer]. This way, users see "pretty" caret error messages
 * instead of ugly stack traces.
 *
 * @param messages A list of messages that describe the mistake in more detail
 */
open class SerializerException(
    val messages: MutableList<String> = mutableListOf(),
    val kind: DiagnosticKind = DiagnosticKind.OTHER,
    val file: File? = null,
    val path: String? = null,
    val listIndex: Int = -1,
) : Exception() {

    /**
     * Converts this exception into a path-only [Diagnostic] carrying its [kind] directly (no message
     * string-matching). The caller fills the precise span via [ConfigLike.enrich], then renders it
     * through the one shared [me.deecaad.core.diagnostic.DiagnosticRenderer], so serializer errors and
     * schema/mechanic diagnostics share a single error system.
     */
    fun toDiagnostic(): Diagnostic {
        val message = if (messages.isEmpty()) "invalid value" else messages.joinToString("; ")
        val source = SourceRef(file, path ?: "", listIndex, "")
        return Diagnostic(Severity.ERROR, kind, message, source, Span.NONE, listOf(), null)
    }

    class Builder {
        private val messages: MutableList<String> = mutableListOf()
        private var file: File? = null
        private var path: String? = null
        private var index: Int = -1

        /**
         * Sets the structured source (file, path, list index) so the resulting exception can become a
         * precise [Diagnostic] via [toDiagnostic].
         */
        fun located(location: ErrorLocation): Builder {
            this.file = location.file
            this.path = location.path
            this.index = location.index
            return this
        }

        private fun finish(kind: DiagnosticKind): SerializerException {
            return SerializerException(messages, kind, file, path, index)
        }

        fun example(exampleValue: String): Builder {
            messages.add("Example value: $exampleValue")
            return this
        }

        fun addMessage(message: String): Builder {
            messages.add(message)
            return this
        }

        fun didYouMean(
            actual: String,
            options: Iterable<String>,
        ) {
            val expected = StringUtil.didYouMean(actual, options, actual.length + 2) ?: return
            messages.add("Did you mean to use '$expected' instead of '$actual'?")
        }

        fun possibleValues(
            actual: String,
            options: Iterable<String>,
            count: Int,
        ): Builder {
            var optionsList = options.toList()
            val actualTable = StringUtil.toCharTable(actual)

            val sortedArr =
                optionsList.sortedWith { a, b ->
                    val aTable = StringUtil.toCharTable(a)
                    val bTable = StringUtil.toCharTable(b)

                    var diffA = abs(actual.length - a.length)
                    var diffB = abs(actual.length - b.length)

                    for (i in actualTable.indices) {
                        diffA += abs(actualTable[i] - aTable[i])
                        diffB += abs(actualTable[i] - bTable[i])
                    }

                    diffA.compareTo(diffB)
                }

            val limitedCount = minOf(sortedArr.size, count)
            val builder = StringBuilder("Showing ")

            if (limitedCount == sortedArr.size) {
                builder.append("All")
            } else {
                builder.append("$limitedCount/${sortedArr.size}")
            }

            builder.append(" Options:")

            if (limitedCount > 0) {
                builder.append(sortedArr.take(limitedCount).joinToString("") { " '$it'" })
            }

            messages.add(builder.toString())
            return this
        }

        fun buildInvalidRange(
            actual: Int,
            min: Int?,
            max: Int?,
        ): SerializerException {
            messages.add("Invalid range! Expected a value between ${min ?: "-∞"} and ${max ?: "∞"}")
            messages.add("Found value: $actual")

            return finish(DiagnosticKind.OUT_OF_RANGE)
        }

        fun buildInvalidRange(
            actual: Double,
            min: Double?,
            max: Double?,
        ): SerializerException {
            messages.add("Invalid range! Expected a value between ${min ?: "-∞"} and ${max ?: "∞"}")
            messages.add("Found value: $actual")

            return finish(DiagnosticKind.OUT_OF_RANGE)
        }

        fun buildInvalidRegistryOption(
            input: String,
            registry: org.bukkit.Registry<*>,
        ): SerializerException {
            return buildInvalidOption(input, registry.map { it.key.key })
        }

        fun <T : Enum<T>> buildInvalidEnumOption(
            input: String,
            enumClass: Class<T>,
        ): SerializerException {
            return buildInvalidOption(input, EnumUtil.getOptions(enumClass))
        }

        fun buildInvalidOption(
            input: String,
            options: Iterable<String>,
        ): SerializerException {
            messages.add("Unknown value '$input'")
            didYouMean(input, options)
            possibleValues(input, options, 5)

            return finish(DiagnosticKind.INVALID_VALUE)
        }

        fun buildMissingRequiredKey(missingKey: String): SerializerException {
            messages.add("Missing required key '$missingKey'")
            messages.add("Make sure you spell it correctly (case sensitive)")

            return finish(DiagnosticKind.MISSING_REQUIRED)
        }

        fun buildInvalidType(
            expectedTyped: String,
            actualValue: Any,
        ): SerializerException {
            messages.add("Invalid type! Expected a $expectedTyped")
            messages.add("Found value: $actualValue")

            return finish(DiagnosticKind.INVALID_TYPE)
        }

        fun build(): SerializerException {
            return finish(DiagnosticKind.OTHER)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
