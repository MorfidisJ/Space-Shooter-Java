import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class SpaceInvaders extends JPanel implements ActionListener, KeyListener, MouseListener {
    private static final int MENU = 0;
    private static final int PLAYING = 1;
    private static final int GAME_OVER = 2;
    private static final int PLAYER_EXPLOSION = 3;
    private static final int LEVEL_TRANSITION = 4;
    private int gameState = MENU;
    private int lives = 3;
    private int explosionFrame = 0;
    private int levelTransitionFrame = 0;
    private float menuGlowIntensity = 0.0f;
    private boolean menuGlowIncreasing = true;

    Timer timer;
    boolean left, right, space;
    int score = 0;
    int highScore = 0;
    int level = 1;
    Random random = new Random();

    private static final int ALIENS_PER_LEVEL = 5;
    private static final int MAX_ALIENS = 40;
    private static final double DIFFICULTY_INCREASE = 0.2;
    private int currentAlienCount = ALIENS_PER_LEVEL;
    private double enemySpeed = 1.0;
    private double swoopProbability = 0.01;

    Player player;
    ArrayList<Bullet> bullets = new ArrayList<>();
    ArrayList<Alien> aliens = new ArrayList<>();
    ArrayList<Explosion> explosions = new ArrayList<>();
    ArrayList<Star> stars = new ArrayList<>();

    private Rectangle startButton = new Rectangle(300, 250, 200, 50);
    private Rectangle exitButton = new Rectangle(300, 350, 200, 50);
    private Rectangle restartButton = new Rectangle(300, 250, 200, 50);
    private Rectangle menuButton = new Rectangle(300, 350, 200, 50);

    public SpaceInvaders() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        player = new Player(375, 520);
        initializeStars();

        timer = new Timer(15, this);
        timer.start();
        addKeyListener(this);
        addMouseListener(this);
    }

    private void startGame() {
        gameState = PLAYING;
        score = 0;
        level = 1;
        lives = 3;
        currentAlienCount = ALIENS_PER_LEVEL;
        enemySpeed = 1.0;
        swoopProbability = 0.01;
        explosionFrame = 0;
        timer.setDelay(15);
        createNewWave();
    }

    private void createNewWave() {
        aliens.clear();
        int rows = (int) Math.ceil(currentAlienCount / 10.0);
        int cols = Math.min(10, currentAlienCount);
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (row * 10 + col < currentAlienCount) {
                    int x = 80 + col * 60;
                    int y = 60 + row * 40;
                    aliens.add(new Alien(x, y, random.nextInt(4)));
                }
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        for (Star star : stars) {
            star.draw(g2d);
        }

        switch (gameState) {
            case MENU:
                drawMenu(g2d);
                break;
            case PLAYING:
                drawGame(g2d);
                break;
            case GAME_OVER:
                drawGameOver(g2d);
                break;
            case PLAYER_EXPLOSION:
                drawGame(g2d);
                drawPlayerExplosion(g2d);
                break;
            case LEVEL_TRANSITION:
                drawGame(g2d);
                drawLevelTransition(g2d);
                break;
        }
    }

    private void drawMenu(Graphics2D g2d) {
        if (menuGlowIncreasing) {
            menuGlowIntensity += 0.02f;
            if (menuGlowIntensity >= 1.0f) menuGlowIncreasing = false;
        } else {
            menuGlowIntensity -= 0.02f;
            if (menuGlowIntensity <= 0.0f) menuGlowIncreasing = true;
        }

        String title = "SPACE INVADERS";
        FontMetrics fm = g2d.getFontMetrics(new Font("Courier", Font.BOLD, 60));
        int titleX = (getWidth() - fm.stringWidth(title)) / 2;
        int titleY = 150;

        for (int i = 0; i < 5; i++) {
            float alpha = Math.max(0, 0.1f - (i * 0.01f));
            g2d.setColor(new Color(0, 255, 0, (int)(alpha * 255)));
            g2d.setFont(new Font("Courier", Font.BOLD, 60));
            g2d.drawString(title, titleX - i, titleY - i);
            g2d.drawString(title, titleX + i, titleY + i);
        }

        g2d.setColor(Color.WHITE);
        g2d.drawString(title, titleX, titleY);

        drawButton(g2d, startButton, "START GAME", new Color(0, 255, 0));
        drawButton(g2d, exitButton, "EXIT", new Color(255, 0, 0));

        g2d.setFont(new Font("Courier", Font.PLAIN, 16));
        g2d.setColor(new Color(200, 200, 200));
        g2d.drawString("Version 1.0", 20, getHeight() - 40);
        g2d.drawString("Use Arrow Keys to Move, Space to Shoot", (getWidth() - fm.stringWidth("Use Arrow Keys to Move, Space to Shoot")) / 2, getHeight() - 20);
    }

    private void drawButton(Graphics2D g2d, Rectangle button, String text, Color baseColor) {
        Point mousePos = getMousePosition();
        boolean isHovered = mousePos != null && button.contains(mousePos);
        
        if (isHovered) {
            for (int i = 0; i < 3; i++) {
                float alpha = (1.0f - (i * 0.3f)) * 0.5f;
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), (int)(alpha * 255)));
                g2d.fill(new Rectangle(
                    button.x - i,
                    button.y - i,
                    button.width + (i * 2),
                    button.height + (i * 2)
                ));
            }
        }
        
        g2d.setColor(isHovered ? baseColor.brighter() : baseColor.darker());
        g2d.fill(button);
        
        g2d.setColor(isHovered ? baseColor.brighter().brighter() : baseColor);
        g2d.setStroke(new BasicStroke(isHovered ? 2 : 1));
        g2d.draw(button);
        
        g2d.setFont(new Font("Courier", Font.BOLD, 24));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = button.x + (button.width - fm.stringWidth(text)) / 2;
        int textY = button.y + (button.height + fm.getAscent()) / 2;
        
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(text, textX + 2, textY + 2);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, textX, textY);
    }

    private void drawGameOver(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(0, 0, 0, 200),
            0, getHeight(), new Color(100, 0, 0, 200)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        String gameOverText = "GAME OVER";
        g2d.setFont(new Font("Courier", Font.BOLD, 70));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(gameOverText)) / 2;
        int textY = 150;
        
        for (int i = 0; i < 8; i++) {
            float alpha = (1.0f - (i * 0.125f)) * 0.5f;
            g2d.setColor(new Color(255, 0, 0, (int)(alpha * 255)));
            g2d.drawString(gameOverText, textX - i, textY - i);
            g2d.drawString(gameOverText, textX + i, textY + i);
        }
        
        g2d.setColor(Color.WHITE);
        g2d.drawString(gameOverText, textX, textY);

        g2d.setFont(new Font("Courier", Font.BOLD, 30));
        String[] stats = {
            "Score: " + score,
            "High Score: " + highScore,
            "Level: " + level
        };
        
        int yPos = 220;
        int spacing = 40;
        
        for (String stat : stats) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.drawString(stat, 
                (getWidth() - fm.stringWidth(stat)) / 2 + 2, 
                yPos + 2);
            
            g2d.setColor(Color.WHITE);
            g2d.drawString(stat, 
                (getWidth() - fm.stringWidth(stat)) / 2, 
                yPos);
            yPos += spacing;
        }

        restartButton.setLocation(300, 350);
        menuButton.setLocation(300, 420);
        drawButton(g2d, restartButton, "RESTART", new Color(0, 255, 0));
        drawButton(g2d, menuButton, "MAIN MENU", new Color(255, 165, 0));
    }

    private void drawPlayerExplosion(Graphics2D g2d) {
        explosionFrame++;
        if (explosionFrame > 30) {
            lives--;
            if (lives <= 0) {
                gameState = GAME_OVER;
                if (score > highScore) {
                    highScore = score;
                }
            } else {
                gameState = PLAYING;
                player = new Player(375, 520);
                explosionFrame = 0;
                timer.setDelay(15);
            }
            return;
        }

        int size = explosionFrame * 4;
        int alpha = 255 - (explosionFrame * 8);
        
        Color[] explosionColors = {
            new Color(255, 100, 0, alpha),
            new Color(255, 255, 0, alpha),
            new Color(255, 0, 0, alpha)
        };
        
        for (int i = 0; i < explosionColors.length; i++) {
            int layerSize = size - (i * 15);
            if (layerSize > 0) {
                g2d.setColor(explosionColors[i]);
                g2d.fillOval(player.x + 20 - layerSize/2, 
                           player.y + 15 - layerSize/2, 
                           layerSize, layerSize);
            }
        }

        for (int i = 0; i < 12; i++) {
            double angle = (i * Math.PI / 6) + (explosionFrame * 0.2);
            int particleX = player.x + 20 + (int)(Math.cos(angle) * size);
            int particleY = player.y + 15 + (int)(Math.sin(angle) * size);
            
            for (int j = 0; j < 3; j++) {
                int trailAlpha = alpha - (j * 50);
                if (trailAlpha > 0) {
                    g2d.setColor(new Color(255, 255, 0, trailAlpha));
                    g2d.fillOval(
                        particleX - 5 - (int)(Math.cos(angle) * j * 5),
                        particleY - 5 - (int)(Math.sin(angle) * j * 5),
                        10, 10
                    );
                }
            }
            
            g2d.setColor(new Color(255, 255, 0, alpha));
            g2d.fillOval(particleX - 5, particleY - 5, 10, 10);
        }

        if (explosionFrame < 10) {
            int shake = (int)(Math.random() * 5) - 2;
            g2d.translate(shake, shake);
        }

        timer.setDelay(30);
    }

    private void drawLevelTransition(Graphics2D g2d) {
        levelTransitionFrame++;
        if (levelTransitionFrame > 60) {
            gameState = PLAYING;
            createNewWave();
            return;
        }

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        String levelText = "LEVEL " + level;
        g2d.setFont(new Font("Courier", Font.BOLD, 50));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(levelText)) / 2;
        int textY = getHeight() / 2;

        float progress = Math.min(1.0f, levelTransitionFrame / 30.0f);
        float scale = 1.0f + (float)Math.sin(progress * Math.PI) * 0.2f;
        float alpha = Math.min(1.0f, (60 - levelTransitionFrame) / 30.0f);

        for (int i = 0; i < 5; i++) {
            float glowAlpha = (1.0f - (i * 0.2f)) * alpha;
            g2d.setColor(new Color(0, 255, 0, (int)(glowAlpha * 255)));
            g2d.setFont(new Font("Courier", Font.BOLD, (int)(50 * scale)));
            fm = g2d.getFontMetrics();
            int glowX = (getWidth() - fm.stringWidth(levelText)) / 2;
            int glowY = getHeight() / 2 + fm.getAscent() / 2;
            g2d.drawString(levelText, glowX - i, glowY - i);
            g2d.drawString(levelText, glowX + i, glowY + i);
        }

        g2d.setColor(new Color(255, 255, 255, (int)(alpha * 255)));
        g2d.drawString(levelText, textX, textY);
    }

    private void drawGame(Graphics2D g2d) {
        if (gameState != PLAYER_EXPLOSION) {
            player.draw(g2d);
        }
        ArrayList<Bullet> bulletsCopy;
        synchronized (bullets) {
            bulletsCopy = new ArrayList<>(bullets);
        }
        for (Bullet bullet : bulletsCopy) {
            bullet.draw(g2d);
        }
        for (Alien a : aliens) {
            a.draw(g2d);
        }
        for (Explosion e : explosions) {
            if (e.update()) {
                e.draw(g2d);
            } else {
                explosions.remove(e);
            }
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Courier", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);
        g2d.drawString("Level: " + level, 20, 60);
        g2d.drawString("High Score: " + highScore, 20, 90);
        
        g2d.setColor(new Color(0, 255, 0));
        for (int i = 0; i < lives; i++) {
            drawLifeIcon(g2d, 20 + i * 30, 120);
        }
    }

    private void drawLifeIcon(Graphics2D g2d, int x, int y) {
        g2d.setColor(new Color(255, 100, 0, 150));
        g2d.fillOval(x + 5, y + 15, 5, 8);
        
        g2d.setColor(new Color(0, 255, 0));
        int[] xPoints = {x, x + 15, x + 30};
        int[] yPoints = {y + 20, y, y + 20};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        g2d.setColor(new Color(100, 200, 255, 200));
        g2d.fillOval(x + 10, y + 8, 10, 10);
        g2d.setColor(new Color(100, 200, 255));
        g2d.fillOval(x + 12, y + 10, 6, 6);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == PLAYING || gameState == PLAYER_EXPLOSION) {
            updateStars();
            if (gameState == PLAYING) {
                updatePlayer();
                updateBullets();
                updateAliens();
                checkCollisions();
                checkLevelComplete();
            }
        }
        repaint();
    }

    private void checkLevelComplete() {
        if (aliens.isEmpty()) {
            gameState = LEVEL_TRANSITION;
            levelTransitionFrame = 0;
            level++;
            currentAlienCount = Math.min(MAX_ALIENS, ALIENS_PER_LEVEL + (level - 1) * 5);
            enemySpeed = 1.0 + (level - 1) * DIFFICULTY_INCREASE;
            swoopProbability = Math.min(0.05, 0.01 + (level - 1) * 0.005);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (gameState == MENU) {
            if (startButton.contains(e.getPoint())) {
                startGame();
            } else if (exitButton.contains(e.getPoint())) {
                System.exit(0);
            }
        } else if (gameState == GAME_OVER) {
            if (restartButton.contains(e.getPoint())) {
                startGame();
            } else if (menuButton.contains(e.getPoint())) {
                gameState = MENU;
            }
        }
    }

    private void updateStars() {
        for (Star star : stars) {
            star.move();
        }
    }

    private void updatePlayer() {
        if (left) player.move(-5);
        if (right) player.move(5);
        if (space && bullets.size() < 3) {
            bullets.add(new Bullet(player.x + 18, player.y));
        }
    }

    private void updateBullets() {
        Iterator<Bullet> bIter = bullets.iterator();
        while (bIter.hasNext()) {
            Bullet b = bIter.next();
            b.move();
            if (b.y < 0) {
                bIter.remove();
            }
        }
    }

    private void updateAliens() {
        boolean edgeHit = false;
        for (Alien a : aliens) {
            if (!a.isSwooping) {
                a.move(enemySpeed);
                if (a.x < 0 || a.x > getWidth() - 40) {
                    edgeHit = true;
                }
            } else {
                a.updateSwoop();
            }

            if (!a.isSwooping && random.nextDouble() < swoopProbability) {
                a.startSwoop();
            }
        }

        if (edgeHit) {
            for (Alien a : aliens) {
                if (!a.isSwooping) {
                    a.dy += 10;
                    a.reverse();
                }
            }
        }
    }

    private void checkCollisions() {
        if (gameState != PLAYING) return;

        Iterator<Alien> aIter = aliens.iterator();
        while (aIter.hasNext()) {
            Alien a = aIter.next();
            
            Iterator<Bullet> bIter = bullets.iterator();
            while (bIter.hasNext()) {
                Bullet b = bIter.next();
                if (a.getBounds().intersects(b.getBounds())) {
                    explosions.add(new Explosion(a.x, a.y));
                    aIter.remove();
                    bIter.remove();
                    score += 10 * a.type;
                    if (score > highScore) {
                        highScore = score;
                    }
                    break;
                }
            }

            if (a.getBounds().intersects(player.getBounds())) {
                handlePlayerHit();
                return;
            }

            if (a.y >= player.y) {
                handlePlayerHit();
                return;
            }
        }
    }

    private void handlePlayerHit() {
        if (gameState == PLAYING) {
            gameState = PLAYER_EXPLOSION;
            explosionFrame = 0;
            bullets.clear();
            aliens.removeIf(a -> Math.abs(a.x - player.x) < 50 && Math.abs(a.y - player.y) < 50);
        }
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) left = true;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = true;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) space = true;
    }

    @Override public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) left = false;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) right = false;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) space = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    class Player {
        int x, y;
        int width = 40, height = 30;
        Color shipColor = new Color(0, 255, 0);
        Color engineColor = new Color(255, 100, 0);

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void move(int dx) {
            x += dx;
            if (x < 0) x = 0;
            if (x > getWidth() - width) x = getWidth() - width;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(engineColor);
            g2d.fillOval(x + 15, y + 25, 10, 15);
            
            g2d.setColor(shipColor);
            int[] xPoints = {x, x + width/2, x + width};
            int[] yPoints = {y + height, y, y + height};
            g2d.fillPolygon(xPoints, yPoints, 3);
            
            g2d.setColor(new Color(100, 200, 255));
            g2d.fillOval(x + 15, y + 10, 10, 10);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Bullet {
        int x, y;
        int speed = 10;
        int width = 4, height = 15;
        Color bulletColor = new Color(255, 255, 100);

        public Bullet(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void move() {
            y -= speed;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 100, 100));
            g2d.fillRect(x, y + height, width, 10);
            
            g2d.setColor(bulletColor);
            g2d.fillRect(x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Alien {
        int x, y;
        int dx = 1;
        int dy = 0;
        int width = 30, height = 30;
        int type;
        boolean isSwooping = false;
        int swoopStartX, swoopStartY;
        double swoopAngle = 0;
        Color[] alienColors = {
            new Color(255, 0, 0),
            new Color(255, 165, 0),
            new Color(255, 255, 0),
            new Color(0, 255, 0)
        };

        public Alien(int x, int y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }

        public void move(double speed) {
            x += (int)(dx * speed);
            y += dy;
            dy = 0;
        }

        public void startSwoop() {
            isSwooping = true;
            swoopStartX = x;
            swoopStartY = y;
            swoopAngle = 0;
        }

        public void updateSwoop() {
            swoopAngle += 0.1;
            x = swoopStartX + (int)(Math.sin(swoopAngle) * 100);
            y = swoopStartY + (int)(swoopAngle * 30);
            
            if (y > 500) {
                isSwooping = false;
                y = swoopStartY;
                x = swoopStartX;
            }
        }

        public void reverse() {
            dx = -dx;
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(alienColors[type]);
            
            switch(type) {
                case 0:
                    g2d.fillRect(x, y, width, height);
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x + 5, y + 5, 8, 8);
                    g2d.fillOval(x + 17, y + 5, 8, 8);
                    break;
                case 1:
                    int[] xPoints = {x, x + width/2, x + width};
                    int[] yPoints = {y + height, y, y + height};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x + 10, y + 10, 10, 10);
                    break;
                case 2:
                    g2d.fillOval(x, y, width, height);
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x + 8, y + 8, 14, 14);
                    break;
                case 3:
                    int[] xDiamond = {x + width/2, x + width, x + width/2, x};
                    int[] yDiamond = {y, y + height/2, y + height, y + height/2};
                    g2d.fillPolygon(xDiamond, yDiamond, 4);
                    g2d.setColor(Color.BLACK);
                    g2d.fillOval(x + 10, y + 10, 10, 10);
                    break;
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    class Explosion {
        int x, y;
        int size = 1;
        int maxSize = 40;
        Color[] colors = {
            new Color(255, 100, 0),
            new Color(255, 255, 0),
            new Color(255, 0, 0)
        };
        int colorIndex = 0;
        int frame = 0;

        public Explosion(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public boolean update() {
            size += 3;
            frame++;
            if (frame % 3 == 0) {
                colorIndex = (colorIndex + 1) % colors.length;
            }
            return size < maxSize;
        }

        public void draw(Graphics2D g2d) {
            for (int i = 0; i < 3; i++) {
                int layerSize = size - (i * 10);
                int alpha = 255 - (size * 255 / maxSize) - (i * 50);
                if (alpha > 0) {
                    g2d.setColor(new Color(
                        colors[colorIndex].getRed(),
                        colors[colorIndex].getGreen(),
                        colors[colorIndex].getBlue(),
                        alpha
                    ));
                    g2d.fillOval(x - layerSize/2, y - layerSize/2, layerSize, layerSize);
                }
            }

            for (int i = 0; i < 8; i++) {
                double angle = (i * Math.PI / 4) + (frame * 0.2);
                int particleX = x + (int)(Math.cos(angle) * size/2);
                int particleY = y + (int)(Math.sin(angle) * size/2);
                g2d.setColor(new Color(255, 255, 0, 255 - (size * 255 / maxSize)));
                g2d.fillOval(particleX - 3, particleY - 3, 6, 6);
            }
        }
    }

    class Star {
        int x, y;
        int speed;
        int size;

        public Star(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.size = speed;
        }

        public void move() {
            y += speed;
            if (y > 600) {
                y = 0;
                x = random.nextInt(800);
            }
        }

        public void draw(Graphics2D g2d) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x, y, size, size);
        }
    }

    private void initializeStars() {
        for (int i = 0; i < 100; i++) {
            stars.add(new Star(random.nextInt(800), random.nextInt(600), random.nextInt(3) + 1));
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    
    @Override
    public void mouseReleased(MouseEvent e) {}
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if (gameState == MENU || gameState == GAME_OVER) {
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Invaders");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        SpaceInvaders game = new SpaceInvaders();
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        game.requestFocusInWindow();
    }
}
