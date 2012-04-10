package dcpu.ide;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import dcpu.AsmMap;
import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Debugger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

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

    private JFrame frame;

    private Dcpu cpu;
    private AsmMap asmMap;
    private Debugger debugger;
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
            short[] bin = assembler.assemble(sourceTextarea.getText());
            cpu.upload(bin);
            memoryModel.fireUpdate(0, bin.length);
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
        openBinButton.setEnabled(false);
        openBinButton.setText("Open Bin");
        openBinButton.setToolTipText("Open and disassemble binaries");
        toolBar1.add(openBinButton);
        saveSrcButton = new JButton();
        saveSrcButton.setEnabled(true);
        saveSrcButton.setText("Save Src");
        saveSrcButton.setToolTipText("Save sources");
        toolBar1.add(saveSrcButton);
        saveBinButton = new JButton();
        saveBinButton.setEnabled(false);
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
        resetButton = new JButton();
        resetButton.setEnabled(false);
        resetButton.setText("Reset");
        resetButton.setToolTipText("Reset CPU");
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
        stepButton.setEnabled(false);
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
        panel1.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(400, 400), null, 0, false));
        sourceTextarea = new JTextArea();
        sourceTextarea.setFont(new Font("Courier New", sourceTextarea.getFont().getStyle(), 12));
        sourceTextarea.setText("; Input your proram here\n:main \n\tSET DAT,0");
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
