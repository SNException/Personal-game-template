import java.awt.*;

public abstract class Entity {

    public final Game game;
    public final Vector2f v2;
    public final int w;
    public final int h;

    public boolean passable = true;

    public Entity(final Game game, final Vector2f v2, final int w, final int h) {
        assert game != null;
        assert v2 != null && v2.x % Game.TILE_SIZE == 0 && v2.y % Game.TILE_SIZE == 0;
        assert w % Game.TILE_SIZE == 0;
        assert h % Game.TILE_SIZE == 0;

        this.game = game;
        this.v2   = v2;
        this.w    = w;
        this.h    = h;
    }

    public void input(final Display.InputHandler input) {
        // override by subclasses who are interested in input
    }

    public abstract void update();
    public abstract void render(final Graphics2D g);
}
