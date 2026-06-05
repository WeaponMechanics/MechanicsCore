package me.deecaad.core.file

import com.cjcrafter.foliascheduler.util.MinecraftVersions
import com.cjcrafter.foliascheduler.util.ReflectionUtil
import com.cryptomorin.xseries.XEntityType
import com.cryptomorin.xseries.XMaterial
import com.cryptomorin.xseries.XSound
import com.cryptomorin.xseries.particles.XParticle
import me.deecaad.core.file.SerializerException.Companion.builder
import me.deecaad.core.file.simple.DoubleSerializer
import me.deecaad.core.file.simple.EnumValueSerializer
import me.deecaad.core.file.simple.RegistryValueSerializer
import me.deecaad.core.utils.RegistryUtil
import me.deecaad.core.utils.SerializerUtil.foundAt
import me.deecaad.core.utils.StringUtil.colorAdventure
import me.deecaad.core.utils.StringUtil.split
import me.deecaad.core.utils.matchAny
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Registry
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import java.io.File
import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import kotlin.collections.ArrayList
import kotlin.math.ceil
import kotlin.math.floor

/**
 * [SerializeData] wraps a [ConfigurationSection] and a key along with useful
 * "validation methods". These methods will throw a [SerializerException] if the server admin
 * input an incorrect value. This allows us, the developers, to quickly and easily check if the
 * config is valid (Without long if/else if/else chains, or otherwise). Uses a builder pattern for
 * nice one-liners.
 *
 * For example, to get a positive integer from config, we can use
 * `SerializeData#of("your.key").assertExists().assertPositive().getInt()`.
 */
class SerializeData {
    val file: File
    val key: String?
    val config: ConfigLike

    constructor(file: File, key: String?, config: ConfigLike) {
        this.file = file
        this.key = key
        this.config = config
    }

    constructor(other: SerializeData, relative: String) {
        this.file = other.file
        this.key = other.getPath(relative)
        this.config = other.config
    }

    companion object {
        /**
         * Test-only drift guard: when recording is on, every key read through [of]/[ofList]/[has] is
         * collected, so a test can assert a serializer reads exactly the keys its schema declares.
         * Guarded by a single boolean check, so production has no measurable cost.
         */
        @JvmStatic
        var recordingAccess: Boolean = false

        @JvmStatic
        val accessedKeys: MutableSet<String> = java.util.Collections.synchronizedSet(LinkedHashSet())

        @JvmStatic
        fun startRecording() {
            accessedKeys.clear()
            recordingAccess = true
        }

        @JvmStatic
        fun stopRecording(): Set<String> {
            recordingAccess = false
            return LinkedHashSet(accessedKeys)
        }

        private fun recordAccess(relative: String?) {
            if (recordingAccess && relative != null)
                accessedKeys.add(relative)
        }
    }

    /**
     * Returns the path to the key.
     *
     * @param relative The non-null relative path.
     * @return The total path + relative path.
     */
    private fun getPath(relative: String?): String? {
        return if (key.isNullOrEmpty()) relative else ("$key.$relative")
    }

    /**
     * Helper method to "move" into a new configuration section. The given relative key should
     * *always* point towards a [ConfigurationSection]
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null serialize data.
     * @throws IllegalArgumentException If no configuration section exists at the location.
     */
    fun move(relative: String): SerializeData {
        return SerializeData(this, relative)
    }

    /**
     * The opposite of [move] method. This method will "step back" to the previous
     * configuration section. For example, if the current key is `a.b.c`, then this method will return
     * `a.b`.
     */
    fun back(): SerializeData {
        val split = key!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
        val key = StringBuilder()

        for (i in 0..<split.size - 1) key.append(split[i]).append('.')

        if (key.isNotEmpty()) key.setLength(key.length - 1)

        return SerializeData(file, key.toString(), config)
    }

    /**
     * Creates a [ConfigAccessor] which accesses the data (stored in config) at
     * `this.key + "." + relative`. The returned accessor can be used to validate arguments.
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null config accessor.
     */
    @JvmOverloads
    fun of(relative: String? = null): ConfigAccessor {
        if (relative == null) {
            val split = key!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            return back().of(split.last())
        }

        recordAccess(relative)
        return ConfigAccessor(relative)
    }

