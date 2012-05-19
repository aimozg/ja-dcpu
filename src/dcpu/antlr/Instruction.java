package dcpu.antlr;

import java.util.List;

import dcpu.antlr.node.ReferenceOpNode;
import dcpu.antlr.node.ValueOpNode;

public class Instruction {
    public enum Type {
        BASIC, SPECIAL, COMMAND, DATA;
    }

    public Type type;
    public String name;
    public int address = -1;     // the memory address after resolution
    public OpNode src;
    public OpNode dst;
    public char[] bin;		// the instruction's data
    public int length;      // these will be filled in
    public int index;       // the index in all instructions

    public Instruction(String name, OpNode dst, OpNode src) {
        this.name = name;
        this.dst = dst;
        this.src = src;
        type = Type.BASIC;
        setCanBeShort();
    }

	public Instruction(String name, OpNode src) {
        this.name = name;
        this.src = src;
        type = Type.SPECIAL;
        setCanBeShort();
    }

	public Instruction(String name) {
	    this.name = name;
	    type = Type.COMMAND;
	}
	
	private void setCanBeShort() {
		if (dst != null && (dst instanceof ValueOpNode)) {
		    ValueOpNode dstValue = (ValueOpNode) dst;
            dstValue.isShortDecided = true;
            dstValue.canBeShort = false;
		}
	}
	
    public boolean srcIsReference() {
    	return src != null && (src instanceof ReferenceOpNode);
    }

    public boolean dstIsReference() {
    	return dst != null && (dst instanceof ReferenceOpNode);
    }
    
    public void setBin(char c, List<Integer> nextWords) {
        length = 1 + nextWords.size();
    	bin = new char[length];
    	bin[0] = c;
    	for(int i=0; i<nextWords.size(); i++) {
    		bin[i+1] = (char) (nextWords.get(i) & 0xffff);
    	}
    }
    
    public int getLength() {
    	return bin == null ? 0 : bin.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Instruction[\nname: ")
          .append(name)
          .append("\n type: ")
          .append(type.toString())
          .append("\n address: ")
          .append(address)
          .append("\n length: ")
          .append(length)
          .append("\n bin: ")
          .append(binAsHex())
          .append("\n src: ")
          .append(src == null ? "<none>" : src.toString())
          .append("\n dst: ")
          .append(dst == null ? "<none>" : dst.toString())
          .append("]\n");
        return sb.toString();
    }
    
    private String binAsHex() {
        if (bin == null) return "<none>";
        StringBuilder sb = new StringBuilder();
        for(char c : bin) {
            sb.append(String.format("0x%04x ", (int) c));
        }
        return sb.toString().trim();
    }
    
}