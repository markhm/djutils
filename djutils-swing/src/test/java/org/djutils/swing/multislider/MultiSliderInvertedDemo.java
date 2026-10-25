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
import javax.swing.event.ChangeListener;

/**
 * MultiSliderReverseDemo shows a number of sliders with a reversed scale.
 * <p>
 * Copyright (c) 2024-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Alexander Verbraeck
 */
public class MultiSliderInvertedDemo extends JFrame
{
    /** */
    private static final long serialVersionUID = 1L;

    /** */
    @SuppressWarnings("checkstyle:needbraces")
    public MultiSliderInvertedDemo()
    {
        setPreferredSize(new Dimension(640, 640));
        setTitle("MultiSliderInvertedDemo");
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        panel.setOpaque(false);
        getContentPane().add(panel);
        getContentPane().setBackground(new Color(204, 255, 204));

        var horSlider = new MultiSlider(100, 200, new int[] {125, 150, 175});
        // horSlider.setUI(new BasicSliderUI(null));
        horSlider.setMajorTickSpacing(25);
        horSlider.setMinorTickSpacing(5);
        horSlider.setPaintTicks(true);
        horSlider.setPaintLabels(true);
        horSlider.setInverted(true);
        panel.add(horSlider, BorderLayout.NORTH);
        horSlider.setThumbLabel(0, "a");
        horSlider.setThumbLabel(1, "b");
        horSlider.setThumbLabel(2, "c");
        horSlider.setDrawThumbLabels(true, 20);

        var vertSlider = new MultiSlider(100, 200, false, new int[] {140, 160});
        // vertSlider.setUI(new MetalSliderUI());
        vertSlider.setMajorTickSpacing(20);
        vertSlider.setMinorTickSpacing(5);
        vertSlider.setPaintTicks(true);
        vertSlider.setPaintLabels(true);
        vertSlider.setPaintTrack(true);
        vertSlider.setInverted(true);
        panel.add(vertSlider, BorderLayout.WEST);
        vertSlider.setThumbLabel(0, "min");
        vertSlider.setThumbLabel(1, "max");
        vertSlider.setDrawThumbLabels(true, 35);

        var horSlider2 = new MultiSlider(1, 10, new int[] {2, 4});
        var labtab = horSlider2.createStandardLabels(2, 2);
        horSlider2.setLabelTable(labtab);
        horSlider2.setMajorTickSpacing(1);
        horSlider2.setMinorTickSpacing(1);
        horSlider2.setPaintTicks(true);
        horSlider2.setPaintLabels(true);
        panel.add(horSlider2, BorderLayout.SOUTH);
        horSlider2.setThumbLabel(0, "s1");
        horSlider2.setThumbLabel(1, "s2");
        horSlider2.setDrawThumbLabels(true, 20);

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
                vertSlider.resetToInitialValues();
                horSlider2.resetToInitialValues();
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
                new MultiSliderInvertedDemo();
            }
        });
    }

}
