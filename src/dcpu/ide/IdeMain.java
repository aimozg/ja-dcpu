package dcpu.ide;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import dcpu.*;
import dcpu.io.InstreamPeripheral;
import dcpu.io.OutstreamPeripheral;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;

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

    private JFrame frame;

    private Dcpu cpu;
    private AsmMap asmMap;
    private Debugger debugger;
    private short[] binary = {};
    private InstreamPeripheral stdin;
    private OutstreamPeripheral stdout;
    private PipedInputStream stdin_pipe;
    private PipedOutputStream stdout_pipe;

    private RegistersModel registersModel;
    private MemoryModel memoryModel;

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
        debugger.attachTo(cpu);
        asmMap = new AsmMap();
        stdin_pipe = new PipedInputStream();
        try {
            stdout_pipe = new PipedOutputStream(stdin_pipe);
        } catch (IOException e) {
            throw new RuntimeException("Weird things happen here...", e);
        }
        stdin = new InstreamPeripheral(stdin_pipe, 16);
        stdout = new OutstreamPeripheral(stdout_pipe);
        cpu.attach(stdin, 0x7);
        cpu.attach(stdout, 0x8);

        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("."));

        registersModel = new RegistersModel(cpu, debugger);
        registersTable.setModel(registersModel);
        memoryModel = new MemoryModel(cpu, debugger);
        memoryTable.setModel(memoryModel);

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
        consoleTextarea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                throw new UnsupportedOperationException();// TODO write .keyTyped method body
            }
        });
    }

    private void step() {
        debugger.breakpointsHalt = false;
        debugger.step();
        registersModel.fireUpdate();
        memoryModel.fireUpdate(0, RAM_SIZE - 1);//TODO optimize
        debugger.breakpointsHalt = true;
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
                binary = new short[len];
                for (int i = 0; i < len; i++) {
                    int lo = inbinf.read();
                    int hi = inbinf.read();
                    if (lo == -1 || hi == -1) throw new IOException("Unable to read\n");
                    binary[i] = (short) ((hi << 8) | lo);
                }
                asmMap = new AsmMap();
                Disassembler dasm = new Disassembler();
                dasm.init(binary);
                // TODO attach asmmap
                StringBuilder sb = new StringBuilder();
                while (dasm.getAddress() < binary.length) {
                    int addr = dasm.getAddress();
                    sb.append(String.format("%-26s ; [%04x] =", dasm.next(), addr));
                    int addr2 = dasm.getAddress();
                    while (addr < addr2) {
                        short i = binary[addr++];
                        sb.append(String.format(" %04x '%s'", i, (i >= 0x20 && i < 0x7f) ? (char) i : '.'));
                    }
                    sb.append("\n");
                }
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
                for (short i : binary) {
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
                sourceTextarea.setText(new String(csources));
                asmMap = new AsmMap();
                binary = new short[0];
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(frame, "Unable to open file", "Error", JOptionPane.ERROR_MESSAGE);
                e1.printStackTrace();
            }
        }
    }

    private void assemble() {
        Assembler assembler = new Assembler();
        assembler.genMap = true;
        try {
            binary = new short[]{};
            binary = assembler.assemble(sourceTextarea.getText());
            cpu.upload(binary);
            memoryModel.fireUpdate(0, binary.length);
            asmMap = assembler.asmmap;
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
        execButton.setEnabled(false);
        execButton.setText("Exec");
        execButton.setToolTipText("Run forever");
        toolBar1.add(execButton);
        pauseButton = new JButton();
        pauseButton.setEnabled(false);
        pauseButton.setText("Pause");
        pauseButton.setToolTipText("Pause execution");
        toolBar1.add(pauseButton);
        runButton = new JButton();
        runButton.setEnabled(false);
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
        breakpointButton.setEnabled(false);
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
        final JScrollPane scrollPane2 = new JScrollPane();
        panel1.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(500, 400), null, 0, false));
        sourceTextarea = new JTextArea();
        sourceTextarea.setFont(new Font("Courier New", sourceTextarea.getFont().getStyle(), 12));
        sourceTextarea.setText("; Input your proram here \n:main\n  \tDAT,0");
        scrollPane2.setViewportView(sourceTextarea);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        consoleTextarea = new JTextArea();
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
