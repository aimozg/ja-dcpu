package dcpu.ide;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Set;

public class SourceRowHeader extends JList<String> {
    private JTextArea sources;
    private DefaultListModel<String> model;
    private Set<Integer> breakpoints;

    public SourceRowHeader(JTextArea sources, Set<Integer> breakpoints) {
        this.sources = sources;
        this.breakpoints = breakpoints;
        this.model = new DefaultListModel<String>();
        setFont(sources.getFont());
        setModel(model);
        // I'm sorry
        setFixedCellHeight(sources.getFont().getSize() + 2);//TODO calculate this in a better way
        documentUpdated();
        // TODO shift breakpoints
        sources.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                documentUpdated();
            }

            public void removeUpdate(DocumentEvent e) {
                documentUpdated();
            }

            public void changedUpdate(DocumentEvent e) {
                documentUpdated();
            }
        });
    }

    private void documentUpdated() {
        if (model.getSize() != sources.getLineCount()) {
            model.clear();
            for (int i = 0; i < sources.getLineCount(); i++) {
                String s = nameFor(i);
                model.addElement(s);
            }
        }
    }

    private String nameFor(int i) {
        return String.valueOf(i + 1) + (breakpoints.contains(i) ? "!" : " ");
    }


    public void breakpointChanged(int line) {
        if (breakpoints.contains(line)) {
            breakpoints.add(line);
        } else {
            breakpoints.remove(line);
        }
        model.set(line, nameFor(line));
    }

}
