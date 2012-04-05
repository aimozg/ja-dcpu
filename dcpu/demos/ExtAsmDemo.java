package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.Disassembler;
import dcpu.Tracer;
import dcpu.io.InstreamPeripheral;
import dcpu.io.OutstreamPeripheral;

import java.io.*;

/**
 * Assembles and executes external asm file
 */
public class ExtAsmDemo {
    public static void main(String[] args) {
        String insrc = null;
        String inbin = null;
        String outbin = null;
        boolean exec = true;
        String outsrc = null;
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
                    outbin = args[ai++];
                } else if (arg.equals("-I")) {
                    if (ai == args.length) fail("Missing argument");
                    inbin = args[ai++];
                } else if (arg.equals("-D")) {
                    if (ai == args.length) fail("Missing argument");
                    outsrc = args[ai++];
                } else if (arg.equals("-x")) {
                    exec = false;
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
                if (insrc != null) {
                    fail("Multiple filenames (%s and %s). Aborting\n", insrc, args);
                }
                insrc = arg;
            }
        }
        if (insrc == null && inbin == null) {
            fail("DCPU-16 Assembler and Emulator demo.\n" +
                    "Usage:\n" +
                    "\tjava -jar ja-dcpu-demo.jar [OPTIONS] [SRCIN]\n" +
                    "Will assemble and execute specified source file\n" +
                    "OPTIONS:\n" +
                    "\t-O BINOUT        save compiled binary to BINOUT file\n" +
                    "\t-I BININ         load binary image and exec this instead of source\n" +
                    "\t-D SRCOUT        disassemble binary image and save to SRCOUT\n" +
                    "\t-x               do not execute code, just disassemble/save\n" +
                    "\t-T[TRACEOPTS]    trace executed instructions to stderr\n" +
                    "\t\tTRACEOPTS might include (no separators):\n" +
                    "\t\tr              print registers value\n" +
                    "\t\tm              print memory at registers addresses\n" +
                    "\t\ts              print stack (8 words)\n" +
                    "\n" +
                    "Hard-coded peripherals:\n" +
                    "\t0x8000-0x8fff    Stdout. Anything written comes to stdout\n" +
                    "\t0x9000-0x9fff    Stdin. Reading returns character typed, or 0xffff if no input yet\n");
        }
        ////////////////////////////////
        try {
            short[] bytecode;
            if (insrc != null) {
                FileInputStream insrcf = new FileInputStream(insrc);
                char[] csources = new char[insrcf.available()];
                new InputStreamReader(insrcf).read(csources, 0, csources.length);

                Assembler assembler = new Assembler();
                bytecode = assembler.assemble(new String(csources));
            } else if (inbin != null) {
                FileInputStream inbinf = new FileInputStream(inbin);
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

            if (outbin != null) {
                FileOutputStream outfile = new FileOutputStream(outbin);
                for (short i : bytecode) {
                    outfile.write(i & 0xff);
                    outfile.write((i >> 8) & 0xff);
                }
                outfile.close();
            }

            if (outsrc != null) {
                PrintStream outsrcf = new PrintStream(outsrc);
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
