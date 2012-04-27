package dcpu;

import dcpu.Dcpu.Reg;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class PeripheralsTest {
    private Assembler assembler;
    private Dcpu dcpu;

    @Before
    public void setUp() {
        assembler = new Assembler();
        dcpu = new Dcpu();
    }

    @Test
    public void testPeripheralAccess() throws Exception {
        // Test a peripheral is read and written to in all modes correctly
        TestPeripheral testPeripheral = new TestPeripheral();
        dcpu.attach(testPeripheral, 0xa); // reading 0xaXXX returns 0x0XXX for any read
        short[] bin = assembler.assemble(
                "SET I, 1\n" +
                        "SET X, 0xA110\n" +

                        "SET A, [0xA100]\n" +
                        "SET B, [0xA100 + I]\n" +
                        "SET Y, [X]\n" +

                        "SET [0xA200], 0x10\n" +
                        "SET [0xA200 + I], 0x11\n" +
                        "SET [X], 0x12\n" +

                        "SET X, 0xA111\n" +
                        "SET [X], 0xf154\n"
        );
        short[] expected = new short[]{
                (short) 0x88c1, 0x7c61, (short) 0xa110, 0x7801,
                (short) 0xa100, 0x5821, (short) 0xa100, 0x2c81,
                (short) 0xc7c1, (short) 0xa200, (short) 0xcac1, (short) 0xa200,
                (short) 0xcd61, 0x7c61, (short) 0xa111, 0x7d61,
                (short) 0xf154
        };
        assertArrayEquals(TestUtils.displayExpected(expected, bin), expected, bin);

        dcpu.upload(bin);
        dcpu.run(2);
        assertEquals("i", 0x1, dcpu.getreg(Reg.I));
        assertEquals("x", (short) 0xA110, dcpu.getreg(Reg.X));

        dcpu.run(1);
        assertEquals("a", 0x0100, dcpu.getreg(Reg.A));
        assertArrayEquals("svip reads", new Integer[]{0x0100}, testPeripheral.reads.toArray());
        dcpu.run(1);
        assertEquals("b", 0x0101, dcpu.getreg(Reg.B));
        assertArrayEquals("svip reads", new Integer[]{0x0100, 0x0101}, testPeripheral.reads.toArray());
        dcpu.run(1);
        assertEquals("y", 0x0110, dcpu.getreg(Reg.Y));
        assertArrayEquals("svip reads", new Integer[]{0x0100, 0x0101, 0x0110}, testPeripheral.reads.toArray());

        dcpu.run(1);
        assertArrayEquals("svip writes addresses", new Integer[]{0x0200}, testPeripheral.writes.keySet().toArray());
        assertArrayEquals("svip writes values", new Short[]{0x10}, testPeripheral.writes.values().toArray());
        dcpu.run(1);
        assertArrayEquals("svip writes addresses", new Integer[]{0x0200, 0x0201}, testPeripheral.writes.keySet().toArray());
        assertArrayEquals("svip writes values", new Short[]{0x10, 0x11}, testPeripheral.writes.values().toArray());
        dcpu.run(1);
        assertArrayEquals("svip writes addresses", new Integer[]{0x0200, 0x0201, 0x0110}, testPeripheral.writes.keySet().toArray());
        assertArrayEquals("svip writes values", new Short[]{0x10, 0x11, 0x12}, testPeripheral.writes.values().toArray());
        dcpu.run(2);
        assertArrayEquals("svip writes addresses", new Integer[]{0x0200, 0x0201, 0x0110, 0x0111}, testPeripheral.writes.keySet().toArray());
        assertArrayEquals("svip writes values", new Short[]{0x10, 0x11, 0x12, (short) 0xf154}, testPeripheral.writes.values().toArray());
    }

    class TestPeripheral extends Dcpu.Peripheral {
        // records reads (which are returned as the offset) and writes to peripheral

        public List<Integer> reads = new ArrayList<Integer>();
        public LinkedHashMap<Integer, Short> writes = new LinkedHashMap<Integer, Short>();

        @Override
        public short onMemget(int offset) {
            reads.add(offset);
            return (short) (offset & 0x0fff);
        }

        @Override
        public void onMemset(int offset, short newval, short oldval) {
            writes.put(offset, newval);
        }
    }
}
