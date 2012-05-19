package dcpu.apps;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.FileNameCompletor;
import jline.History;
import jline.SimpleCompletor;
import dcpu.AntlrAssembler;
import dcpu.Dcpu;
import dcpu.Dcpu.Reg;
import dcpu.Disassembler;
import dcpu.Tracer;
import dcpu.hw.GenericKeyboard;
import dcpu.hw.MonitorLEM1802;
import dcpu.hw.MonitorWindow;

public class CliDCPU {

    private static final Pattern hexPattern = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern binPattern = Pattern.compile("0b\\d+");
    private static final Pattern decPattern = Pattern.compile("\\d+");
    private static final Pattern numPattern = Pattern.compile("(" + hexPattern.pattern() + ")|(" + binPattern.pattern() + ")|(" + decPattern.pattern() + ")");

    private static final Pattern commandParser = Pattern.compile("\\s*([^\\s]+)\\s*(.*)"); // capture a word followed by its args 

    // is there a better way to do this? All static else the ENUMs can't access them.
    private static AntlrAssembler assembler;
    private static Dcpu dcpu;
    private static Disassembler disassembler;
    private static Tracer tracer;
    private static boolean showingDisplay = false;
    private static MonitorWindow monitorWindow;
    private static MonitorLEM1802 monitorDevice;
    private static GenericKeyboard keyboardDevice;

    public enum Cmd {
        QUIT("quit") {
            @Override
            public void execute(String[] args) {
                System.exit(0);
            }

            @Override
            public String usage() {
                return formatHelp(name, "exit application.");
            }
        },

        LOAD("load") {
            @Override
            public void execute(String[] args) {
                if ("".equals(args[0]) || args.length > 1) {
                    System.err.println(usage());
                    return;
                }
                File in = new File(args[0]);
                if (!in.canRead()) {
                    System.err.println("Could not find file: >" + in.getAbsolutePath() + "<");
                    return;
                }
                try {
                    assembler = new AntlrAssembler();
                    char[] bin = assembler.assemble(new FileReader(in));
                    dcpu.reset();
                    dcpu.memzero();
                    dcpu.upload(bin);
                } catch (Exception e) {
                    System.err.println("Error: Couldn't read file " + in.getAbsolutePath());
                    e.printStackTrace(System.err);
                }
            }

            @Override
            public String usage() {
                return formatHelp(name + " <infile.asm>", "loads and assembles input file.");
            }
        },

        RUN("run") {
            @Override
            public void execute(String[] args) {
                int steps;
                try {
                    steps = getNextArgAsNumber(args[0]);
                } catch (NumberFormatException e) {
                    System.err.printf("Error: %s is not valid format for a number.\n", args[1]);
                    return;
                }
                dcpu.run(steps);
            }

            @Override
            public String usage() {
                return formatHelp(name + " [n]", "run n instructions (default = 1). Hitting enter on blank line will run 1 instruction.");
            }
        },

        RESET("reset") {
            @Override
            public void execute(String[] args) {
                dcpu.reset();
                dcpu.memzero();
            }

            @Override
            public String usage() {
                return formatHelp(name, "reset dcpu");
            }
        },

