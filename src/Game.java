import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;

public final class Game {

    // 4:3
    public static final int WIDTH  = 320;
    public static final int HEIGHT = 240;
    public static final int TILE_SIZE = 16;

    static {
        assert WIDTH  % TILE_SIZE == 0;
        assert HEIGHT % TILE_SIZE == 0;
    }

    private HashMap<RenderingHints.Key, Object> renderingHints = null;
    private HashMap<String, Image> imageCache = null;

    private ArrayList<Entity> entities = null;
    private Player player = null;
    private Camera camera = null;

    public Game() {
        assert EventQueue.isDispatchThread();
        new Display(this, WIDTH, HEIGHT, 60.0d);
    }

    public void init() {
        renderingHints = new HashMap<>();
        renderingHints.put(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_OFF);
        renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        renderingHints.put(RenderingHints.KEY_COLOR_RENDERING,     RenderingHints.VALUE_COLOR_RENDER_SPEED);
        renderingHints.put(RenderingHints.KEY_DITHERING,           RenderingHints.VALUE_DITHER_DISABLE);
        renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS,   RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        renderingHints.put(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        renderingHints.put(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_SPEED);
        renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,      RenderingHints.VALUE_STROKE_PURE);
        renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        imageCache = new HashMap<>();

        loadOverworld();
    }

    // TODO(nschultz): Way later, we need our own build-in editor.
    private void loadOverworld() {
        final BufferedImage image = (BufferedImage) fetchImage("res/overworld.png");

        camera = new Camera(image.getWidth() * Game.TILE_SIZE, image.getHeight() * Game.TILE_SIZE);
        if (entities != null) entities.clear();
        entities = new ArrayList<>(image.getWidth() * image.getHeight());

        final int w = image.getWidth();
        final int h = image.getHeight();
        for (int x = 0; x < w; ++x) {
            for (int y = 0; y < h; ++y) {
                final int rgb = image.getRGB(x, y);

                final Color color = new Color(rgb, /*hasAlpha*/ true);
                final int r = color.getRed();
                final int g = color.getGreen();
                final int b = color.getBlue();

                // Evaluate the pixels colors and populate the world accordingly!
                if (r == 255 && g == 0 && b == 0) {
                    // add grasstile under the player, so we do not leave a hole
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png")));

                    player = new Player(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE));
                } else if (r == 0 && g == 127 && b == 14) {
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png")));
                } else if (r == 0 && g == 38 && b == 255) {
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/water.png")));
                } else if (r == 62 && g == 86 && b == 0) {
                    // add grasstile under the tree, so we do not leave a hole
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png")));

                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/tree.png")));
                } else {
                    System.err.printf("Uknown tile value %s\n", color.toString());
                }
            }
        }
        assert player != null : "Overworld must have the player somewhere!";
    }

    public Image fetchImage(final String file) {
        assert file != null;

        if (imageCache.get(file) != null) {
            // fetch image out of the cache (instead of loading a new one from disk again)
            return imageCache.get(file);
        }

        try {
            final Image image = ImageIO.read(new File(file));
            imageCache.put(file, image);
            return image;
        } catch (final IOException ex) {
            System.err.printf("Failed to load asset '%s'!\n", file);
            System.exit(-1);
        }

        assert false;
        return null;
    }

    public void destroy() {
    }


    public void input(final Display.InputHandler input) {
        for (final Entity e : entities) {
            if (!camera.isInsideViewPort(e)) {
                continue;
            }
            e.input(input);
        }
        player.input(input);
    }

    public void update() {
        for (final Entity e : entities) {
            if (!camera.isInsideViewPort(e)) {
                continue;
            }
            e.update();
        }
        player.update();
        camera.centerOnEntity(player);
    }

    public void render(final Graphics2D g) {
        g.setRenderingHints(renderingHints);

        g.setColor(new Color(10, 50, 10));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.translate(-camera.xCam, -camera.yCam);
        for (final Entity e : entities) {
            if (!camera.isInsideViewPort(e)) {
                continue;
            }
            e.render(g);
        }
        player.render(g);
        g.translate(camera.xCam, camera.yCam);

        // renderTextBox(g, new Font("SansSerif", Font.PLAIN, 14), Color.WHITE, "Hi my name is FooBar. I am the one who will rescue the queen and end all suffering!", new Rectangle(50, 50, 200, 50));
    }

    public void renderTextBox(final Graphics2D g, final Font font, final Color color, final String str, final Rectangle rect) {
        assert g     != null;
        assert font  != null;
        assert color != null;
        assert str   != null;
        assert rect  != null;

        g.setFont(font);
        g.setColor(color);

        final String[] words = str.split(" ");
        final StringBuilder sbuffer = new StringBuilder(str.length());
        final StringBuilder end = new StringBuilder(str.length());
        for (int i = 0; i < words.length; ++i) {
            sbuffer.append(words[i]).append(" ");
            int strWidth = -1;
            if (i != words.length - 1) {
                strWidth = g.getFontMetrics().stringWidth(sbuffer.toString() + words[i + 1]);
            } else {
                strWidth = g.getFontMetrics().stringWidth(sbuffer.toString() + words[i]);
            }
            if (strWidth >= rect.width) {
                sbuffer.append("\n");
                end.append(sbuffer.toString());
                sbuffer.setLength(0);
            }
        }
        end.append(sbuffer.toString());

        final int strHeight = g.getFontMetrics().getAscent();
        int x = rect.x;
        int y = rect.y;
        final String[] lines = end.toString().split("\n");
        for (final String line : lines) {
            g.drawString(line, x, y += strHeight);
        }
    }

    private final class Camera {

        public float xCam;
        public float yCam;

        private final int mapWidth;
        private final int mapHeight;

        public Camera(final int mapWidth, final int mapHeight) {
            this.mapWidth  = mapWidth;
            this.mapHeight = mapHeight;
        }

        public void centerOnEntity(final Entity e) {
            assert e != null;

            if (!(e.v2.x < (WIDTH * 0.5f) - (e.w * 0.5)) && (!(e.v2.x > mapWidth - (WIDTH * 0.5f) - (e.w * 0.5)))) {
                final float xCenter = (e.v2.x - WIDTH  * 0.5f) + (e.w * 0.5f);
                xCam = xCenter;
            }

            if (!(e.v2.y < (HEIGHT * 0.5f) - (e.h * 0.5)) && (!(e.v2.y > mapHeight - (HEIGHT * 0.5f) - (e.h * 0.5)))) {
                final float yCenter = (e.v2.y - HEIGHT * 0.5f) + (e.h * 0.5f);
                yCam = yCenter;
            }
        }

        public boolean isInsideViewPort(final Entity source) {
            assert source != null;

            if (source.v2.x + source.w  < xCam) return false;
            if (source.v2.y + source.h  < yCam) return false;
            if (source.v2.x > xCam + WIDTH)     return false;
            if (source.v2.y > yCam + HEIGHT)    return false;

            return true;
        }
    }
}
