import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;

public class Chip8Emulator extends JPanel implements KeyListener, ActionListener {
    private final byte[] memory = new byte[4096];
    private final byte[] V = new byte[16];
    private int I, pc;
    private final int[] stack = new int[16];
    private int sp;
    private int delayTimer, soundTimer;
    private final boolean[] keys = new boolean[16];
    private final boolean[] display = new boolean[64 * 32];
    private final Timer timer;
    private boolean waitingForKeyPress = false;
    private int keyRegister = 0;
    private Clip audioClip;

    public Chip8Emulator() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        setPreferredSize(new Dimension(640, 320));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();
        loadBeepWavFile();

        reset();
        loadROMWithDialog();

        timer = new Timer(1000 / 60, this);
        timer.start();
    }

    private void loadBeepWavFile() throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        String currentPath = System.getProperty("user.dir");
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(currentPath + "\\audio" + "\\beep.wav"));
        audioClip = AudioSystem.getClip();
        audioClip.open(audioStream);
    }

    private void playBeepWavFile() {
        if (audioClip != null) {
            audioClip.setFramePosition(0);
            audioClip.start();
        }
    }

    private void loadROMWithDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a CHIP-8 ROM File");

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File rom = chooser.getSelectedFile();
            try {
                byte[] data = Files.readAllBytes(rom.toPath());
                if (data.length + 0x200 > memory.length) {
                    throw new IOException("ROM file is too large");
                }
                System.arraycopy(data, 0, memory, 0x200, data.length);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error loading ROM: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            System.exit(0);
        }
    }

    private void reset() {
        Arrays.fill(memory, (byte) 0);
        Arrays.fill(V, (byte) 0);
        Arrays.fill(stack, 0);
        Arrays.fill(display, false);
        Arrays.fill(keys, false);
        I = pc = 0x200;
        sp = delayTimer = soundTimer = 0;
        waitingForKeyPress = false;
        loadFontset();
    }

    private void loadFontset() {
        byte[] fontset = {
                (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
                (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
                (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
                (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
                (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
                (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
                (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
                (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
                (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
                (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
        };
        System.arraycopy(fontset, 0, memory, 0x50, fontset.length);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                if (display[y * 64 + x]) {
                    g.setColor(Color.WHITE);
                    g.fillRect(x * 10, y * 10, 10, 10);
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(x * 10, y * 10, 10, 10);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!waitingForKeyPress) {
            for (int i = 0; i < 10; i++)
                emulateCycle();
        }
        repaint();

        if (delayTimer > 0)
            delayTimer--;
        if (soundTimer > 0) {
            if (soundTimer == 1)
                playBeepWavFile();
            soundTimer--;
        }
    }

    private void emulateCycle() {
        int opcode = (memory[pc] << 8) | (memory[pc + 1] & 0xFF);
        int x = (opcode >> 8) & 0xF;
        int y = (opcode >> 4) & 0xF;
        int nnn = opcode & 0xFFF;
        int kk = opcode & 0xFF;
        int n = opcode & 0xF;

        pc += 2;

        switch (opcode & 0xF000) {
            case 0x0000:
                if (opcode == 0x00E0) {
                    Arrays.fill(display, false);
                } else if (opcode == 0x00EE) {
                    sp--;
                    pc = stack[sp];
                }
                break;
            case 0x1000:
                pc = nnn;
                break;
            case 0x2000:
                stack[sp++] = pc;
                pc = nnn;
                break;
            case 0x3000:
                if (V[x] == (byte) kk)
                    pc += 2;
                break;
            case 0x4000:
                if (V[x] != (byte) kk)
                    pc += 2;
                break;
            case 0x5000:
                if (V[x] == V[y])
                    pc += 2;
                break;
            case 0x6000:
                V[x] = (byte) kk;
                break;
            case 0x7000:
                V[x] += (byte) kk;
                break;
            case 0x8000:
                switch (n) {
                    case 0x0:
                        V[x] = V[y];
                        break;
                    case 0x1:
                        V[x] |= V[y];
                        break;
                    case 0x2:
                        V[x] &= V[y];
                        break;
                    case 0x3:
                        V[x] ^= V[y];
                        break;
                    case 0x4:
                        int sum = (V[x] & 0xFF) + (V[y] & 0xFF);
                        V[0xF] = (byte) (sum > 255 ? 1 : 0);
                        V[x] = (byte) sum;
                        break;
                    case 0x5:
                        V[0xF] = (byte) ((V[x] & 0xFF) >= (V[y] & 0xFF) ? 1 : 0);
                        V[x] -= V[y];
                        break;
                    case 0x6:
                        V[0xF] = (byte) (V[x] & 1);
                        V[x] = (byte) ((V[x] & 0xFF) >>> 1);
                        break;
                    case 0x7:
                        V[0xF] = (byte) ((V[y] & 0xFF) >= (V[x] & 0xFF) ? 1 : 0);
                        V[x] = (byte) (V[y] - V[x]);
                        break;
                    case 0xE:
                        V[0xF] = (byte) ((V[x] & 0x80) >>> 7);
                        V[x] = (byte) ((V[x] & 0xFF) << 1);
                        break;
                }
                break;
            case 0x9000:
                if (V[x] != V[y])
                    pc += 2;
                break;
            case 0xA000:
                I = nnn;
                break;
            case 0xB000:
                pc = nnn + (V[0] & 0xFF);
                break;
            case 0xC000:
                V[x] = (byte) ((int) (Math.random() * 0xFF) & kk);
                break;
            case 0xD000: {
                V[0xF] = 0;
                for (int row = 0; row < n; row++) {
                    byte sprite = memory[I + row];
                    for (int col = 0; col < 8; col++) {
                        int px = (V[x] + col) & 0x3F;
                        int py = (V[y] + row) & 0x1F;
                        int idx = py * 64 + px;
                        boolean bit = ((sprite >> (7 - col)) & 1) == 1;
                        if (bit && display[idx])
                            V[0xF] = 1;
                        display[idx] ^= bit;
                    }
                }
                break;
            }
            case 0xE000:
                if (kk == 0x9E && keys[V[x] & 0xF])
                    pc += 2;
                else if (kk == 0xA1 && !keys[V[x] & 0xF])
                    pc += 2;
                break;
            case 0xF000:
                switch (kk) {
                    case 0x07:
                        V[x] = (byte) delayTimer;
                        break;
                    case 0x0A:
                        waitingForKeyPress = true;
                        keyRegister = x;
                        pc -= 2;
                        break;
                    case 0x15:
                        delayTimer = V[x] & 0xFF;
                        break;
                    case 0x18:
                        soundTimer = V[x] & 0xFF;
                        break;
                    case 0x1E:
                        I += V[x] & 0xFF;
                        break;
                    case 0x29:
                        I = 0x50 + ((V[x] & 0xF) * 5);
                        break;
                    case 0x33: {
                        int value = V[x] & 0xFF;
                        memory[I] = (byte) (value / 100);
                        memory[I + 1] = (byte) ((value / 10) % 10);
                        memory[I + 2] = (byte) (value % 10);
                        break;
                    }
                    case 0x55:
                        for (int i = 0; i <= x; i++)
                            memory[I + i] = V[i];
                        I += x + 1;
                        break;
                    case 0x65:
                        for (int i = 0; i <= x; i++)
                            V[i] = memory[I + i];
                        I += x + 1;
                        break;
                }
                break;
        }
    }

    private int mapKey(int code) {
        return switch (code) {
            case KeyEvent.VK_X -> 0x0;
            case KeyEvent.VK_1 -> 0x1;
            case KeyEvent.VK_2 -> 0x2;
            case KeyEvent.VK_3 -> 0x3;
            case KeyEvent.VK_Q -> 0x4;
            case KeyEvent.VK_W -> 0x5;
            case KeyEvent.VK_E -> 0x6;
            case KeyEvent.VK_A -> 0x7;
            case KeyEvent.VK_S -> 0x8;
            case KeyEvent.VK_D -> 0x9;
            case KeyEvent.VK_Z -> 0xA;
            case KeyEvent.VK_C -> 0xB;
            case KeyEvent.VK_4 -> 0xC;
            case KeyEvent.VK_R -> 0xD;
            case KeyEvent.VK_F -> 0xE;
            case KeyEvent.VK_V -> 0xF;
            default -> -1;
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = mapKey(e.getKeyCode());
        if (k != -1) {
            keys[k] = true;
            if (waitingForKeyPress) {
                V[keyRegister] = (byte) k;
                waitingForKeyPress = false;
                pc += 2;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = mapKey(e.getKeyCode());
        if (k != -1)
            keys[k] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Java CHIP-8 Emulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Chip8Emulator emulator = null;
            try {
                emulator = new Chip8Emulator();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
                e.printStackTrace();
            }
            frame.setResizable(false);
            frame.setContentPane(emulator);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}