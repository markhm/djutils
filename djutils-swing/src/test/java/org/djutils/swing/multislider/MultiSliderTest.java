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
 * MultiSliderTest tests the functions of the MultiSlider.
 * <p>
 * Copyright (c) 2024-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Alexander Verbraeck
 */
@ExtendWith(CacioExtension.class)
public class MultiSliderTest
{

    /**
     * Create a MultiSlider with three underlying sliders and test the basic getters and setters.
     */
    @Test
    public void testGetSet()
    {
        testGetSet(SwingConstants.HORIZONTAL);
        testGetSet(SwingConstants.VERTICAL);
        testGetSet(-1);
    }

    /**
     * Test get/set of a multislider with an orientation.
     * @param initialOrientation the orientation
     */
    private void testGetSet(final int initialOrientation)
    {
        MultiSlider ms;
        int orientation = initialOrientation;
        if (initialOrientation == -1)
        {
            ms = new MultiSlider(0, 100, new int[] {25, 50, 75});
            orientation = SwingConstants.HORIZONTAL;
        }
        else
        {
            ms = new MultiSlider(0, 100, orientation == SwingConstants.HORIZONTAL, new int[] {25, 50, 75});
        }
        assertEquals(0, ms.getMinimum());
        assertEquals(100, ms.getMaximum());
        assertEquals(3, ms.getNumberOfThumbs());
        assertEquals(25, ms.getValue(0));
        assertEquals(50, ms.getValue(1));
        assertEquals(75, ms.getValue(2));
        assertEquals(orientation, ms.getOrientation());
        assertFalse(ms.isBusy());

        JSlider js0 = ms.getSlider(0);
        assertEquals(25, js0.getValue());
        JSlider js1 = ms.getSliders()[1];
        assertEquals(50, js1.getValue());

        ms.setThumbLabel(0, "min");
        ms.setThumbLabel(2, "max");
        assertEquals("min", ms.getThumbLabel(0));
        assertEquals("", ms.getThumbLabel(1));
        assertEquals("max", ms.getThumbLabel(2));
        ms.setDrawThumbLabels(true, 20);
        assertTrue(ms.isDrawThumbLabels());
        ms.setDrawThumbLabels(false, 0);
        assertFalse(ms.isDrawThumbLabels());

        ms.setValue(1, 40);
        assertEquals(40, ms.getValue(1));
        assertEquals(40, js1.getValue());
        assertEquals(25, ms.getValue(0));
        assertEquals(75, ms.getValue(2));
        ms.resetToInitialValues();
        assertEquals(25, ms.getValue(0));
        assertEquals(50, ms.getValue(1));
        assertEquals(75, ms.getValue(2));

        ms.setMinimum(10);
        ms.setMaximum(90);
        assertEquals(10, ms.getMinimum());
        assertEquals(90, ms.getMaximum());
        assertEquals(25, ms.getValue(0));
        assertEquals(50, ms.getValue(1));
        assertEquals(75, ms.getValue(2));
        ms.setMinimum(0);
        ms.setMaximum(100);

        int oldExtent = ms.getExtent();
        ms.setExtent(oldExtent + 2);
        assertEquals(oldExtent + 2, ms.getExtent());
        ms.setExtent(oldExtent);
        assertEquals(oldExtent, ms.getExtent());

        assertFalse(ms.getInverted());
        ms.setInverted(true);
        assertTrue(ms.getInverted());
        ms.setInverted(false);
        assertFalse(ms.getInverted());

        ms.setMajorTickSpacing(10);
        assertEquals(10, ms.getMajorTickSpacing());
        ms.setMajorTickSpacing(20);
        assertEquals(20, ms.getMajorTickSpacing());
        ms.setMinorTickSpacing(10);
        assertEquals(10, ms.getMinorTickSpacing());
        ms.setMinorTickSpacing(5);
        assertEquals(5, ms.getMinorTickSpacing());

        ms.setPaintTicks(false);
        assertFalse(ms.getPaintTicks());
        ms.setPaintTicks(true);
        assertTrue(ms.getPaintTicks());

        ms.setPaintTrack(false);
        assertFalse(ms.getPaintTrack());
        ms.setPaintTrack(true);
        assertTrue(ms.getPaintTrack());

        ms.setSnapToTicks(true);
        assertTrue(ms.getSnapToTicks());
        ms.setSnapToTicks(false);
        assertFalse(ms.getSnapToTicks());

        ms.setPaintLabels(true);
        assertTrue(ms.getPaintLabels());
        ms.setPaintLabels(false);
        assertFalse(ms.getPaintLabels());

        assertEquals(ms.getOrientation() == SwingConstants.HORIZONTAL, ms.isHorizontal());
        assertEquals(ms.getOrientation() == SwingConstants.VERTICAL, ms.isVertical());
        ms.setOrientation(1 - orientation);
        assertNotEquals(orientation, ms.getOrientation());
        assertEquals(ms.getOrientation() == SwingConstants.HORIZONTAL, ms.isHorizontal());
        assertEquals(ms.getOrientation() == SwingConstants.VERTICAL, ms.isVertical());
        ms.setOrientation(orientation);
        assertEquals(orientation, ms.getOrientation());
        assertEquals(ms.getOrientation() == SwingConstants.HORIZONTAL, ms.isHorizontal());
        assertEquals(ms.getOrientation() == SwingConstants.VERTICAL, ms.isVertical());
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
        MultiSlider ms = new MultiSlider(0, 100, orientation == SwingConstants.HORIZONTAL, new int[] {25, 50, 75});
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
            for (int i = 0; i < 10; i++)
            {
                ml.mousePressed(new MouseEvent(ms, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, x, y, x, y, 1,
                        false, MouseEvent.BUTTON1));
                Thread.sleep(50);
                assertNotEquals(-1, ms.getBusySlider());
                assertTrue(ms.isBusy());
                assertTrue(ms.isBusySlider(ms.getBusySlider()));
                // check there is only one busy slider
                int count = 0;
                for (int j = 0; j < ms.getNumberOfThumbs(); j++)
                {
                    count += ms.isBusySlider(j) ? 1 : 0;
                }
                assertEquals(1, count);
                ml.mouseReleased(new MouseEvent(ms, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, x, y, 1,
                        false, MouseEvent.BUTTON1));
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
        }

        Thread.sleep(250);

        String s = "0=" + ms.getValue(0) + ", 1=" + ms.getValue(1) + ", 2=" + ms.getValue(2);
        if (orientation == SwingConstants.HORIZONTAL)
        {
            if (inverted)
            {
                assertEquals(85, ms.getValue(2), "Hor, inv - " + s);
            }
            else
            {
                assertEquals(15, ms.getValue(0), "Hor, not - " + s);
            }
        }
        else
        {
            if (inverted)
            {
                assertEquals(15, ms.getValue(0), "vert, inv - " + s);
            }
            else
            {
                assertEquals(85, ms.getValue(2), "vert, not - " + s);
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
        MultiSlider ms = new MultiSlider(0, 100, new int[] {25, 50, 75});
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
        ms.setValue(0, 20);
        frame.revalidate();
        frame.pack();
        Thread.sleep(500);
        assertTrue(ms.getHeight() > oldHeight, "ms.GetHeight() = " + ms.getHeight() + "; oldHeight = " + oldHeight);

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setThumbLabel(-1, "x");
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setThumbLabel(1000, "x");
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.getThumbLabel(-1);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.getThumbLabel(1000);
            }
        });

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
        MultiSlider ms = new MultiSlider(0, 100, new int[] {25, 50, 75});
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
        UnitTest.testFail(new UnitTest.Assignment<MultiSlider>()
        {
            @Override
            public MultiSlider assign() throws Throwable
            {
                return new MultiSlider(0, 100, new int[] {});
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<MultiSlider>()
        {
            @Override
            public MultiSlider assign() throws Throwable
            {
                return new MultiSlider(0, 100, new int[] {-10});
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<MultiSlider>()
        {
            @Override
            public MultiSlider assign() throws Throwable
            {
                return new MultiSlider(0, 100, new int[] {50, 100, 150});
            }
        });

        UnitTest.testFail(new UnitTest.Assignment<MultiSlider>()
        {
            @Override
            public MultiSlider assign() throws Throwable
            {
                return new MultiSlider(200, 100, new int[] {150});
            }
        });

        final var ms = new MultiSlider(1, 10, new int[] {2, 4});
        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setValue(1, 20);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setValue(1, -20);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMaximum(0);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMaximum(1);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMinimum(20);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.setMinimum(10);
            }
        });
    }

