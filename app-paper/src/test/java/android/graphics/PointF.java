package android.graphics;

public class PointF {

    public final float x;
    public final float y;

    public PointF(float x, float y) {
        this.setX(x);
        this.setY(y);
    }

    @Override
    public String toString() {
        return "PointF(" + getX() + ", " + getY() + ")";
    }
}
