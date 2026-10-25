package org.djutils.swing.multislider;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.plaf.basic.BasicSliderUI;

import org.djutils.swing.multislider.AbstractMultiSlider.FinalValueChangeListener;

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
public class MultiSliderDemo extends JFrame
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    @SuppressWarnings("checkstyle:needbraces")
    public MultiSliderDemo()
    {
        setPreferredSize(new Dimension(640, 640));
        setTitle("MultiSliderDemo");
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setOpaque(false);
        getContentPane().add(panel);
        getContentPane().setBackground(new Color(204, 255, 204));

        var horSlider = new MultiSlider(0, 100, true, new int[] {25, 50, 75, 80});
        // horSlider.setUI(new BasicSliderUI(null));
        horSlider.setMajorTickSpacing(25);
        horSlider.setMinorTickSpacing(5);
        horSlider.setPaintTicks(true);
        horSlider.setPaintLabels(true);
        panel.add(horSlider, BorderLayout.NORTH);
        horSlider.setThumbLabel(0, "a");
        horSlider.setThumbLabel(1, "b");
        horSlider.setThumbLabel(2, "c");
        horSlider.setDrawThumbLabels(true, 20);
        horSlider.setPassing(true);
        horSlider.setOverlap(true);

        var vertSlider = new MultiSlider(0, 100, false, new int[] {40, 60});
        // vertSlider.setUI(new MetalSliderUI());
        vertSlider.setMajorTickSpacing(20);
        vertSlider.setMinorTickSpacing(5);
        vertSlider.setPaintTicks(true);
        vertSlider.setPaintLabels(true);
        vertSlider.setPaintTrack(true);
        panel.add(vertSlider, BorderLayout.WEST);
        vertSlider.setThumbLabel(0, "min");
        vertSlider.setThumbLabel(1, "max");
        vertSlider.setDrawThumbLabels(true, 35);
        vertSlider.setPassing(true);
        vertSlider.setOverlap(true);

        var horSlider2 = new MultiSlider(0, 10, true, new int[] {2, 5, 7});
        horSlider2.setMajorTickSpacing(1);
        horSlider2.setMinorTickSpacing(1);
        horSlider2.setPaintTicks(true);
        horSlider2.setPaintLabels(true);
        panel.add(horSlider2, BorderLayout.SOUTH);
        horSlider2.setThumbLabel(0, "x");
        horSlider2.setThumbLabel(1, "y");
        horSlider2.setThumbLabel(2, "z");
        horSlider2.setDrawThumbLabels(true, 20);
        horSlider2.setSnapToTicks(true);
        horSlider2.setUI(new BasicSliderUI(null));
        horSlider2.setPassing(true);
        horSlider2.setOverlap(true);

        var button = new JButton("RESET");
        button.setPreferredSize(new Dimension(100, 25));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        panel.add(buttonPanel, BorderLayout.EAST);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(button);
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.setOpaque(false);
        button.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(final ActionEvent e)
            {
                horSlider.resetToInitialValues();
                horSlider2.resetToInitialValues();
                vertSlider.resetToInitialValues();
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        acl(horSlider, "hor1");
        acl(vertSlider, "vert");
        acl(horSlider2, "hor2");
    }

    /**
     * Add change listener.
     * @param ms the multislider
     * @param name the string with the variable name
     */
    void acl(final MultiSlider ms, final String name)
    {
        ms.addFinalValueChangeListener(new FinalValueChangeListener()
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
                new MultiSliderDemo();
            }
        });
    }

}