        MEM("mem") {
            @Override
            public void execute(String[] args) {
                int start;
                int end;
                int numWordsPerLine = 16;

                if ("".equals(args[0]) || args.length > 3) {
                    System.err.println("Error: bad args.\n" + usage());
                    return;
                }
                try {
                    start = getAddress(args[0]);
                } catch (NumberFormatException e) {
                    System.err.printf("Error: %s is not valid format for a number.\n", args[0]);
                    return;
                }
                end = start;
                if (args.length > 1) {
                    try {
                        end = getAddress(args[1]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Error: %s is not valid format for a number.\n", args[1]);
                        return;
                    }
                }
                if (args.length > 2) {
                    try {
                        numWordsPerLine = numberToInt(args[2]);
                    } catch (NumberFormatException e) {
                        System.err.printf("Error: %s is not valid format for a number.\n", args[2]);
                        return;
                    }
                }
                // start printing
                int range = end - start + 1;
                int numLines = range / numWordsPerLine;
                int numLeftOver = range - numLines * numWordsPerLine;
                for (int i = 0; i < numLines; i++) {
                    System.out.printf("(%04x) :", start + i * numWordsPerLine);
                    for (int j = 0; j < numWordsPerLine; j++) {
                        System.out.printf(" %04x", (int) dcpu.mem[start + i * numWordsPerLine + j]);
                    }
                    System.out.print(" | ");
                    for (int j = 0; j < numWordsPerLine; j++) {
                        printShortChars(start + i * numWordsPerLine + j);
                    }
                    System.out.println();
                }

                if (numLeftOver > 0) {
                    System.out.printf("(%04x) :", start + numLines * numWordsPerLine);
                    for (int j = 0; j < numLeftOver; j++) {
                        System.out.printf(" %04x", (int) dcpu.mem[start + numLines * numWordsPerLine + j]);
                    }
                    for (int i = 0; i < (numWordsPerLine - numLeftOver); i++) {
                        System.out.print("     "); // spacer to make chars line up 
                    }
                    System.out.print(" | ");
                    for (int j = 0; j < numLeftOver; j++) {
                        printShortChars(start + numLines * numWordsPerLine + j);
                    }
                }
                System.out.println();

            }

            private void printShortChars(int addr) {
                char s = dcpu.mem[addr];
                char c1 = (char) ((s & 0xff00) >> 8);
                char c2 = (char) (s & 0x00ff);
                if (c1 < 0x20 || c1 > 0x7e) c1 = '.';
                if (c2 < 0x20 || c2 > 0x7e) c2 = '.';
                System.out.printf(" %c%c", c1, c2);
            }

            @Override
            public String usage() {
                return formatHelp(name + " <start> [end [wordsPerLine]]", "displays memory and ascii. if end isn't specified, shows only start word. default 16 words per line");
            }
        },

        REG("reg") {
            @Override
            public void execute(String[] args) {
                Tracer.outputRegisters(System.out, dcpu);
            }

            @Override
            public String usage() {
                return formatHelp(name, "displays register values");
            }

        },

        TOGGLEREG("togglereg") {
            @Override
            public void execute(String[] args) {
                boolean isPrintRegisters = tracer.togglePrintRegisters();
                System.out.println("register printing is " + (isPrintRegisters ? "on" : "off"));
            }

            @Override
            public String usage() {
                return formatHelp(name, "toggles automatic register printing on execution, currently " + (tracer.getPrintRegisters() ? "on" : "off"));
            }
        },

        NEXTINSTRUCTION("next") {
            @Override
            public void execute(String[] args) {
                int num;
                try {
                    num = getNextArgAsNumber(args[0]);
                } catch (NumberFormatException e) {
                    System.err.printf("Error: %s is not valid format for a number.\n", args[0]);
                    return;
                }
                disassembler.setAddress(dcpu.pc());
                for (int i = 0; i < num; i++) {
                    System.out.println(disassembler.next(true));
                }
            }

            @Override
            public String usage() {
                return formatHelp(name + " [n]", "displays next [n] instructions, default = 1");
            }
        },

        DISPLAY("display") {
            @Override
            public void execute(String[] args) {
                if (!showingDisplay) {
                    keyboardDevice = new GenericKeyboard(MonitorLEM1802.MANUFACTURER_ID, 16);
                    monitorDevice = new MonitorLEM1802();
                    monitorWindow = new MonitorWindow(dcpu, monitorDevice, false);
                    monitorWindow.addKeyListener(keyboardDevice);
                    dcpu.attach(keyboardDevice);
                    dcpu.attach(monitorDevice);
                    monitorWindow.getFrame().addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            showingDisplay = false;
                            monitorWindow.close();
                        }
                    });
                    monitorWindow.show();
                    showingDisplay = true;
                } else {
                    monitorWindow.close();
                    dcpu.detach(keyboardDevice);
                    dcpu.detach(monitorDevice);
                    monitorWindow = null;
                    showingDisplay = false;
                }
            }

