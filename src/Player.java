import java.awt.*;
import java.awt.event.*;

public final class Player extends Entity {

    private final Image image;

    private boolean moveUp    = false;
    private boolean moveDown  = false;
    private boolean moveLeft  = false;
    private boolean moveRight = false;
    private boolean doneMoving = false;
    private int movementRemaining = 0;

    public Player(final Game game, final Vector2f v2) {
        super(game, v2, Game.TILE_SIZE, Game.TILE_SIZE);

        image = game.fetchImage("res/player.png");
    }

    @Override
    public void input(final Display.InputHandler input) {
        if (moveUp | moveDown | moveLeft | moveRight) return;

        if (input.isKeyPressed(KeyEvent.VK_W)) {
            moveUp = true;
            doneMoving = true;
            movementRemaining = Game.TILE_SIZE;
            // game.playSoundFile("res/walk.wav", -10, false);
        } else if (input.isKeyPressed(KeyEvent.VK_S)) {
            moveDown = true;
            doneMoving = true;
            movementRemaining = Game.TILE_SIZE;
            // game.playSoundFile("res/walk.wav", -10, false);
        } else if (input.isKeyPressed(KeyEvent.VK_A)) {
            moveLeft = true;
            doneMoving = true;
            movementRemaining = Game.TILE_SIZE;
            // game.playSoundFile("res/walk.wav", -10, false);
        } else if (input.isKeyPressed(KeyEvent.VK_D)) {
            moveRight = true;
            doneMoving = true;
            movementRemaining = Game.TILE_SIZE;
            // game.playSoundFile("res/walk.wav", -10, false);
        }
    }

    @Override
    public void update() {
       if (moveUp) {
            if (movementRemaining == 0) {
                moveUp = false;
            } else {
                v2.y -= 1;
                movementRemaining -= 1;
            }
        } else if (moveDown) {
            if (movementRemaining == 0) {
                moveDown = false;
            } else {
                v2.y += 1;
                movementRemaining -= 1;
            }
        } else if (moveLeft) {
            if (movementRemaining == 0) {
                moveLeft = false;
            } else {
                v2.x -= 1;
                movementRemaining -= 1;
            }
        } else if (moveRight) {
            if (movementRemaining == 0) {
                moveRight = false;
            } else {
                v2.x += 1;
                movementRemaining -= 1;
            }
        }

        if (movementRemaining == 0 && !moveUp && !moveDown && !moveLeft && !moveRight) {
            assert v2.x % Game.TILE_SIZE == 0 : "Player ended on wrong coordinates.";
            assert v2.y % Game.TILE_SIZE == 0 : "Player ended on wrong coordinates.";

            if (doneMoving) {
                doneMoving = false;
            }
        }
    }

    @Override
    public void render(final Graphics2D g) {
        g.drawImage(image, (int) v2.x, (int) v2.y, w, h, null);
    }
}
