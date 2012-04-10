package dcpu.ide;

import dcpu.Dcpu;
import dcpu.Debugger;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.LinkedList;
import java.util.List;

/**
 * TableModel for DCPU memory
 */
public class MemoryModel implements TableModel {

    private final Dcpu cpu;
    private final Debugger debugger;

    public MemoryModel(Dcpu cpu, Debugger debugger) {
        this.cpu = cpu;
        this.debugger = debugger;
        // TODO add listeners
    }

    public int getRowCount() {
        return Dcpu.RAM_SIZE / 16;
    }

    public int getColumnCount() {
        return 16 + 1;
    }

    private static final String[] columnNames = {"Addr",
            "-0", "-1", "-2", "-3", "-4", "-5", "6", "7",
            "-8", "-9", "-a", "-b", "-c", "-d", "e", "f",
    };

    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) return String.format("%04x-", rowIndex * 16);
        return cpu.memget(rowIndex * 16 + columnIndex - 1);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != 0 && aValue != null) {
            short value;
            if (aValue instanceof Number) {
                value = ((Number) aValue).shortValue();
            } else if (aValue instanceof String) {
                String sValue = (String) aValue;
                try {
                    if (sValue.startsWith("0x")) {
                        value = (short) Integer.parseInt(sValue.substring(2), 16);
                    } else if (sValue.startsWith("0b")) {
                        value = (short) Integer.parseInt(sValue.substring(2), 2);
                    } else {
                        value = (short) Integer.parseInt(sValue);
                    }
                } catch (NumberFormatException ignored) {
                    return;
                }
            } else return;
            cpu.memset(rowIndex * 16 + columnIndex - 1, value); // TODO debugger flag
        }
    }

    private List<TableModelListener> tableModelListeners = new LinkedList<TableModelListener>();

    public void addTableModelListener(TableModelListener l) {
        tableModelListeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        tableModelListeners.remove(l);
    }
}
