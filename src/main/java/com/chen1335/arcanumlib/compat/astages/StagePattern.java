package com.chen1335.arcanumlib.compat.astages;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
* 阶段名匹配器：literal 精确、wildcard 通配、regex 正则（语义对齐 AStages 的 StageMatcher）。
* json 可只写字符串（等价 literal）或对象 {"pattern":"chapter_*","type":"wildcard"}；只有 literal 会回落成字符串。
*/
public record StagePattern(String pattern, Type type) {

    /** 通配/正则需要编译后的 Pattern，缓存起来避免每次掉落都重新编译。 */
    private static final Map<String, Pattern> COMPILED = new ConcurrentHashMap<>();

    public static final Codec<Type> TYPE_CODEC = StringRepresentable.fromEnum(Type::values);

    public static final Codec<StagePattern> CODEC = Codec.either(
                    Codec.STRING,
                    RecordCodecBuilder.<StagePattern>create(instance -> instance.group(
                                    Codec.STRING.fieldOf("pattern").forGetter(StagePattern::pattern),
                                    TYPE_CODEC.optionalFieldOf("type", Type.LITERAL).forGetter(StagePattern::type))
                            .apply(instance, StagePattern::new)))
            .xmap(
                    either -> either.map(StagePattern::literal, pattern -> pattern),
                    stage -> stage.type == Type.LITERAL ? Either.left(stage.pattern) : Either.right(stage));

    public static StagePattern literal(String stage) {
        return new StagePattern(stage, Type.LITERAL);
    }

    public boolean matches(String stage) {
        return switch (this.type) {
            case LITERAL -> this.pattern.equals(stage);
            case WILDCARD -> COMPILED.computeIfAbsent("wildcard:" + this.pattern, key -> compileWildcard(this.pattern))
                    .matcher(stage).matches();
            case REGEX -> COMPILED.computeIfAbsent("regex:" + this.pattern, key -> Pattern.compile(this.pattern))
                    .matcher(stage).matches();
        };
    }

    /** 逐段引号化再拼 {@code .*}：直接对整串 {@code Pattern.quote} 会把 * 一起引掉，通配就失效了。 */
    private static Pattern compileWildcard(String pattern) {
        String[] parts = pattern.split("\\*", -1);
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(parts[i]));
        }

        return Pattern.compile(regex.append('$').toString());
    }

    public enum Type implements StringRepresentable {
        LITERAL,
        WILDCARD,
        REGEX;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
