package dcpu.antlr;

public class Instruction {
    public enum Type {
        BASIC, SPECIAL, COMMAND, DATA;
    }

    public Type type;
    public String name;
    public int length;      // number of words this instruction leads to
    public int address;     // the cpu address after resolution
    public OpNode src;
    public OpNode dst;

    public Instruction(String name, OpNode dst, OpNode src) {
        this.name = name;
        this.dst = dst;
        this.src = src;
        type = Type.BASIC;
    }

    public Instruction(String name, OpNode src) {
        this.name = name;
        this.src = src;
        type = Type.SPECIAL;
    }

    public Instruction(String name) {
        this.name = name;
        type = Type.COMMAND;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Instruction[\nname: ")
          .append(name)
          .append("\n type: ")
          .append(type.toString())
          .append("\n length: ")
          .append(length)
          .append("\n address: ")
          .append(address)
          .append("\n src: ")
          .append(src == null ? "<none>" : src.toString())
          .append("\n dst: ")
          .append(dst == null ? "<none>" : dst.toString())
          .append("]\n");
        return sb.toString();
    }

}