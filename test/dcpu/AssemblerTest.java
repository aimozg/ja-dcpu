package dcpu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

public class AssemblerTest {

	private Assembler assembler;
	
	private static String[] REGISTERS = new String[] {"A", "B", "C", "X", "Y", "Z", "I", "J"};
	private static String[] VAL_COMMANDS = new String[] {"POP", "PEEK", "PUSH", "SP", "PC", "O"};
	private static Map<String, Integer> AVALUES = new LinkedHashMap<String, Integer>();
	private static Map<String, Integer> BVALUES = new LinkedHashMap<String, Integer>();
	private static final String A_BIG_LITERAL = "f154"; // used where a word is needed for A, e.g. "SET [0xf154 + I], 0x20"
	private static final String B_BIG_LITERAL = "face"; // used where a word is needed for B, e.g. "SET A, [0xface]
	private static Integer[] EXTRA_WORDS = new Integer[] {0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 1,1,1,1,1,1,1,1, 0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
	private static String[] OP_COMMANDS = new String[] {"NBI", "SET", "ADD", "SUB", "MUL", "DIV", "MOD", "SHL", "SHR", "AND", "BOR", "XOR", "IFE", "IFN", "IFG", "IFB"};
	
	static {
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put(REGISTERS[i].toUpperCase(), 0 + i);
			BVALUES.put(REGISTERS[i].toUpperCase(), 0 + i);
		}
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put("[" + REGISTERS[i].toUpperCase() + "]", 8 + i);
			BVALUES.put("[" + REGISTERS[i].toUpperCase() + "]", 8 + i);
		}
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put("[0x" + A_BIG_LITERAL.toUpperCase() + " + " + REGISTERS[i].toUpperCase() + "]", 16 + i);
			BVALUES.put("[0x" + B_BIG_LITERAL.toUpperCase() + " + " + REGISTERS[i].toUpperCase() + "]", 16 + i);
		}
		for(int i = 0; i < VAL_COMMANDS.length; i++) {
			AVALUES.put(VAL_COMMANDS[i].toUpperCase(), 0x18 + i);
			BVALUES.put(VAL_COMMANDS[i].toUpperCase(), 0x18 + i);
		}
		AVALUES.put("[0x" + A_BIG_LITERAL.toUpperCase() + "]", 0x1e);
		AVALUES.put("0x" + A_BIG_LITERAL.toUpperCase(), 0x1f);
		BVALUES.put("[0x" + B_BIG_LITERAL.toUpperCase() + "]", 0x1e);
		BVALUES.put("0x" + B_BIG_LITERAL.toUpperCase(), 0x1f);
		for (int i = 0; i < 0x20; i++) {
			AVALUES.put(String.format("0x%02X", i), 0x20 + i);
			BVALUES.put(String.format("0x%02X", i), 0x20 + i);
		}

		// do it all again in lower case
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put(REGISTERS[i].toLowerCase(), 0 + i);
			BVALUES.put(REGISTERS[i].toLowerCase(), 0 + i);
		}
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put("[" + REGISTERS[i].toLowerCase() + "]", 8 + i);
			BVALUES.put("[" + REGISTERS[i].toLowerCase() + "]", 8 + i);
		}
		for(int i = 0; i < REGISTERS.length; i++) {
			AVALUES.put("[0x" + A_BIG_LITERAL.toLowerCase() + " + " + REGISTERS[i].toLowerCase() + "]", 16 + i);
			BVALUES.put("[0x" + B_BIG_LITERAL.toLowerCase() + " + " + REGISTERS[i].toLowerCase() + "]", 16 + i);
		}
		for(int i = 0; i < VAL_COMMANDS.length; i++) {
			AVALUES.put(VAL_COMMANDS[i].toLowerCase(), 0x18 + i);
			BVALUES.put(VAL_COMMANDS[i].toLowerCase(), 0x18 + i);
		}
		AVALUES.put("[0x" + A_BIG_LITERAL.toLowerCase() + "]", 0x1e);
		AVALUES.put("0x" + A_BIG_LITERAL.toLowerCase(), 0x1f);
		BVALUES.put("[0x" + B_BIG_LITERAL.toLowerCase() + "]", 0x1e);
		BVALUES.put("0x" + B_BIG_LITERAL.toLowerCase(), 0x1f);
		for (int i = 0; i < 0x20; i++) {
			AVALUES.put(String.format("0x%02x", i).toLowerCase(), 0x20 + i);
			BVALUES.put(String.format("0x%02x", i).toLowerCase(), 0x20 + i);
		}
	}
	
	@Before
	public void setUp() {
		assembler = new Assembler();
	}
	
	@Test
	public void testAssemblePureComments() {
		short[] code = assembler.assemble("; this is a comment");
		assertEquals("Should be no code", 0, code.length);
	}
	
	@Test
	public void testOpcodes() throws Exception {
		// Test every OP command except NBI for every A,B combination.
		// We also test every upper and lower case variation of both op command and codes and literals
		for (int opCode = 1; opCode < OP_COMMANDS.length; opCode++) {
			for (Entry<String, Integer> aEntry : AVALUES.entrySet()) {
				String aKey = aEntry.getKey(); // e.g. "A"
				Integer aValue = aEntry.getValue(); // e.g. 0
				int aExtraWords = EXTRA_WORDS[aValue];
				
				for (Entry<String, Integer> bEntry : BVALUES.entrySet()) {
					String bKey = bEntry.getKey();
					Integer bValue = bEntry.getValue();
					int bExtraWords = EXTRA_WORDS[bValue];
					// OP A, B = (6 bits for B) (6 bits for A) (4 bits for opcode)
					short expectedCode = (short) (((0x000f & opCode) + (0x03f0 & (aValue << 4)) + (0xfc00 & (bValue << 10))) & 0xffff);
					
					assertAssembly(OP_COMMANDS[opCode].toLowerCase() + " " + aKey + ", " + bKey, expectedCode, aExtraWords, bExtraWords);
					assertAssembly(OP_COMMANDS[opCode].toUpperCase() + " " + aKey + ", " + bKey, expectedCode, aExtraWords, bExtraWords);
				}
			}
		}		
	}

	private void assertAssembly(String assembly, short expectedCode, int aExtraWords, int bExtraWords) {
		int numWords = 1 + aExtraWords + bExtraWords;
		short[] code = assembler.assemble(assembly);
		short[] expected = new short[numWords];
		expected[0] = expectedCode;
		
		String firstLiteralToUse = aExtraWords > 0 ? A_BIG_LITERAL : B_BIG_LITERAL; // decide if the first extra word is on A or B when we only have 1 extra word
		if (numWords > 1) expected[1] = (short) (Integer.parseInt(firstLiteralToUse, 16) & 0xffff);
		if (numWords > 2) expected[2] = (short) (Integer.parseInt(B_BIG_LITERAL, 16) & 0xffff); // will always be B if we get 3 words
		assertEquals("Expected " + numWords + " words", numWords, code.length);
		assertArrayEquals(expected, code);
	}

}