    /**
     * Creates a [ConfigListAccessor] which accesses the data (stored in config) at
     * `this.key + ".' + relative`. The returned accessor can be used to validate arguments.
     *
     * @param relative The non-null, non-empty key relative to this.key.
     * @return The non-null config list accessor.
     */
    @JvmOverloads
    fun ofList(relative: String? = null): ConfigListAccessor {
        if (relative == null) {
            val split = key!!.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            return back().ofList(split.last())
        }
        recordAccess(relative)
        return ConfigListAccessor(relative)
    }

    /**
     * Returns `true` if the given relative config key exists. Otherwise, this method will
     * return false. Usually, you should use [ConfigAccessor.assertExists].
     *
     * @param relative The non-null relative key.
     * @return `true` if the key exists.
     */
    fun has(relative: String?): Boolean {
        recordAccess(relative)
        return config.contains(getPath(relative))
    }

    /**
     * When there is no method in [ConfigAccessor] to match a specific configuration error, you
     * may check for it manually and use this method to throw a "general" exception.
     *
     * Make sure to keep messages clear and concise. There is no limit to how many messages you may give
     * to the player, but make sure that each message is *important* and contains *useful*
     * information.
     *
     * @param relative The nullable relative key.
     * @param messages The non-empty list of messages to include.
     * @return The non-null constructed exception.
     */
    fun exception(
        relative: String?,
        vararg messages: String,
    ): SerializerException {
        require(messages.isNotEmpty()) { "Hey you! Yeah you! Don't be lazy, add messages!" }

        var key = this.key
        if (!relative.isNullOrEmpty()) key = getPath(relative)

        return SerializerException(foundAt(file, key!!), mutableListOf(*messages))
    }

    /**
     * When there is no method in [ConfigListAccessor] to match a specific configuration error,
     * you may check for it manually and use this method to throw a "general" exception.
     *
     * @param relative The nullable relative key.
     * @param index The index (NOT index + 1) of the element that had the error.
     * @param messages The non-empty list of messages to include
     * @return The non-null constructed exception.
     */
    fun listException(
        relative: String?,
        index: Int,
        vararg messages: String,
    ): SerializerException {
        require(messages.isNotEmpty()) { "Hey you! Yeah you! Don't be lazy, add messages!" }

        var key = this.key
        if (!relative.isNullOrEmpty()) key = getPath(relative)

        return SerializerException(foundAt(file, key!!, index + 1), mutableListOf(*messages))
    }

