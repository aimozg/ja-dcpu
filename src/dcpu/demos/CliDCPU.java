package dcpu.demos;

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
import jline.SimpleCompletor;
import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Disassembler;
import dcpu.Tracer;

public class CliDCPU {
    
    private static final Pattern commandParser = Pattern.compile("\\s*([^\\s]+)\\s*(.*)"); // capture a word followed by its args 
    
    private static Dcpu dcpu;
    private static Disassembler disassembler;
    private static Tracer tracer;
    
    public enum Cmd {
        QUIT("quit") {
            @Override public void execute(String[] args) {
                System.exit(0);
            }
            @Override public String usage() {
                return formatHelp(name, "exit application.");
            }
        },
        
        LOAD("load") {
            @Override public void execute(String[] args) {
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
                    short[] bin = new Assembler().assemble(new FileReader(in));
                    dcpu.reset();
                    dcpu.upload(bin);
                } catch (Exception e) {
                    System.err.println("Error: Couldn't read file " + in.getAbsolutePath());
                    e.printStackTrace(System.err);
                }
            }
            @Override public String usage() {
                return formatHelp(name + " <infile.asm>", "loads and assembles input file.");
            }
        },
        
        RUN("run") {
            @Override public void execute(String[] args) {
                int steps = getNextArgAsNumber(args);
                dcpu.run(steps);
            }
            @Override public String usage() {
                return formatHelp(name + " [n]", "run n instructions (default = 1). Hitting enter on blank line will run 1 instruction.");
            }
        },
        
        RESET("reset") {
            @Override public void execute(String[] args) {
                dcpu.reset();
            }
            @Override public String usage() {
                return formatHelp(name, "reset dcpu");
            }
        },
        
        MEM("mem") {
            @Override public void execute(String[] args) {
                if ("".equals(args[0])) {
                    System.err.println("Error: no start address specified.\n" + usage());
                    return;
                }
                if (args.length == 1) {
                    System.out.println(dcpu._dmem(numberToInt(args[0])));
                } else {
                    System.err.println("TODO: multi range memory output!");
                }
            }
            @Override public String usage() {
                return formatHelp(name + " <start> [<end>]", "displays memory from start. if end not specified, shows only start");
            }
        },
        
        REG("reg") {
            @Override public void execute(String[] args) {
                Tracer.outputRegisters(System.out, dcpu);
            }

            @Override public String usage() {
                return formatHelp(name, "displays register values");
            }
            
        },

        TOGGLEREG("togglereg") {
            @Override public void execute(String[] args) {
                boolean isPrintRegisters = tracer.togglePrintRegisters();
                System.out.println("register printing is " + (isPrintRegisters ? "on" : "off"));
            }

            @Override public String usage() {
                return formatHelp(name, "toggles automatic register printing on execution");
            }
        },
        
        NEXTINSTRUCTION("next") {
            @Override public void execute(String[] args) {
                int num = getNextArgAsNumber(args);
                for (int i = 0; i < num; i++) {
                    System.out.println(disassembler.next(false));
                }
            }
            @Override public String usage() {
                return formatHelp(name + " [n]", "displays next [n] instructions, default = 1");
            }
        },
        
        HELP("help") {
            @Override public void execute(String[] args) {
                System.out.println();
                for (String cmd : Cmd.getCmds()) {
                    System.out.println(Cmd.l(cmd).usage());
                }
            }
            @Override public String usage() {
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
        private static int getNextArgAsNumber(String[] args) {
            int num = 1;
            if (!"".equals(args[0])) {
                if (Assembler.numPattern.matcher(args[0]).matches()) {
                    num = numberToInt(args[0]);
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
        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);
        
        dcpu = new Dcpu();
        tracer = new Tracer(System.out);
        tracer.install(dcpu);
        disassembler = new Disassembler();
        disassembler.init(dcpu.mem);
        
        List<Completor> completors = new LinkedList<Completor>();
        completors.add(new SimpleCompletor(Cmd.getCmds()));
        completors.add(new FileNameCompletor());
        reader.addCompletor(new ArgumentCompletor(completors));
        // PrintWriter out = new PrintWriter(System.out);

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
