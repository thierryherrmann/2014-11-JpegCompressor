package com.thn.jpegcompressor;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.sun.jmx.snmp.Timestamp;

public class MainGui extends JPanel implements MyLogger
{
    private static final String PREF_NAME = "inputDirectory";
    JTextArea log;
    JFileChooser fc;

    public MainGui()
    {
        super(new BorderLayout());
        log = new JTextArea(10, 80);
        log.setMargin(new Insets(5, 5, 5, 5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        // Create a file chooser
        fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        add(logScrollPane, BorderLayout.CENTER);
    }

    @Override
    public void log(final String aString)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                log.append(aString + System.getProperty("line.separator"));
            }
        });
    }
    
    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event
     * dispatch thread.
     */
    private static void createAndShowGUI()
    {
        // Create and set up the window.
        JFrame frame = new JFrame("JPEGCompressor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final MainGui mainGui = new MainGui();
        frame.add(mainGui);
        frame.pack();
        frame.setVisible(true);

        String defaultValue = System.getProperty("user.home");
        Preferences prefs = Preferences.userNodeForPackage(JPEGCompressor.class);
        String directory = prefs.get(PREF_NAME, defaultValue);
        mainGui.fc.setCurrentDirectory(new File(directory));

        int returnVal = mainGui.fc.showOpenDialog(mainGui);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            final File file = mainGui.fc.getSelectedFile();
            prefs.put(PREF_NAME, file.getAbsolutePath());
            
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    JPEGCompressor compressor = new JPEGCompressor(mainGui, new File("c:/windows/temp"));
                    try
                    {
                        long now = System.currentTimeMillis();
                        DirectoryWalker.execute(file, compressor);
                        mainGui.log("finished (" + new Timestamp(System.currentTimeMillis()) + ")");
                        mainGui.log("duration : " + ((System.currentTimeMillis() - now)/1000) + " seconds");
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }).start();
        }
        else
        {
            System.exit(-1);
        }
    }

    public static void main(String[] args)
    {
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                createAndShowGUI();
            }
        });
    }
}
