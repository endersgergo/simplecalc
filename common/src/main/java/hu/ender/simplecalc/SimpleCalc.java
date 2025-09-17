package hu.ender.simplecalc;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.platform.Platform;
import hu.ender.simplecalc.functions.*;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntitySection;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;
import net.objecthunter.exp4j.function.Function;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.naming.OperationNotSupportedException;
import java.security.KeyException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Supplier;

public final class SimpleCalc {
    public static final String MOD_ID = "simplecalc";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final Function HEAP_MEMORY_FUNCTION = new HeapMemoryFunction();
    private static final Function FRACT_FUNCTION = new Function("fract") {
        @Override
        public double apply(double... args) {
            return args[0] - Math.floor(args[0]);
        }
    };
    private static final HashMap<UUID, Map<String, Double>> STORED_RESULTS = new HashMap<>();
    private static final Set<String> DEFAULT_VARIABLES = Set.of("x", "y", "z", "mods", "players", "pi, tau", "π", "τ");
    private static final UUID SERVER_SHARED_UUID = new UUID(0, 0);
    private static final Map<String, Double> CLEARABLE_EMPTY_MAP = new Map<>() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Double get(Object key) {
            throw new NotImplementedException();
        }

        @Override
        public @Nullable Double put(String key, Double value) {
            throw new NotImplementedException();
        }

        @Override
        public Double remove(Object key) {
            throw new NotImplementedException();
        }

        @Override
        public void putAll(@NotNull Map<? extends String, ? extends Double> m) {
            throw new NotImplementedException();
        }

        @Override
        public void clear() {

        }

        @Override
        public @NotNull Set<String> keySet() {
            return Set.of();
        }

        @Override
        public @NotNull Collection<Double> values() {
            return List.of();
        }

