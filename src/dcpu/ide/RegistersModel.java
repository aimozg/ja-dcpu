package dcpu.ide;

import dcpu.Dcpu;
import dcpu.Debugger;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.LinkedList;
import java.util.List;

import static dcpu.Dcpu.*;


/**
 * TableModel for registers
 * <p/>
 * TODO cycle count
 * TODO display mode switch
 */
public class RegistersModel implements TableModel {

    private final Dcpu cpu;
    private final Debugger debugger;

    public RegistersModel(Dcpu cpu, Debugger debugger) {
        this.cpu = cpu;
        this.debugger = debugger;
        // TODO add listener to debugger
    }

    public int getRowCount() {
        return REGS_COUNT;
    }

    public int getColumnCount() {
        return 2;
    }

    private static String[] columnNames = {"Register", "Value (hex)"};

    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }

    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return (columnIndex == 1);
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        // TODO use Register enum
        if (columnIndex == 0) return MEM_NAMES[rowIndex];
        return Integer.toHexString(cpu.memget(M_A + rowIndex) & 0xffff);//TODO debugger flag
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 1 && aValue != null) {
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
            cpu.memset(M_A + rowIndex, value); // TODO debugger flag
        }
    }

    private List<TableModelListener> tableModelListeners = new LinkedList<TableModelListener>();

    public void addTableModelListener(TableModelListener l) {
        tableModelListeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        tableModelListeners.remove(l);
    }

    public void fireUpdate() {
        for (TableModelListener tableModelListener : tableModelListeners) {
            tableModelListener.tableChanged(new TableModelEvent(this, 0, REGS_COUNT));
        }
    }
}