    /**
     * Wraps a configuration KEY (which points to a list of values) to some helper functions to
     * facilitate data serialization. The
     */
    inner class ConfigListAccessor(private val relative: String?) {
        // Stores the class arguments, which is used to check the format
        private val arguments: MutableList<SimpleSerializer<*>> = ArrayList()
        private var requiredArgs: Int = 0

        fun <T : Any> addArgument(serializer: SimpleSerializer<T>): ConfigListAccessor {
            arguments.add(serializer)
            return this
        }

        fun requireAllPreviousArgs(): ConfigListAccessor {
            requiredArgs = arguments.size
            return this
        }

        /**
         * Asserts that this key exists in the configuration. This method ensures that the user explicitly
         * defined a value for the key.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        @Throws(SerializerException::class)
        fun assertExists(): ConfigListAccessor {
            if (!has(relative)) {
                throw builder()
                    .locationRaw(location)
                    .buildMissingRequiredKey(relative!!)
            }

            return this
        }

        @Throws(SerializerException::class)
        fun assertList(): List<List<Optional<Any>>> {
            check(arguments.isNotEmpty()) { "Need to set arguments before assertions" }

            // A formatted string like: <material*> <integer*> <true/false>
            // This helps the user understand what they need to put in
            val expectedInputFormat = StringBuilder("<")
            for (i in arguments.indices) {
                val arg = arguments[i]
                expectedInputFormat.append(arg.typeName)
                if (i < requiredArgs) expectedInputFormat.append('*')

                if (i != arguments.size - 1) expectedInputFormat.append("> <")
            }
            expectedInputFormat.append('>')

            // The first step is to assert that the value stored at this key
            // is a list (of any generic-type).
            val value = config[getPath(relative)]
            if (value == null) return listOf()

            if (value !is List<*>) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidType("list of $expectedInputFormat", value)
            }

            // Use assertExists for required keys
            if (value.isEmpty()) return listOf()

            // The resulting list of parsed values
            val listOfParsedData: MutableList<List<Optional<Any>>> = ArrayList()

            for (i in value.indices) {
                val string = value[i]?.toString()
                val parsedData: MutableList<Optional<Any>> = ArrayList()

                // Empty string in config is probably a mistake (Perhaps they
                // forgot to save?). Instead of ignoring this, we should tell
                // the user (playing it safe).
                if (string == null || string.trim { it <= ' ' }.isEmpty()) {
                    throw listException(
                        relative,
                        i,
                        "$relative does not allow empty elements in the list.",
                        "Valid Format: $expectedInputFormat",
                    )
                }

                // Each element in the list should be a string of values
                // separated by a standard delimiter (Either '~' or '-' or ' ')
                val split = split(string)

                // Missing required data
                if (split.size < requiredArgs) {
                    throw listException(
                        relative,
                        i,
                        "$relative requires the first $requiredArgs arguments to be defined.",
                        "For value: $string",
                        "You are missing " + (requiredArgs - split.size) + " arguments",
                        "Valid Format: $expectedInputFormat",
                    )
                }

                for (j in split.indices) {
                    // Extra data check. This happens when the user adds more
                    // data than what the list can take. For example, if this
                    // list uses the format 'string-int' and the user inputs
                    // 'string-int-double', then this will be triggered.

                    if (arguments.size <= j) {
                        throw listException(
                            relative,
                            i,
                            "Invalid list format, " + relative + " can only use " + arguments.size + " arguments.",
                            "Found Value: $string",
                            "Valid Format: $expectedInputFormat",
                        )
                    }

                    val component = split[j]
                    val argument = arguments[j]
                    val parsedValue = argument.deserialize(component, getLocation(i))
                    parsedData.add(Optional.of(parsedValue))
                }

                // Fill up the rest of the arguments with empty values
                for (j in split.size until arguments.size) {
                    parsedData.add(Optional.empty<Any>())
                }

                listOfParsedData.add(parsedData)
            }

            return listOfParsedData.toList() // immutable list
        }

        val location: String
            get() {
                return if (relative.isNullOrEmpty()) {
                    config.getLocation(file, key)
                } else {
                    config.getLocation(file, getPath(relative))
                }
            }

        fun getLocation(index: Int): String {
            return if (relative.isNullOrEmpty()) {
                foundAt(file, key!!, index + 1)
            } else {
                foundAt(file, getPath(relative)!!, index + 1)
            }
        }
    }

    /**
     * Wraps a configuration KEY to some helper functions to facilitate data serialization. The (public)
     * methods of this class will throw a [SerializerException] if the configuration is invalid.
     *
     * The methods of this class follow the Builder pattern.
     */
    inner class ConfigAccessor(private val relative: String) {
        private var exists = false

        /**
         * Asserts that this key exists in the configuration. This method ensures that the user explicitly
         * defined a value for the key.
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the key is not explicitly defined.
         */
        @Throws(SerializerException::class)
        fun assertExists(): ConfigAccessor {
            if (!has(relative)) {
                throw builder()
                    .locationRaw(location)
                    .buildMissingRequiredKey(relative)
            }
            this.exists = true

            return this
        }

        /**
         * Returns `true` when the object stored in this location matches the given
         * `type`.
         *
         * @param type Which type to check for
         * @return true, if the value matched the type.
         */
        fun `is`(type: Class<*>): Boolean {
            require(!type.isPrimitive) { "Silly developer, $type is a primitive type! Check wrapper classes instead." }
            val value = config[getPath(relative)]

            return value != null && type.isAssignableFrom(value.javaClass)
        }

        /**
         * Asserts that the value at this key is an instance of the given class. Ensures that the datatype
         * matches what the developer expected the user to give.
         *
         * @param type The non-null data type to match.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the type does not match.
         */
        @Throws(SerializerException::class)
        fun assertType(type: Class<*>): ConfigAccessor {
            val value = config[getPath(relative)]

            // Use assertExists for required keys
            if (value != null) {
                val actual: Class<*> = value.javaClass
                if (!type.isAssignableFrom(actual)) {
                    throw builder()
                        .locationRaw(location)
                        .buildInvalidType(type.simpleName, value)
                }
            }

            return this
        }

        /**
         * Asserts that the value at this key is a number of any type. The check is done by checking the
         * value can be type-casted to a double. Note that if you want a more specific number type (for
         * example, an integer), you should use [assertType].
         *
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the type is not a number.
         */
        @Throws(SerializerException::class)
        fun getNumber(): Optional<Number> {
            var value = config[getPath(relative)]

            // Use assertExists for required keys
            if (value == null) {
                return Optional.empty()
            }

            // If the value is a string, attempt to parse it as a number
            if (value is String) {
                value = DoubleSerializer().deserialize(value, location)
            }

            try {
                return Optional.of(value as Number)
            } catch (ex: ClassCastException) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidType("number", value)
            }
        }

        /**
         * Returns the integer value of the config, or throws an exception if the value is not a number.
         * Note that this method will also throw an exception if the input is explicitly a double. For
         * example, `1.0` is a valid integer which will be parsed as `1`, but
         * `1.1` will throw an exception.
         *
         * @return The integer from config, or [OptionalInt.empty].
         * @throws SerializerException If the config value is not an integer.
         */
        @Throws(SerializerException::class)
        fun getInt(): OptionalInt {
            val num = getNumber()
            if (num.isEmpty) {
                return OptionalInt.empty()
            }

            val numValue = num.get().toDouble()
            if (floor(numValue).compareTo(ceil(numValue)) != 0) {
                throw builder()
                    .locationRaw(location)
                    .addMessage("Expected an integer WITHOUT any decimal (floating point) value")
                    .buildInvalidType("integer", num)
            }

            return OptionalInt.of(numValue.toInt())
        }

        /**
         * Returns the double value of the config, or throws an exception if the value is not a number.
         *
         * @return The double from config.
         * @throws SerializerException If the config value is not a double.
         */
        @Throws(SerializerException::class)
        fun getDouble(): OptionalDouble {
            val num = getNumber()
            if (num.isEmpty) {
                return OptionalDouble.empty()
            }

            return OptionalDouble.of(num.get().toDouble())
        }

        /**
         * Returns the boolean value of the config, or throws an exception if the value is not a boolean.
         *
         * @return The boolean from config.
         * @throws SerializerException If the config value is not a boolean.
         */
        @Throws(SerializerException::class)
        fun getBool(): Optional<Boolean> {
            val value = config[getPath(relative)]
            if (value == null) {
                return Optional.empty()
            }

            if (value is Boolean) {
                return Optional.of(value)
            }

            if (value is String) {
                if (value.toString().trim().equals("true", ignoreCase = true)) return Optional.of(true)
                if (value.toString().trim().equals("false", ignoreCase = true)) return Optional.of(false)
            }

            throw builder()
                .locationRaw(location)
                .buildInvalidType("boolean", value)
        }

        /**
         * Asserts that the value at this key is a number, AND that the number is within the inclusive
         * range. Note that if you want a more specific number type (for example, an integer), you should
         * use [getInt].
         *
         * @param min Inclusive minimum bound.
         * @param max Inclusive maximum bound.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the value is not in range.
         * @throws IllegalArgumentException If min larger than max.
         */
        @Throws(SerializerException::class)
        fun assertRange(
            min: Int? = null,
            max: Int? = null,
        ): ConfigAccessor {
            if (min != null && max != null) require(min <= max) { "min > max" }
            if (min == null && max == null) throw IllegalArgumentException("min and max cannot be null")

            // Use assertExists for required keys
            val value = getNumber()
            if (value.isPresent) {
                // Silently strips away float point data (without exception)

                val num = value.get().toInt()
                if (min != null && num < min || max != null && num > max) {
                    throw builder()
                        .locationRaw(location)
                        .buildInvalidRange(num, min, max)
                }
            }

            return this
        }

        /**
         * Asserts that the value at this key is a number, AND that the number is within the inclusive
         * range. Note that if you want a more specific number type (for example, an integer), you should
         * use [getInt].
         *
         * @param min Inclusive minimum bound.
         * @param max Inclusive maximum bound.
         * @return A non-null reference to this accessor (builder pattern).
         * @throws SerializerException If the value is not in range.
         * @throws IllegalArgumentException If min larger than max.
         */
        @Throws(SerializerException::class)
        fun assertRange(
            min: Double? = null,
            max: Double? = null,
        ): ConfigAccessor {
            if (min != null && max != null) require(min <= max) { "min > max" }
            if (min == null && max == null) throw IllegalArgumentException("min and max cannot be null")

            // Use assertExists for required keys
            val value = getNumber()
            if (value.isPresent) {
                val num = value.get().toDouble()
                if (min != null && num < min || max != null && num > max) {
                    throw builder()
                        .locationRaw(location)
                        .buildInvalidRange(num, min, max)
                }
            }

            return this
        }

        val location: String
            get() {
                return config.getLocation(file, getPath(relative))
            }

        /**
         * Gets the data stored at this relative key. Note that this method (basically) requires a previous
         * call to [assertExists], especially for primitive types. When the key is optional, use
         * [get] to define a default value.
         *
         * @param <T> The expected data-type of the data.
         * @return The data stored at this relative key.
         */
        fun <T : Any> get(clazz: Class<T>): Optional<T> {
            assertType(clazz)
            val value = config[getPath(relative)]

            if (value == null) {
                return Optional.empty()
            }

            return Optional.of(clazz.cast(value))
        }

        /**
         * Serializes an enum value from config. If the key is not defined, then `defaultValue`
         * is returned. If the user defines a string that doesn't match any enum, an
         * exception is thrown.
         *
         * @param clazz The non-null enum class.
         * @param <T> The enum type.
         * @return The serialized enum type, or defaultValue.
         * @throws SerializerException If there is a misconfiguration in config.
         </T> */
        @Throws(SerializerException::class)
        fun <T : Enum<T>> getEnum(clazz: Class<T>): Optional<T> {
            val input = config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            val firstEnumFound = EnumValueSerializer(clazz, false).deserialize(input, location).first()
            return Optional.of(firstEnumFound)
        }

        /**
         * Uses [XMaterial] to parse the material from the string.
         *
         * @return The material from config, or defaultValue.
         * @throws SerializerException If the user defined an invalid material.
         */
        @Throws(SerializerException::class)
        fun getMaterial(): Optional<XMaterial> {
            var input =
                config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            // Wildcards are not allowed for singleton enums, they are only
            // allowed for lists.
            input = input.trim()
            val xmat = XMaterial.matchXMaterial(input)
            if (xmat.isEmpty) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidEnumOption(input, Material::class.java)
            }

            val parsed = xmat.get()
            if (!parsed.isSupported) {
                throw exception(
                    relative,
                    "Your version, " + MinecraftVersions.getCurrent() + ", doesn't support '" + parsed.name + "'",
                    "Try using a different material or update your server to a newer version!",
                )
            }

            return Optional.of(parsed)
        }

