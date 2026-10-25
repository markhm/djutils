package org.djutils.swing.multislider;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * MultiSliderDemo demonstrates a horizontal slider and a vertical slider, as well as setValue() through a reset button.
 * <p>
 * Copyright (c) 2024-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Alexander Verbraeck
 */
public class MultiSliderRestrictionDemo extends JFrame
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    @SuppressWarnings("checkstyle:needbraces")
    public MultiSliderRestrictionDemo()
    {
        setPreferredSize(new Dimension(840, 640));
        setTitle("MultiSliderRestrictionDemo");
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setOpaque(false);
        getContentPane().add(panel);
        getContentPane().setBackground(new Color(204, 255, 204));

        JPanel sliderPanel = new JPanel();
        sliderPanel.setPreferredSize(new Dimension(480, 600));
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.setOpaque(false);
        panel.add(sliderPanel);

        sliderPanel.add(Box.createVerticalGlue());

        sliderPanel.add(new JLabel("No passing"));
        sliderPanel.add(new JLabel("     "));
        var s1 = new MultiSlider(0, 100, new int[] {25, 50, 75, 80});
        s1.setPreferredSize(new Dimension(480, 10));
        s1.setMajorTickSpacing(25);
        s1.setMinorTickSpacing(5);
        s1.setPaintTicks(true);
        s1.setPaintLabels(true);
        sliderPanel.add(s1);
        s1.setThumbLabel(0, "a");
        s1.setThumbLabel(1, "b");
        s1.setThumbLabel(2, "c");
        s1.setDrawThumbLabels(true, 20);
        s1.setPassing(false);

        sliderPanel.add(Box.createVerticalGlue());

        sliderPanel.add(new JLabel("     "));
        sliderPanel.add(new JLabel("No overlap"));
        sliderPanel.add(new JLabel("     "));
        var s2 = new MultiSlider(0, 100, new int[] {40, 60});
        s2.setPreferredSize(new Dimension(480, 10));
        s2.setMajorTickSpacing(20);
        s2.setMinorTickSpacing(5);
        s2.setPaintTicks(true);
        s2.setPaintLabels(true);
        s2.setPaintTrack(true);
        sliderPanel.add(s2);
        s2.setThumbLabel(0, "min");
        s2.setThumbLabel(1, "max");
        s2.setDrawThumbLabels(true, 20);
        s2.setOverlap(false);

        sliderPanel.add(Box.createVerticalGlue());

        sliderPanel.add(new JLabel("     "));
        sliderPanel.add(new JLabel("No passing and no overlap"));
        sliderPanel.add(new JLabel("     "));
        var s3 = new MultiSlider(0, 10, new int[] {2, 5, 7});
        s3.setPreferredSize(new Dimension(480, 10));
        s3.setMajorTickSpacing(1);
        s3.setMinorTickSpacing(1);
        s3.setPaintTicks(true);
        s3.setPaintLabels(true);
        sliderPanel.add(s3);
        s3.setThumbLabel(0, "x");
        s3.setThumbLabel(1, "y");
        s3.setThumbLabel(2, "z");
        s3.setDrawThumbLabels(true, 20);
        s3.setSnapToTicks(true);
        s3.setUI(new BasicSliderUI(null));
        s3.setPassing(false);
        s3.setOverlap(false);

        sliderPanel.add(new JLabel("     "));
        sliderPanel.add(new JLabel("No passing and no overlap"));
        sliderPanel.add(new JLabel("     "));
        var s4 = new MultiSlider(1, 8, new int[] {1, 3});
        s4.setPreferredSize(new Dimension(480, 10));
        var labtab = new Hashtable<Integer, JComponent>();
        for (int i = 1; i < 9; i++)
            labtab.put(i, new JLabel(String.valueOf((char) ('A' + i - 1))));
        s4.setLabelTable(labtab);
        s4.setMajorTickSpacing(1);
        s4.setMinorTickSpacing(1);
        s4.setPaintTicks(true);
        s4.setPaintLabels(true);
        sliderPanel.add(s4);
        s4.setThumbLabel(0, "choice 1");
        s4.setThumbLabel(1, "choice 2");
        s4.setDrawThumbLabels(true, 20);
        s4.setSnapToTicks(true);
        s4.setUI(new BasicSliderUI(null));
        s4.setPassing(false);
        s4.setOverlap(false);

        sliderPanel.add(Box.createVerticalGlue());

        JPanel emptyPanel = new JPanel();
        emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
        emptyPanel.setPreferredSize(new Dimension(120, 600));
        panel.add(emptyPanel);

        var button = new JButton("RESET");
        button.setPreferredSize(new Dimension(100, 25));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setPreferredSize(new Dimension(200, 600));
        panel.add(buttonPanel);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(button);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.setOpaque(false);
        button.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                s1.resetToInitialValues();
                s2.resetToInitialValues();
                s3.resetToInitialValues();
                s4.resetToInitialValues();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        acl(s1, "s1");
        acl(s2, "s2");
        acl(s3, "s3");
        acl(s4, "s4");
    }

    /**
     * Add change listener.
     * @param ms the multislider
     * @param name the string with the variable name
     */
    void acl(final MultiSlider ms, final String name)
    {
        ms.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(final ChangeEvent e)
            {
                MultiSlider s = (MultiSlider) e.getSource();
                System.out.print(name);
                for (int i = 0; i < s.getNumberOfThumbs(); i++)
                {
                    System.out.print(", Thumb " + i + ": " + s.getValue(i));
                }
                System.out.println();
            }
        });
    }

    /**
     * @param args args
     */
    public static void main(final String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                new MultiSliderRestrictionDemo();
            }
        });
    }

}
