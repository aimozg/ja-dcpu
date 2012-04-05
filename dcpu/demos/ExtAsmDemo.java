package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Disassembler;
import dcpu.Tracer;
import dcpu.io.InstreamPeripheral;
import dcpu.io.OutstreamPeripheral;

import java.io.*;
import java.util.Map;

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
                    "\n" +
                    "Hard-coded peripherals:\n" +
                    "\t0x8000-0x8fff    Stdout. Anything written comes to stdout\n" +
                    "\t0x9000-0x9fff    Stdin. Reading returns character typed, or 0xffff if no input yet\n");
        }
        ////////////////////////////////
        try {
            short[] bytecode;
            if (srcin != null) {
                FileInputStream insrcf = new FileInputStream(srcin);
                char[] csources = new char[insrcf.available()];
                new InputStreamReader(insrcf).read(csources, 0, csources.length);
                Assembler assembler = new Assembler();
                if (mapout != null) assembler.genMap = true;
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
                    for (Map.Entry<String, Short> symbol : assembler.asmmap.symbolMap.entrySet()) {
                        mapoutf.printf(";; \"%s\"=0x%04x", symbol.getKey(), symbol.getValue());
                        mapoutf.println();
                    }
                    mapoutf.println(";;;;SRCMAP");
                    for (Map.Entry<Integer, Short> line : assembler.asmmap.srcMap.entrySet()) {
                        mapoutf.printf(";; %d=0x%04x", line.getKey(), line.getValue());
                        mapoutf.println();
                    }
                    mapoutf.println(";;;;CODE");
                    int one = assembler.asmmap.code.nextSetBit(0);
                    while (one != -1) {
                        int zero = assembler.asmmap.code.nextClearBit(one);
                        mapoutf.printf(";; code 0x%04x-0x%04x", one, zero - 1);
                        mapoutf.println();
                        one = assembler.asmmap.code.nextSetBit(zero);
                    }
                    mapoutf.println(";;;;MAPEND");
                }

            } else if (binin != null) {
                FileInputStream inbinf = new FileInputStream(binin);
                int len = inbinf.available();
                if (len % 2 == 1) fail("Odd file size (0x%x)\n", len);
                len /= 2;
                if (len > 0x10000) fail("Too large file (0x%x)\n", len);
                bytecode = new short[len];
                for (int i = 0; i < len; i++) {
                    int lo = inbinf.read();
                    int hi = inbinf.read();
                    if (lo == -1 || hi == -1) fail("IO Exception\n");
                    bytecode[i] = (short) ((hi << 8) | lo);
                }
            } else bytecode = new short[1];

            if (binout != null) {
                FileOutputStream outfile = new FileOutputStream(binout);
                for (short i : bytecode) {
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
                    outsrcf.printf("%-26s ; [%04x] =", das.next(), addr);
                    int addr2 = das.getAddress();
                    while (addr < addr2) {
                        short i = bytecode[addr++];
                        outsrcf.printf(" %04x '%s'", i, (i >= 0x20 && i < 0x7f) ? (char) i : '.');
                    }
                    outsrcf.println();
                }
            }

            if (exec) {
                Dcpu cpu = new Dcpu();
                cpu.upload(bytecode);
                OutstreamPeripheral stdout = new OutstreamPeripheral(System.out);
                cpu.attach(stdout, 0x8);
                InstreamPeripheral stdin = new InstreamPeripheral(System.in, 100);
                cpu.attach(stdin, 0x9);

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
