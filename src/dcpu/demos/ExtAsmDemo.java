package dcpu.demos;

import dcpu.AntlrAssembler;
import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Disassembler;
import dcpu.Tracer;
import dcpu.hw.GenericClock;
import dcpu.hw.GenericKeyboard;
import dcpu.hw.MonitorLEM1802;
import dcpu.hw.MonitorWindow;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assembles and executes external asm file
 */
public class ExtAsmDemo {
    public static void main(String[] args) {
        String srcin = null;
        String binin = null;
        String binout = null;
        boolean exec = true;
        String srcout = null;
        String mapout = null;
        boolean trace = false;
        boolean traceregs = false;
        boolean tracemem = false;
        boolean tracestack = false;
        boolean hw_lem1802 = true;
        boolean hw_kbd = true;
        boolean hw_clk = true;
        int ai = 0;
        while (ai < args.length) {
            String arg = args[ai++];
            if (arg.startsWith("-")) {
                if (arg.equals("-O")) {
                    if (ai == args.length) fail("Missing argument");
                    binout = args[ai++];
                } else if (arg.equals("-I")) {
                    if (ai == args.length) fail("Missing argument");
                    binin = args[ai++];
                } else if (arg.equals("-D")) {
                    if (ai == args.length) fail("Missing argument");
                    srcout = args[ai++];
                } else if (arg.equals("-x")) {
                    exec = false;
                } else if (arg.equals("-M")) {
                    if (ai == args.length) fail("Missing argument");
                    mapout = args[ai++];
                } else if (arg.startsWith("-T")) {
                    trace = true;
                    for (char c : arg.substring(2).toCharArray()) {
                        switch (c) {
                            case 'r':
                                traceregs = true;
                                break;
                            case 'm':
                                tracemem = true;
                                break;
                            case 's':
                                tracestack = true;
                                break;
                            default:
                                fail("Unknown trace param " + c);
                                break;
                        }
                    }
                } else if (arg.startsWith("-hw:")) {
                    Matcher matcher = Pattern.compile("^-hw:(\\p{Alnum}+)=(y|n)$").matcher(arg);
                    if (!matcher.matches()) fail("Bad hardware key");
                    String hw = matcher.group(1);
                    boolean yes = matcher.group(2).equals("y");
                    if (hw.equals("lem1802")) {
                        hw_lem1802 = yes;
                    } else if (hw.equals("kbd")) {
                        hw_kbd = yes;
                    } else if (hw.equals("clk")) {
                        hw_clk = yes;
                    } else fail("Bad hardware %s", hw);
                } else {
                    // TODO options: run some cycles
                    fail("Unrecognized option `%s` . Aborting\n", arg);
                }
            } else {
                if (srcin != null) {
                    fail("Multiple filenames (%s and %s). Aborting\n", srcin, args);
                }
                srcin = arg;
            }
        }
        if (srcin == null && binin == null) {
            fail("DCPU-16 Assembler and Emulator demo.\n" +
                    "Usage:\n" +
                    "\tjava -jar ja-dcpu-demo.jar [OPTIONS] [SRCIN]\n" +
                    "Will assemble and execute specified source file\n" +
                    "OPTIONS:\n" +
                    "\t-O BINOUT        save compiled binary to file BINOUT\n" +
                    "\t-I BININ         load binary image and exec this instead of source\n" +
                    "\t-D SRCOUT        disassemble binary image and save to file SRCOUT\n" +
                    "\t-x               do not execute code, just disassemble/save\n" +
                    "\t-T[TRACEOPTS]    trace executed instructions to stderr\n" +
                    "\t\tTRACEOPTS might include (no separators):\n" +
                    "\t\tr              print registers value\n" +
                    "\t\tm              print memory at registers addresses\n" +
                    "\t\ts              print stack (8 words)\n" +
                    "\t-M MAPOUT        print compilation map to file (requires SOURCE)\n" +
                    "\t-hw:DEVICE=y/n   enable (y) or disable (n) hardware DEVICE\n" +
                    "\n" +
                    "Devices:\n" +
                    "\tlem1802          LEM1802 Monitor\n" +
                    "\tkbd              Generic Keyboard (manufacturer = Nya Elektriska)\n" +
                    "\tclk              Generic Clock (manufacturer = Nya Elektriska)\n" +
                    "lem1802, kbd, clk are enabled by default\n");
        }
        ////////////////////////////////
        try {
            char[] bytecode;
            if (srcin != null) {
                FileInputStream insrcf = new FileInputStream(srcin);
                char[] csources = new char[insrcf.available()];
                new InputStreamReader(insrcf).read(csources, 0, csources.length);
                AntlrAssembler assembler = new AntlrAssembler();
                if (mapout != null) assembler.setGenerateMap(true);
                String ssources = new String(csources);
                bytecode = assembler.assemble(ssources);

                if (mapout != null) {
                    /*ArrayList<String> lines = new ArrayList<String>();
                    BufferedReader brdr = new BufferedReader(new StringReader(ssources));
                    while(true){
                        String line = brdr.readLine();
                        if (line == null) break;
                        lines.add(line);
                    }*/
                    PrintStream mapoutf = new PrintStream(mapout);
                    mapoutf.println(";;;;MAPSTART");
                    mapoutf.println(";;;;SYMBOLS");
                    for (Map.Entry<String, Character> symbol : assembler.getAsmMap().symbolMap.entrySet()) {
                        mapoutf.printf(";; \"%s\"=0x%04x", symbol.getKey(), (int) symbol.getValue());
                        mapoutf.println();
                    }
                    mapoutf.println(";;;;SRCMAP");
                    for (Map.Entry<Integer, Character> line : assembler.getAsmMap().srcMap.entrySet()) {
                        mapoutf.printf(";; %d=0x%04x", line.getKey(), (int) line.getValue());
                        mapoutf.println();
                    }
                    mapoutf.println(";;;;CODE");
                    int one = assembler.getAsmMap().code.nextSetBit(0);
                    while (one != -1) {
                        int zero = assembler.getAsmMap().code.nextClearBit(one);
                        mapoutf.printf(";; code 0x%04x-0x%04x", one, zero - 1);
                        mapoutf.println();
                        one = assembler.getAsmMap().code.nextSetBit(zero);
                    }
                    mapoutf.println(";;;;MAPEND");
                }

            } else if (binin != null) {
                FileInputStream inbinf = new FileInputStream(binin);
                int len = inbinf.available();
                if (len % 2 == 1) fail("Odd file size (0x%x)\n", len);
                len /= 2;
                if (len > 0x10000) fail("Too large file (0x%x)\n", len);
                bytecode = new char[len];
                for (int i = 0; i < len; i++) {
                    int lo = inbinf.read();
                    int hi = inbinf.read();
                    if (lo == -1 || hi == -1) fail("IO Exception\n");
                    bytecode[i] = (char) ((hi << 8) | lo);
                }
            } else bytecode = new char[1];

            if (binout != null) {
                FileOutputStream outfile = new FileOutputStream(binout);
                for (char i : bytecode) {
                    outfile.write(i & 0xff);
                    outfile.write((i >> 8) & 0xff);
                }
                outfile.close();
            }

            if (srcout != null) {
                PrintStream outsrcf = new PrintStream(srcout);
                Disassembler das = new Disassembler();
                das.init(bytecode);
                while (das.getAddress() < bytecode.length) {
                    int addr = das.getAddress();
                    outsrcf.printf("%-26s ; [%04x] =", das.next(true), addr);
                    int addr2 = das.getAddress();
                    while (addr < addr2) {
                        char i = bytecode[addr++];
                        outsrcf.printf(" %04x '%s'", (int) i, (i >= 0x20 && i < 0x7f) ? i : '.');
                    }
                    outsrcf.println();
                }
            }

            if (exec) {
                Dcpu cpu = new Dcpu();
                cpu.upload(bytecode);

                GenericKeyboard keyboard = null;
                if (hw_kbd) {
                    keyboard = new GenericKeyboard(MonitorLEM1802.MANUFACTURER_ID, 16);
                    cpu.attach(keyboard);
                }

                if (hw_lem1802) {
                    MonitorLEM1802 monitor = new MonitorLEM1802();
                    cpu.attach(monitor);
                    MonitorWindow monitorWindow = new MonitorWindow(cpu, monitor, true);
                    monitorWindow.addKeyListener(keyboard);
                    monitorWindow.show();
                }

                if (hw_clk) {
                    GenericClock clock = new GenericClock(MonitorLEM1802.MANUFACTURER_ID);//Nya Elektriska
                    cpu.attach(clock);
                }

                if (trace) {
                    Tracer tracer = new Tracer(System.err);
                    tracer.printMemAtReg(tracemem);
                    tracer.printRegisters(traceregs);
                    tracer.printStack(tracestack ? 8 : 0);
                    tracer.install(cpu);
                }

                cpu.run();
            }

        } catch (IOException e) {
            e.printStackTrace();
            fail("");
        }
    }

    @SuppressWarnings("unused")
    private static void print(String s) {
        System.out.print(s);
    }

    private static void fail(String fmt, Object... args) {
        printf(fmt, args);
        System.exit(-1);
    }

    private static void printf(String fmt, Object... args) {
        System.out.printf(fmt, args);
    }
}
