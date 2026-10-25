package org.djutils.swing.multislider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.djutils.test.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioExtension;

/**
 * LinearMultiSliderTest tests the functions of the LinearMultiSlider.
 * <p>
 * Copyright (c) 2024-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Alexander Verbraeck
 */
@ExtendWith(CacioExtension.class)
public class LinearMultiSliderTest
{

    /**
     * Create a LinearMultiSlider with three underlying sliders and test the basic getters and setters.
     */
    @Test
    public void testGetSetDouble()
    {
        testGetSetDouble(SwingConstants.HORIZONTAL);
        testGetSetDouble(SwingConstants.VERTICAL);
        testGetSetDouble(-1);
    }

    /** */
    class DoubleSlider50 extends LinearMultiSlider<Double>
    {
        /** */
        private static final long serialVersionUID = 1L;

        /**
         * @param horizontal true = hor, false = vert
         * @param initialValues 1 or more new values
         */
        DoubleSlider50(final boolean horizontal, final Double... initialValues)
        {
            super(0.0, 50.0, 101, horizontal, initialValues);
        }

        /**
         * @param initialValues 1 or more new values
         */
        DoubleSlider50(final Double... initialValues)
        {
            super(0.0, 50.0, 101, initialValues);
        }

        /** {@inheritDoc} */
        @Override
        protected Double mapIndexToValue(final int index)
        {
            return Double.valueOf(index / 2.0);
        }
    }

    /** */
    class DoubleSlider extends LinearMultiSlider<Double>
    {
        /** */
        private static final long serialVersionUID = 1L;

        /**
         * @param min lowest value
         * @param max highest value
         * @param unitTicks number of ticks on the scale (1 + number of intervals)
         * @param horizontal true = hor, false = vert
         * @param initialValues 1 or more new values
         */
        DoubleSlider(final double min, final double max, final int unitTicks, final boolean horizontal,
                final Double... initialValues)
        {
            super(min, max, unitTicks, horizontal, initialValues);
        }

        /** {@inheritDoc} */
        @Override
        protected Double mapIndexToValue(final int index)
        {
            return Double.valueOf(index * (getMaximum() - getMinimum()) / (1.0 * (getUnitTicks() - 1)));
        }
    }

    /**
     * Test get/set of a multislider with an orientation.
     * @param initialOrientation the orientation
     */
    private void testGetSetDouble(final int initialOrientation)
    {
        LinearMultiSlider<Double> ms;
        int orientation = initialOrientation;
        if (initialOrientation == -1)
        {
            ms = new DoubleSlider50(10.0, 25.0, 30.0);
            orientation = SwingConstants.HORIZONTAL;
        }
        else
        {
            ms = new DoubleSlider50(initialOrientation == SwingConstants.HORIZONTAL, 10.0, 25.0, 30.0);
        }
        assertEquals(0.0, ms.getMinimum(), 1E-6);
        assertEquals(50.0, ms.getMaximum(), 1E-6);
        assertEquals(3, ms.getNumberOfThumbs());
        assertEquals(101, ms.getUnitTicks());
        assertEquals(10.0, ms.getValue(0), 1E-6);
        assertEquals(25.0, ms.getValue(1), 1E-6);
        assertEquals(30.0, ms.getValue(2), 1E-6);
        assertEquals(orientation, ms.getOrientation());
        assertFalse(ms.isBusy());

        JSlider js0 = ms.getSlider(0);
        assertEquals(20, js0.getValue());
        JSlider js1 = ms.getSliders()[1];
        assertEquals(50, js1.getValue());
        JSlider js2 = ms.getSliders()[2];
        assertEquals(60, js2.getValue());

        ms.setThumbLabel(0, "min");
        ms.setThumbLabel(2, "max");
        assertEquals("min", ms.getThumbLabel(0));
        assertEquals("", ms.getThumbLabel(1));
        assertEquals("max", ms.getThumbLabel(2));
        ms.setDrawThumbLabels(true, 20);
        assertTrue(ms.isDrawThumbLabels());
        ms.setDrawThumbLabels(false, 0);
        assertFalse(ms.isDrawThumbLabels());

        ms.setValue(1, 20.0);
        assertEquals(10.0, ms.getValue(0), 1E-6);
        assertEquals(20.0, ms.getValue(1), 1E-6);
        assertEquals(30.0, ms.getValue(2), 1E-6);
        assertEquals(40, js1.getValue());
        ms.resetToInitialValues();
        assertEquals(10.0, ms.getValue(0), 1E-6);
        assertEquals(25.0, ms.getValue(1), 1E-6);
        assertEquals(30.0, ms.getValue(2), 1E-6);
    }

