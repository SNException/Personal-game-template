import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;

public final class Game {

    private HashMap<RenderingHints.Key, Object> renderingHints;
    private final Display display;

    // 4:3
    private static final int WIDTH  = 320;
    private static final int HEIGHT = 240;

    private static final int TILE_SIZE = 8;

    private Image testImage;

    public Game() {
        assert EventQueue.isDispatchThread();
        display = new Display(this, WIDTH, HEIGHT, 60.0d);
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

        try {
            testImage = ImageIO.read(new File("res/player.png"));
        } catch (final IOException ex) {
            System.err.println("Missing assets!");
            System.exit(-1);
        }
    }

    public void destroy() {
    }

    public void update(final Display.InputHandler input) {
        if (input.isKeyPressed(KeyEvent.VK_W)) {
            y -= TILE_SIZE;
        } else if (input.isKeyPressed(KeyEvent.VK_S)) {
            y += TILE_SIZE;
        } else if (input.isKeyPressed(KeyEvent.VK_A)) {
            x -= TILE_SIZE;
        } else if (input.isKeyPressed(KeyEvent.VK_D)) {
            x += TILE_SIZE;
        }
    }

    private float x = (WIDTH  / 2) - (20 / 2);
    private float y = (HEIGHT / 2) - (20 / 2);

    public void render(final Graphics2D g) {
        g.setRenderingHints(renderingHints);

        g.setColor(new Color(10, 50, 10));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g.setColor(Color.WHITE);
        g.drawString("hello, world!", 50, 100);

        // scale images up a bit so we have more room for the font stuff (since we can increase the backbuffer size)
        g.drawImage(testImage, (int) x, (int) y, 16, 16, null);
    }
}