        /**
         * Wraps [getMaterial] and returns the material as an [ItemStack], so you
         * don't have to depend on XSeries and relocate it.
         *
         * @return The material as an item, or defaultValue.
         * @throws SerializerException If the user defined an invalid material.
         */
        @Throws(SerializerException::class)
        fun getMaterialAsItem(): Optional<ItemStack> {
            val xmat = getMaterial()

            if (xmat.isEmpty) {
                return Optional.empty()
            }

            val parsed =
                xmat.get().parseItem()
                    ?: throw exception(
                        relative,
                        "Your version, " + MinecraftVersions.getCurrent() + ", doesn't support '" + xmat.get().name + "'",
                        "Try using a different material or update your server to a newer version!",
                    )

            return Optional.of(parsed)
        }

        /**
         * Uses [XEntityType] to parse the [EntityType].
         *
         * @return The entity type from config, or defaultValue.
         * @throws SerializerException If the user defined an invalid entity type.
         */
        @Throws(SerializerException::class)
        fun getEntityType(): Optional<EntityType> {
            var input =
                config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            // Wildcards are not allowed for singleton enums, they are only
            // allowed for lists.
            input = input.trim()
            val entityType = XEntityType.of(input)
            if (entityType.isEmpty) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidEnumOption(input, EntityType::class.java)
            }