    /**
     * Test making labels.
     */
    @Test
    public void testLabelTable()
    {
        var ms = new MultiSlider(1, 10, new int[] {2, 4});
        var labtab = ms.createStandardLabels(2, 2);
        assertEquals(5, labtab.size());
        assertEquals("2", ((JLabel) labtab.get(2)).getText());
        assertEquals("4", ((JLabel) labtab.get(4)).getText());
        assertEquals("6", ((JLabel) labtab.get(6)).getText());
        assertEquals("8", ((JLabel) labtab.get(8)).getText());
        assertEquals("10", ((JLabel) labtab.get(10)).getText());
        ms.setLabelTable(labtab);

        labtab = ms.createStandardLabels(2);
        assertEquals(5, labtab.size());
        assertEquals("1", ((JLabel) labtab.get(1)).getText());
        assertEquals("3", ((JLabel) labtab.get(3)).getText());
        assertEquals("5", ((JLabel) labtab.get(5)).getText());
        assertEquals("7", ((JLabel) labtab.get(7)).getText());
        assertEquals("9", ((JLabel) labtab.get(9)).getText());
        ms.setLabelTable(labtab);

        var lt2 = ms.getLabelTable();
        assertEquals(5, lt2.size());
        assertEquals("1", ((JLabel) lt2.get(1)).getText());
        assertEquals("3", ((JLabel) lt2.get(3)).getText());
        assertEquals("5", ((JLabel) lt2.get(5)).getText());
        assertEquals("7", ((JLabel) lt2.get(7)).getText());
        assertEquals("9", ((JLabel) lt2.get(9)).getText());

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.createStandardLabels(0);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.createStandardLabels(-1);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.createStandardLabels(1, -1);
            }
        });

        UnitTest.testFail(new UnitTest.Execution()
        {
            @Override
            public void execute() throws Throwable
            {
                ms.createStandardLabels(1, 1000);
            }
        });
    }

    /** array with values. */
    private int[] v;

    /**
     * @return v[0]
     */
    private int v0()
    {
        return this.v[0];
    }

    /**
     * @return v[1]
     */
    private int v1()
    {
        return this.v[1];
    }

    /**
     * Test change listeners.
     */
    @Test
    public void testChangeListeners()
    {
        var ms = new MultiSlider(0, 100, new int[] {20, 40});
        this.v = new int[] {20, 40};
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

        this.v = new int[] {25, 40};
        ms.setValue(0, 25);

        ms.removeChangeListener(cl);
        assertEquals(nrListeners, ms.getChangeListeners().length);
    }

    /**
     * Test final value change listeners.
     */
    @Test
    public void testFinalValueChangeListeners()
    {
        var ms = new MultiSlider(0, 100, new int[] {20, 40});
        this.v = new int[] {20, 40};
        int nrListeners = ms.getFinalValueChangeListeners().length;
        var cl = new MultiSlider.FinalValueChangeListener()
        {
            @Override
            public void stateChanged(final ChangeEvent e)
            {
                assertEquals(v0(), ms.getValue(0));
                assertEquals(v1(), ms.getValue(1));
            }
        };
        ms.addFinalValueChangeListener(cl);
        assertEquals(nrListeners + 1, ms.getFinalValueChangeListeners().length);

        this.v = new int[] {25, 40};
        ms.setValue(0, 25);
        this.v = new int[] {25, 60};
        ms.setValue(1, 60);

        ms.removeFinalValueChangeListener(cl);
        assertEquals(nrListeners, ms.getFinalValueChangeListeners().length);
    }

    /**
     * Test passing restrictions.
     */
    @Test
    public void testPassingRestrictions()
    {
        var ms = new MultiSlider(0, 100, new int[] {20, 40, 60});
        ms.setPassing(true);
        assertTrue(ms.getPassing());
        ms.setPassing(false);
        assertFalse(ms.getPassing());
        ms.setValue(0, 30);
        assertEquals(30, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(0, 40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(0, 45);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(2, 50);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(50, ms.getValue(2));
        ms.setValue(2, 40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));
        ms.setValue(2, 10);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));

        ms.setPassing(true);
        assertTrue(ms.getPassing());
        ms.setValue(0, 45);
        ms.setValue(2, 10);
        assertEquals(45, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(10, ms.getValue(2));
    }

    /**
     * Test overlap restrictions.
     */
    @Test
    public void testOverlapRestrictions()
    {
        // note that no overlap implies no passing.
        var ms = new MultiSlider(0, 100, new int[] {20, 40, 60});
        ms.setOverlap(true);
        assertTrue(ms.getOverlap());
        ms.setOverlap(false);
        assertFalse(ms.getOverlap());
        ms.setValue(0, 30);
        assertEquals(30, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(0, 40);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(0, 45);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.setValue(2, 50);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(50, ms.getValue(2));
        ms.setValue(2, 40);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.setValue(2, 10);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.setValue(1, 10);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.setValue(1, 50);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));

        ms.setOverlap(true);
        assertTrue(ms.getOverlap());
        ms.setValue(0, 40);
        ms.setValue(2, 40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));
    }

    /**
     * Test passing restrictions, based on changing the underlying slider values.
     */
    @Test
    public void testPassingRestrictionsSlider()
    {
        var ms = new MultiSlider(0, 100, new int[] {20, 40, 60});
        ms.setPassing(true);
        assertTrue(ms.getPassing());
        ms.setPassing(false);
        assertFalse(ms.getPassing());
        ms.getSlider(0).setValue(30);
        assertEquals(30, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(0).setValue(40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(0).setValue(45);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(2).setValue(50);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(50, ms.getValue(2));
        ms.getSlider(2).setValue(40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));
        ms.getSlider(2).setValue(10);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));

        ms.setPassing(true);
        assertTrue(ms.getPassing());
        ms.getSlider(0).setValue(45);
        ms.getSlider(2).setValue(10);
        assertEquals(45, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(10, ms.getValue(2));
    }

    /**
     * Test overlap restrictions.
     */
    @Test
    public void testOverlapRestrictionsSlider()
    {
        // note that no overlap implies no passing.
        var ms = new MultiSlider(0, 100, new int[] {20, 40, 60});
        ms.setOverlap(true);
        assertTrue(ms.getOverlap());
        ms.setOverlap(false);
        assertFalse(ms.getOverlap());
        ms.getSlider(0).setValue(30);
        assertEquals(30, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(0).setValue(40);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(0).setValue(45);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(60, ms.getValue(2));
        ms.getSlider(2).setValue(50);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(50, ms.getValue(2));
        ms.getSlider(2).setValue(40);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.getSlider(2).setValue(10);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.getSlider(2).setValue(10);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));
        ms.getSlider(1).setValue(50);
        assertEquals(39, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(41, ms.getValue(2));

        ms.setOverlap(true);
        assertTrue(ms.getOverlap());
        ms.getSlider(0).setValue(40);
        ms.getSlider(2).setValue(40);
        assertEquals(40, ms.getValue(0));
        assertEquals(40, ms.getValue(1));
        assertEquals(40, ms.getValue(2));
    }

    /**
     * Test passing restrictions, based on changing the min/max values.
     */
    @Test
    public void testPassingRestrictionsMinMax()
    {
        var ms = new MultiSlider(0, 100, true, 20, 40, 60);
        ms.setPassing(false);
        assertFalse(ms.getPassing());
        ms.setMinimum(50);
        assertEquals(50, ms.getValue(0));
        assertEquals(50, ms.getValue(1));
        assertEquals(60, ms.getValue(2));

        ms = new MultiSlider(0, 100, false, 20, 40, 60);
        ms.setPassing(false);
        assertFalse(ms.getPassing());
        ms.setMaximum(10);
        assertEquals(10, ms.getValue(0));
        assertEquals(10, ms.getValue(1));
        assertEquals(10, ms.getValue(2));
    }

    /**
     * Test overlap restrictions, based on changing the min/max values.
     */
    @Test
    public void testOverlapRestrictionsMinMax()
    {
        var ms = new MultiSlider(0, 100, true, new int[] {20, 40, 60});
        ms.setOverlap(false);
        assertFalse(ms.getOverlap());
        ms.setMinimum(50);
        assertEquals(50, ms.getValue(0));
        assertEquals(51, ms.getValue(1));
        assertEquals(60, ms.getValue(2));

        ms = new MultiSlider(0, 100, false, new int[] {20, 40, 60});
        ms.setOverlap(false);
        assertFalse(ms.getOverlap());
        ms.setMaximum(10);
        assertEquals(8, ms.getValue(0));
        assertEquals(9, ms.getValue(1));
        assertEquals(10, ms.getValue(2));
    }

    /**
     * Ensure Look and Feel exists in the headless case for the unit tests.
     * @param ms the multislider
     */
    private void setLookAndFeel(final MultiSlider ms)
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
