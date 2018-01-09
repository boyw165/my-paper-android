package android.graphics;

public class PointF {

    public final float x;
    public final float y;

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "PointF(" + x + ", " + y + ")";
    }
}
