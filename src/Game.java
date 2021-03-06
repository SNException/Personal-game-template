import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.sound.sampled.*;

// TODO(nschultz): How about that Game.java has multiple class instances available (e.g TileHandler, ImageHandler ... etc)
public final class Game {

    private final Display display;

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

    private Font mainFont = null;
    private ArrayList<Entity> entities = null;
    private Player player = null;
    private Camera camera = null;

    public enum State {
        MENU,
        OVER_WORLD,
        TRANSITION;
    }
    private State state = State.MENU;

    private GameState menuState = null;
    private GameState overworldState = null;
    private GameState transitionState = null;

    private int selectedMenuItem = 0;
    private String[] menuItems = new String[] {
        "Start",
        "Exit"
    };

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

        mainFont = new Font("Monospaced", Font.BOLD, 14);
        imageCache = new HashMap<>();

        loadOverworld();

        menuState = new MenuState();
        overworldState = new OverWorldState();

        // TODO(nschultz): Play this when it is less obnoxious!
        // playSoundFile("res/retro_bg.wav", 0, true);

        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
    }

    // TODO(nschultz): Way later, we need our own build-in editor.
    private void loadOverworld() {
        final BufferedImage image = (BufferedImage) fetchImage("res/overworld.png");

        camera = new Camera(image.getWidth() * TILE_SIZE, image.getHeight() * TILE_SIZE);
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
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png"), true));

                    player = new Player(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE));
                } else if (r == 0 && g == 127 && b == 14) {
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png"), true));
                } else if (r == 0 && g == 38 && b == 255) {
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/water.png"), false));
                } else if (r == 62 && g == 86 && b == 0) {
                    // add grasstile under the tree, so we do not leave a hole
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png"), false));

                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/tree.png"), false));
                } else if (r == 96 && g == 80 && b == 0) {
                    // add mountaintile under the tree, so we do not leave a hole
                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/grass.png"), false));

                    entities.add(new SimpleTile(this, new Vector2f(x * Game.TILE_SIZE, y * Game.TILE_SIZE), fetchImage("res/mountain.png"), false));
                } else {
                    assert false : String.format("Uknown tile value %s\n", color.toString());
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

    public enum Dir {
        NORTH,
        SOUTH,
        WEST,
        EAST;
    }

    public Entity getNextTileFrom(final Entity src, final Dir dir) {
        assert src != null;
        assert dir != null;

        for (final Entity e : entities) {
            switch (dir) {
                case NORTH: {
                    if (e.v2.x == src.v2.x && e.v2.y == (src.v2.y - TILE_SIZE)) {
                        return e;
                    }
                } break;

                case SOUTH: {
                    if (e.v2.x == src.v2.x && e.v2.y == (src.v2.y + TILE_SIZE)) {
                        return e;
                    }
                } break;

                case WEST: {
                    if (e.v2.x  == (src.v2.x - TILE_SIZE) && e.v2.y == src.v2.y) {
                        return e;
                    }
                } break;

                case EAST: {
                    if (e.v2.x == (src.v2.x + TILE_SIZE) && e.v2.y == src.v2.y) {
                        return e;
                    }
                } break;

                default: {
                    assert false : "Unknown Dir!";
                } break;
            }
        }

        return null;
    }

    public boolean canMoveToTile(final Entity src, final Dir dir) {
        assert src != null;
        assert dir != null;

        final Entity e = getNextTileFrom(src, dir);
        return e != null && e.passable;
    }

    public void destroy() {
    }

    public void onNextFrame(final Graphics2D g, final Display.InputHandler input) {
        assert g     != null;
        assert input != null;

        processInput(input);
        update();
        render(g);
    }

    private void switchState(final State newState) {
        assert newState != null;

        transitionState = new StateTransitionState(newState);
        state = State.TRANSITION;
    }

    private void processInput(final Display.InputHandler input) {
        switch (state) {
            case MENU: {
                menuState.processInput(input);
            } break;

            case OVER_WORLD: {
                overworldState.processInput(input);
            } break;

            case TRANSITION: {
                transitionState.processInput(input);
            } break;

            default: {
                assert false;
            } break;
        }
    }

    private void update() {

        switch (state) {
            case MENU: {
                menuState.update();
            } break;

            case OVER_WORLD: {
                overworldState.update();
            } break;

            case TRANSITION: {
                transitionState.update();
            } break;

            default: {
                assert false;
            } break;
        }
    }

    private void render(final Graphics2D g) {
        g.setRenderingHints(renderingHints);

        switch (state) {
            case MENU: {
                menuState.render(g);
            } break;

            case OVER_WORLD: {
                overworldState.render(g);
            } break;

            case TRANSITION: {
                transitionState.render(g);
            } break;

            default: {
                assert false;
            } break;
        }
    }

    // TODO(nschultz): I am loading this file over and over and over again.
    // I need to put it into memory ONCE and then play it.
    public void playSoundFile(final String file, final float decibel, final boolean loop) {
        assert file != null;

        try {
            final Clip clip = (Clip) AudioSystem.getLine(new Line.Info(Clip.class));
            clip.addLineListener(new LineListener() {
                @Override
                public void update(final LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                }
            });

            clip.open(AudioSystem.getAudioInputStream(new File(file)));
            clip.setFramePosition(0);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            final FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(decibel);
            clip.start();
        } catch (final Exception ex) {
            ex.printStackTrace(System.err); // TODO(nschultz): Improve
        }
    }

    private Dimension calcStringSize(final Graphics2D g, final String str) {
        assert g   != null;
        assert str != null;

        final int strWidth = g.getFontMetrics().stringWidth(str);
        final int strHeight = g.getFontMetrics().getAscent();

        return new Dimension(strWidth, strHeight);
    }

    public void renderTextBox(final Graphics2D g, final Font font, final Color fg, final String str, final Rectangle rect, final Color bg) {
        assert g     != null;
        assert font  != null;
        assert fg    != null;
        assert str   != null;
        assert rect  != null;

        if (bg != null) {
            g.setColor(bg);
            g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);
        }

        g.setFont(font);
        g.setColor(fg);

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

    private interface GameState {
        void processInput(final Display.InputHandler input);
        void update();
        void render(final Graphics2D g);
    }

    private final class MenuState implements GameState {

        @Override
        public void processInput(final Display.InputHandler input) {
            if (input.isKeyDown(KeyEvent.VK_SPACE)) {
                if (selectedMenuItem == 0) {
                    switchState(State.OVER_WORLD);
                } else {
                    display.free();
                }
            }

            if (input.isKeyDown(KeyEvent.VK_W)) {
                playSoundFile("res/select.wav", -10f, false);
                selectedMenuItem = 0;
            }

            if (input.isKeyDown(KeyEvent.VK_S)) {
                playSoundFile("res/select.wav", -10f, false);
                selectedMenuItem = 1;
            }
        }

        @Override
        public void update() {
        }

        @Override
        public void render(final Graphics2D g) {
            g.setColor(new Color(0, 0, 0));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(mainFont.deriveFont((float) 32));
            final String str = "untitled";
            final Dimension dim = calcStringSize(g, str);

            g.drawString(str, (WIDTH / 2) - (dim.width / 2), (HEIGHT / 2) - (dim.height / 2));

            g.setColor(Color.WHITE);
            g.setFont(mainFont.deriveFont((float) 18));
            final int len = menuItems.length;
            for (int i = 0; i < len; ++i) {
                String item = menuItems[i];
                if (selectedMenuItem == i) {
                    item = "> " + item + " <";
                }
                final Dimension itemDim = calcStringSize(g, item);
                g.drawString(item, (WIDTH / 2) - (itemDim.width / 2), ((HEIGHT / 2) - (itemDim.height / 2)) + ((i + 1) * 32));
            }
        }
    }

    private final class OverWorldState implements GameState {

        @Override
        public void processInput(final Display.InputHandler input) {
            if (input.isKeyPressed(KeyEvent.VK_ESCAPE)) {
                switchState(State.MENU);
                return;
            }

            for (final Entity e : entities) {
                if (!camera.isInsideViewPort(e)) {
                    continue;
                }
                e.input(input);
            }
            player.input(input);
        }

        @Override
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

        @Override
        public void render(final Graphics2D g) {
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
        }
    }

    private final class StateTransitionState implements GameState {

        private final State newState;

        private float transitionBoxW = 0;
        private float transitionBoxH = 0;

        public StateTransitionState(final State newState) {
            assert newState != null;
            this.newState = newState;
        }

        @Override
        public void processInput(final Display.InputHandler input) {
            // we do not process any input in this state
        }

        @Override
        public void update() {
            if (transitionBoxW >= WIDTH && transitionBoxH >= HEIGHT) {
                state = newState;
            } else {
                // increment must be 4:3
                transitionBoxW += 16;
                transitionBoxH += 12;
            }
            return;
        }

        @Override
        public void render(final Graphics2D g) {
            g.setColor(Color.BLACK);
            g.fillRect((int) ((WIDTH / 2) - (transitionBoxW / 2)), (int) ((HEIGHT / 2) - (transitionBoxH / 2)), (int) transitionBoxW, (int) transitionBoxH);
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
