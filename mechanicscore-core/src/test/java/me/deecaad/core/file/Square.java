package me.deecaad.core.file;

import org.jetbrains.annotations.NotNull;

public class Square implements Serializer<Square> {

    private Vec2 offset;
    private int length;
    private double r, g, b;

    /**
     * Default constructor for serializer
     */
    public Square() {
    }

    public Square(Vec2 offset, int length, double r, double g, double b) {
        this.offset = offset;
        this.length = length;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    @Override
    public String getKeyword() {
        return "Square";
    }

    @Override
    public @NotNull Square serialize(@NotNull SerializeData data) throws SerializerException {
        Vec2 offset = data.of("Offset").serialize(Vec2.class).orElse(null);
        int length = data.of("Length").assertExists().assertRange(0, null).getInt().getAsInt();
        double r = data.of("R").assertRange(0.0, 1.0).getDouble().orElse(0.0);
        double g = data.of("G").assertRange(0.0, 1.0).getDouble().orElse(0.0);
        double b = data.of("B").assertRange(0.0, 1.0).getDouble().orElse(0.0);

        if (length == 0)
            throw data.exception("Length", "'Length' may not be '0'");

        return new Square(offset, length, r, g, b);
    }

    @Override
    public String toString() {
        return "Square{" +
            "offset=" + offset +
            ", length=" + length +
            ", r=" + r +
            ", g=" + g +
            ", b=" + b +
            '}';
    }

    public static class Vec2 implements Serializer<Vec2> {

        private int x, y;
        private boolean absolute;

        /**
         * Default constructor for serializer
         */
        public Vec2() {
        }

        public Vec2(int x, int y, boolean absolute) {
            this.x = x;
            this.y = y;
        }

        @NotNull @Override
        public Vec2 serialize(@NotNull SerializeData data) throws SerializerException {
            int x = data.of("X").assertExists().getInt().getAsInt();
            int y = data.of("Y").assertExists().getInt().getAsInt();
            boolean absolute = data.of("Absolute").getBool().orElse(true);

            return new Vec2(x, y, absolute);
        }

        @Override
        public String toString() {
            return "Vec2{" +
                "x=" + x +
                ", y=" + y +
                '}';
        }
    }
}
