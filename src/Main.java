import java.awt.*;

public final class Main {

    static {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (assertionsEnabled) {
            System.out.println("Running with assertions enabled.");
        }
    }

    public static void main(final String[] args) {
        // enable hardware accl
        // System.setProperty("sun.java2d.opengl", "True");

        EventQueue.invokeLater(() -> {
            new Game();
        });
    }
}