        @Override
        public @NotNull Set<Entry<String, Double>> entrySet() {
            return Set.of();
        }
    };

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            final var expression = Commands.argument("expression", StringArgumentType.greedyString());
            dispatcher.register(
                    Commands.literal("calc")
                            .then(Commands.literal("eval").then(expression.executes(SimpleCalc::executeEval)))
                            .then(Commands.literal("help").executes(SimpleCalc::executeHelp))
                            .then(Commands.literal("clear").executes(ctx -> executeClear(ctx, null))
                                    .then(Commands.literal("for")
                                            .then(Commands.argument("target", EntityArgument.player()).requires(arg -> arg.hasPermission(2)).executes(ctx -> executeClear(ctx, EntityArgument.getPlayer(ctx, "target"))))))
            );
            dispatcher.register(Commands.literal("c").then(expression.executes(SimpleCalc::executeEval)));
        });
    }

    static Function[] getCustomFunctions(CommandContext<CommandSourceStack> context) {
        return new Function[] { new DistanceToFunction(context), new EntitiesFunction(context), new EntitiesFunction(context), new EntitiesAABBFunction(context), new TpsFunction(context), HEAP_MEMORY_FUNCTION, FRACT_FUNCTION };
    }

    static int executeHelp (CommandContext<CommandSourceStack> context) {
        final Style fnName = Style.EMPTY.withColor(ChatFormatting.AQUA);
        final Style section = Style.EMPTY.withBold(true);
        context.getSource().sendSystemMessage(
                Component
                        .literal("calc usage:\n")
                        .append("  /calc eval <expression> - evaluate expression\n")
                        .append("  /calc clear             - clears stored results")
                        .append("  /calc help              - prints this help\n")
                        .append("  /c <expression>         - short hand for /calc eval\n\n")
                        .append(Component.literal("Available built-in functions\n").setStyle(section))
                        .append(Component.literal("  abs").setStyle(fnName))            .append(": absolute value\n")
                        .append(Component.literal("  acos").setStyle(fnName))           .append(": arc cosine\n")
                        .append(Component.literal("  asin").setStyle(fnName))           .append(": arc sine\n")
                        .append(Component.literal("  atan").setStyle(fnName))           .append(": arc tangent\n")
                        .append(Component.literal("  cbrt").setStyle(fnName))           .append(": cubic root\n")
                        .append(Component.literal("  ceil").setStyle(fnName))           .append(": nearest upper integer\n")
                        .append(Component.literal("  cos").setStyle(fnName))            .append(": cosine\n")
                        .append(Component.literal("  cosh").setStyle(fnName))           .append(": hyperbolic cosine\n")
                        .append(Component.literal("  exp").setStyle(fnName))            .append(": euler's number raised to the power (e^x)\n")
                        .append(Component.literal("  floor").setStyle(fnName))          .append(": nearest lower integer\n")
                        .append(Component.literal("  log").setStyle(fnName))            .append(": logarithmus naturalis (base e)\n")
                        .append(Component.literal("  log10").setStyle(fnName))          .append(": logarithm (base 10)\n")
                        .append(Component.literal("  log2").setStyle(fnName))           .append(": logarithm (base 2)\n")
                        .append(Component.literal("  sin").setStyle(fnName))            .append(": sine\n")
                        .append(Component.literal("  sinh").setStyle(fnName))           .append(": hyperbolic sine\n")
                        .append(Component.literal("  sqrt").setStyle(fnName))           .append(": square root\n")
                        .append(Component.literal("  tan").setStyle(fnName))            .append(": tangent\n")
                        .append(Component.literal("  tanh").setStyle(fnName))           .append(": hyperbolic tangent\n")
                        .append(Component.literal("  signum").setStyle(fnName))         .append(": signum function\n\n")
                        .append(Component.literal("Functions added by the mod\n").setStyle(section))
                        .append(Component.literal("  distanceTo(x,y,z)").setStyle(fnName)).append(": distance to coordinates from current position\n")
                        .append(Component.literal("  entities(range)").setStyle(fnName))  .append(": count entities within radius (in blocks)\n")
                        .append(Component.literal("  entitiesAABB(x1,y1,z1,x2,y2,z2)").setStyle(fnName))   .append(": count entities in AABB (2x vec3) (").append(
                                Component.literal("What is AABB?").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00008f)).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://en.wikipedia.org/wiki/Bounding_volume")))).append(")\n")
                        .append(Component.literal("  tps()").setStyle(fnName))              .append(": current server tps\n")
                        .append(Component.literal("  fract(x)").setStyle(fnName))         .append(": gets the fractional part of a real number")
                        .append(Component.literal("  heapMemory(arg)").setStyle(fnName))        .append(": gets total or used memory (allocated to VM) 0: total, 1: used\n\n")
                        .append(Component.literal("Available variables\n").setStyle(section))
                        .append("  pi, tau, π, τ - (constant, built-in) self-explanatory constants provided by exp4j\n")
                        .append("  x, y, z       - position variables, resolve to source position\n")
                        .append("  mods          - number of mods loaded\n")
                        .append("  players       - number of players connected\n\n")
                        .append("You can also use assignment expressions to store results in named variables for later retrieval, like ")
                        .append(Component.literal("myVar")
                                        .setStyle(Style.EMPTY
                                                .withColor(ChatFormatting.AQUA)))
                        .append(" = ")
                        .append(Component
                                .literal("expression")
                                .setStyle(Style.EMPTY
                                        .withColor(ChatFormatting.LIGHT_PURPLE)))
                        .append(" to store result of <expression> to myVar. All names must match the [a-zA-Z0-9]+ (same as \\w+) RegExp expression."));
        return Command.SINGLE_SUCCESS;
    }

    static boolean hasAssignmentExpression(final String input, final Supplier<String[]> parts) {
        final int equalityPosition = input.indexOf('=');
        if (equalityPosition == -1)
            return false;
        final String varName = input.substring(0, equalityPosition).trim();
        if (!varName.codePoints().allMatch(Character::isLetterOrDigit))
            return false;
        parts.get()[0] = varName;
        parts.get()[1] = input.substring(equalityPosition+1).trim();
        return true;
    }

    static int executeEval(CommandContext<CommandSourceStack> context) {
        final String input = context.getArgument("expression", String.class);
        final Supplier<String[]> parts = Suppliers.memoize(() -> new String[2]); // Avoids superfluous array creation
        final boolean shouldStore = hasAssignmentExpression(input, parts);
        if (shouldStore && DEFAULT_VARIABLES.contains(parts.get()[0]))
        {
            context.getSource().sendSystemMessage(Component.literal("Cannot store expression result to a built-in variable/constant."));
            return 0;
        }
        Expression expression;
        try {
            expression = new ExpressionBuilder(shouldStore ? parts.get()[1] : input)
                    .functions(getCustomFunctions(context))
                    .variables("x","y","z","players","mods")
                    .variables(STORED_RESULTS
                            .getOrDefault(context
                                    .getSource()
                                    .isPlayer() ?
                                    Objects.requireNonNull(context.getSource().getPlayer()).getUUID() :
                                    SERVER_SHARED_UUID, Map.of())
                            .keySet()).build();
        } catch (IllegalArgumentException e) {
                context.getSource().sendSystemMessage(
                        Component.literal(e.getMessage())
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.RED)));
                LOGGER.error(e.getMessage());
            return 0;
        }
        expression
                .setVariable("x", context.getSource().getPosition().x)
                .setVariable("y", context.getSource().getPosition().y)
                .setVariable("z", context.getSource().getPosition().z)
                .setVariable("mods", Platform.getMods().size())
                .setVariable("players", context.getSource().getServer().getPlayerCount())
                .setVariables(STORED_RESULTS.getOrDefault(context.getSource().isPlayer() ? Objects.requireNonNull(context.getSource().getPlayer()).getUUID() : SERVER_SHARED_UUID, Map.of()));
        ValidationResult validationResult = expression.validate();
        if (!validationResult.isValid()) {
            if (context.getSource().isPlayer()) {
                MutableComponent errorMsg =
                        Component.literal("Invalid expression!")
                        .setStyle(Style.EMPTY
                            .withColor(ChatFormatting.RED));
                for (String msg : validationResult.getErrors())
                    errorMsg.append("  ").append(msg).append("\n");
                context.getSource().sendSystemMessage(errorMsg);
            } else {
                StringBuilder errorMsg = new StringBuilder();
                errorMsg.append("Invalid expression!\n");

                for (String msg : validationResult.getErrors())
                    errorMsg.append("\t").append(msg).append("\n");
                errorMsg.deleteCharAt(errorMsg.lastIndexOf("\n"));
                LOGGER.error(errorMsg.toString());
            }
            return 0;
        }
        double result = expression.evaluate();
        if (shouldStore)
            STORED_RESULTS.computeIfAbsent(context.getSource().isPlayer() ? Objects.requireNonNull(context.getSource().getPlayer()).getUUID() : SERVER_SHARED_UUID, key -> new HashMap<>()).put(parts.get()[0], result);
        DecimalFormat df = new DecimalFormat("#.#####");
        var message = shouldStore ? Component.literal(parts.get()[0]).append(" = ").append(parts.get()[1]) : Component.literal(input);
        message = message
                .append(" = ")
                .append(Component
                        .literal(df.format(result))
                        .setStyle(Style.EMPTY.withColor(ChatFormatting.AQUA)));
        context.getSource().sendSystemMessage(message);
        if (shouldStore)
            LOGGER.info("Expression: {} = {} = {}", parts.get()[0], parts.get()[1], df.format(result));
        else
            LOGGER.info("Expression: {} = {}", input, df.format(result));
        return Command.SINGLE_SUCCESS;
    }

    private static int executeClear(CommandContext<CommandSourceStack> context, @Nullable ServerPlayer player) {
        if (player != null) {
            STORED_RESULTS.getOrDefault(player.getUUID(), CLEARABLE_EMPTY_MAP).clear();
        }
        else if (context.getSource().isPlayer())
            STORED_RESULTS.getOrDefault(Objects.requireNonNull(context.getSource().getPlayer()).getUUID(), CLEARABLE_EMPTY_MAP).clear();
        else
            STORED_RESULTS.getOrDefault(SERVER_SHARED_UUID, CLEARABLE_EMPTY_MAP).clear();
        context.getSource().sendSystemMessage(Component.literal("Results cleared!"));
        return Command.SINGLE_SUCCESS;
    }
}
