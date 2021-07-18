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
        EventQueue.invokeLater(() -> {
            new Game();
        });
    }
}
