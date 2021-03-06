package main_Package;

import java.awt.Color;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.Socket;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Random;
import javax.swing.AbstractButton;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.JFrame;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.awt.HeadlessException;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.Semaphore;
import javax.swing.ButtonGroup;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import sun.awt.Mutex;

/**
 * @author mn3m
 */
public class Main_Frame extends JFrame {

    LinkedList<Process> ready_queue;
    LinkedList<Process> ready_queue_backup;
    LinkedList<Process> processes_memory_list;
    LinkedList<Thread> producers_list;
    LinkedList<Integer> buffer;
    LinkedList<Thread> consumers_list;
    LinkedList<Thread> writers_list;
    LinkedList<Thread> readers_list;
    ArrayList<Memory_Location> memory;
    LinkedList<Memory_Location> Back_Store;

    DefaultTableModel proc_table_model;
    DefaultTableModel disk_table_model;
    DefaultTableModel editing_table_model;
    DefaultTableModel deadlock_table_model;
    DefaultTableModel memory_table_model;
    DefaultTableModel processes_memory_table_model;
    DefaultTableModel page_table_model;
    DefaultTableModel pw_table_model;
    DefaultTableModel buffer_table_model;
    DefaultTableModel cr_table_model;

    int editing_table_number = -1;
    Map<Integer, Integer> ready_queue_map = new HashMap<>();
    boolean is_map_empty = true;
    int process_count = 0;
    boolean is_bb = true;
    int read_count = 0;
    boolean full = false;
    boolean empty = true;

    int max_size = 10;

    int negative_ones = 0;

    int shared_variable = 0;
    final Mutex bb_mutex = new Mutex();
    Semaphore sm = new Semaphore(2);

    final int PROC_TABLE = 0;
    final int DISK_TABLE = 1;
    final int DEADLOCK_TABLE = 2;
    final int PROC_MEM_TABLE = 3;

    boolean client_connected = false;
    boolean server_started = false;
    Client client;
    Server server;

    private BufferedReader input;
    private OutputStream output;
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 115200;
    private CommPortIdentifier portId = null;
    private final LinkedList<CommPortIdentifier> portsList;
    private SerialPort serialPort = null;
    boolean op_connected = false;
    boolean ip_connected = false;

    java.lang.Process graphing_process;

    int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
    int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;

    long starting_time;