    /**
     * Test mouse clicks for a multislider with a horizontal and vertical orientation and an inverted and non-inverted
     * direction.
     * @throws AWTException on AWT or Swing error
     * @throws InterruptedException when sleep is interrupted
     * @throws InvocationTargetException on error in invokeAndWait
     */
    @Test
    public void testMouseClicks() throws AWTException, InterruptedException, InvocationTargetException
    {
        testMouseClicks(SwingConstants.HORIZONTAL, false);
        testMouseClicks(SwingConstants.VERTICAL, false);
        testMouseClicks(SwingConstants.HORIZONTAL, true);
        testMouseClicks(SwingConstants.VERTICAL, true);
    }

    /**
     * Test mouse clicks for a multislider with the given orientation.
     * @param orientation the orientation
     * @param inverted whether the scale is inverted or not
     * @throws AWTException on AWT or Swing error
     * @throws InterruptedException when sleep is interrupted
     * @throws InvocationTargetException on error in invokeAndWait
     */
    public void testMouseClicks(final int orientation, final boolean inverted)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        JFrame frame = new JFrame();
        frame.setVisible(false);
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setSize(new Dimension(400, 400));
        frame.setLocationRelativeTo(null);
        frame.setLocation(0, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 400));
        LinearMultiSlider<Double> ms = new DoubleSlider50(orientation == SwingConstants.HORIZONTAL, 10.0, 25.0, 30.0);
        setLookAndFeel(ms);
        ms.setInverted(inverted);
        panel.add(ms, orientation == SwingConstants.VERTICAL ? BorderLayout.WEST : BorderLayout.NORTH);
        frame.add(panel);
        frame.validate();
        frame.pack();

        Thread.sleep(500);

        int x = 10;
        int y = 10;
        for (var mml : ms.getDispatcherPane().getMouseMotionListeners())
        {
            SwingUtilities.invokeAndWait(() ->
            { mml.mouseMoved(new MouseEvent(ms, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false)); });
        }

        for (var ml : ms.getDispatcherPane().getMouseListeners())
        {
            ml.mousePressed(new MouseEvent(ms, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            Thread.sleep(50);
            assertNotEquals(-1, ms.getBusySlider());
            assertTrue(ms.isBusy());
            ml.mouseReleased(new MouseEvent(ms, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            int counter = 0;
            while (ms.isBusy() && counter++ < 100)
            {
                Thread.sleep(10);
            }
            assertEquals(-1, ms.getBusySlider());
            assertFalse(ms.isBusy());
            assertTrue(counter < 100);
            Thread.sleep(50);
        }

        Thread.sleep(250);

        String s = "Values are: 0=" + ms.getValue(0) + ", 1=" + ms.getValue(1) + ", 2=" + ms.getValue(2);
        if (orientation == SwingConstants.HORIZONTAL)
        {
            if (inverted)
            {
                assertEquals(30.5, ms.getValue(2), 1E-6, "Hor, inv - " + s);
            }
            else
            {
                assertEquals(9.5, ms.getValue(0), 1E-6, "Hor, not - " + s);
            }
        }
        else
        {
            if (inverted)
            {
                assertEquals(9.5, ms.getValue(0), 1E-6, "vert, inv - " + s);
            }
            else
            {
                assertEquals(30.5, ms.getValue(2), 1E-6, "vert, not - " + s);
            }
        }

        // move in and out; test 'clicked'
        for (var ml : ms.getDispatcherPane().getMouseListeners())
        {
            ml.mouseEntered(
                    new MouseEvent(ms, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, x, y, x, y, 0, false, 0));
            assertEquals(-1, ms.getBusySlider());
            assertFalse(ms.isBusy());
            Thread.sleep(200);

            ml.mouseExited(new MouseEvent(ms, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, 100, 100, 100, 100, 0,
                    false, 0));
            assertEquals(-1, ms.getBusySlider());
            assertFalse(ms.isBusy());
            Thread.sleep(200);

            ml.mouseClicked(new MouseEvent(ms, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            Thread.sleep(200);
            assertEquals(-1, ms.getBusySlider());
            assertFalse(ms.isBusy());
        }

        // test drag to (10, 10)
        for (var ml : ms.getDispatcherPane().getMouseListeners())
        {
            ml.mousePressed(new MouseEvent(ms, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            Thread.sleep(150);
        }
        for (var mml : ms.getDispatcherPane().getMouseMotionListeners())
        {
            mml.mouseDragged(new MouseEvent(ms, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            Thread.sleep(50);
        }
        for (var ml : ms.getDispatcherPane().getMouseListeners())
        {
            ml.mouseReleased(new MouseEvent(ms, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, x, y, 1, false,
                    MouseEvent.BUTTON1));
            Thread.sleep(150);
        }

        frame.dispose();
    }

    /**
     * Test label panel of a multislider with a horizontal orientation.
     * @throws AWTException on AWT or Swing error
     * @throws InterruptedException when sleep is interrupted
     */
    @Test
    public void testLabelPanelHorizontal() throws AWTException, InterruptedException
    {
        JFrame frame = new JFrame();
        frame.setVisible(false);
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setSize(new Dimension(400, 400));
        frame.setLocationRelativeTo(null);
        frame.setLocation(0, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 400));
        LinearMultiSlider<Double> ms = new DoubleSlider50(true, 10.0, 25.0, 30.0);
        setLookAndFeel(ms);
        ms.setDrawThumbLabels(false, 0);
        panel.add(ms, BorderLayout.NORTH);
        frame.add(panel);
        frame.validate();
        frame.pack();
        Thread.sleep(500);
        int oldHeight = ms.getHeight();

        ms.setThumbLabel(0, "a");
        ms.setThumbLabel(1, "b");
        ms.setThumbLabel(2, "c");
        ms.setDrawThumbLabels(true, 20);
        ms.revalidate();
        ms.repaint();
        ms.getLabelPanel().invalidate();
        ms.getLabelPanel().repaint();
        ms.setUI(ms.getUI()); // very bold way to force complete redraw...
        ms.getLabelPanel().paintComponent(ms.getGraphics());
        ms.setValue(0, 15.5);
        frame.revalidate();
        frame.pack();
        Thread.sleep(500);
        assertTrue(ms.getHeight() > oldHeight, "ms.GetHeight() = " + ms.getHeight() + "; oldHeight = " + oldHeight);

        frame.dispose();
    }

    /**
     * Test font and UI.
     * @throws InterruptedException when sleep is interrupted
     * @throws UnsupportedLookAndFeelException when MetalLookAndFeel unavailable
     * @throws IllegalAccessException when MetalLookAndFeel unavailable
     * @throws InstantiationException when MetalLookAndFeel unavailable
     * @throws ClassNotFoundException when MetalLookAndFeel unavailable
     */
    @Test
    public void testUI() throws InterruptedException, ClassNotFoundException, InstantiationException, IllegalAccessException,
            UnsupportedLookAndFeelException
    {
        JFrame frame = new JFrame();
        frame.setVisible(false);
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setSize(new Dimension(400, 400));
        frame.setLocationRelativeTo(null);
        frame.setLocation(0, 0);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setPreferredSize(new Dimension(400, 400));
        LinearMultiSlider<Double> ms = new DoubleSlider50(true, 10.0, 25.0, 30.0);
        setLookAndFeel(ms);
        ms.setDrawThumbLabels(false, 0);
        panel.add(ms, BorderLayout.NORTH);
        frame.add(panel);
        frame.validate();
        frame.pack();
        Thread.sleep(500);

        ms.setUI(new BasicSliderUI(null));
        assertEquals("BasicSliderUI", ms.getUI().getClass().getSimpleName());
        assertEquals("SliderUI", ms.getUIClassID());
        UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        ms.updateUI();
        assertEquals("MetalSliderUI", ms.getUI().getClass().getSimpleName());

        ms.setFont(new Font("Dialog", Font.ITALIC, 12));
    }

    /**
     * Test errors in creating and using sliders.
     */
    @Test
    public void testErrors()
    {
        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(70.0, 50.0, 101, true, 10.0, 20.0, 30.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(0.0, 50.0, 0, true, 10.0, 20.0, 40.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(0.0, 50.0, 1, true, 10.0, 20.0, 40.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(0.0, 50.0, 101, true, 10.0, 20.0, 60.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(10.0, 60.0, 101, true, 0.0, 20.0, 40.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(0.0, 50.0, 101, true, 30.0, 20.0, 40.0);
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<LinearMultiSlider<Double>>()
        {
            @Override
            public LinearMultiSlider<Double> assign() throws Throwable
            {
                return new DoubleSlider(0.0, 50.0, 101, true);
            }
        });

        final var ms = new DoubleSlider50(true, 10.0, 20.0, 40.0);

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setValue(1, 75.0);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setValue(1, -50.0);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMaximum(-50.0);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMinimum(100.0);
            }
        });
    }

    /**
     * Test making labels.
     */
    @Test
    public void testLabelTable()
    {
        var ms = new DoubleSlider50(true, 10.0, 40.0);

        var labtab = ms.createStandardLabels(20);
        assertEquals(6, labtab.size());
        assertEquals("0.0", ((JLabel) labtab.get(0)).getText());
        assertEquals("10.0", ((JLabel) labtab.get(20)).getText());
        assertEquals("20.0", ((JLabel) labtab.get(40)).getText());
        assertEquals("30.0", ((JLabel) labtab.get(60)).getText());
        assertEquals("40.0", ((JLabel) labtab.get(80)).getText());
        assertEquals("50.0", ((JLabel) labtab.get(100)).getText());
        ms.setLabelTable(labtab);

        labtab = ms.createStandardLabels(20, 10);
        assertEquals(5, labtab.size());
        assertEquals("5.0", ((JLabel) labtab.get(10)).getText());
        assertEquals("15.0", ((JLabel) labtab.get(30)).getText());
        assertEquals("25.0", ((JLabel) labtab.get(50)).getText());
        assertEquals("35.0", ((JLabel) labtab.get(70)).getText());
        assertEquals("45.0", ((JLabel) labtab.get(90)).getText());
        ms.setLabelTable(labtab);

        var lt2 = ms.getLabelTable();
        assertEquals(5, lt2.size());
        assertEquals("5.0", ((JLabel) labtab.get(10)).getText());
        assertEquals("15.0", ((JLabel) labtab.get(30)).getText());
        assertEquals("25.0", ((JLabel) labtab.get(50)).getText());
        assertEquals("35.0", ((JLabel) labtab.get(70)).getText());
        assertEquals("45.0", ((JLabel) labtab.get(90)).getText());
    }

    /** array with values. */
    private Double[] v;

    /**
     * @return v[0]
     */
    private Double v0()
    {
        return this.v[0];
    }

    /**
     * @return v[1]
     */
    private Double v1()
    {
        return this.v[1];
    }

    /**
     * Test listeners.
     */
    @Test
    public void testListeners()
    {
        var ms = new DoubleSlider50(true, 10.0, 40.0);
        this.v = new Double[] {10.0, 40.0};
        int nrListeners = ms.getChangeListeners().length;
        var cl = new ChangeListener()
        {
            @Override
            public void stateChanged(final ChangeEvent e)
            {
                assertEquals(v0(), ms.getValue(0));
                assertEquals(v1(), ms.getValue(1));
            }
        };
        ms.addChangeListener(cl);
        assertEquals(nrListeners + 1, ms.getChangeListeners().length);

        this.v = new Double[] {10.0, 30.0};
        ms.setValue(1, 30.0);

        ms.removeChangeListener(cl);
        assertEquals(nrListeners, ms.getChangeListeners().length);
    }

    /**
     * Ensure Look and Feel exists in the headless case for the unit tests.
     * @param ms the multislider
     */
    private void setLookAndFeel(final LinearMultiSlider<?> ms)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
        }
    }
}
