import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HexFormat;

public class Chip8Debugger extends JFrame {
    private final Chip8Emulator emulator;
    private final JTextArea memoryDisplay;
    private final JTable registerTable;
    private final JTextArea currentInstructionDisplay;
    private final JTable stackTable;
    private final JLabel pcLabel;
    private final Timer debuggerTimer;

    public Chip8Debugger(Chip8Emulator emulator) {
        this.emulator = emulator;
        setTitle("CHIP-8 Debugger Window");
        setSize(1300, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        pcLabel = new JLabel("PC: 0x000");
        currentInstructionDisplay = new JTextArea("Current Instruction: ");
        currentInstructionDisplay.setEditable(false);
        currentInstructionDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));

        topPanel.add(pcLabel);
        topPanel.add(new JScrollPane(currentInstructionDisplay));

        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3));

        memoryDisplay = new JTextArea();
        memoryDisplay.setEditable(false);
        memoryDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane memoryScrollPane = new JScrollPane(memoryDisplay);
        memoryScrollPane.setBorder(BorderFactory.createTitledBorder("Memory"));
        centerPanel.add(memoryScrollPane);

        String[] registerColumns = { "Register", "Value" };
        DefaultTableModel registerModel = new DefaultTableModel(registerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        registerTable = new JTable(registerModel);
        registerTable.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane registerScrollPane = new JScrollPane(registerTable);
        registerScrollPane.setBorder(BorderFactory.createTitledBorder("Registers (V0-VF)"));
        centerPanel.add(registerScrollPane);

        String[] stackColumns = {"Index", "Address"};
        DefaultTableModel stackModel = new DefaultTableModel(stackColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        stackTable = new JTable(stackModel);
        stackTable.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane stackScrollPane = new JScrollPane(stackTable);
        stackScrollPane.setBorder(BorderFactory.createTitledBorder("Stack"));
        centerPanel.add(stackScrollPane);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(false);

        debuggerTimer = new Timer(0, _ -> updateDebugger());
        debuggerTimer.start();
    }

    public void updateDebugger() {
        if (emulator.isPaused) {
            return;
        }
        byte[] memory = this.emulator.memory;
        int pc = this.emulator.pc;
        StringBuilder sb = new StringBuilder();
        HexFormat hex = HexFormat.of().withPrefix("0x");

        for (int i = 0; i <= memory.length - 1; i += 16) {
            sb.append("0x" + (hex.formatHex(new byte[] {(byte) (i >> 8), (byte) i})).toString().replace("0x", "")).append(": ");
            for (int j = 0; j < 16; j++) {
                if (i + j < memory.length) {
                    int val = memory[i + j] & 0xFF;
                    sb.append(String.format("%02X", val)).append(" ");
                } else {
                    sb.append("   ");
                }
            }
            sb.append("\n");
        }
        memoryDisplay.setText(sb.toString());
        memoryDisplay.setCaretPosition(0);

        byte[] V = this.emulator.V;
        DefaultTableModel registerModel = (DefaultTableModel) registerTable.getModel();
        registerModel.setRowCount(0);

        for (int i = 0; i < V.length; i++) {
            registerModel.addRow(new Object[] {"V" + Integer.toHexString(i).toUpperCase(),hex.formatHex(new byte[] {V[i]})});
        }

        int I = this.emulator.I;
        registerModel.addRow(new Object[] {"I", hex.formatHex(new byte[] {(byte) I})});

        pcLabel.setText("PC: " + hex.formatHex(new byte[] { (byte) (pc & 0xFFF) }));

        int opcode = 0;
        try {
            opcode = (memory[pc] << 8) | (memory[pc + 1] & 0xFFF);
        } catch (Exception e) {
            return;
        }

        currentInstructionDisplay.setText("Current Instruction: " + String.format("0x%X", opcode  & 0xFFFF));

        int[] stack = this.emulator.stack;
        int sp = this.emulator.sp;
        DefaultTableModel stackModel = (DefaultTableModel) stackTable.getModel();
        stackModel.setRowCount(0);
        for (int i = 0; i < sp; i++) {
            stackModel.addRow(new Object[] {i, hex.formatHex(new byte[] {(byte) stack[i]})});
        }
    }
}
