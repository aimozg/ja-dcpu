package dcpu.ide;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import computer.AWTKeyMapping;
import computer.VirtualKeyboard;
import computer.VirtualMonitor;
import dcpu.*;
import dcpu.io.PanelPeripheral;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

import static dcpu.Dcpu.RAM_SIZE;

/**
 * Created by IntelliJ IDEA.
 * User: aimozg
 * Date: 08.04.12
 * Time: 12:45
 */
public class IdeMain {
    private JTextArea sourceTextarea;
    private JTable registersTable;
    private JTable memoryTable;
    private JTextArea consoleTextarea;
    private JButton openSrcButton;
    private JButton saveSrcButton;
    private JButton asmButton;
    private JButton execButton;
    private JButton runButton;
    private JButton resetButton;
    private JButton stepButton;
    private JButton breakpointButton;
    private JButton saveBinButton;
    private JButton openBinButton;
    private JButton pauseButton;
    private JButton clearButton;
    private JPanel rootPanel;
    private JScrollPane memoryScrollPane;
    private JButton hardResetButton;
    private JScrollPane sourceScrollPane;

    private JFrame frame;

    private Dcpu cpu;
    private AsmMap asmMap;
    private Debugger debugger;
    private char[] binary = {};
    private Set<Integer> srcBreakpoints = new HashSet<Integer>(); // Line starts from 1
    private Thread cpuThread;

    private PanelPeripheral panelPeripheral;
    private VirtualKeyboard virtualKeyboard;
    private VirtualMonitor virtualMonitor;

    private RegistersModel registersModel;
    private MemoryModel memoryModel;

    private SourceRowHeader sourceRowHeader;

