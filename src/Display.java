import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.lang.management.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;

public final class Display {

    private final Game game;
    private final MainLoop mainLoop;
    private final double hz;
    private final InputHandler input;
    private final Frame frame;
    private final Canvas canvas;
    private final BufferedImage backBuffer;
    private final BufferStrategy bufferStrategy;
    private final HashMap<RenderingHints.Key, Object> renderingHints;
    private final Graphics2D g;

    private double xScale  = 1;
    private double yScale  = 1;
    private double xCenter = 0;
    private double yCenter = 0;

    private final int width;
    private final int height;

    private enum DebugLevel {
        NONE,
        LIGHT,
        HEAVY;
    }

    private DebugLevel debug = DebugLevel.LIGHT;

    public Display(final Game game, final int width, final int height, final double hz) {
        assert game != null;
        assert width > 0 && height > 0 && hz > 0;

        this.width  = width;
        this.height = height;
        this.game   = game;
        this.hz     = hz;

        create_frame: {
            canvas = new Canvas();
            input = new InputHandler();
            canvas.addKeyListener(input);
            canvas.setIgnoreRepaint(true);
            canvas.setFocusable(true);

            frame = new Frame("untitled");
            frame.addWindowListener(new CustomWindowAdapter());
            frame.addComponentListener(new CustomComponentAdapter());
            frame.add(canvas);

            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setSize(screenSize.width / 2, screenSize.height / 2);
            frame.setLocationRelativeTo(null);

            frame.setVisible(true);
        }

        add_render_hints: {
            renderingHints = new HashMap<>();
            renderingHints.put(RenderingHints.KEY_ANTIALIASING,        RenderingHints.VALUE_ANTIALIAS_ON);
            renderingHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            renderingHints.put(RenderingHints.KEY_COLOR_RENDERING,     RenderingHints.VALUE_COLOR_RENDER_SPEED);
            renderingHints.put(RenderingHints.KEY_DITHERING,           RenderingHints.VALUE_DITHER_DISABLE);
            renderingHints.put(RenderingHints.KEY_FRACTIONALMETRICS,   RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            renderingHints.put(RenderingHints.KEY_INTERPOLATION,       RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            renderingHints.put(RenderingHints.KEY_RENDERING,           RenderingHints.VALUE_RENDER_SPEED);
            renderingHints.put(RenderingHints.KEY_STROKE_CONTROL,      RenderingHints.VALUE_STROKE_PURE);
            renderingHints.put(RenderingHints.KEY_TEXT_ANTIALIASING,   RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }

        alloc_backbuffer: {
            final GraphicsEnvironment gfxEnv      = GraphicsEnvironment.getLocalGraphicsEnvironment();
            final GraphicsDevice gfxDevice        = gfxEnv.getDefaultScreenDevice();
            final GraphicsConfiguration gfxConfig = gfxDevice.getDefaultConfiguration();
            backBuffer = gfxConfig.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
            g = backBuffer.createGraphics();

            canvas.createBufferStrategy(2); // two buffers are always supported TODO(nschultz): Check if we can use 3
            bufferStrategy = canvas.getBufferStrategy();
        }

        hack_scheduler_granularity: {
            //
            // https://stackoverflow.com/questions/824110/accurate-sleep-for-java-on-windows
            //
            // "If timing is crucial to your application, then an inelegant but practical way to get
            // round these bugs is to leave a daemon thread running throughout the duration of your
            // application that simply sleeps for a large prime number of milliseconds (Long.MAX_VALUE will do).
            // This way, the interrupt period will be set once per invocation of your application,
            // minimising the effect on the system clock, and setting the sleep granularity to 1ms even
            // where the default interrupt period isn't 15ms."
            //
            final Thread thread = new Thread(() -> {
                while (true) {
                    sleepMillis(Long.MAX_VALUE);
                }
            });
            thread.setName("granularity_hack_thread");
            thread.setDaemon(true);
            thread.start();
        }

        start_mainloop: {
            mainLoop = new MainLoop();
            final Thread thread = new Thread(mainLoop);
            thread.setName("mainloop_thread");
            thread.setDaemon(true);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }
    }

    private void update() {
        if (input.isKeyUp(KeyEvent.VK_F11)) {
            final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (frame.isUndecorated()) {
                // windowed
                frame.dispose();
                frame.setUndecorated(false);
                frame.setSize(screenSize.width / 2, screenSize.height / 2);
                frame.setLocationRelativeTo(null);
                frame.setCursor(Cursor.getDefaultCursor());
                frame.setVisible(true);
            } else {
                // fullscreen
                frame.dispose();
                frame.setUndecorated(true);
                frame.setSize(screenSize.width, screenSize.height);
                frame.setLocationRelativeTo(null);
                frame.setCursor(frame.getToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(), null));
                frame.setVisible(true);
            }
        } else if (input.isKeyUp(KeyEvent.VK_F12)) {
            switch (debug) {
                case NONE: {
                    debug = DebugLevel.LIGHT;
                } break;

                case LIGHT: {
                    debug = DebugLevel.HEAVY;
                } break;

                case HEAVY: {
                    debug = DebugLevel.NONE;
                } break;

                default: {
                    assert false;
                }
            }
        }

        game.update(input);

        input.update(); // must be the last call inside this function
    }

    private void render() {
        // TODO(nschultz): reset graphics object for the game

        // render game code onto the backbuffer
        game.render(g);

        // I use a bufferstrategy so I can render stuff independent of the scaled backbuffer. For example
        // the debug information.
        do {
            do {
                final Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
                g.setRenderingHints(renderingHints);

                // clear the canvas
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

                g.drawImage(backBuffer, (int) xCenter, (int) yCenter, (int) (width * xScale), (int) (height * yScale), null);
                // g.drawImage(backBuffer, 0, 0, canvas.getWidth(), canvas.getHeight(), null);

                if (debug != DebugLevel.NONE) {
                    renderDebugInfo(g);
                }

                g.dispose();
            } while (bufferStrategy.contentsRestored());

            // show the frame
            bufferStrategy.show();

            // flush display buffer to ensure the frame is displayed immediately and NOT buffered!
            Toolkit.getDefaultToolkit().sync();

        } while (bufferStrategy.contentsLost());
    }

    private final Font mainFont = new Font("SansSerif", Font.PLAIN, 24);

    public void renderDebugInfo(final Graphics2D g) {
        g.setFont(mainFont);

        frame_time: {
            g.setColor(mainLoop.isLagging() ? Color.RED : Color.WHITE);
            final String frameTimeStr =  String.format("%.3f/%.3f ms", mainLoop.rawFrameTimeMillis, mainLoop.cookedFrameTimeMillis);
            final int sw = g.getFontMetrics().stringWidth(frameTimeStr);
            g.drawString(frameTimeStr, canvas.getWidth() - (sw + 24), 32);
        }

        if (debug != DebugLevel.HEAVY) return;

        memory: {
            // this approach of calculating only works when we set the 'Xms' and 'Xmx' to the same value
            final double maxHeapMemoryMb  = (double) Runtime.getRuntime().maxMemory()  / Math.pow(1024.0d, 2.0d);
            final double usedHeapMemoryMb = maxHeapMemoryMb - ((double) Runtime.getRuntime().freeMemory() / Math.pow(1024.0d, 2.0d));

            final String memStr =  String.format("%.1f/%.1f mb", usedHeapMemoryMb, maxHeapMemoryMb);
            final int sw = g.getFontMetrics().stringWidth(memStr);
            g.setColor(Color.WHITE);
            g.drawString(memStr, canvas.getWidth() - (sw + 24), 64);
        }

        jit_info: {
            final CompilationMXBean jitBean = ManagementFactory.getCompilationMXBean();
            if (jitBean.isCompilationTimeMonitoringSupported()) {
                final String jitStr =  String.format("%s ms", jitBean.getTotalCompilationTime());
                final int sw = g.getFontMetrics().stringWidth(jitStr);
                g.setColor(Color.WHITE);
                g.drawString(jitStr, canvas.getWidth() - (sw + 24), 96);
            }
        }

        gc_info: {
            final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans(); // can not cache this, since it can change during runtime
            long gcTotalTime = 0;
            long gcTotalCount = 0;
            for (final GarbageCollectorMXBean gcBean : gcBeans) {
                final long gcTime  = gcBean.getCollectionTime();
                final long gcCount = gcBean.getCollectionCount();
                if (gcTime  == -1) continue;
                if (gcCount == -1) continue;

                gcTotalTime  += gcTime;
                gcTotalCount += gcCount;
            }
            final String gcStr = String.format("%s (%s) ms", gcTotalTime, gcTotalCount);
            final int sw = g.getFontMetrics().stringWidth(gcStr);
            g.setColor(Color.WHITE);
            g.drawString(gcStr, canvas.getWidth() - (sw + 24), 128);
        }

        frame_count: {
            g.setColor(Color.WHITE);
            final String frameCountStr = mainLoop.totalFramesRendered + " frames";
            final int sw = g.getFontMetrics().stringWidth(frameCountStr);
            g.drawString(frameCountStr, canvas.getWidth() - (sw + 24), 160);
        }
    }

    private void sleepMillis(final long millis) {
        assert millis > 0;

        try {
            Thread.sleep(millis, 0);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private final class CustomWindowAdapter extends WindowAdapter {

        @Override
        public void windowClosing(final WindowEvent evt) {
            cleanup: {
                game.destroy();
                mainLoop.running = false;
                g.dispose();
                ((Frame) evt.getSource()).dispose();
                Runtime.getRuntime().gc();
                Runtime.getRuntime().runFinalization();
            }
            System.exit(0);
        }
    }

    private final class CustomComponentAdapter extends ComponentAdapter {

        @Override
        public void componentResized(final ComponentEvent evt) {
            xScale = canvas.getWidth()  / backBuffer.getWidth();
            yScale = canvas.getHeight() / backBuffer.getHeight();
            if (xScale < 1) xScale = 1;
            if (yScale < 1) yScale = 1;
            if (xScale > yScale) xScale = yScale;
            if (yScale > xScale) yScale = xScale;

            xCenter = (canvas.getWidth()  - backBuffer.getWidth()  * xScale) / 2;
            yCenter = (canvas.getHeight() - backBuffer.getHeight() * yScale) / 2;
        }
    }

    private final class MainLoop implements Runnable {

        public volatile boolean running = false;

        public long totalFramesRendered     = 0;
        public double cookedFrameTimeMillis = 0;
        public double rawFrameTimeMillis    = 0;

        private final double targetTimeMillis = 1000.0d / hz;
        private final long OVERSLEEP_GUARD = estimateSchedulerGranularity();

        @Override
        public void run() {
            running = true;

            game.init();
            while (running) {
                double startTimeMillis = now();

                try {
                    EventQueue.invokeAndWait(() -> {
                        update();
                        render();
                        totalFramesRendered += 1;
                    });
                } catch (final InvocationTargetException ex) {
                    if (ex.getCause() instanceof AssertionError) {
                        final StackTraceElement frame = ex.getCause().getStackTrace()[0];
                        final String message = String.format("assert tripped: %s:%s", frame.getFileName(), frame.getLineNumber());
                        System.err.println(message);
                        System.exit(-1);
                    } else {
                        ex.printStackTrace(System.err);
                        System.exit(-1);
                    }
                } catch (final InterruptedException ex) {
                    assert false : "Not supposed to interrupt this thread!";
                }

                double workTimeMillis = now() - startTimeMillis;
                rawFrameTimeMillis = workTimeMillis;
                if (workTimeMillis < targetTimeMillis) {
                    assert !EventQueue.isDispatchThread() : "Must not sleep on UI thread!";

                    // We still have time, so we have to artifically wait to reach the target time
                    final long waitTime = (long) (targetTimeMillis - workTimeMillis);
                    if (waitTime - OVERSLEEP_GUARD > 0) {
                        sleepMillis(waitTime - OVERSLEEP_GUARD);
                    }

                    // busy wait the rest of time
                    while (workTimeMillis < targetTimeMillis) {
                        workTimeMillis = now() - startTimeMillis;
                    }

                } else {
                    // We have missed our target time :(
                }

                cookedFrameTimeMillis = now() - startTimeMillis;
            }
        }

        private boolean isLagging() {
            return rawFrameTimeMillis > targetTimeMillis;
        }

        private double now() {
            return System.nanoTime() / 1000000.0d;
        }

        private long estimateSchedulerGranularity() {
            final long[] result = new long[50];
            for (int i = 0, l = result.length; i < l; ++i) {
                final double beforeSleep = now();
                sleepMillis(1);
                long deltaTime = (long) (now() - beforeSleep);
                if (deltaTime == 0) {
                    deltaTime = 1;
                }
                result[i] = deltaTime;
            }

            Arrays.sort(result);
            return result[0];
        }
    }

    public final class InputHandler extends KeyAdapter {

        private final boolean[] keys = new boolean[Short.MAX_VALUE / 2]; // pressed in current frame
        private final boolean[] lastKeys = new boolean[keys.length];     // pressed in last frame
        {
            assert keys.length == lastKeys.length;
        }

        // gets called every frame
        public void update() {
            for (int i = 0; i < keys.length; ++i) {
                lastKeys[i] = keys[i];
            }
        }

        public boolean isKeyPressed(final int code) {
            return keys[code];
        }

        public boolean isKeyUp(final int code) {
            return !keys[code] && lastKeys[code];
        }

        public boolean isKeyDown(final int code) {
            return keys[code] && !lastKeys[code];
        }

        @Override
        public void keyPressed(final KeyEvent evt) {
            keys[evt.getKeyCode()] = true;
        }

        @Override
        public void keyReleased(final KeyEvent evt) {
            keys[evt.getKeyCode()] = false;
        }
    }
}
