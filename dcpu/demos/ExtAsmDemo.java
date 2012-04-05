package dcpu.demos;

import dcpu.Assembler;
import dcpu.Dcpu;
import dcpu.io.InstreamPeripheral;
import dcpu.io.OutstreamPeripheral;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

/**
 * Assembles and executes external asm file
 */
public class ExtAsmDemo {
    public static void main(String[] args) {
        String filename = null;
        String outbin = null;
        int ai = 0;
        while (ai < args.length) {
            String arg = args[ai++];
            if (arg.startsWith("-")) {
                if (arg.equals("-O")) {
                    if (ai == args.length) fail("Missing argument");
                    outbin = args[ai++];
                } else {
                    // TODO options: run binary, decompile binary, trace, run some cycles
                    fail("Unrecognized option `%s` . Aborting\n", arg);
                }
            } else {
                if (filename != null) {
                    fail("Multiple filenames (%s and %s). Aborting\n", filename, args);
                }
                filename = arg;
            }
        }
        if (filename == null) {
            fail("DCPU-16 Assembler and Emulator demo.\n" +
                    "Usage:\n" +
                    "\tjava -jar ja-dcpu-demo.jar [OPTIONS] SOURCE\n" +
                    "Will assemble and execute specified file\n" +
                    "OPTIONS:\n" +
                    "\t-O BINOUT        save compiled binary to BINOUT file\n");
        }
        ////////////////////////////////
        try {
            FileInputStream fileInputStream = null;
            fileInputStream = new FileInputStream(filename);
            char[] csources = new char[fileInputStream.available()];
            new InputStreamReader(fileInputStream).read(csources, 0, csources.length);

            Assembler assembler = new Assembler();
            short[] bytecode = assembler.assemble(new String(csources));

            if (outbin != null) {
                FileOutputStream outfile = new FileOutputStream(outbin);
                for (short i : bytecode) {
                    outfile.write((i >> 8) & 0xff);
                    outfile.write(i & 0xff);
                }
                outfile.close();
            }
            Dcpu cpu = new Dcpu();
            cpu.upload(bytecode);
            OutstreamPeripheral stdout = new OutstreamPeripheral(System.out);
            cpu.attach(stdout, 0x8);
            InstreamPeripheral stdin = new InstreamPeripheral(System.in, 100);
            cpu.attach(stdin, 0x9);

            cpu.run();

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
