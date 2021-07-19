import java.awt.*;

public final class SimpleTile extends Entity {

    private Image image = null;

    public SimpleTile(final Game game, final Vector2f v2, final Image image, final boolean passable) {
        super(game, v2, Game.TILE_SIZE, Game.TILE_SIZE);

        assert image != null;
        this.image = image;
        this.passable = passable;
    }

    @Override
    public void update() {
    }

    @Override
    public void render(final Graphics2D g) {
        g.drawImage(image, (int) v2.x, (int) v2.y, w, h, null);
    }
}