            val parsed =
                entityType.get().get()
                    ?: throw exception(
                        relative,
                        "Your version, " + MinecraftVersions.getCurrent() + ", doesn't support '" + entityType.get().name + "'",
                        "Try using a different material or update your server to a newer version!",
                    )

            return Optional.of(parsed)
        }

        /**
         * Uses [XSound] to parse a [Sound]. Going through XSeries keeps configs working across
         * Minecraft versions and accepts both the legacy 'ENTITY_GENERIC_EXPLODE' and the namespaced
         * 'entity.generic.explode' forms.
         *
         * @return The sound from config, or empty.
         * @throws SerializerException If the user defined an invalid sound.
         */
        @Throws(SerializerException::class)
        fun getSound(): Optional<Sound> {
            var input = config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            input = input.trim()
            val xsound = XSound.of(input)
            if (xsound.isEmpty) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidOption(input, XSound.REGISTRY.map { it.name() })
            }

            val parsed =
                xsound.get().get()
                    ?: throw exception(
                        relative,
                        "Your version, " + MinecraftVersions.getCurrent() + ", doesn't support '" + xsound.get().name() + "'",
                        "Try using a different sound or update your server to a newer version!",
                    )

            return Optional.of(parsed)
        }

        /**
         * Uses [XParticle] to parse the [Particle].
         *
         * @return The particle from config, or defaultValue.
         * @throws SerializerException If the user defined an invalid particle.
         */
        @Throws(SerializerException::class)
        fun getParticle(): Optional<Particle> {
            var input =
                config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            // Wildcards are not allowed for singleton enums, they are only
            // allowed for lists.
            input = input.trim()
            val particle = XParticle.of(input)
            if (particle.isEmpty) {
                throw builder()
                    .locationRaw(location)
                    .buildInvalidEnumOption(input, Particle::class.java)
            }

            val parsed =
                particle.get().get()
                    ?: throw exception(
                        relative,
                        "Your version, " + MinecraftVersions.getCurrent() + ", doesn't support '" + particle.get().name + "'",
                        "Try using a different material or update your server to a newer version!",
                    )

            return Optional.of(parsed)
        }

        /**
         * Parses any item from a bukkit registry. Check the [Registry] class
         * for more information.
         */
        @Throws(SerializerException::class)
        @JvmOverloads
        fun <T : Keyed> getBukkitRegistry(
            clazz: Class<T>,
            registry: Registry<T> =
                Bukkit.getRegistry(clazz)
                    ?: throw IllegalArgumentException("Registry for ${clazz.simpleName} does not exist."),
        ): Optional<T> {
            val input =
                config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            val firstItemFound =
                RegistryValueSerializer(clazz, false, registry).deserialize(
                    input.trim().lowercase(),
                    location,
                ).first()

            return Optional.of(firstItemFound)
        }

        /**
         * Parses a namespaced key from the config. The format should be `namespace:key`.
         */
        fun getNamespacedKey(): Optional<NamespacedKey> {
            val input =
                config.getString(getPath(relative))

            // Use assertExists for required keys
            if (input == null) {
                return Optional.empty()
            }

            val split = input.split(":".toRegex()).toTypedArray()
            if (split.size != 2) {
                throw builder()
                    .locationRaw(location)
                    .addMessage("Expected a namespaced key in the format 'namespace:key'")
                    .example("minecraft:stone")
                    .buildInvalidType("namespaced key", input)
            }

            return Optional.of(NamespacedKey(split[0], split[1]))
        }

        /**
         * Returns the string value of the config, adjusted to fit the adventure format. Adventure text is
         * formatting using html-like tags instead of the legacy `&` symbol. If the
         * string in config contains the legacy color system, we will attempt to convert it.
         *
         *
         * The returned string should be parsed using
         * [net.kyori.adventure.text.minimessage.MiniMessage]. You may use MechanicsCore's instance
         * [me.deecaad.core.MechanicsCore.message].
         *
         * @return The converted string from config.
         */
        fun getAdventure(): Optional<String> {
            val value = config.getString(getPath(relative))

            // Use assertExists for required keys
            if (value == null) {
                return Optional.empty()
            }

            return Optional.of(colorAdventure(value))
        }

        /**
         * Returns one type from a registry. The exact type is unknown, and is determined by the
         * [InlineSerializer.UNIQUE_IDENTIFIER] present in configuration. If no value has been defined
         * in config, then the default value is returned.
         *
         * @param registry The non-null registry of possible types to use.
         * @param <T> The superclass type.
         * @return A serialized instance.
         * @throws SerializerException If there are any errors in config.
         */
        @Suppress("UNCHECKED_CAST")
        @Throws(SerializerException::class)
        fun <T : InlineSerializer<T>> serializeRegistry(registry: Registry<T>): Optional<T> {
            var result: T? = null
            forEachRegistryEntry(registry, true) { serializer, nested ->
                result = (serializer as Serializer<T>).serialize(nested)
            }
            return Optional.ofNullable(result)
        }

        /**
         * This method is similar to [serializeRegistry], but instead of allowing every type from
         * a registry, 1 specific type is allowed.
         *
         * @param impliedType The serializer.
         * @param <T> The type to create.
         * @return The serialized instance.
         * @throws SerializerException If there are any errors in config.
         */
        @Throws(SerializerException::class)
        fun <T : Any> serializeRegistryImplied(impliedType: InlineSerializer<T>): Optional<T> {
            if (config !is MapConfigLike) throw UnsupportedOperationException("Cannot use registries with $config")
            if (!has(relative)) {
                return Optional.empty()
            }

            val map = config[getPath(relative)] as Map<String, *>
            val identifier = map[InlineSerializer.UNIQUE_IDENTIFIER]

            // We have to make sure that the user used the "JSON Format" in the string.
            if (identifier != null && RegistryUtil.matches(identifier.toString(), impliedType)) {
                throw exception(
                    relative,
                    "Expected a '${impliedType.key}' but got a '$identifier'",
                )
            }

            val temp: ConfigLike =
                MapConfigLike(map as Map<String, MapConfigLike.Holder?>).setDebugInfo(
                    config.file,
                    config.path,
                    config.fullLine,
                )

            val nested = SerializeData(file, null, temp)
            return Optional.of(impliedType.serialize(nested))
        }

        @Suppress("UNCHECKED_CAST")
        @Throws(SerializerException::class)
        fun <T : InlineSerializer<T>> getRegistryList(registry: Registry<T>): List<T> {
            val returnValue: MutableList<T> = ArrayList()
            forEachRegistryEntry(registry, false) { serializer, nested ->
                returnValue.add((serializer as Serializer<T>).serialize(nested))
            }
            return returnValue
        }

        /**
         * Shared traversal of a registry-of-serializers key. Resolves each chosen serializer by its
         * [InlineSerializer.UNIQUE_IDENTIFIER] and hands the caller the picked serializer plus the
         * child [SerializeData] of its inline args, leaving the action to decide what to do (the
         * schema validator recurses the nested schema; [serializeRegistry]/[getRegistryList]
         * construct). Throws on an unknown registry id or a malformed list element.
         *
         * @param registry The registry of inline serializers.
         * @param single   true for a single inline value, false for a list of them.
         * @param action   Receives (picked serializer, child data) per entry.
         */
        @Throws(SerializerException::class)
        fun forEachRegistryEntry(
            registry: Registry<out Keyed>,
            single: Boolean,
            action: java.util.function.BiConsumer<Serializer<*>, SerializeData>,
        ) {
            if (config !is MapConfigLike) throw UnsupportedOperationException("Cannot use registries with $config")
            if (!has(relative)) return

            if (single) {
                val map = assertExists().get(MutableMap::class.java)
                val temp: ConfigLike =
                    MapConfigLike(map.get() as MutableMap<String, MapConfigLike.Holder>)
                        .setDebugInfo(config.file, config.path, config.fullLine)
                val nested = SerializeData(file, null, temp)

                val key = nested.of(InlineSerializer.UNIQUE_IDENTIFIER).assertExists().get(String::class.java).get()
                val base = registry.matchAny(key)
                    ?: throw builder().locationRaw(location).buildInvalidRegistryOption(key, registry)
                action.accept(base as Serializer<*>, nested)
            } else {
                val list = config.getList(getPath(relative)) as List<MapConfigLike.Holder?>
                for (i in list.indices) {
                    val map = list[i]!!.value as? Map<*, *>
                        ?: throw listException(
                            relative,
                            i,
                            "Expected an inline serializer like 'sound(sound=entity.generic.explosion)', but instead got '${list[i]!!.value}'",
                        )
                    val id = (map[InlineSerializer.UNIQUE_IDENTIFIER] as? MapConfigLike.Holder)?.value?.toString()
                        ?: throw listException(relative, i, "Could not identify any valid type")
                    val serializer = registry.matchAny(id)
                        ?: throw builder().locationRaw(location).buildInvalidRegistryOption(id, registry)
                    val temp: ConfigLike =
                        MapConfigLike(map as Map<String, MapConfigLike.Holder>).setDebugInfo(
                            config.file,
                            config.path,
                            config.fullLine,
                        )
                    val nested = SerializeData(file, null, temp)
                    action.accept(serializer as Serializer<*>, nested)
                }
            }
        }

        @Throws(SerializerException::class)
        fun <T : InlineSerializer<T>> getImpliedList(impliedType: T): List<T> {
            if (config !is MapConfigLike) throw UnsupportedOperationException("Cannot use registries with $config")
            if (!has(relative)) return listOf()

            val list = config.getList(getPath(relative)) as List<MapConfigLike.Holder?>
            val returnValue: MutableList<T> = ArrayList()

            for (i in list.indices) {
                val map =
                    list[i]!!.value as? Map<*, *> ?: throw listException(
                        relative,
                        i,
                        "Expected an inline serializer like 'sound(sound=entity.generic.explosion)', but instead got '${list[i]!!.value}'",
                    )

                val identifier = map[InlineSerializer.UNIQUE_IDENTIFIER]
                if (identifier != null && !RegistryUtil.matches(identifier.toString(), impliedType)) {
                    throw listException(
                        relative,
                        i,
                        "Expected a '${impliedType.inlineKeyword}' but got a '$identifier'",
                    )
                }

                val temp: ConfigLike =
                    MapConfigLike(map as Map<String?, MapConfigLike.Holder?>).setDebugInfo(
                        config.file,
                        config.path,
                        config.fullLine,
                    )

                val nested = SerializeData(file, null, temp)
                returnValue.add(impliedType.serialize(nested))
            }

            return returnValue
        }

        /**
         * Uses the given serializer class to attempt to serialize an object from this relative key.
         *
         * @param <S> The serializer type.
         * @param <T> The serialized type.
         * @return The serialized object.
         * @throws SerializerException If there is a mistake in config found during serialization.
         */
        @Throws(SerializerException::class)
        inline fun <reified S : Serializer<T>, T : Any> serialize(): Optional<T> {
            return serialize(S::class.java)
        }

        /**
         * Uses the given class as a serializer and attempts to serialize an
         * object from this relative key.
         *
         * @param serializerClass The non-null serializer class.
         * @param <S> The serializer type.
         * @param <T> The serialized type.
         * @return The serialized object.
         * @throws SerializerException If there is a mistake in config found during serialization.
         */
        @Throws(SerializerException::class)
        fun <S : Serializer<T>, T : Any> serialize(serializerClass: Class<S>): Optional<T> {
            val serializer = ReflectionUtil.getConstructor(serializerClass).newInstance()
            return serialize(serializer)
        }

        /**
         * Uses the given serializer and attempts to serialize an object from
         * this relative key.
         *
         * @param serializer The non-null serializer.
         * @param <S> The serializer type.
         * @param <T> The serialized type.
         * @return The serialized object.
         */
        @Throws(SerializerException::class)
        fun <S : Serializer<T>, T : Any> serialize(serializer: S): Optional<T> {
            // Use assertExists for required keys
            if (!has(relative)) {
                return Optional.empty()
            }

            val data = SerializeData(this@SerializeData, relative)
            return Optional.of(serializer.serialize(data))
        }
    }
}