    Runnable produce = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (bb_mutex) {
                        while (full) {
                            bb_mutex.wait();
                        }
//                        while (full);
                        int x = Math.abs(new Random().nextInt(100));
                        buffer.addLast(x);
                        if (buffer.size() >= max_size) {
                            full = true;
                        }
                        System.out.println("I Thread # " + Thread.currentThread().getId() + " Produced " + x);

                        clear_table(buffer_table_model);
                        populate_buffer_table_coloumn(buffer_table_model, buffer);

                        System.out.println("Current list:");
                        buffer.forEach(s -> System.out.print(s + "\t"));
                        System.out.println();
                        empty = false;
                        bb_mutex.notify();
                        Thread.sleep(new Random().nextInt(500));
                    }
                }
            } catch (InterruptedException ex) {
                System.out.println(Thread.currentThread().getName() + " was interrupted");
            }
        }
    };

    Runnable consume = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    synchronized (bb_mutex) {
                        while (empty) {
                            bb_mutex.wait();
                        }
//                        while (empty);
                        int x = buffer.getFirst();
                        buffer.removeFirst();
                        int size = 0;
                        if (buffer.isEmpty()) {
                            empty = true;
                        }
                        System.out.println("I Thread # " + Thread.currentThread().getId() + " Conusmed " + x);

                        clear_table(buffer_table_model);
                        populate_buffer_table_coloumn(buffer_table_model, buffer);

                        System.out.println("Current list:");
                        buffer.forEach(s -> System.out.print(s + "\t"));
                        System.out.println();
                        full = false;
                        bb_mutex.notify();
                        Thread.sleep(new Random().nextInt(500));
                    }
                }
            } catch (InterruptedException ex) {
                System.out.println(Thread.currentThread().getName() + " was interrupted");
            }
        }
    };

    Runnable write = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    shared_variable += 5;
                    System.out.println("Updating from Thread #" + Thread.currentThread().getId());
                    System.out.println("shared variable = " + shared_variable);
                    current_label.setText("Current: " + shared_variable);
                    rw_label.setText("Writting from " + Thread.currentThread().getName());
                    Thread.sleep(new Random().nextInt(5000));
                } catch (InterruptedException ex) {
                    System.out.println(Thread.currentThread().getName() + " was interrupted");
                    break;
                }
            }
        }
    };

    Runnable read = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    try {
                        sm.acquire();
                    } catch (InterruptedException ex) {
                        System.err.println(ex.toString());
                    }
                    sm.release();
                    System.out.println("Reading from Thread #" + Thread.currentThread().getId());
                    System.out.println("shared variable = " + shared_variable);
                    rw_label.setText("Reading from " + Thread.currentThread().getName());
                    Thread.sleep(new Random().nextInt(4000));
                } catch (InterruptedException ex) {
                    System.out.println(Thread.currentThread().getName() + " was interrupted");
                    break;
                }
            }
        }
    };

    javax.swing.UIManager.LookAndFeelInfo info = javax.swing.UIManager.getInstalledLookAndFeels()[3];

    private String get_selected_button_text(ButtonGroup btn_group) {
        String button_text = null;
        for (Enumeration<AbstractButton> buttons = btn_group.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                button_text = button.getText();
            }
        }
        return button_text;
    }

    private void clear_table(DefaultTableModel tm) {
        int table_length = tm.getRowCount();
        for (int i = 0; i < table_length; i++) {
            tm.removeRow(0);
        }
    }

    private void populate_table_coloumn(DefaultTableModel tm, LinkedList<Thread> queue) {
        for (Thread t : queue) {
            Object[] data = new Object[]{t.getName()};
            tm.addRow(data);
        }
    }

    private void populate_buffer_table_coloumn(DefaultTableModel tm, LinkedList<Integer> queue) {
        for (Integer i : queue) {
            Object[] data = new Object[]{i.toString()};
            tm.addRow(data);
        }
    }

    private void populate_memory_table(DefaultTableModel tm, ArrayList<Memory_Location> queue) {
        int x = 0;
        for (Memory_Location m : queue) {
            Object[] data = {x++, m.PID, m.Data, m.Usage, NumberFormat.getNumberInstance(Locale.US).format(m.Allocation_Time)};
            tm.addRow(data);
        }
    }

    private void populate_page_table(DefaultTableModel tm, LinkedList<Map.Entry> queue) {
        queue.forEach(p -> {
            Object[] data = new Object[]{};
            if ((int) p.getValue() == -1) {
                data = new Object[]{p.getKey(), "N/A"};
            } else {
                data = new Object[]{p.getKey(), p.getValue()};
            }
            tm.addRow(data);
        });
    }

    private void populate_table(DefaultTableModel tm, LinkedList<Process> queue, int table_number) {
        switch (table_number) {
            case 0:
                queue.forEach(p -> {
                    Object[] data = {p.PID, p.Arrival_Time, p.Burst_Time, p.Priority};
                    tm.addRow(data);
                });
                break;
            case 1:
                queue.forEach(p -> {
                    Object[] data = {p.PID, p.Sector};
                    tm.addRow(data);
                });
                break;
            case 2:
                queue.forEach(p -> {
                    Object[] data = {p.PID, p.Need.get_A(), p.Need.get_B(), p.Need.get_C()};
                    tm.addRow(data);
                });
                break;
            case 3:
                queue.forEach(p -> {
                    Object[] data = {p.PID, p.get_page_count()};
                    tm.addRow(data);
                });
                break;
            default:
                break;
        }
    }

    private void save_changed_table(int table_to_save) {
        clear_table(proc_table_model);
        clear_table(disk_table_model);
        clear_table(deadlock_table_model);
        switch (table_to_save) {
            case 0:
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    proc_table_model.addRow(data);
                }
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    Process temp = new Process(Integer.parseInt(data[0].toString()),
                            Integer.parseInt(data[1].toString()),
                            Integer.parseInt(data[2].toString()),
                            Integer.parseInt(data[3].toString()));
                    ready_queue.add(temp);
                }
                break;
            case 1:
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    disk_table_model.addRow(data);
                }
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    Process temp = new Process(Integer.parseInt(data[0].toString()),
                            Integer.parseInt(data[1].toString()));
                    ready_queue.add(temp);
                }
                break;
            case 2:
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    deadlock_table_model.addRow(data);
                }
                for (int i = 0; i < editing_table_model.getRowCount(); i++) {
                    Object[] data = new Object[4];
                    for (int j = 0; j < editing_table_model.getColumnCount(); j++) {
                        data[j] = editing_table_model.getValueAt(i, j);
                    }
                    Process temp = new Process(Integer.parseInt(data[0].toString()),
                            new Resource(Integer.parseInt(data[1].toString()),
                                    Integer.parseInt(data[2].toString()),
                                    Integer.parseInt(data[3].toString())));
                    ready_queue.add(temp);
                }
                break;
            default:
                break;
        }
        ready_queue.forEach(i -> ready_queue_map.put(i.PID, i.Sector));
        clear_table(editing_table_model);
        editing_table_frame.setVisible(false);
    }

    private void show_disk_results_with_graph() {
        int current_position_head = new Random().nextInt(200);
        String buttonText = get_selected_button_text(disk_scheduler_button_group);
        if (buttonText.toLowerCase().contains("sstf")) {
            try {
                String args = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.SSTF);
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args = args.replace(" ", " -> ");
                args = args.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args.substring(args.indexOf("[") + 1, args.indexOf("]"));
                appendToPane(disk_log_area, "[SSTF] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        } else if (buttonText.toLowerCase().contains("look")) {
            try {
                String args = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.C_LOOK);
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args = args.replace(" ", " -> ");
                args = args.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args.substring(args.indexOf("[") + 1, args.indexOf("]"));
                appendToPane(disk_log_area, "[C LOOK] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        } else {
            try {
                ready_queue_map.clear();
                ready_queue.forEach(i -> ready_queue_map.put(i.PID, i.Sector));
                String args1 = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.SSTF);

                ready_queue.clear();
                Iterator it = ready_queue_map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
                }
                String args2 = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.C_LOOK);

                ready_queue.clear();
                it = ready_queue_map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
                }
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args1 + " s" + args2
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args1 + " s" + args2
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args1 = args1.replace(" ", " -> ");
                args1 = args1.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");

                args2 = args2.replace(" ", " -> ");
                args2 = args2.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args1.substring(args1.indexOf("[") + 1, args1.indexOf("]"));
                appendToPane(disk_log_area, "[SSTF] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args1 + "\n", Color.black, true, false);
                appendToPane(disk_log_area, "[C LOOK] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args2 + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        }
        disk_log_area.setCaretPosition(disk_log_area.getDocument().getLength());
    }

    private void show_disk_results_with_graph(int current_position_head) {
        String buttonText = get_selected_button_text(disk_scheduler_button_group);
        if (buttonText.toLowerCase().contains("sstf")) {
            try {
                String args = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.SSTF);
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args = args.replace(" ", " -> ");
                args = args.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args.substring(args.indexOf("[") + 1, args.indexOf("]"));
                appendToPane(disk_log_area, "[SSTF] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        } else if (buttonText.toLowerCase().contains("look")) {
            try {
                String args = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.C_LOOK);
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args = args.replace(" ", " -> ");
                args = args.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args.substring(args.indexOf("[") + 1, args.indexOf("]"));
                appendToPane(disk_log_area, "[C LOOK] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        } else {
            try {
                ready_queue_map.clear();
                ready_queue.forEach(i -> ready_queue_map.put(i.PID, i.Sector));
                String args1 = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.SSTF);

                ready_queue.clear();
                Iterator it = ready_queue_map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
                }
                String args2 = new Disk_Scheduler(ready_queue, current_position_head).Schedule(Disk_Scheduler.C_LOOK);

                ready_queue.clear();
                it = ready_queue_map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
                }
                String[] cmd = new String[]{};
                if (info.getName().toLowerCase().equals("windows")) {
                    cmd = new String[]{
                        "cmd", "/c", "python scripts_and_helpers\\disk_graphing\\disk_plotter.py" + args1 + " s" + args2
                    };
                } else {
                    cmd = new String[]{
                        "/bin/sh", "-c", "python scripts_and_helpers/disk_graphing/disk_plotter.py" + args1 + " s" + args2
                    };
                }
                System.out.println(cmd[2]);
                graphing_process = new ProcessBuilder(cmd).start();

                args1 = args1.replace(" ", " -> ");
                args1 = args1.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");

                args2 = args2.replace(" ", " -> ");
                args2 = args2.replaceFirst(" -> ", "[").replaceFirst(" -> ", "] -> ");
                String head_starting_position = args1.substring(args1.indexOf("[") + 1, args1.indexOf("]"));
                appendToPane(disk_log_area, "[SSTF] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args1 + "\n", Color.black, true, false);
                appendToPane(disk_log_area, "[C LOOK] ", Color.RED, true, false);
                appendToPane(disk_log_area, "Head Starting Position: " + head_starting_position + "\n" + args2 + "\n\n", Color.black, true, false);
            } catch (IOException ex) {
                System.err.println(ex.toString());
            }
        }
        disk_log_area.setCaretPosition(disk_log_area.getDocument().getLength());
    }

    /**
     * @param pane the JTextPane where you want to append text to
     * @param text the text that you want to append to the JTextPane, Note: add
     * an end line character in the end if you want new text to be appended in a
     * new line
     * @param color the color of the text
     * @param bold if true the text will be appended in bold, else it will be
     * appended PLAIN
     * @param italic if true the text will be appended in italic, else it will
     * be appended PLAIN or Bold as specified by the bold parameter
     */
    private void appendToPane(JTextPane pane, String msg, Color color, boolean bold, boolean italic) {
        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet attr = new SimpleAttributeSet();
        try {
            StyleConstants.setForeground(attr, color);
            StyleConstants.setBold(attr, bold);
            StyleConstants.setItalic(attr, italic);
            doc.insertString(doc.getLength(), msg, attr);
            pane.setCaretPosition(pane.getDocument().getLength());
        } catch (BadLocationException ex) {
            System.err.println(ex.toString());
        }
    }

    public synchronized void close() {
        if (serialPort != null) {
            serialPort.removeEventListener();
            serialPort.close();
        }
    }

    public Main_Frame() {
        initComponents();
        ready_queue = new LinkedList<>();
        portsList = new LinkedList<>();
        memory = new ArrayList<>();
        processes_memory_list = new LinkedList<>();
        Back_Store = new LinkedList<>();
        producers_list = new LinkedList<>();
        buffer = new LinkedList<>();
        consumers_list = new LinkedList<>();
        writers_list = new LinkedList<>();
        readers_list = new LinkedList<>();

        proc_table_model = (DefaultTableModel) processes_table.getModel();
        disk_table_model = (DefaultTableModel) disk_table.getModel();
        editing_table_model = (DefaultTableModel) editing_table.getModel();
        deadlock_table_model = (DefaultTableModel) deadlock_table.getModel();
        memory_table_model = (DefaultTableModel) memory_table.getModel();
        processes_memory_table_model = (DefaultTableModel) processes_memory_table.getModel();
        page_table_model = (DefaultTableModel) page_table.getModel();
        pw_table_model = (DefaultTableModel) pw_table.getModel();
        buffer_table_model = (DefaultTableModel) buffer_table.getModel();
        cr_table_model = (DefaultTableModel) cr_table.getModel();

        current_label.setVisible(false);
        rw_label.setVisible(false);

        processes_memory_table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selected_row = 0;
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();

                int firstIndex = e.getFirstIndex();
                int lastIndex = e.getLastIndex();
                boolean isAdjusting = e.getValueIsAdjusting();

                if (lsm.isSelectionEmpty()) {
                    System.out.println(" <none>");
                } else {
                    // Find out which indexes are selected.
                    int minIndex = lsm.getMinSelectionIndex();
                    int maxIndex = lsm.getMaxSelectionIndex();
                    for (int i = minIndex; i <= maxIndex; i++) {
                        if (lsm.isSelectedIndex(i)) {
                            selected_row = i;
                            int process_id = (int) processes_memory_table.getValueAt(selected_row, 0);
                            LinkedList<Map.Entry> page_table_list = new LinkedList<>();
                            for (Process p : processes_memory_list) {
                                if (p.PID == process_id) {
                                    page_table_list = p.get_page_table();
                                    break;
                                }
                            }
                            clear_table(page_table_model);
                            populate_page_table(page_table_model, page_table_list);
                        }
                    }
                }
            }
        });

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        for (int i = 0; i < memory_table.getColumnCount(); i++) {
            memory_table.setDefaultRenderer(memory_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < processes_table.getColumnCount(); i++) {
            processes_table.setDefaultRenderer(processes_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < processes_memory_table.getColumnCount(); i++) {
            processes_memory_table.setDefaultRenderer(processes_memory_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < disk_table.getColumnCount(); i++) {
            disk_table.setDefaultRenderer(disk_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < editing_table.getColumnCount(); i++) {
            editing_table.setDefaultRenderer(editing_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < deadlock_table.getColumnCount(); i++) {
            deadlock_table.setDefaultRenderer(deadlock_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < page_table.getColumnCount(); i++) {
            page_table.setDefaultRenderer(page_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < pw_table.getColumnCount(); i++) {
            pw_table.setDefaultRenderer(pw_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < buffer_table.getColumnCount(); i++) {
            buffer_table.setDefaultRenderer(buffer_table.getColumnClass(i), renderer);
        }
        for (int i = 0; i < cr_table.getColumnCount(); i++) {
            cr_table.setDefaultRenderer(cr_table.getColumnClass(i), renderer);
        }
        // repaint to show table cell changes
        processes_table.updateUI();
        disk_table.updateUI();
        editing_table.updateUI();
        deadlock_table.updateUI();
        memory_table.updateUI();
        processes_memory_table.updateUI();
        page_table.updateUI();
        pw_table.updateUI();
        buffer_table.updateUI();
        cr_table.updateUI();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        scheduling_frame = new javax.swing.JFrame();
        scheduling_table_scroll_pane = new javax.swing.JScrollPane();
        processes_table = new javax.swing.JTable();
        fifo_radio = new javax.swing.JRadioButton();
        npsjf_radio = new javax.swing.JRadioButton();
        rr_radio = new javax.swing.JRadioButton();
        sched_btn = new javax.swing.JButton();
        edit_scheduler_table_row = new javax.swing.JButton();
        log_area_scroll_pane = new javax.swing.JScrollPane();
        sched_log_area = new javax.swing.JTextPane();
        sched_rand_btn = new javax.swing.JButton();
        scheduler_button_group = new javax.swing.ButtonGroup();
        editing_table_frame = new javax.swing.JFrame();
        editing_table_frame_scroll_pane = new javax.swing.JScrollPane();
        editing_table = new javax.swing.JTable();
        ok_button = new javax.swing.JButton();
        new_row_btn = new javax.swing.JButton();
        delete_row_btn = new javax.swing.JButton();
        server_frame = new javax.swing.JFrame();
        server_chat_panel = new javax.swing.JScrollPane();
        server_chat_pane = new javax.swing.JTextPane();
        server_snd_btn = new javax.swing.JButton();
        server_msg_field = new javax.swing.JTextField();
        client_frame = new javax.swing.JFrame();
        client_chat_panel = new javax.swing.JScrollPane();
        client_chat_pane = new javax.swing.JTextPane();
        client_msg_field = new javax.swing.JTextField();
        client_snd_btn = new javax.swing.JButton();
        about_frame = new javax.swing.JFrame();
        name2 = new javax.swing.JLabel();
        name3 = new javax.swing.JLabel();
        name1 = new javax.swing.JLabel();
        name4 = new javax.swing.JLabel();
        name5 = new javax.swing.JLabel();
        logo_label = new javax.swing.JLabel();
        development_team_title_label = new javax.swing.JLabel();
        output_frame = new javax.swing.JFrame();
        output_search_btn = new javax.swing.JButton();
        output_portsListComoBox = new javax.swing.JComboBox<>();
        output_cnct_discnctBtn = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        brightness_slider = new javax.swing.JSlider();
        brightness_label = new javax.swing.JLabel();
        min_label = new javax.swing.JLabel();
        max_label = new javax.swing.JLabel();
        slow_input_frame = new javax.swing.JFrame();
        slow_input_search_btn = new javax.swing.JButton();
        slow_input_portsListComoBox = new javax.swing.JComboBox<>();
        slow_input_cnct_discnctBtn = new javax.swing.JButton();
        fast_input_frame = new javax.swing.JFrame();
        fast_input_search_btn = new javax.swing.JButton();
        fast_input_portsListComoBox = new javax.swing.JComboBox<>();
        fast_input_cnct_discnctBtn = new javax.swing.JButton();
        disk_scheduling_frame = new javax.swing.JFrame();
        disk_scheduling_table_scroll_pane = new javax.swing.JScrollPane();
        disk_table = new javax.swing.JTable();
        sstf_radio = new javax.swing.JRadioButton();
        clook_radio = new javax.swing.JRadioButton();
        disk_edit_table = new javax.swing.JButton();
        disk_sched_btn = new javax.swing.JButton();
        disk_log_area_scroll_pane = new javax.swing.JScrollPane();
        disk_log_area = new javax.swing.JTextPane();
        cmp_radio = new javax.swing.JRadioButton();
        disk_sched_rand_btn = new javax.swing.JButton();
        disk_scheduler_button_group = new javax.swing.ButtonGroup();
        deadlock_frame = new javax.swing.JFrame();
        disk_scheduling_table_scroll_pane1 = new javax.swing.JScrollPane();
        deadlock_table = new javax.swing.JTable();
        prevention_radio = new javax.swing.JRadioButton();
        avoidance_radio = new javax.swing.JRadioButton();
        deadlock_edit_table = new javax.swing.JButton();
        deadlock_chk_btn = new javax.swing.JButton();
        disk_log_area_scroll_pane1 = new javax.swing.JScrollPane();
        deadlock_log_area = new javax.swing.JTextPane();
        deadlock_sched_rand_btn = new javax.swing.JButton();
        deadlock_button_group = new javax.swing.ButtonGroup();
        memory_frame = new javax.swing.JFrame();
        memory_table_scroll_pane = new javax.swing.JScrollPane();
        memory_table = new javax.swing.JTable();
        memory_lru_radio = new javax.swing.JRadioButton();
        memory_fifo_radio = new javax.swing.JRadioButton();
        processes_frame = new javax.swing.JFrame();
        processes_table_scroll_pane = new javax.swing.JScrollPane();
        processes_memory_table = new javax.swing.JTable();
        page_table_scroll_pane = new javax.swing.JScrollPane();
        page_table = new javax.swing.JTable();
        add_process_btn = new javax.swing.JButton();
        save_data_btn = new javax.swing.JButton();
        processes_label = new javax.swing.JLabel();
        page_table_label = new javax.swing.JLabel();
        proc_data_rand_btn = new javax.swing.JButton();
        memory_button_group = new javax.swing.ButtonGroup();
        sync_frame = new javax.swing.JFrame();
        bounded_radio = new javax.swing.JRadioButton();
        rw_radio = new javax.swing.JRadioButton();
        current_label = new javax.swing.JLabel();
        pw_table_scroll_pane = new javax.swing.JScrollPane();
        pw_table = new javax.swing.JTable();
        pw_add_btn = new javax.swing.JButton();
        cr_add_btn = new javax.swing.JButton();
        pw_rem_btn = new javax.swing.JButton();
        cr_rem_btn = new javax.swing.JButton();
        buffer_table_scroll_pane = new javax.swing.JScrollPane();
        buffer_table = new javax.swing.JTable();
        cr_table_scroll_pane = new javax.swing.JScrollPane();
        cr_table = new javax.swing.JTable();
        rw_label = new javax.swing.JLabel();
        sync_button_group = new javax.swing.ButtonGroup();
        mem_btn = new javax.swing.JButton();
        memory_ = new javax.swing.JButton();
        proc_btn = new javax.swing.JButton();
        io_btn = new javax.swing.JButton();
        file_sys_btn = new javax.swing.JButton();
        main_menu_bar = new javax.swing.JMenuBar();
        file_menu = new javax.swing.JMenu();
        exit_item = new javax.swing.JMenuItem();
        proc_menu_item = new javax.swing.JMenu();
        sched_item = new javax.swing.JMenuItem();
        sync_item = new javax.swing.JMenuItem();
        deadlock_item = new javax.swing.JMenuItem();
        mem_menu = new javax.swing.JMenu();
        paging_item = new javax.swing.JMenuItem();
        file_sys_menu = new javax.swing.JMenu();
        disk_sched_item = new javax.swing.JMenuItem();
        io_menu = new javax.swing.JMenu();
        slow_input_item = new javax.swing.JMenuItem();
        fast_input_item = new javax.swing.JMenuItem();
        output_item = new javax.swing.JMenuItem();
        net_menu = new javax.swing.JMenu();
        chat_menu = new javax.swing.JMenu();
        server_item = new javax.swing.JMenuItem();
        client_item = new javax.swing.JMenuItem();
        help_menu = new javax.swing.JMenu();
        doc_item = new javax.swing.JMenuItem();
        about_item = new javax.swing.JMenuItem();

        scheduling_frame.setTitle("Process Scheduling");
        scheduling_frame.setResizable(false);
        scheduling_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                scheduling_frameWindowClosing(evt);
            }
        });

        processes_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PID", "Arrival Time", "Burst Time", "Priority"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        processes_table.getTableHeader().setReorderingAllowed(false);
        scheduling_table_scroll_pane.setViewportView(processes_table);

        scheduler_button_group.add(fifo_radio);
        fifo_radio.setSelected(true);
        fifo_radio.setText("FIFO");

        scheduler_button_group.add(npsjf_radio);
        npsjf_radio.setText("SJF");

        scheduler_button_group.add(rr_radio);
        rr_radio.setText("RR");

        sched_btn.setText("Schedule");
        sched_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sched_btnActionPerformed(evt);
            }
        });

        edit_scheduler_table_row.setText("Edit Table");
        edit_scheduler_table_row.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edit_scheduler_table_rowActionPerformed(evt);
            }
        });

        sched_log_area.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));
        log_area_scroll_pane.setViewportView(sched_log_area);

        sched_rand_btn.setText("Randomize");
        sched_rand_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sched_rand_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout scheduling_frameLayout = new javax.swing.GroupLayout(scheduling_frame.getContentPane());
        scheduling_frame.getContentPane().setLayout(scheduling_frameLayout);
        scheduling_frameLayout.setHorizontalGroup(
            scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scheduling_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 470, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scheduling_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sched_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(scheduling_frameLayout.createSequentialGroup()
                        .addComponent(fifo_radio)
                        .addGap(18, 18, 18)
                        .addComponent(npsjf_radio)
                        .addGap(18, 18, 18)
                        .addComponent(rr_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(sched_rand_btn)
                        .addGap(18, 18, 18)
                        .addComponent(edit_scheduler_table_row)))
                .addContainerGap())
            .addComponent(log_area_scroll_pane)
        );
        scheduling_frameLayout.setVerticalGroup(
            scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scheduling_frameLayout.createSequentialGroup()
                .addComponent(scheduling_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(edit_scheduler_table_row)
                    .addComponent(fifo_radio)
                    .addComponent(npsjf_radio)
                    .addComponent(rr_radio)
                    .addComponent(sched_rand_btn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(sched_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(log_area_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        editing_table_frame.setTitle("Edit Table");
        editing_table_frame.setResizable(false);
        editing_table_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                editing_table_frameWindowClosing(evt);
            }
        });

        editing_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PID", "Arrival Time", "Burst Time", "Priority"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        editing_table.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        editing_table.setDropMode(javax.swing.DropMode.ON);
        editing_table.getTableHeader().setReorderingAllowed(false);
        editing_table_frame_scroll_pane.setViewportView(editing_table);

        ok_button.setText("OK");
        ok_button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ok_buttonActionPerformed(evt);
            }
        });

        new_row_btn.setText("New Row");
        new_row_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new_row_btnActionPerformed(evt);
            }
        });

        delete_row_btn.setText("Delete Row");
        delete_row_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_row_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout editing_table_frameLayout = new javax.swing.GroupLayout(editing_table_frame.getContentPane());
        editing_table_frame.getContentPane().setLayout(editing_table_frameLayout);
        editing_table_frameLayout.setHorizontalGroup(
            editing_table_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(editing_table_frame_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
            .addGroup(editing_table_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(editing_table_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(delete_row_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(new_row_btn, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ok_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        editing_table_frameLayout.setVerticalGroup(
            editing_table_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editing_table_frameLayout.createSequentialGroup()
                .addComponent(editing_table_frame_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 231, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(editing_table_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(editing_table_frameLayout.createSequentialGroup()
                        .addComponent(new_row_btn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(delete_row_btn))
                    .addComponent(ok_button, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        server_frame.setTitle("Server");
        server_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                server_frameWindowClosing(evt);
            }
        });

        server_chat_pane.setEditable(false);
        server_chat_pane.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        server_chat_pane.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        server_chat_panel.setViewportView(server_chat_pane);

        server_snd_btn.setText("Send");
        server_snd_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                server_snd_btnActionPerformed(evt);
            }
        });

        server_msg_field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                server_msg_fieldKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout server_frameLayout = new javax.swing.GroupLayout(server_frame.getContentPane());
        server_frame.getContentPane().setLayout(server_frameLayout);
        server_frameLayout.setHorizontalGroup(
            server_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(server_chat_panel)
            .addGroup(server_frameLayout.createSequentialGroup()
                .addComponent(server_msg_field, javax.swing.GroupLayout.DEFAULT_SIZE, 424, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(server_snd_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        server_frameLayout.setVerticalGroup(
            server_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(server_frameLayout.createSequentialGroup()
                .addComponent(server_chat_panel, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(server_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(server_snd_btn)
                    .addComponent(server_msg_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        client_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                client_frameWindowClosing(evt);
            }
        });

        client_chat_pane.setEditable(false);
        client_chat_pane.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        client_chat_pane.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        client_chat_panel.setViewportView(client_chat_pane);

        client_msg_field.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                client_msg_fieldKeyPressed(evt);
            }
        });

        client_snd_btn.setText("Send");
        client_snd_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                client_snd_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout client_frameLayout = new javax.swing.GroupLayout(client_frame.getContentPane());
        client_frame.getContentPane().setLayout(client_frameLayout);
        client_frameLayout.setHorizontalGroup(
            client_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(client_chat_panel)
            .addGroup(client_frameLayout.createSequentialGroup()
                .addComponent(client_msg_field, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(client_snd_btn)
                .addContainerGap())
        );
        client_frameLayout.setVerticalGroup(
            client_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(client_frameLayout.createSequentialGroup()
                .addComponent(client_chat_panel, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(client_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(client_msg_field, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(client_snd_btn))
                .addContainerGap())
        );

        about_frame.setTitle("About");
        about_frame.setResizable(false);

        name2.setFont(new java.awt.Font("Segoe Print", 1, 14)); // NOI18N
        name2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        name2.setText("Khalid Taha Ahmed");

        name3.setFont(new java.awt.Font("Segoe Print", 1, 14)); // NOI18N
        name3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        name3.setText("Eslam Khalid Tawfik");

        name1.setFont(new java.awt.Font("Segoe Print", 1, 14)); // NOI18N
        name1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        name1.setText("Ahmed Samir Demerdash");

        name4.setFont(new java.awt.Font("Segoe Print", 1, 14)); // NOI18N
        name4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        name4.setText("Rana Tarek Qunswa");

        name5.setFont(new java.awt.Font("Segoe Print", 1, 14)); // NOI18N
        name5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        name5.setText("AbdAlMoniem Osama AlHifnawy");

        logo_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        logo_label.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/logo.png"))); // NOI18N

        development_team_title_label.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        development_team_title_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        development_team_title_label.setText("Development Team:");

        javax.swing.GroupLayout about_frameLayout = new javax.swing.GroupLayout(about_frame.getContentPane());
        about_frame.getContentPane().setLayout(about_frameLayout);
        about_frameLayout.setHorizontalGroup(
            about_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(name2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(name3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(name4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(name5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(name1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(development_team_title_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(logo_label, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        about_frameLayout.setVerticalGroup(
            about_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, about_frameLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(logo_label, javax.swing.GroupLayout.PREFERRED_SIZE, 440, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(development_team_title_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(name1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(name2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(name3, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(name4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(name5))
        );

        output_frame.setTitle("Output");
        output_frame.setResizable(false);
        output_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                output_frameWindowClosing(evt);
            }
        });

        output_search_btn.setText("Search COM Ports");
        output_search_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                output_search_btnActionPerformed(evt);
            }
        });

        output_cnct_discnctBtn.setText("Connect to Port");
        output_cnct_discnctBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                output_cnct_discnctBtnActionPerformed(evt);
            }
        });

        statusLabel.setFont(new java.awt.Font("Consolas", 0, 14)); // NOI18N
        statusLabel.setText("Status: Disconnected");

        brightness_slider.setFont(new java.awt.Font("Tahoma", 1, 24)); // NOI18N
        brightness_slider.setMajorTickSpacing(5);
        brightness_slider.setMaximum(255);
        brightness_slider.setPaintTicks(true);
        brightness_slider.setValue(0);
        brightness_slider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        brightness_slider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                brightness_sliderStateChanged(evt);
            }
        });

        brightness_label.setFont(new java.awt.Font("Consolas", 1, 14)); // NOI18N
        brightness_label.setText("Brightness:");

        min_label.setText("min");

        max_label.setText("max");

        javax.swing.GroupLayout output_frameLayout = new javax.swing.GroupLayout(output_frame.getContentPane());
        output_frame.getContentPane().setLayout(output_frameLayout);
        output_frameLayout.setHorizontalGroup(
            output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(output_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(output_frameLayout.createSequentialGroup()
                        .addComponent(output_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(output_portsListComoBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(output_cnct_discnctBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)))
                    .addComponent(brightness_slider, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, output_frameLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(statusLabel))
                    .addGroup(output_frameLayout.createSequentialGroup()
                        .addComponent(brightness_label)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(output_frameLayout.createSequentialGroup()
                        .addComponent(min_label)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(max_label)))
                .addContainerGap())
        );
        output_frameLayout.setVerticalGroup(
            output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(output_frameLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(output_frameLayout.createSequentialGroup()
                        .addComponent(output_portsListComoBox, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(output_cnct_discnctBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(output_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(brightness_label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(output_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(min_label)
                    .addComponent(max_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(brightness_slider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addContainerGap())
        );

        slow_input_frame.setTitle("Slow Input");
        slow_input_frame.setResizable(false);

        slow_input_search_btn.setText("Search COM Ports");
        slow_input_search_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                slow_input_search_btnActionPerformed(evt);
            }
        });

        slow_input_cnct_discnctBtn.setText("Connect to Port");
        slow_input_cnct_discnctBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                slow_input_cnct_discnctBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout slow_input_frameLayout = new javax.swing.GroupLayout(slow_input_frame.getContentPane());
        slow_input_frame.getContentPane().setLayout(slow_input_frameLayout);
        slow_input_frameLayout.setHorizontalGroup(
            slow_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slow_input_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(slow_input_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(slow_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(slow_input_portsListComoBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(slow_input_cnct_discnctBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE))
                .addContainerGap())
        );
        slow_input_frameLayout.setVerticalGroup(
            slow_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(slow_input_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(slow_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(slow_input_frameLayout.createSequentialGroup()
                        .addComponent(slow_input_portsListComoBox, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(slow_input_cnct_discnctBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(slow_input_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fast_input_frame.setTitle("Fast Input");
        fast_input_frame.setResizable(false);

        fast_input_search_btn.setText("Search COM Ports");
        fast_input_search_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fast_input_search_btnActionPerformed(evt);
            }
        });

        fast_input_cnct_discnctBtn.setText("Connect to Port");
        fast_input_cnct_discnctBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fast_input_cnct_discnctBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout fast_input_frameLayout = new javax.swing.GroupLayout(fast_input_frame.getContentPane());
        fast_input_frame.getContentPane().setLayout(fast_input_frameLayout);
        fast_input_frameLayout.setHorizontalGroup(
            fast_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fast_input_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(fast_input_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(fast_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fast_input_portsListComoBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(fast_input_cnct_discnctBtn, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE))
                .addContainerGap())
        );
        fast_input_frameLayout.setVerticalGroup(
            fast_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(fast_input_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(fast_input_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(fast_input_frameLayout.createSequentialGroup()
                        .addComponent(fast_input_portsListComoBox, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fast_input_cnct_discnctBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fast_input_search_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        disk_scheduling_frame.setTitle("Disk Scheduling");
        disk_scheduling_frame.setResizable(false);

        disk_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PID", "Sector"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        disk_table.getTableHeader().setReorderingAllowed(false);
        disk_scheduling_table_scroll_pane.setViewportView(disk_table);

        disk_scheduler_button_group.add(sstf_radio);
        sstf_radio.setSelected(true);
        sstf_radio.setText("SSTF");

        disk_scheduler_button_group.add(clook_radio);
        clook_radio.setText("C Look");

        disk_edit_table.setText("Edit Table");
        disk_edit_table.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disk_edit_tableActionPerformed(evt);
            }
        });

        disk_sched_btn.setText("Schedule");
        disk_sched_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disk_sched_btnActionPerformed(evt);
            }
        });

        disk_log_area.setEditable(false);
        disk_log_area.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));
        disk_log_area.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        disk_log_area_scroll_pane.setViewportView(disk_log_area);

        disk_scheduler_button_group.add(cmp_radio);
        cmp_radio.setText("Compare Both");

        disk_sched_rand_btn.setText("Randomize");
        disk_sched_rand_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disk_sched_rand_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout disk_scheduling_frameLayout = new javax.swing.GroupLayout(disk_scheduling_frame.getContentPane());
        disk_scheduling_frame.getContentPane().setLayout(disk_scheduling_frameLayout);
        disk_scheduling_frameLayout.setHorizontalGroup(
            disk_scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(disk_log_area_scroll_pane, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(disk_scheduling_table_scroll_pane)
            .addGroup(disk_scheduling_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(disk_scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(disk_sched_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(disk_scheduling_frameLayout.createSequentialGroup()
                        .addComponent(sstf_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(clook_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmp_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(disk_sched_rand_btn)
                        .addGap(18, 18, 18)
                        .addComponent(disk_edit_table)))
                .addContainerGap())
        );
        disk_scheduling_frameLayout.setVerticalGroup(
            disk_scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(disk_scheduling_frameLayout.createSequentialGroup()
                .addComponent(disk_scheduling_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(disk_scheduling_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sstf_radio)
                    .addComponent(clook_radio)
                    .addComponent(disk_edit_table)
                    .addComponent(cmp_radio)
                    .addComponent(disk_sched_rand_btn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(disk_sched_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disk_log_area_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        deadlock_frame.setTitle("Deadlock");
        deadlock_frame.setResizable(false);

        deadlock_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PID", "Need A", "Need B", "Need C"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        deadlock_table.getTableHeader().setReorderingAllowed(false);
        disk_scheduling_table_scroll_pane1.setViewportView(deadlock_table);

        deadlock_button_group.add(prevention_radio);
        prevention_radio.setSelected(true);
        prevention_radio.setText("Detection");

        deadlock_button_group.add(avoidance_radio);
        avoidance_radio.setText("Avoidance");

        deadlock_edit_table.setText("Edit Table");
        deadlock_edit_table.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deadlock_edit_tableActionPerformed(evt);
            }
        });

        deadlock_chk_btn.setText("Check");
        deadlock_chk_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deadlock_chk_btnActionPerformed(evt);
            }
        });

        deadlock_log_area.setEditable(false);
        deadlock_log_area.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));
        deadlock_log_area.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        disk_log_area_scroll_pane1.setViewportView(deadlock_log_area);

        deadlock_sched_rand_btn.setText("Randomize");
        deadlock_sched_rand_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deadlock_sched_rand_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout deadlock_frameLayout = new javax.swing.GroupLayout(deadlock_frame.getContentPane());
        deadlock_frame.getContentPane().setLayout(deadlock_frameLayout);
        deadlock_frameLayout.setHorizontalGroup(
            deadlock_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(disk_log_area_scroll_pane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(disk_scheduling_table_scroll_pane1)
            .addGroup(deadlock_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deadlock_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deadlock_chk_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(deadlock_frameLayout.createSequentialGroup()
                        .addComponent(prevention_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(avoidance_radio)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(deadlock_sched_rand_btn)
                        .addGap(18, 18, 18)
                        .addComponent(deadlock_edit_table)))
                .addContainerGap())
        );
        deadlock_frameLayout.setVerticalGroup(
            deadlock_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deadlock_frameLayout.createSequentialGroup()
                .addComponent(disk_scheduling_table_scroll_pane1, javax.swing.GroupLayout.DEFAULT_SIZE, 260, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(deadlock_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(prevention_radio)
                    .addComponent(avoidance_radio)
                    .addComponent(deadlock_edit_table)
                    .addComponent(deadlock_sched_rand_btn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(deadlock_chk_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(disk_log_area_scroll_pane1, javax.swing.GroupLayout.PREFERRED_SIZE, 132, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        memory_frame.setTitle("Memory");
        memory_frame.setResizable(false);
        memory_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                memory_frameWindowClosing(evt);
            }
        });

        memory_table.setAutoCreateRowSorter(true);
        memory_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Frame Number", "PID", "Data", "Usage", "Allocation Time (ms)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        memory_table.setDropMode(javax.swing.DropMode.ON);
        memory_table.setRowSelectionAllowed(false);
        memory_table.getTableHeader().setReorderingAllowed(false);
        memory_table_scroll_pane.setViewportView(memory_table);

        memory_button_group.add(memory_lru_radio);
        memory_lru_radio.setText("LRU");

        memory_button_group.add(memory_fifo_radio);
        memory_fifo_radio.setSelected(true);
        memory_fifo_radio.setText("FIFO");

        javax.swing.GroupLayout memory_frameLayout = new javax.swing.GroupLayout(memory_frame.getContentPane());
        memory_frame.getContentPane().setLayout(memory_frameLayout);
        memory_frameLayout.setHorizontalGroup(
            memory_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(memory_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 471, Short.MAX_VALUE)
            .addGroup(memory_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(memory_lru_radio)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(memory_fifo_radio)
                .addContainerGap())
        );
        memory_frameLayout.setVerticalGroup(
            memory_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(memory_frameLayout.createSequentialGroup()
                .addComponent(memory_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(memory_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memory_lru_radio)
                    .addComponent(memory_fifo_radio))
                .addContainerGap())
        );

        processes_frame.setTitle("Processes");
        processes_frame.setResizable(false);
        processes_frame.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                processes_frameWindowClosing(evt);
            }
        });

        processes_memory_table.setAutoCreateRowSorter(true);
        processes_memory_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "PID", "Page Count"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        processes_memory_table.getTableHeader().setReorderingAllowed(false);
        processes_table_scroll_pane.setViewportView(processes_memory_table);

        page_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Page Number", "Frame Number"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        page_table.getTableHeader().setReorderingAllowed(false);
        page_table_scroll_pane.setViewportView(page_table);

        add_process_btn.setText("Add Process");
        add_process_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_process_btnActionPerformed(evt);
            }
        });

        save_data_btn.setText("Edit Data");
        save_data_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save_data_btnActionPerformed(evt);
            }
        });

        processes_label.setFont(new java.awt.Font("Consolas", 1, 12)); // NOI18N
        processes_label.setText("Processes:");

        page_table_label.setFont(new java.awt.Font("Consolas", 1, 12)); // NOI18N
        page_table_label.setText("Page Table:");

        proc_data_rand_btn.setText("Randomize Data");
        proc_data_rand_btn.setEnabled(false);
        proc_data_rand_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proc_data_rand_btnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout processes_frameLayout = new javax.swing.GroupLayout(processes_frame.getContentPane());
        processes_frame.getContentPane().setLayout(processes_frameLayout);
        processes_frameLayout.setHorizontalGroup(
            processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processes_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(processes_frameLayout.createSequentialGroup()
                        .addComponent(add_process_btn)
                        .addGap(18, 18, 18)
                        .addComponent(proc_data_rand_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(save_data_btn))
                    .addComponent(processes_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(processes_frameLayout.createSequentialGroup()
                        .addComponent(processes_label)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addGap(18, 18, 18)
                .addGroup(processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(page_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(page_table_label))
                .addContainerGap())
        );
        processes_frameLayout.setVerticalGroup(
            processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(processes_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(processes_label)
                    .addComponent(page_table_label))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(processes_frameLayout.createSequentialGroup()
                        .addComponent(processes_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(processes_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(save_data_btn)
                            .addComponent(add_process_btn)
                            .addComponent(proc_data_rand_btn))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(page_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        sync_frame.setTitle("Synchronization");
        sync_frame.setResizable(false);

        sync_button_group.add(bounded_radio);
        bounded_radio.setSelected(true);
        bounded_radio.setText("Bounded Buffer");
        bounded_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bounded_radioActionPerformed(evt);
            }
        });

        sync_button_group.add(rw_radio);
        rw_radio.setText("Reader / Writer");
        rw_radio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rw_radioActionPerformed(evt);
            }
        });

        current_label.setFont(new java.awt.Font("Consolas", 1, 24)); // NOI18N
        current_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        pw_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Producers"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        pw_table.getTableHeader().setReorderingAllowed(false);
        pw_table_scroll_pane.setViewportView(pw_table);

        pw_add_btn.setText("Add Producer");
        pw_add_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pw_add_btnActionPerformed(evt);
            }
        });

        cr_add_btn.setText("Add Consumer");
        cr_add_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cr_add_btnActionPerformed(evt);
            }
        });

        pw_rem_btn.setText("Remove Producer");
        pw_rem_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pw_rem_btnActionPerformed(evt);
            }
        });

        cr_rem_btn.setText("Remove Consumer");
        cr_rem_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cr_rem_btnActionPerformed(evt);
            }
        });

        buffer_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Buffer"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        buffer_table.getTableHeader().setReorderingAllowed(false);
        buffer_table_scroll_pane.setViewportView(buffer_table);

        cr_table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Consumers"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        cr_table.getTableHeader().setReorderingAllowed(false);
        cr_table_scroll_pane.setViewportView(cr_table);

        rw_label.setFont(new java.awt.Font("Consolas", 1, 18)); // NOI18N
        rw_label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout sync_frameLayout = new javax.swing.GroupLayout(sync_frame.getContentPane());
        sync_frame.getContentPane().setLayout(sync_frameLayout);
        sync_frameLayout.setHorizontalGroup(
            sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sync_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(sync_frameLayout.createSequentialGroup()
                        .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(bounded_radio)
                            .addComponent(rw_radio)
                            .addComponent(pw_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(sync_frameLayout.createSequentialGroup()
                                .addComponent(buffer_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cr_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(current_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(rw_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(sync_frameLayout.createSequentialGroup()
                        .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pw_add_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cr_add_btn, javax.swing.GroupLayout.DEFAULT_SIZE, 149, Short.MAX_VALUE))
                        .addGap(154, 154, 154)
                        .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(pw_rem_btn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cr_rem_btn, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        sync_frameLayout.setVerticalGroup(
            sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(sync_frameLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(bounded_radio)
                    .addComponent(current_label, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(rw_label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(rw_radio, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(7, 7, 7)
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(buffer_table_scroll_pane, javax.swing.GroupLayout.DEFAULT_SIZE, 201, Short.MAX_VALUE)
                    .addComponent(pw_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(cr_table_scroll_pane, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(21, 21, 21)
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(pw_add_btn)
                    .addComponent(pw_rem_btn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(sync_frameLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cr_add_btn)
                    .addComponent(cr_rem_btn))
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("iCode");
        setResizable(false);

        mem_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/memory.png"))); // NOI18N
        mem_btn.setToolTipText("Memory Management");
        mem_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mem_btnActionPerformed(evt);
            }
        });

        memory_.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/network.png"))); // NOI18N
        memory_.setToolTipText("Network");
        memory_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                memory_ActionPerformed(evt);
            }
        });

        proc_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/process.png"))); // NOI18N
        proc_btn.setToolTipText("Processes");
        proc_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proc_btnActionPerformed(evt);
            }
        });

        io_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/io.png"))); // NOI18N
        io_btn.setToolTipText("Input/Output");
        io_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                io_btnActionPerformed(evt);
            }
        });

        file_sys_btn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/main_Package/Pics/disk.jpg"))); // NOI18N
        file_sys_btn.setToolTipText("File System");
        file_sys_btn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                file_sys_btnActionPerformed(evt);
            }
        });

        file_menu.setText("File");

        exit_item.setText("Exit");
        exit_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exit_itemActionPerformed(evt);
            }
        });
        file_menu.add(exit_item);

        main_menu_bar.add(file_menu);

        proc_menu_item.setText("Processes");

        sched_item.setText("Scheduling");
        sched_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sched_itemActionPerformed(evt);
            }
        });
        proc_menu_item.add(sched_item);

        sync_item.setText("Synchronization");
        sync_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sync_itemActionPerformed(evt);
            }
        });
        proc_menu_item.add(sync_item);

        deadlock_item.setText("Deadlock");
        deadlock_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deadlock_itemActionPerformed(evt);
            }
        });
        proc_menu_item.add(deadlock_item);

        main_menu_bar.add(proc_menu_item);

        mem_menu.setText("Memory");

        paging_item.setText("Paging");
        paging_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                paging_itemActionPerformed(evt);
            }
        });
        mem_menu.add(paging_item);

        main_menu_bar.add(mem_menu);

        file_sys_menu.setText("File System");

        disk_sched_item.setText("Disk Scheduling");
        disk_sched_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disk_sched_itemActionPerformed(evt);
            }
        });
        file_sys_menu.add(disk_sched_item);

        main_menu_bar.add(file_sys_menu);

        io_menu.setText("I/O");

        slow_input_item.setText("Slow Input");
        slow_input_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                slow_input_itemActionPerformed(evt);
            }
        });
        io_menu.add(slow_input_item);

        fast_input_item.setText("Fast Input");
        fast_input_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fast_input_itemActionPerformed(evt);
            }
        });
        io_menu.add(fast_input_item);

        output_item.setText("Output");
        output_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                output_itemActionPerformed(evt);
            }
        });
        io_menu.add(output_item);

        main_menu_bar.add(io_menu);

        net_menu.setText("Network");

        chat_menu.setText("Simple Chat");

        server_item.setText("Server");
        server_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                server_itemActionPerformed(evt);
            }
        });
        chat_menu.add(server_item);

        client_item.setText("Client");
        client_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                client_itemActionPerformed(evt);
            }
        });
        chat_menu.add(client_item);

        net_menu.add(chat_menu);

        main_menu_bar.add(net_menu);

        help_menu.setText("Help");

        doc_item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_MASK));
        doc_item.setText("Documentation");
        help_menu.add(doc_item);

        about_item.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        about_item.setText("About");
        about_item.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                about_itemActionPerformed(evt);
            }
        });
        help_menu.add(about_item);

        main_menu_bar.add(help_menu);

        setJMenuBar(main_menu_bar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proc_btn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(io_btn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(memory_, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(mem_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(file_sys_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mem_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proc_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(file_sys_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(memory_, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(io_btn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sched_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sched_btnActionPerformed
        try {
            clear_table(proc_table_model);
            String buttonText = get_selected_button_text(scheduler_button_group);
            LinkedList<temp_process> temp = new LinkedList<>();
            if (buttonText.toLowerCase().contains("fifo")) {
                ready_queue = new Scheduler(ready_queue, sched_log_area).sort(Scheduler.FIFO);
                populate_table(proc_table_model, ready_queue, this.PROC_TABLE);
            } else if (buttonText.toLowerCase().contains("sjf")) {
                ready_queue = new Scheduler(ready_queue, sched_log_area).sort(Scheduler.SJF);
                populate_table(proc_table_model, ready_queue, this.PROC_TABLE);
            } else if (buttonText.toLowerCase().contains("rr")) {
                temp.clear();
                ready_queue.forEach(i -> temp.add(new temp_process(i.PID, i.Burst_Time, i.Arrival_Time, i.Priority)));
                ready_queue = new Scheduler(ready_queue, sched_log_area).sort(Scheduler.RR, 40);
                populate_table(proc_table_model, ready_queue, this.PROC_TABLE);
                ready_queue.clear();
                temp.forEach(i -> ready_queue.add(new Process(i.PID, i.Burst_Time, i.Arrival_Time, i.Priority)));
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }//GEN-LAST:event_sched_btnActionPerformed

    private void edit_scheduler_table_rowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edit_scheduler_table_rowActionPerformed
        scheduling_frame.setVisible(false);
        editing_table_number = this.PROC_TABLE;
        clear_table(editing_table_model);

        String[] colNames = {"PID", "Arrival Time", "Burst Time", "Priority"};

        JTableHeader th = editing_table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        for (int i = 0; i < colNames.length; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setHeaderValue(colNames[i]);
        }
        th.repaint();

        for (int i = 0; i < proc_table_model.getRowCount(); i++) {
            Object[] data = new Object[4];
            for (int j = 0; j < proc_table_model.getColumnCount(); j++) {
                data[j] = proc_table_model.getValueAt(i, j);
            }
            editing_table_model.addRow(data);
        }
        editing_table_frame.setSize(500, 350);
        editing_table_frame.setVisible(true);
    }//GEN-LAST:event_edit_scheduler_table_rowActionPerformed

    private void new_row_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_new_row_btnActionPerformed
        editing_table_model.addRow(new Object[4]);
    }//GEN-LAST:event_new_row_btnActionPerformed

    private void ok_buttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ok_buttonActionPerformed
        ready_queue.clear();
        ready_queue_map.clear();
        if (editing_table_number == this.PROC_TABLE) {
            save_changed_table(this.PROC_TABLE);
            scheduling_frame.setVisible(true);
        } else if (editing_table_number == this.DISK_TABLE) {
            save_changed_table(this.DISK_TABLE);
            disk_scheduling_frame.setVisible(true);
        } else if (editing_table_number == this.DEADLOCK_TABLE) {
            save_changed_table(this.DEADLOCK_TABLE);
            deadlock_frame.setVisible(true);
        }
    }//GEN-LAST:event_ok_buttonActionPerformed

    private void editing_table_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_editing_table_frameWindowClosing
        editing_table_frame.setVisible(false);
        if (editing_table_number == this.PROC_TABLE) {
            scheduling_frame.setVisible(true);
        } else if (editing_table_number == this.DISK_TABLE) {
            disk_scheduling_frame.setVisible(true);
        } else if (editing_table_number == this.DEADLOCK_TABLE) {
            deadlock_frame.setVisible(true);
        }
    }//GEN-LAST:event_editing_table_frameWindowClosing

    private void delete_row_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_row_btnActionPerformed
        editing_table_model.removeRow(editing_table.getSelectedRow());
    }//GEN-LAST:event_delete_row_btnActionPerformed

    private void sched_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sched_itemActionPerformed
        ready_queue.clear();
        clear_table(proc_table_model);
        clear_table(editing_table_model);
        for (int i = 0; i < 15; i++) {
            ready_queue.add(new Process(i,
                    new Random().nextInt(100),
                    new Random().nextInt(30),
                    new Random().nextInt(6)));
        }

        populate_table(proc_table_model, ready_queue, this.PROC_TABLE);
        scheduling_frame.setSize(500, 550);
        scheduling_frame.setVisible(true);
    }//GEN-LAST:event_sched_itemActionPerformed

    private void server_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_server_itemActionPerformed
        if (!server_started) {
            server_frame.setSize(500, 400);
            server_frame.setVisible(true);
            server_msg_field.setEnabled(false);
            server_snd_btn.setEnabled(false);
            server_chat_pane.setText(null);
            server_started = true;
            server = new Server(2000, server_msg_field, server_chat_pane, server_snd_btn);
            server.accept_connections();
        } else {
            JOptionPane.showMessageDialog(this, "Server is already started", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_server_itemActionPerformed

    private void server_msg_fieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_server_msg_fieldKeyPressed
        if (evt.getKeyCode() == 10) {   //enter key is pressed
            String line = server_msg_field.getText();
            if (line.length() != 0) {
                server.send_message(line, false);
            }
        }
    }//GEN-LAST:event_server_msg_fieldKeyPressed

    private void client_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_client_itemActionPerformed
        try {
            if (!client_connected) {
                String user_input = JOptionPane.showInputDialog(this, "Enter server ip address and port number separated by \":\": ", "Server IP:Port number", JOptionPane.PLAIN_MESSAGE);
                if (user_input != null) {
                    String[] ip_port = user_input.split(":");
                    client = new Client(new Socket(ip_port[0], Integer.parseInt(ip_port[1])), client_chat_pane);
                    client_frame.setSize(500, 400);
                    client_frame.setVisible(true);
                    client_frame.setTitle(client.get_socket().getInetAddress().toString() + "::" + client.get_id());
                    client_chat_pane.setText(null);
                    client_connected = true;
                    client.read();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Client is already connected", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println(ex.toString());
            client_connected = false;
        } catch (ArrayIndexOutOfBoundsException ex) {
            JOptionPane.showMessageDialog(this, "Invalid IP and Port address", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println(ex.toString());
            client_connected = false;
        } catch (HeadlessException | NumberFormatException ex) {
            System.err.println(ex.toString());
            client_connected = false;
        }
    }//GEN-LAST:event_client_itemActionPerformed

    private void client_msg_fieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_client_msg_fieldKeyPressed
        if (evt.getKeyCode() == 10) {   //enter key is pressed
            String line = client_msg_field.getText();
            if (line.length() != 0) {
                client.send(line);
                client_msg_field.setText(null);
            }
        }
    }//GEN-LAST:event_client_msg_fieldKeyPressed

    private void exit_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exit_itemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exit_itemActionPerformed

    private void server_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_server_frameWindowClosing
        server.end_connections();
        server_started = false;
    }//GEN-LAST:event_server_frameWindowClosing

    private void client_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_client_frameWindowClosing
        client.send("::client::quit::" + client.get_id());
        client_connected = false;
    }//GEN-LAST:event_client_frameWindowClosing

    private void deadlock_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deadlock_itemActionPerformed
        ready_queue.clear();
        for (int i = 0; i < 15; i++) {
            ready_queue.add(new Process(i));
        }

        populate_table(deadlock_table_model, ready_queue, this.DEADLOCK_TABLE);
        deadlock_frame.setSize(500, 550);
        deadlock_frame.setVisible(true);
    }//GEN-LAST:event_deadlock_itemActionPerformed

    private void memory_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_memory_ActionPerformed
        int choice = JOptionPane.showOptionDialog(this,
                "Run Client or Server application ?",
                "Choose a Program",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[]{
                    "Server", "Client", "Cancel"
                },
                "Server");
        if (choice == JOptionPane.YES_OPTION) {
            server_itemActionPerformed(evt);
        } else if (choice == JOptionPane.NO_OPTION) {
            client_itemActionPerformed(evt);
        }
    }//GEN-LAST:event_memory_ActionPerformed

    private void about_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_about_itemActionPerformed
        int screen_width = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screen_height = Toolkit.getDefaultToolkit().getScreenSize().height;

        about_frame.setLocation(screen_width / 3, screen_height / 8);
        about_frame.setSize(280, 650);
        about_frame.setVisible(true);
    }//GEN-LAST:event_about_itemActionPerformed

    private void output_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_itemActionPerformed
        output_frame.setSize(520, 220);
        output_frame.setVisible(true);
        brightness_slider.setEnabled(false);
    }//GEN-LAST:event_output_itemActionPerformed

    private void output_search_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_search_btnActionPerformed
        output_portsListComoBox.removeAllItems();
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        portsList.clear();

        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            output_portsListComoBox.addItem(currPortId.getName());
            portsList.add(currPortId);
        }
    }//GEN-LAST:event_output_search_btnActionPerformed

    private void output_cnct_discnctBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_output_cnct_discnctBtnActionPerformed
        if (!op_connected) {
            try {
                portId = portsList.get(output_portsListComoBox.getSelectedIndex());
                serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
                output_cnct_discnctBtn.setText("Disconnect from Port");
                statusLabel.setText("Status: Connected");
                brightness_slider.setEnabled(true);
                op_connected = !op_connected;
                serialPort.setSerialPortParams(DATA_RATE,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
                output = serialPort.getOutputStream();
            } catch (NullPointerException e) {
                System.err.println(e.toString());
            } catch (UnsupportedCommOperationException | PortInUseException | IOException ex) {
                System.err.println(ex.toString());
            }
        } else {
            close();
            output_cnct_discnctBtn.setText("Connect to Port");
            statusLabel.setText("Status: Disonnected");
            brightness_slider.setEnabled(false);
            op_connected = !op_connected;
        }
    }//GEN-LAST:event_output_cnct_discnctBtnActionPerformed

    private void brightness_sliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_brightness_sliderStateChanged
        try {
            if (op_connected) {
                if (!brightness_slider.getValueIsAdjusting()) {
                    String value = String.valueOf(brightness_slider.getValue());
                    System.out.println(value);
                    output.write(value.getBytes());
                    output.flush();
                }
            }
        } catch (IOException ex) {
            System.err.println(ex.toString());
        }
    }//GEN-LAST:event_brightness_sliderStateChanged

    private void output_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_output_frameWindowClosing
        if (op_connected) {
            close();
        }
    }//GEN-LAST:event_output_frameWindowClosing

    private void slow_input_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slow_input_itemActionPerformed
        slow_input_frame.setSize(520, 130);
        slow_input_frame.setVisible(true);
    }//GEN-LAST:event_slow_input_itemActionPerformed

    private void slow_input_search_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slow_input_search_btnActionPerformed
        slow_input_portsListComoBox.removeAllItems();
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        portsList.clear();

        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            slow_input_portsListComoBox.addItem(currPortId.getName());
            portsList.add(currPortId);
        }
    }//GEN-LAST:event_slow_input_search_btnActionPerformed

    @SuppressWarnings("empty-statement")
    private void slow_input_cnct_discnctBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slow_input_cnct_discnctBtnActionPerformed
        if (!ip_connected) {
            String com_num = slow_input_portsListComoBox.getSelectedItem().toString();
            ip_connected = !ip_connected;
            slow_input_cnct_discnctBtn.setEnabled(false);
            slow_input_cnct_discnctBtn.setText("Connected");
            new Thread(()
                    -> {
                try {
                    String[] cmd = new String[]{};
                    if (info.getName().toLowerCase().equals("windows")) {
                        cmd = new String[]{
                            "cmd", "/c", "python scripts_and_helpers\\io_graphing\\slow\\plotter.py " + com_num
                        };
                    } else {
                        cmd = new String[]{
                            "/bin/sh", "-c", "python scripts_and_helpers/io_graphing/slow/plotter.py " + com_num
                        };
                    }

                    graphing_process = new ProcessBuilder(cmd).start();
                    while (graphing_process.isAlive()) ;
                    slow_input_cnct_discnctBtn.setEnabled(true);
                    slow_input_cnct_discnctBtn.setText("Connect to Port");
                    ip_connected = !ip_connected;
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                }
            }).start();
        } else {
            slow_input_cnct_discnctBtn.setEnabled(true);
            slow_input_cnct_discnctBtn.setText("Connect to Port");
            ip_connected = !ip_connected;
        }
    }//GEN-LAST:event_slow_input_cnct_discnctBtnActionPerformed

    private void server_snd_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_server_snd_btnActionPerformed
        server_msg_fieldKeyPressed(new java.awt.event.KeyEvent(this, 0, 0, 0, 10));
    }//GEN-LAST:event_server_snd_btnActionPerformed

    private void client_snd_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_client_snd_btnActionPerformed
        client_msg_fieldKeyPressed(new java.awt.event.KeyEvent(this, 0, 0, 0, 10));
    }//GEN-LAST:event_client_snd_btnActionPerformed

    private void fast_input_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fast_input_itemActionPerformed
        fast_input_frame.setSize(520, 130);
        fast_input_frame.setVisible(true);
        fast_input_portsListComoBox.removeAllItems();
        fast_input_cnct_discnctBtn.setText("Connect to PORT");
        fast_input_cnct_discnctBtn.setEnabled(true);
    }//GEN-LAST:event_fast_input_itemActionPerformed

    private void fast_input_search_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fast_input_search_btnActionPerformed
        fast_input_portsListComoBox.removeAllItems();
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        portsList.clear();

        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            fast_input_portsListComoBox.addItem(currPortId.getName());
            portsList.add(currPortId);
        }
    }//GEN-LAST:event_fast_input_search_btnActionPerformed

    private void fast_input_cnct_discnctBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fast_input_cnct_discnctBtnActionPerformed
        if (!ip_connected) {
            String com_num = fast_input_portsListComoBox.getSelectedItem().toString();
            ip_connected = !ip_connected;
            fast_input_cnct_discnctBtn.setEnabled(false);
            fast_input_cnct_discnctBtn.setText("Connected");
            new Thread(()
                    -> {
                try {
                    String[] cmd = new String[]{};
                    if (info.getName().toLowerCase().equals("windows")) {
                        cmd = new String[]{
                            "cmd", "/c", "graphing.bat " + com_num
                        };
                    } else {
                        cmd = new String[]{
                            "/bin/sh", "-c", "scripts_and_helpers/io_graphing/fast/linux/p_oscilloscope " + com_num
                        };
                    }
                    graphing_process = new ProcessBuilder(cmd).start();
                    ip_connected = !ip_connected;
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                }
            }).start();
        } else {
            fast_input_cnct_discnctBtn.setEnabled(true);
            fast_input_cnct_discnctBtn.setText("Connect to Port");
            ip_connected = !ip_connected;
        }
    }//GEN-LAST:event_fast_input_cnct_discnctBtnActionPerformed

    private void disk_edit_tableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disk_edit_tableActionPerformed
        disk_scheduling_frame.setVisible(false);
        editing_table_number = this.DISK_TABLE;
        clear_table(editing_table_model);

        String[] colNames = {"PID", "Sector", null, null};

        JTableHeader th = editing_table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        for (int i = 0; i < colNames.length; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setHeaderValue(colNames[i]);
        }
        th.repaint();

        for (int i = 0; i < disk_table_model.getRowCount(); i++) {
            Object[] data = new Object[2];
            for (int j = 0; j < disk_table_model.getColumnCount(); j++) {
                data[j] = disk_table_model.getValueAt(i, j);
            }
            editing_table_model.addRow(data);
        }
        editing_table_frame.setSize(500, 350);
        editing_table_frame.setVisible(true);
    }//GEN-LAST:event_disk_edit_tableActionPerformed

    private void scheduling_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_scheduling_frameWindowClosing

    }//GEN-LAST:event_scheduling_frameWindowClosing

    private void disk_sched_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disk_sched_btnActionPerformed
        String result = JOptionPane.showInputDialog(this, "What is the current head position ?\nLeave empty for randomising.",
                "Head Position", JOptionPane.QUESTION_MESSAGE);

        if (result.trim().length() > 0) {
            try {
                int head_position = Integer.parseInt(result);
                if (head_position < 0) {
                    throw new NumberFormatException();
                }
                if (is_map_empty) {
                    is_map_empty = false;
                    ready_queue.forEach(i -> ready_queue_map.put(i.PID, i.Sector));
                    show_disk_results_with_graph(head_position);
                } else {
                    ready_queue.clear();
                    Iterator it = ready_queue_map.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
                    }
                    show_disk_results_with_graph(head_position);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Head position must be a positive integer !!!",
                        "Position Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Head position must be a positive integer !!!");
            }
        } else if (is_map_empty) {
            is_map_empty = false;
            ready_queue.forEach(i -> ready_queue_map.put(i.PID, i.Sector));
            show_disk_results_with_graph();
        } else {
            ready_queue.clear();
            Iterator it = ready_queue_map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                ready_queue.add(new Process((int) pair.getKey(), (int) pair.getValue()));
            }
            show_disk_results_with_graph();
        }
    }//GEN-LAST:event_disk_sched_btnActionPerformed

    private void disk_sched_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disk_sched_itemActionPerformed
        ready_queue.clear();
        clear_table(disk_table_model);
        clear_table(editing_table_model);
        for (int i = 0; i < 15; i++) {
            ready_queue.add(new Process(i, Math.abs(new Random().nextInt() % 200)));
        }

        populate_table(disk_table_model, ready_queue, this.DISK_TABLE);
        disk_scheduling_frame.setSize(500, 550);
        disk_scheduling_frame.setVisible(true);
    }//GEN-LAST:event_disk_sched_itemActionPerformed

    private void sched_rand_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sched_rand_btnActionPerformed
        ready_queue.clear();
        int length = proc_table_model.getRowCount();
        clear_table(proc_table_model);
        clear_table(editing_table_model);
        for (int i = 0; i < length; i++) {
            ready_queue.add(new Process(i,
                    new Random().nextInt(100),
                    new Random().nextInt(30),
                    new Random().nextInt(6)));
        }

        populate_table(proc_table_model, ready_queue, this.PROC_TABLE);
    }//GEN-LAST:event_sched_rand_btnActionPerformed

    private void disk_sched_rand_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disk_sched_rand_btnActionPerformed
        ready_queue.clear();
        int length = disk_table_model.getRowCount();
        clear_table(disk_table_model);
        clear_table(editing_table_model);
        for (int i = 0; i < length; i++) {
            ready_queue.add(new Process(i, Math.abs(new Random().nextInt() % 200)));
        }

        populate_table(disk_table_model, ready_queue, this.DISK_TABLE);
    }//GEN-LAST:event_disk_sched_rand_btnActionPerformed

    private void file_sys_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_file_sys_btnActionPerformed
        disk_sched_itemActionPerformed(evt);
    }//GEN-LAST:event_file_sys_btnActionPerformed

    private void io_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_io_btnActionPerformed
        String choice = JOptionPane.showInputDialog(this,
                "1. Slow Input\n2. Fast Input\n3. Output",
                "Choose a Program",
                JOptionPane.QUESTION_MESSAGE);
        try {
            int ch = Integer.MIN_VALUE;
            if (choice != null) {
                ch = Integer.parseInt(choice);
            }
            switch (ch) {
                case Integer.MIN_VALUE:
                    break;
                case 1:
                    slow_input_itemActionPerformed(evt);
                    break;
                case 2:
                    fast_input_itemActionPerformed(evt);
                    break;
                case 3:
                    output_itemActionPerformed(evt);
                    break;
                default:
                    throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Choose a valid program.\nEnter a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Choose a valid program");
        }
    }//GEN-LAST:event_io_btnActionPerformed

    private void proc_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proc_btnActionPerformed
        String choice = JOptionPane.showInputDialog(this,
                "1. Scheduling\n2. Synchronization\n3. Deadlock",
                "Choose a Program",
                JOptionPane.QUESTION_MESSAGE);
        try {
            int ch = Integer.MIN_VALUE;
            if (choice != null) {
                ch = Integer.parseInt(choice);
            }
            switch (ch) {
                case Integer.MIN_VALUE:
                    break;
                case 1:
                    sched_itemActionPerformed(evt);
                    break;
                case 2:
                    sync_itemActionPerformed(evt);
                    break;
                case 3:
                    deadlock_itemActionPerformed(evt);
                    break;
                default:
                    throw new NumberFormatException();
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Choose a valid program.\nEnter a positive integer.", "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Choose a valid program");
        }
    }//GEN-LAST:event_proc_btnActionPerformed

    private void sync_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sync_itemActionPerformed
        sync_frame.setSize(475, 370);
        sync_frame.setVisible(true);
    }//GEN-LAST:event_sync_itemActionPerformed

    private void deadlock_edit_tableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deadlock_edit_tableActionPerformed
        deadlock_frame.setVisible(false);
        editing_table_number = this.DEADLOCK_TABLE;
        clear_table(editing_table_model);

        String[] colNames = {"PID", "Need A", "Need B", "Need C"};

        JTableHeader th = editing_table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        for (int i = 0; i < colNames.length; i++) {
            TableColumn tc = tcm.getColumn(i);
            tc.setHeaderValue(colNames[i]);
        }
        th.repaint();

        for (int i = 0; i < deadlock_table_model.getRowCount(); i++) {
            Object[] data = new Object[4];
            for (int j = 0; j < deadlock_table_model.getColumnCount(); j++) {
                data[j] = deadlock_table_model.getValueAt(i, j);
            }
            editing_table_model.addRow(data);
        }
        editing_table_frame.setSize(500, 350);
        editing_table_frame.setVisible(true);
    }//GEN-LAST:event_deadlock_edit_tableActionPerformed

    private void deadlock_chk_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deadlock_chk_btnActionPerformed
        String buttonText = get_selected_button_text(deadlock_button_group);
        if (buttonText.toLowerCase().contains("detection")) {
            String choice = JOptionPane.showInputDialog(this, "Enter the available resources separated by spaces");
            ArrayList<Integer> resources = new ArrayList<>();
            try {
                if (choice.trim().length() > 0) {
                    String[] temp = choice.trim().split(" ");
                    for (String i : temp) {
                        resources.add(Integer.parseInt(i));
                    }
                    if (temp.length != 3) {
                        throw new NumberFormatException();
                    }
                    ready_queue.forEach(i
                            -> {
                        System.out.printf("PID: %d\tNeed A: %d\tNeed B: %d\tNeed C: %d\n",
                                i.PID, i.Need.get_A(), i.Need.get_B(), i.Need.get_C());
                    });

                    System.out.println("\nChecking for Deadlock...");
                    Resource available = new Resource(resources.get(0), resources.get(1), resources.get(2));
                    String result = new Deadlock(ready_queue, available).detect();
                    deadlock_log_area.setText(null);
                    appendToPane(deadlock_log_area, String.format("[Available: %d - %d - %d]\n",
                            available.get_A(), available.get_B(), available.get_C()), Color.RED, true, false);
                    appendToPane(deadlock_log_area, result + "\n", Color.BLACK, true, false);
                } else {
                    ready_queue.forEach(i
                            -> {
                        System.out.printf("PID: %d\tNeed A: %d\tNeed B: %d\tNeed C: %d\n",
                                i.PID, i.Need.get_A(), i.Need.get_B(), i.Need.get_C());
                    });

                    System.out.println("\nChecking for Deadlock...");
                    Resource available = new Resource();
                    String result = new Deadlock(ready_queue, available).detect();
                    deadlock_log_area.setText(null);
                    appendToPane(deadlock_log_area, String.format("[Available: %d - %d - %d]\n",
                            available.get_A(), available.get_B(), available.get_C()), Color.RED, true, false);
                    appendToPane(deadlock_log_area, result + "\n", Color.BLACK, true, false);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Available resources must be 3 positive integers separated by spaces !!!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println(ex.toString());
            }
        } else {
            String choice = JOptionPane.showInputDialog(this, "Enter the available resources separated by spaces");
            ArrayList<Integer> resources = new ArrayList<>();
            try {
                if (choice.trim().length() > 0) {
                    String[] temp = choice.trim().split(" ");
                    for (String i : temp) {
                        resources.add(Integer.parseInt(i));
                    }
                    if (temp.length != 3) {
                        throw new NumberFormatException();
                    }

                    ready_queue.forEach(i
                            -> {
                        System.out.printf("PID: %d\tNeed A: %d\tNeed B: %d\tNeed C: %d\n",
                                i.PID, i.Need.get_A(), i.Need.get_B(), i.Need.get_C());
                    });
                    System.out.println("\nChecking for Deadlock...");
                    Resource available = new Resource(resources.get(0), resources.get(1), resources.get(2));

                    deadlock_log_area.setText(null);

                    appendToPane(deadlock_log_area, String.format("[Available: %d - %d - %d]\nChecking for Deadlock...\n",
                            available.get_A(), available.get_B(), available.get_C()), Color.RED, true, false);

                    String result = new Deadlock(ready_queue, available).avoid();

                    appendToPane(deadlock_log_area, result + "\n", Color.BLACK, true, false);
                } else {
                    ready_queue.forEach(i
                            -> {
                        System.out.printf("PID: %d\tNeed A: %d\tNeed B: %d\tNeed C: %d\n",
                                i.PID, i.Need.get_A(), i.Need.get_B(), i.Need.get_C());
                    });
                    System.out.println("\nChecking for Deadlock...");
                    Resource available = new Resource();

                    deadlock_log_area.setText(null);
                    appendToPane(deadlock_log_area, String.format("[Available: %d - %d - %d]\nChecking for Deadlock...\n",
                            available.get_A(), available.get_B(), available.get_C()), Color.RED, true, false);

                    String result = new Deadlock(ready_queue, available).avoid();

                    appendToPane(deadlock_log_area, result + "\n", Color.BLACK, true, false);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Available resources must be 3 positive integers separated by spaces !!!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.err.println(ex.toString());
            }
        }
    }//GEN-LAST:event_deadlock_chk_btnActionPerformed

    private void deadlock_sched_rand_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deadlock_sched_rand_btnActionPerformed
        ready_queue.clear();
        int length = deadlock_table_model.getRowCount();
        clear_table(deadlock_table_model);
        clear_table(editing_table_model);
        for (int i = 0; i < length; i++) {
            ready_queue.add(new Process(i));
        }

        populate_table(deadlock_table_model, ready_queue, this.DEADLOCK_TABLE);
    }//GEN-LAST:event_deadlock_sched_rand_btnActionPerformed

    private void mem_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mem_btnActionPerformed
        paging_itemActionPerformed(evt);
    }//GEN-LAST:event_mem_btnActionPerformed

    private void add_process_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_process_btnActionPerformed
        String s = JOptionPane.showInputDialog(this, "How Many Pages will the process require ?",
                "Page Numbers", JOptionPane.QUESTION_MESSAGE);
        boolean change = false;

        LinkedList<Memory_Location> memory_before_allocation = new LinkedList<>();

        memory.forEach(m -> memory_before_allocation.add(m));

        try {
            if (s != null) {
                if (s.trim().length() > 0) {
                    int page_count = Integer.parseInt(s);
                    if (page_count > memory.size()) {
                        throw new IllegalAccessError();
                    }
                    Process p = new Process(++process_count);
                    String button_text = get_selected_button_text(memory_button_group);
                    if (button_text.toLowerCase().contains("fifo")) {
                        p.allocate_memory(page_count, memory, Process.FIFO, Back_Store, starting_time);
                    } else {
                        p.allocate_memory(page_count, memory, Process.LRU, Back_Store, starting_time);
                    }

                    processes_memory_list.add(p);
                    LinkedList<Integer> preemptied = new LinkedList<>();
                    for (int i = 0; i < memory.size(); i++) {
                        if ((memory.get(i).PID != memory_before_allocation.get(i).PID) && memory_before_allocation.get(i).PID != -1) {
                            preemptied.add(memory_before_allocation.get(i).PID);
                            change = true;
                        }
                    }

                    if (change) {
                        for (int i = 0; i < processes_memory_list.size(); i++) {
                            int current_pid_page_count = 0;
                            for (int j = 0; j < preemptied.size(); j++) {
                                if ((processes_memory_list.get(i).PID == preemptied.get(j))) {
                                    current_pid_page_count++;
                                }
                            }
                            int pc = processes_memory_list.get(i).get_page_count();
                            processes_memory_list.get(i).set_page_count(pc - current_pid_page_count);
                        }
                    }

                    processes_memory_list.forEach(i -> i.data_addition(0, 0, memory));
                    clear_table(processes_memory_table_model);
                    clear_table(memory_table_model);
                    populate_memory_table(memory_table_model, memory);
                    populate_table(processes_memory_table_model, processes_memory_list, this.PROC_MEM_TABLE);
                } else {
                    int page_count = Math.abs(new Random().nextInt(9) + 1);
                    Process p = new Process(++process_count);
                    String button_text = get_selected_button_text(memory_button_group);
                    if (button_text.toLowerCase().contains("fifo")) {
                        p.allocate_memory(page_count, memory, Process.FIFO, Back_Store, starting_time);
                    } else {
                        p.allocate_memory(page_count, memory, Process.LRU, Back_Store, starting_time);
                    }

                    processes_memory_list.add(p);
                    LinkedList<Integer> preemptied = new LinkedList<>();
                    for (int i = 0; i < memory.size(); i++) {
                        if ((memory.get(i).PID != memory_before_allocation.get(i).PID) && memory_before_allocation.get(i).PID != -1) {
                            preemptied.add(memory_before_allocation.get(i).PID);
                            change = true;
                        }
                    }

                    if (change) {
                        for (int i = 0; i < processes_memory_list.size(); i++) {
                            int current_pid_page_count = 0;
                            for (int j = 0; j < preemptied.size(); j++) {
                                if ((processes_memory_list.get(i).PID == preemptied.get(j))) {
                                    current_pid_page_count++;
                                }
                            }
                            int pc = processes_memory_list.get(i).get_page_count();
                            processes_memory_list.get(i).set_page_count(pc - current_pid_page_count);
                        }
                    }

                    processes_memory_list.forEach(i -> i.data_addition(0, 0, memory));
                    clear_table(processes_memory_table_model);
                    clear_table(memory_table_model);
                    populate_memory_table(memory_table_model, memory);
                    populate_table(processes_memory_table_model, processes_memory_list, this.PROC_MEM_TABLE);
                }
            }
            proc_data_rand_btn.setEnabled(true);
        } catch (NumberFormatException ex) {
            System.err.println(ex.toString());
        } catch (IllegalAccessError ex) {
            System.err.println(ex.toString());
            JOptionPane.showMessageDialog(this, "Illegal Access\nYou are trying to access an invalid memory space",
                    "Illegal Access Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_add_process_btnActionPerformed

    private void save_data_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save_data_btnActionPerformed
        try {
            int selected_row = processes_memory_table.getSelectedRow();
            int process_id = (int) processes_memory_table.getValueAt(selected_row, 0);

            System.out.println(process_id);
            Process process_to_be_edited = null;
            int index_of_process_to_be_edited = 0;
            for (int i = 0; i < processes_memory_list.size(); i++) {
                if (processes_memory_list.get(i).PID == process_id) {
                    process_to_be_edited = processes_memory_list.get(i);
                    break;
                }
            }
            process_to_be_edited.data_addition(memory);
            clear_table(memory_table_model);
            populate_memory_table(memory_table_model, memory);
        } catch (IndexOutOfBoundsException ex) {
            JOptionPane.showMessageDialog(this, "Select a row first", "Row Selection Error", JOptionPane.QUESTION_MESSAGE);
        }
    }//GEN-LAST:event_save_data_btnActionPerformed

    private void bounded_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bounded_radioActionPerformed
        writers_list.forEach(p -> p.interrupt());
        readers_list.forEach(c -> c.interrupt());

        writers_list.clear();
        readers_list.clear();
        buffer.clear();

        clear_table(buffer_table_model);
        clear_table(pw_table_model);
        clear_table(cr_table_model);

        is_bb = true;
        current_label.setVisible(false);
        rw_label.setVisible(false);
        pw_add_btn.setText("Add Producer");
        pw_rem_btn.setText("Remove Producer");
        cr_add_btn.setText("Add Consumer");
        cr_rem_btn.setText("Remove Consumer");

        JTableHeader th = pw_table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        TableColumn tc = tcm.getColumn(0);
        tc.setHeaderValue("Producers");
        th.repaint();

        th = buffer_table.getTableHeader();
        tcm = th.getColumnModel();
        tc = tcm.getColumn(0);
        tc.setHeaderValue("Buffer");
        th.repaint();

        th = cr_table.getTableHeader();
        tcm = th.getColumnModel();
        tc = tcm.getColumn(0);
        tc.setHeaderValue("Consumers");
        th.repaint();
    }//GEN-LAST:event_bounded_radioActionPerformed

    private void rw_radioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rw_radioActionPerformed
        producers_list.forEach(p -> p.interrupt());
        consumers_list.forEach(c -> c.interrupt());

        producers_list.clear();
        consumers_list.clear();
        buffer.clear();

        clear_table(buffer_table_model);
        clear_table(pw_table_model);
        clear_table(cr_table_model);

        is_bb = false;
        current_label.setVisible(true);
        rw_label.setVisible(true);
        pw_add_btn.setText("Add Writer");
        pw_rem_btn.setText("Remove Writer");
        cr_add_btn.setText("Add Reader");
        cr_rem_btn.setText("Remove Reader");

        JTableHeader th = pw_table.getTableHeader();
        TableColumnModel tcm = th.getColumnModel();
        TableColumn tc = tcm.getColumn(0);
        tc.setHeaderValue("Writers");
        th.repaint();

        th = buffer_table.getTableHeader();
        tcm = th.getColumnModel();
        tc = tcm.getColumn(0);
        tc.setHeaderValue(null);
        th.repaint();

        th = cr_table.getTableHeader();
        tcm = th.getColumnModel();
        tc = tcm.getColumn(0);
        tc.setHeaderValue("Readers");
        th.repaint();
    }//GEN-LAST:event_rw_radioActionPerformed

    private void pw_add_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pw_add_btnActionPerformed
        if (is_bb) {
            Thread t = new Thread(produce);
            t.start();
            producers_list.add(t);

            clear_table(pw_table_model);
            populate_table_coloumn(pw_table_model, producers_list);
        } else {
            Thread t = new Thread(write);
            t.start();
            writers_list.add(t);

            clear_table(pw_table_model);
            populate_table_coloumn(pw_table_model, writers_list);
        }
    }//GEN-LAST:event_pw_add_btnActionPerformed

    private void cr_add_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cr_add_btnActionPerformed
        if (is_bb) {
            Thread t = new Thread(consume);
            t.start();
            consumers_list.add(t);

            clear_table(cr_table_model);
            populate_table_coloumn(cr_table_model, consumers_list);
        } else {
            Thread t = new Thread(read);
            t.start();
            readers_list.add(t);

            clear_table(cr_table_model);
            populate_table_coloumn(cr_table_model, readers_list);
        }
    }//GEN-LAST:event_cr_add_btnActionPerformed

    private void pw_rem_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pw_rem_btnActionPerformed
        if (is_bb) {
            try {
                producers_list.getFirst().interrupt();
                producers_list.removeFirst();

                clear_table(pw_table_model);
                populate_table_coloumn(pw_table_model, producers_list);
            } catch (NoSuchElementException ex) {
                System.err.println("Producers list is empty");
            }
        } else {
            try {
                writers_list.getFirst().interrupt();
                writers_list.removeFirst();

                clear_table(pw_table_model);
                populate_table_coloumn(pw_table_model, writers_list);
            } catch (NoSuchElementException ex) {
                System.err.println("Writers list is empty");
            }
        }

    }//GEN-LAST:event_pw_rem_btnActionPerformed

    private void cr_rem_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cr_rem_btnActionPerformed
        if (is_bb) {
            try {
                consumers_list.getFirst().interrupt();
                consumers_list.removeFirst();

                clear_table(cr_table_model);
                populate_table_coloumn(cr_table_model, consumers_list);
            } catch (NoSuchElementException ex) {
                System.err.println("Consumers list is empty");
            }
        } else {
            try {
                readers_list.getFirst().interrupt();
                readers_list.removeFirst();

                clear_table(cr_table_model);
                populate_table_coloumn(cr_table_model, readers_list);
            } catch (NoSuchElementException ex) {
                System.err.println("Readers list is empty");
            }
        }
    }//GEN-LAST:event_cr_rem_btnActionPerformed

    private void paging_itemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_paging_itemActionPerformed
        memory.clear();
        processes_memory_list.clear();
        clear_table(processes_memory_table_model);
        clear_table(memory_table_model);
        for (int i = 0; i < 20; i++) {
            memory.add(new Memory_Location(0, 0, -1, 0));
        }
        populate_memory_table(memory_table_model, memory);
        memory_table.getColumnModel().getColumn(0).setMinWidth(80);
        memory_table.getColumnModel().getColumn(0).setMaxWidth(90);

        memory_table.getColumnModel().getColumn(1).setMinWidth(30);
        memory_table.getColumnModel().getColumn(1).setMaxWidth(40);

        memory_table.getColumnModel().getColumn(2).setMinWidth(40);
        memory_table.getColumnModel().getColumn(2).setMaxWidth(50);

        memory_table.getColumnModel().getColumn(3).setMinWidth(40);
        memory_table.getColumnModel().getColumn(3).setMaxWidth(50);

        memory_frame.setLocation(screen_width - 650, screen_height - 600);
        memory_frame.setSize(340, 408);
        memory_frame.setVisible(true);

        processes_frame.setSize(530, 360);
        processes_frame.setVisible(true);

        starting_time = System.currentTimeMillis();
    }//GEN-LAST:event_paging_itemActionPerformed

    private void proc_data_rand_btnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proc_data_rand_btnActionPerformed
        processes_memory_list.forEach(i -> {
            int pc = i.get_page_count();
            if (pc > 0) {
                i.data_addition(Math.abs(new Random().nextInt(pc)), memory);
            }
        });

        clear_table(memory_table_model);
        populate_memory_table(memory_table_model, memory);
    }//GEN-LAST:event_proc_data_rand_btnActionPerformed

    private void processes_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_processes_frameWindowClosing
        memory.clear();
        processes_memory_list.clear();
        clear_table(processes_memory_table_model);
        clear_table(memory_table_model);
        memory_frame.setVisible(false);
    }//GEN-LAST:event_processes_frameWindowClosing

    private void memory_frameWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_memory_frameWindowClosing
        memory.clear();
        processes_memory_list.clear();
        clear_table(processes_memory_table_model);
        clear_table(memory_table_model);
        processes_frame.setVisible(false);
    }//GEN-LAST:event_memory_frameWindowClosing

    public static void main(String args[]) {
        try {
            javax.swing.UIManager.LookAndFeelInfo info = javax.swing.UIManager.getInstalledLookAndFeels()[3];
            javax.swing.UIManager.setLookAndFeel(info.getClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            System.err.println(ex);
        }
        //</editor-fold>

        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            Main_Frame main_frame = new Main_Frame();
            int width = Toolkit.getDefaultToolkit().getScreenSize().width;
            int height = Toolkit.getDefaultToolkit().getScreenSize().height;
            main_frame.setLocation(width / 4, height / 5);
            main_frame.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JFrame about_frame;
    private javax.swing.JMenuItem about_item;
    private javax.swing.JButton add_process_btn;
    private javax.swing.JRadioButton avoidance_radio;
    private javax.swing.JRadioButton bounded_radio;
    private javax.swing.JLabel brightness_label;
    private javax.swing.JSlider brightness_slider;
    private javax.swing.JTable buffer_table;
    private javax.swing.JScrollPane buffer_table_scroll_pane;
    private javax.swing.JMenu chat_menu;
    private javax.swing.JTextPane client_chat_pane;
    private javax.swing.JScrollPane client_chat_panel;
    private javax.swing.JFrame client_frame;
    private javax.swing.JMenuItem client_item;
    private javax.swing.JTextField client_msg_field;
    private javax.swing.JButton client_snd_btn;
    private javax.swing.JRadioButton clook_radio;
    private javax.swing.JRadioButton cmp_radio;
    private javax.swing.JButton cr_add_btn;
    private javax.swing.JButton cr_rem_btn;
    private javax.swing.JTable cr_table;
    private javax.swing.JScrollPane cr_table_scroll_pane;
    private javax.swing.JLabel current_label;
    private javax.swing.ButtonGroup deadlock_button_group;
    private javax.swing.JButton deadlock_chk_btn;
    private javax.swing.JButton deadlock_edit_table;
    private javax.swing.JFrame deadlock_frame;
    private javax.swing.JMenuItem deadlock_item;
    private javax.swing.JTextPane deadlock_log_area;
    private javax.swing.JButton deadlock_sched_rand_btn;
    private javax.swing.JTable deadlock_table;
    private javax.swing.JButton delete_row_btn;
    private javax.swing.JLabel development_team_title_label;
    private javax.swing.JButton disk_edit_table;
    private javax.swing.JTextPane disk_log_area;
    private javax.swing.JScrollPane disk_log_area_scroll_pane;
    private javax.swing.JScrollPane disk_log_area_scroll_pane1;
    private javax.swing.JButton disk_sched_btn;
    private javax.swing.JMenuItem disk_sched_item;
    private javax.swing.JButton disk_sched_rand_btn;
    private javax.swing.ButtonGroup disk_scheduler_button_group;
    private javax.swing.JFrame disk_scheduling_frame;
    private javax.swing.JScrollPane disk_scheduling_table_scroll_pane;
    private javax.swing.JScrollPane disk_scheduling_table_scroll_pane1;
    private javax.swing.JTable disk_table;
    private javax.swing.JMenuItem doc_item;
    private javax.swing.JButton edit_scheduler_table_row;
    private javax.swing.JTable editing_table;
    private javax.swing.JFrame editing_table_frame;
    private javax.swing.JScrollPane editing_table_frame_scroll_pane;
    private javax.swing.JMenuItem exit_item;
    private javax.swing.JButton fast_input_cnct_discnctBtn;
    private javax.swing.JFrame fast_input_frame;
    private javax.swing.JMenuItem fast_input_item;
    private javax.swing.JComboBox<String> fast_input_portsListComoBox;
    private javax.swing.JButton fast_input_search_btn;
    private javax.swing.JRadioButton fifo_radio;
    private javax.swing.JMenu file_menu;
    private javax.swing.JButton file_sys_btn;
    private javax.swing.JMenu file_sys_menu;
    private javax.swing.JMenu help_menu;
    private javax.swing.JButton io_btn;
    private javax.swing.JMenu io_menu;
    private javax.swing.JScrollPane log_area_scroll_pane;
    private javax.swing.JLabel logo_label;
    private javax.swing.JMenuBar main_menu_bar;
    private javax.swing.JLabel max_label;
    private javax.swing.JButton mem_btn;
    private javax.swing.JMenu mem_menu;
    private javax.swing.JButton memory_;
    private javax.swing.ButtonGroup memory_button_group;
    private javax.swing.JRadioButton memory_fifo_radio;
    private javax.swing.JFrame memory_frame;
    private javax.swing.JRadioButton memory_lru_radio;
    private javax.swing.JTable memory_table;
    private javax.swing.JScrollPane memory_table_scroll_pane;
    private javax.swing.JLabel min_label;
    private javax.swing.JLabel name1;
    private javax.swing.JLabel name2;
    private javax.swing.JLabel name3;
    private javax.swing.JLabel name4;
    private javax.swing.JLabel name5;
    private javax.swing.JMenu net_menu;
    private javax.swing.JButton new_row_btn;
    private javax.swing.JRadioButton npsjf_radio;
    private javax.swing.JButton ok_button;
    private javax.swing.JButton output_cnct_discnctBtn;
    private javax.swing.JFrame output_frame;
    private javax.swing.JMenuItem output_item;
    private javax.swing.JComboBox<String> output_portsListComoBox;
    private javax.swing.JButton output_search_btn;
    private javax.swing.JTable page_table;
    private javax.swing.JLabel page_table_label;
    private javax.swing.JScrollPane page_table_scroll_pane;
    private javax.swing.JMenuItem paging_item;
    private javax.swing.JRadioButton prevention_radio;
    private javax.swing.JButton proc_btn;
    private javax.swing.JButton proc_data_rand_btn;
    private javax.swing.JMenu proc_menu_item;
    private javax.swing.JFrame processes_frame;
    private javax.swing.JLabel processes_label;
    private javax.swing.JTable processes_memory_table;
    private javax.swing.JTable processes_table;
    private javax.swing.JScrollPane processes_table_scroll_pane;
    private javax.swing.JButton pw_add_btn;
    private javax.swing.JButton pw_rem_btn;
    private javax.swing.JTable pw_table;
    private javax.swing.JScrollPane pw_table_scroll_pane;
    private javax.swing.JRadioButton rr_radio;
    private javax.swing.JLabel rw_label;
    private javax.swing.JRadioButton rw_radio;
    private javax.swing.JButton save_data_btn;
    private javax.swing.JButton sched_btn;
    private javax.swing.JMenuItem sched_item;
    private javax.swing.JTextPane sched_log_area;
    private javax.swing.JButton sched_rand_btn;
    private javax.swing.ButtonGroup scheduler_button_group;
    private javax.swing.JFrame scheduling_frame;
    private javax.swing.JScrollPane scheduling_table_scroll_pane;
    private javax.swing.JTextPane server_chat_pane;
    private javax.swing.JScrollPane server_chat_panel;
    private javax.swing.JFrame server_frame;
    private javax.swing.JMenuItem server_item;
    private javax.swing.JTextField server_msg_field;
    private javax.swing.JButton server_snd_btn;
    private javax.swing.JButton slow_input_cnct_discnctBtn;
    private javax.swing.JFrame slow_input_frame;
    private javax.swing.JMenuItem slow_input_item;
    private javax.swing.JComboBox<String> slow_input_portsListComoBox;
    private javax.swing.JButton slow_input_search_btn;
    private javax.swing.JRadioButton sstf_radio;
    private javax.swing.JLabel statusLabel;
    private javax.swing.ButtonGroup sync_button_group;
    private javax.swing.JFrame sync_frame;
    private javax.swing.JMenuItem sync_item;
    // End of variables declaration//GEN-END:variables

    class temp_process {

        public int PID;
        public int Burst_Time;
        public int Arrival_Time;
        public int Waiting_Time;
        public int TurnAround_Time;
        public int Priority;
        public int Sector;
        public Resource Need;

        public temp_process(int PID, int Burst_Time, int Arrival_Time, int Priority) {
            this.PID = PID;
            this.Burst_Time = Burst_Time;
            this.Arrival_Time = Arrival_Time;
            this.Priority = Priority;
        }
    }
}