    private JFileChooser fileChooser;
    private FileNameExtensionFilter asmFilter = new FileNameExtensionFilter("Assembler source (.asm|.dasm)", "asm", "dasm");
    private FileNameExtensionFilter binFilter = new FileNameExtensionFilter("Binary file (.bin)", "bin");

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new IdeMain();
    }

    public IdeMain() {
        cpu = new Dcpu();
        debugger = new Debugger();
        debugger.breakpointListener = new PreListener<Character>() {
            public void preExecute(Character arg) {
                breakpointHit(arg);
            }
        };
        debugger.attachTo(cpu);
        asmMap = new AsmMap();

        VirtualMonitor display = new VirtualMonitor(cpu.mem, 0x8000);
        VirtualKeyboard keyboard = new VirtualKeyboard(cpu.mem, 0x9000, new AWTKeyMapping());
        PanelPeripheral panelPeripheral = new PanelPeripheral(display, keyboard);
        // TODO PanelPeripheral's frame kills whole app on window close. Should do nothing instead. Something like pP.getFrame().setDCO(code_for_do_nothing).
        cpu.attach(panelPeripheral, -1); // don't care about the line, just want it to render the screen from cpu memory


        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));

        registersModel = new RegistersModel(cpu, debugger);
        registersTable.setModel(registersModel);
        memoryModel = new MemoryModel(cpu, debugger);
        memoryTable.setModel(memoryModel);

        sourceRowHeader = new SourceRowHeader(sourceTextarea, srcBreakpoints);
        sourceRowHeader.setBackground(Color.LIGHT_GRAY);
        sourceScrollPane.setRowHeaderView(sourceRowHeader);

        frame = new JFrame("JA-DCPU IDE");
        frame.setContentPane(rootPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        openSrcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openSrc();
            }
        });
        asmButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                assemble();
            }
        });
        saveSrcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveSrc();
            }
        });
        hardResetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cpu.memzero();
                memoryModel.fireUpdate(0, RAM_SIZE);
                cpu.reset();
                registersModel.fireUpdate();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cpu.reset();
                registersModel.fireUpdate();
            }
        });
        saveBinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveBin();
            }
        });
        openBinButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openBin();
            }
        });
        stepButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                step();
            }
        });
        consoleTextarea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                virtualKeyboard.keyTyped(e.getKeyChar());
            }

            @Override
            public void keyPressed(KeyEvent e) {
                virtualKeyboard.keyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                virtualKeyboard.keyReleased(e.getKeyCode());
            }

        });
        breakpointButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                toggleBreakpoint();
            }
        });
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                debugger.breakpointsHalt = true;
                runCpu();
            }
        });
        execButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                debugger.breakpointsHalt = false;
                runCpu();
            }
        });
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cpu.halt = true;
            }
        });
    }

    private void breakpointHit(char pc) {
        registersModel.fireUpdate();
        memoryModel.fireUpdate(0, RAM_SIZE - 1);//TODO optimize
        Integer srcline = asmMap.bin2src(pc);
        if (srcline != null) {
            try {
                sourceTextarea.requestFocus();
                sourceTextarea.setCaretPosition(sourceTextarea.getLineStartOffset(srcline - 1));
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private void runCpu() {
        if (cpuThread != null && cpuThread.isAlive()) return;
        runButton.setEnabled(false);
        execButton.setEnabled(false);
        stepButton.setEnabled(false);
        pauseButton.setEnabled(true);
        cpuThread = new Thread(new Runnable() {
            public void run() {
                debugger.run();
                // on halt
                runButton.setEnabled(true);
                execButton.setEnabled(true);
                stepButton.setEnabled(true);
                pauseButton.setEnabled(false);
                registersModel.fireUpdate();
                memoryModel.fireUpdate(0, RAM_SIZE - 1);//TODO optimize
            }
        }, "CpuThread");
        cpuThread.setDaemon(true);
        cpuThread.start();
    }

    private void toggleBreakpoint() {
        try {
            int lineno = sourceTextarea.getLineOfOffset(sourceTextarea.getCaretPosition()) + 1;
            Character asmaddr = asmMap.src2bin(lineno);
            if (srcBreakpoints.contains(lineno)) {
                srcBreakpoints.remove(lineno);
                if (asmaddr != null) debugger.setBreakpoint(asmaddr, false);
            } else {
                srcBreakpoints.add(lineno);
                if (asmaddr != null) debugger.setBreakpoint(asmaddr, true);
            }

            sourceRowHeader.breakpointChanged(lineno);
        } catch (BadLocationException e1) {
            e1.printStackTrace();
        }
    }

    private void step() {
        debugger.breakpointsHalt = false;
        debugger.step();
        registersModel.fireUpdate();
        memoryModel.fireUpdate(0, RAM_SIZE - 1);//TODO optimize
    }

    private void openBin() {
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(binFilter);
        fileChooser.setFileFilter(binFilter);
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                FileInputStream inbinf = new FileInputStream(fileChooser.getSelectedFile());
                int len = inbinf.available();
                if (len % 2 == 1) throw new IOException(String.format("Odd file size (0x%x)\n", len));
                len /= 2;
                if (len > 0x10000) throw new IOException(String.format("Too large file (0x%x)\n", len));
                binary = new char[len];
                for (int i = 0; i < len; i++) {
                    int lo = inbinf.read();
                    int hi = inbinf.read();
                    if (lo == -1 || hi == -1) throw new IOException("Unable to read\n");
                    binary[i] = (char) ((hi << 8) | lo);
                }
                asmMap = new AsmMap();
                Disassembler dasm = new Disassembler();
                dasm.init(binary);
                // TODO attach asmmap
                StringBuilder sb = new StringBuilder();
                while (dasm.getAddress() < binary.length) {
                    int addr = dasm.getAddress();
                    sb.append(String.format("%-26s ; [%04x] =", dasm.next(true), addr));
                    int addr2 = dasm.getAddress();
                    while (addr < addr2) {
                        char i = binary[addr++];
                        sb.append(String.format(" %04x '%s'", (int) i, (i >= 0x20 && i < 0x7f) ? (char) i : '.'));
                    }
                    sb.append("\n");
                }
                srcBreakpoints.clear();
                sourceRowHeader.breakpointsChanged();
                sourceTextarea.setText(sb.toString());
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(frame, "Unable to open file: %s" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        }
    }

    private void saveBin() {
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(binFilter);
        fileChooser.setFileFilter(binFilter);
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (fileChooser.getFileFilter() == binFilter && !binFilter.accept(file)) {
                    file = new File(file.getAbsolutePath() + binFilter.getExtensions()[0]);
                }
                if (file.exists()) {
                    if (JOptionPane.showConfirmDialog(frame, "File exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION)
                            != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                FileOutputStream output = new FileOutputStream(file);
                for (char i : binary) {
                    output.write(i & 0xff);
                    output.write((i >> 8) & 0xff);
                }
                output.close();
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(frame, "Unable to open file", "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        }
    }

    private void saveSrc() {
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(asmFilter);
        fileChooser.setFileFilter(asmFilter);
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (fileChooser.getFileFilter() == asmFilter && !asmFilter.accept(file)) {
                    file = new File(file.getAbsolutePath() + asmFilter.getExtensions()[0]);
                }
                if (file.exists()) {
                    if (JOptionPane.showConfirmDialog(frame, "File exists. Overwrite?", "Confirm", JOptionPane.YES_NO_OPTION)
                            != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                PrintStream output = new PrintStream(file);
                output.print(sourceTextarea.getText());
                output.close();
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(frame, "Unable to open file", "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        }
    }

    private void openSrc() {
        fileChooser.resetChoosableFileFilters();
        fileChooser.addChoosableFileFilter(asmFilter);
        fileChooser.setFileFilter(asmFilter);
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try {
                FileInputStream input = new FileInputStream(fileChooser.getSelectedFile());
                char[] csources = new char[input.available()];
                new InputStreamReader(input).read(csources, 0, csources.length);
                srcBreakpoints.clear();
                sourceRowHeader.breakpointsChanged();
                sourceTextarea.setText(new String(csources));
                asmMap = new AsmMap();
                binary = new char[0];
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(frame, "Unable to open file", "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        }
    }

    private void assemble() {
        AntlrAssembler assembler = new AntlrAssembler();
        assembler.setGenerateMap(true);
        try {
            binary = new char[]{};
            binary = assembler.assemble(sourceTextarea.getText());
            cpu.upload(binary);
            memoryModel.fireUpdate(0, binary.length);
            asmMap = assembler.getAsmMap();
            for (Character addr : debugger.getBreakpoints()) {
                debugger.setBreakpoint(addr, false);
            }
            for (Integer breakpoint : srcBreakpoints) {
                Character addr = asmMap.src2bin(breakpoint);
                if (addr != null) {// TODO if null, mark breakpoint somehow
                    debugger.setBreakpoint(addr, true);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Compilation error " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JToolBar toolBar1 = new JToolBar();
        rootPanel.add(toolBar1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 20), null, 0, false));
        openSrcButton = new JButton();
        openSrcButton.setText("Open Src");
        openSrcButton.setToolTipText("Open source file");
        toolBar1.add(openSrcButton);
        openBinButton = new JButton();
        openBinButton.setEnabled(true);
        openBinButton.setText("Open Bin");
        openBinButton.setToolTipText("Open and disassemble binaries");
        toolBar1.add(openBinButton);
        saveSrcButton = new JButton();
        saveSrcButton.setEnabled(true);
        saveSrcButton.setText("Save Src");
        saveSrcButton.setToolTipText("Save sources");
        toolBar1.add(saveSrcButton);
        saveBinButton = new JButton();
        saveBinButton.setEnabled(true);
        saveBinButton.setText("Save Bin");
        saveBinButton.setToolTipText("Save assembled binary");
        toolBar1.add(saveBinButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator1);
        asmButton = new JButton();
        asmButton.setText("Asm");
        asmButton.setToolTipText("Assemble sources");
        toolBar1.add(asmButton);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator2);
        hardResetButton = new JButton();
        hardResetButton.setEnabled(true);
        hardResetButton.setText("Hard Reset");
        hardResetButton.setToolTipText("Hard Reset - zeroize memory and reupload binary");
        toolBar1.add(hardResetButton);
        resetButton = new JButton();
        resetButton.setEnabled(true);
        resetButton.setText("Reset");
        resetButton.setToolTipText("Reset CPU (registers to zero)");
        toolBar1.add(resetButton);
        execButton = new JButton();
        execButton.setEnabled(true);
        execButton.setText("Exec");
        execButton.setToolTipText("Run forever");
        toolBar1.add(execButton);
        pauseButton = new JButton();
        pauseButton.setEnabled(false);
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause execution");
        toolBar1.add(pauseButton);
        runButton = new JButton();
        runButton.setEnabled(true);
        runButton.setText("Run");
        runButton.setToolTipText("Run until breakpoint/reserved");
        toolBar1.add(runButton);
        stepButton = new JButton();
        stepButton.setEnabled(true);
        stepButton.setText("Step");
        stepButton.setToolTipText("Execute one instruction");
        toolBar1.add(stepButton);
        final JToolBar.Separator toolBar$Separator3 = new JToolBar.Separator();
        toolBar1.add(toolBar$Separator3);
        breakpointButton = new JButton();
        breakpointButton.setEnabled(true);
        breakpointButton.setText("Breakpoint");
        breakpointButton.setToolTipText("Toggle breakpoint on instruction address");
        toolBar1.add(breakpointButton);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Source");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Memory");
        panel1.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Registers");
        panel1.add(label3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memoryScrollPane = new JScrollPane();
        panel1.add(memoryScrollPane, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(600, -1), null, 0, false));
        memoryTable = new JTable();
        memoryScrollPane.setViewportView(memoryTable);
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(100, -1), null, 0, false));
        registersTable = new JTable();
        scrollPane1.setViewportView(registersTable);
        sourceScrollPane = new JScrollPane();
        panel1.add(sourceScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 400), null, 0, false));
        sourceTextarea = new JTextArea();
        sourceTextarea.setFont(new Font("Courier New", sourceTextarea.getFont().getStyle(), 12));
        sourceTextarea.setText("; Input your program here\n            set a, 1\n            add a, 1\n            ife a, 2\n                set a, 3\n:mainloop\n            ife [message + I], 0\n                set pc, end\n            set a, [message + I]\n            add a, 0xA100\n            set [0x8000 + I], a\n            add i, 1\n            set pc, mainloop\n:message    dat \"Hello, world!\", 0\n:end        set pc, end");
        sourceScrollPane.setViewportView(sourceTextarea);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setVisible(false);
        rootPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        consoleTextarea = new JTextArea();
        consoleTextarea.setEditable(true);
        panel2.add(consoleTextarea, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 200), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Console");
        panel2.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearButton = new JButton();
        clearButton.setText("Clear");
        panel2.add(clearButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