            @Override
            public String usage() {
                return formatHelp(name, "toggle VDU display");
            }
        },

        HELP("help") {
            @Override
            public void execute(String[] args) {
                System.out.println();
                for (String cmd : Cmd.getCmds()) {
                    System.out.println(Cmd.l(cmd).usage());
                }
            }

            @Override
            public String usage() {
                return formatHelp("help", "display supported commands.");
            }
        };

        private static final Map<String, Cmd> LOOKUP = new TreeMap<String, Cmd>();
        public final String name;

        static {
            for (Cmd c : EnumSet.allOf(Cmd.class)) {
                LOOKUP.put(c.name, c);
            }
        }

        public static Cmd l(String name) {
            return LOOKUP.get(name);
        }

        public static String[] getCmds() {
            return LOOKUP.keySet().toArray(new String[0]);
        }

        private static String formatHelp(String cmdWithArgs, String helpText) {
            return String.format("%-20s ; %s", cmdWithArgs, helpText);
        }

        private static int getNextArgAsNumber(String numToTest) {
            int num = 1;
            if (!"".equals(numToTest)) {
                if (numPattern.matcher(numToTest).matches()) {
                    num = numberToInt(numToTest);
                }
            }
            return num;
        }

        private static int numberToInt(String input) {
            int val;
            if (input.startsWith("0x")) val = Integer.parseInt(input.substring(2), 16);
            else if (input.startsWith("0b")) val = Integer.parseInt(input.substring(2, 2));
            else val = Integer.parseInt(input);
            return val;
        }

        private static int getAddress(String addressString) {
            addressString = addressString.toLowerCase();
            int returnAddress;
            if ("pc".equals(addressString)) {
                returnAddress = dcpu.pc();
            } else if ("start".equals(addressString)) {
                returnAddress = 0;
            } else if ("end".equals(addressString)) {
                returnAddress = 0xffff;
            } else if ("a".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.A);
            } else if ("b".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.B);
            } else if ("c".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.C);
            } else if ("x".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.X);
            } else if ("y".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.Y);
            } else if ("z".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.Z);
            } else if ("i".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.I);
            } else if ("j".equals(addressString)) {
                returnAddress = dcpu.getreg(Reg.J);
            } else if ("sp".equals(addressString)) {
                returnAddress = dcpu.sp();
            } else {
                returnAddress = numberToInt(addressString);
            }
            return returnAddress;
        }

        Cmd(String name) {
            this.name = name;
        }

        public abstract void execute(String[] args);

        public abstract String usage();
    }

    public static void main(String[] args) throws IOException {
        new CliDCPU().startCli();
    }

    public void startCli() throws IOException {
        System.out.println("CliDCPU by fraoch using ja-dcpu by aimozg.\nPress tab for command list. Can do command and file completion.\nType 'help' for commands supported.\n");
        File historyFile = new File(System.getProperty("user.home"), ".clidcpu_history");
        History history = new History(historyFile);
        history.setMaxSize(0x4000);
        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        reader.setHistory(history);
        reader.setUseHistory(true);

        dcpu = new Dcpu();
        tracer = new Tracer(System.out);
        tracer.install(dcpu);
        disassembler = new Disassembler();
        disassembler.init(dcpu.mem);
        assembler = new AntlrAssembler();

        List<Completor> completors = new LinkedList<Completor>();
        completors.add(new SimpleCompletor(Cmd.getCmds()));
        completors.add(new FileNameCompletor());
        reader.addCompletor(new ArgumentCompletor(completors));

        String line;
        while ((line = reader.readLine("# ")) != null) {
            if (line.equals("")) line = "run 1";
            Matcher m = commandParser.matcher(line);
            if (m.matches()) {
                String command = m.group(1);
                String cmdargs = m.group(2);
                Cmd cmd = Cmd.l(command);
                if (cmd != null) {
                    cmd.execute(cmdargs.split(" "));
                } else {
                    System.err.println("Unrecognised input: " + line);
                }
            }
        }
    }

}
