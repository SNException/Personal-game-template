import java.awt.*;
import java.io.*;

public final class Main {

    static {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (assertionsEnabled) {
            System.out.println("Running with assertions enabled.");
        }
    }

    private static void preventMultipleInstancesOfProgram() {
        final File lock = new File("game.lock");
        if (lock.exists()) {
            javax.swing.JOptionPane.showMessageDialog(null, "Game is already running!", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            System.err.println("Game is already running!");
            System.exit(0);
        }
        try {
            lock.createNewFile();
        } catch (final IOException ex) {
        }

        lock.deleteOnExit();
    }

    public static void main(final String[] args) {
        // enable hardware accl
        // System.setProperty("sun.java2d.opengl", "True");

        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception ex) {
        }

        preventMultipleInstancesOfProgram();

        EventQueue.invokeLater(() -> {
            new Game();
        });
    }
}
