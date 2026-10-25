package org.djutils.swing.multislider;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;

import org.djutils.exceptions.Throw;
import org.djutils.logger.CategoryLogger;

/**
 * The {@code AbstractMultiSlider} forms the base of implementing a slider with multiple thumbs. The AbstractMultiSlider is
 * implemented by drawing a number of sliders on top of each other using an Swing {@code OverlayManager}, and passing the mouse
 * events from a glass pane on top to the correct slider(s). The class is a {@code ChangeListener} to listen to the changes of
 * individual sliders underneath.
 * <p>
 * Several models exist to indicate whether thumbs can pass each other or not, or be on top of each other or not.
 * <p>
 * The {@code AbstractMultiSlider} stores all values internally as int. Only when getting or setting values (or, e.g., the
 * minimum or maximum), the generic type T is used.
 * <p>
 * Copyright (c) 2024-2026 Delft University of Technology, Jaffalaan 5, 2628 BX Delft, the Netherlands. All rights reserved. See
 * for project information <a href="https://djutils.org" target="_blank"> https://djutils.org</a>. The DJUTILS project is
 * distributed under a three-clause BSD-style license, which can be found at
 * <a href="https://djutils.org/docs/license.html" target="_blank"> https://djutils.org/docs/license.html</a>.
 * <p>
 * @author Alexander Verbraeck
 * @param <T> the type of values that the {@code AbstractMultiSlider} stores and returns
 */
public abstract class AbstractMultiSlider<T> extends JComponent
{
    /** */
    private static final long serialVersionUID = 1L;

    /** the sliders that are stacked on top of each other. */
    private JSlider[] sliders;

    /** the current slider number in process (e.g., drag operation, mouse down). */
    private transient int busySlider = -1;

    /** whether an operation is busy or not. */
    private transient boolean busy = false;

    /** the 'glass pane' on top of the sliders to dispatch the mouse clicks and drags to the correct slider. */
    private final DispatcherPane dispatcherPane;

    /** the panel in which the thumb labels can be drawn if this is wanted. */
    private final LabelPanel labelPanel;

    /** the initial index values of the labels for the reset function. */
    private final int[] initialIndexValues;

    /** the last known index values of the labels for the state change function. */
    private final int[] lastIndexValues;

    /** the labels per thumb (per underlying slider). */
    private final Map<Integer, String> thumbLabels = new HashMap<>();

    /** whether we draw thumb labels or not. */
    private boolean drawThumbLabels = false;

    /** the track size lowest pixel (to calculate width for horizontal slider; height for vertical slider). */
    private int trackSizeLoPx;

    /** the track size highest pixel (to calculate width for horizontal slider; height for vertical slider). */
    private int trackSizeHiPx;

    /** MultiSlider restriction on passing. */
    private boolean passing = false;

    /** MultiSlider restriction on overlap. */
    private boolean overlap = true;

    /** busy testing whether the state change of an underlying slider is okay, to avoid stack overflow. */
    private boolean testingStateChange = false;

    /**
     * Only one <code>ChangeEvent</code> is needed for the {@code MultiSlider} since the event's only (read-only) state is the
     * source property. The source of events generated here is always "this". The event is created the first time that an event
     * notification is fired.
     */
    private transient ChangeEvent changeEvent = null;

    /**
     * Creates a slider with the specified orientation and the specified minimum, maximum, and initial values. The orientation
     * can be either horizontal or vertical.
     * @param minIndex the minimum index value of the slider
     * @param maxIndex the maximum index value of the slider
     * @param horizontal the orientation of the slider; true for horizontal, false for vertical
     * @param initialIndexValues the initial index values of the thumbs of the slider
     * @throws IllegalArgumentException if initial values are outside the min-max range, or if the number of thumbs is 0, or
     *             when the values are not in increasing order (which is important for restricting passing and overlap)
     */
    public AbstractMultiSlider(final int minIndex, final int maxIndex, final boolean horizontal,
            final int... initialIndexValues)
    {
        Throw.when(initialIndexValues.length == 0, IllegalArgumentException.class, "the number of thumbs cannot be zero");
        Throw.when(minIndex >= maxIndex, IllegalArgumentException.class, "min should be less than max");
        for (int i = 0; i < initialIndexValues.length; i++)
        {
            Throw.when(initialIndexValues[i] < minIndex || initialIndexValues[i] > maxIndex, IllegalArgumentException.class,
                    "all initial value should be between min and max (inclusive)");
            Throw.when(i > 0 && initialIndexValues[i] < initialIndexValues[i - 1], IllegalArgumentException.class,
                    "all initial value should be in increasing order or overlap");
        }

        // store the initial value array in a safe copy
        this.initialIndexValues = new int[initialIndexValues.length];
        this.lastIndexValues = new int[initialIndexValues.length];

        // based on the orientation, add a JPanel with two subpanels: one for the labels, and one for the sliders
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, horizontal ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));
        topPanel.setOpaque(false);
        add(topPanel, horizontal ? BorderLayout.NORTH : BorderLayout.WEST);

        // create the label panel
        this.labelPanel = new LabelPanel(this);
        this.labelPanel.setLayout(new BorderLayout());
        this.labelPanel.setPreferredSize(new Dimension(1000, 0));
        this.labelPanel.setOpaque(false);
        topPanel.add(this.labelPanel);

        // create the slider panel
        JPanel sliderPanel = new JPanel();
        sliderPanel.setOpaque(false);
        topPanel.add(sliderPanel);

        // put a glass pane on top that dispatches the mouse event to all panes
        this.dispatcherPane = new DispatcherPane(this);
        sliderPanel.add(this.dispatcherPane);

        // make the sliders and show them. Slider 0 at the bottom. This one will get ticks, etc.
        sliderPanel.setLayout(new OverlayLayout(sliderPanel));
        this.sliders = new JSlider[initialIndexValues.length];
        for (int i = initialIndexValues.length - 1; i >= 0; i--)
        {
            this.initialIndexValues[i] = initialIndexValues[i]; // create copy
            this.lastIndexValues[i] = initialIndexValues[i];
            var slider = new JSlider(horizontal ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL, minIndex, maxIndex,
                    initialIndexValues[i]);
            this.sliders[i] = slider;
            slider.setOpaque(false);
            slider.setPaintTrack(i == 0);
            sliderPanel.add(slider);

            // ensure movability of the slider by setting it again
            slider.setValue(initialIndexValues[i]);

            // set the initial labels (issue #71)
            this.thumbLabels.put(i, "");
        }

        this.dispatcherPane.setPreferredSize(new Dimension(this.sliders[0].getSize()));
        calculateTrackSize();

        // listen to resize events to (re)set the track width or height
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(final ComponentEvent e)
            {
                super.componentResized(e);
                AbstractMultiSlider.this.setUI(getUI());
                calculateTrackSize();
                AbstractMultiSlider.this.dispatcherPane.revalidate();
                AbstractMultiSlider.this.labelPanel.revalidate();
                AbstractMultiSlider.this.labelPanel.repaint();
            }
        });

        // listen to state changes of underlying sliders and check if restrictions are met
        for (int i = 0; i < this.sliders.length; i++)
        {
            final int index = i;
            this.sliders[index].addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(final ChangeEvent e)
                {
                    if (!AbstractMultiSlider.this.testingStateChange)
                    {
                        AbstractMultiSlider.this.testingStateChange = true;
                        checkRestrictions(index);
                        AbstractMultiSlider.this.testingStateChange = false;
                    }
                    AbstractMultiSlider.this.fireStateChanged();
                }
            });
        }

        invalidate();
    }

    /**
     * Return the individual sliders, where slider[0] contains the formatting.
     * @return the individual sliders
     */
    protected JSlider[] getSliders()
    {
        return this.sliders;
    }

    /**
     * Return an individual slider with index i.
     * @param i the index for which to retrieve the slider
     * @return the individual slider with index i
     */
    public JSlider getSlider(final int i)
    {
        return this.sliders[i];
    }

    /**
     * Return the number of thumbs on this multislider.
     * @return the number of thumbs on this multislider
     */
    public int getNumberOfThumbs()
    {
        return this.sliders.length;
    }

    /**
     * Indicate that slider i is busy (e.g., mouse-down or a drag operation).
     * @param i the slider number that is busy
     */
    protected void setBusySlider(final int i)
    {
        this.busySlider = i;
    }

    /**
     * Return whether slider i is busy (e.g., mouse-down or a drag operation).
     * @param i the slider number to check
     * @return whether slier i is busy or not
     */
    protected boolean isBusySlider(final int i)
    {
        return this.busySlider == i;
    }

    /**
     * Return which slider is busy (e.g., mouse-down or a drag operation). The function returns -1 if no slider is busy.
     * @return the slider number of the busy slider, or -1 if no slider is busy with an action
     */
    protected int getBusySlider()
    {
        return this.busySlider;
    }

    /**
     * Return whether one of the sliders is busy (e.g., mouse-down or a drag operation). Note that the 'busy' flag is set BEFORE
     * the mouse event (e.g., mouse released, mouse exited) is handled. This means that when 'busy' is true, no operation is
     * taking place.
     * @return whether one of the sliders is busy or not
     */
    public boolean isBusy()
    {
        return this.busy;
    }

    /**
     * Set whether one of the sliders is busy (e.g., mouse-down or a drag operation). Note that the 'busy' flag has to be set
     * BEFORE the mouse event (e.g., mouse released, mouse exited) is handled. This means that when 'busy' is true, no operation
     * is taking place.
     * @param busy set whether one of the sliders is busy or not
     */
    protected void setBusy(final boolean busy)
    {
        this.busy = busy;
    }

    /**
     * Reset the slider values to the initial values.
     */
    public void resetToInitialValues()
    {
        for (int i = 0; i < this.sliders.length; i++)
        {
            this.sliders[i].setValue(this.initialIndexValues[i]);
            this.sliders[i].invalidate();
            this.sliders[i].repaint();
        }
        fireFinalValueChanged();
        invalidate();
        repaint();
    }

    /**
     * Return the label panel in which thumb labels can be drawn. The labels move with the thumbs.
     * @return the label panel in which thumb labels can be drawn
     */
    protected LabelPanel getLabelPanel()
    {
        return this.labelPanel;
    }

    /**
     * Set the thumb label for thumb i to the given label.
     * @param i the thumb number
     * @param label the label to display
     * @throws IndexOutOfBoundsException when thumb number is out of bounds
     */
    public void setThumbLabel(final int i, final String label)
    {
        Throw.when(i < 0 || i >= getNumberOfThumbs(), IndexOutOfBoundsException.class, "thumb number %d is out of bounds", i);
        this.thumbLabels.put(i, label);
        invalidate();
    }

    /**
     * Get the thumb label for thumb i.
     * @param i the thumb number
     * @return the label to display
     * @throws IndexOutOfBoundsException when thumb number is out of bounds
     */
    public String getThumbLabel(final int i)
    {
        Throw.when(i < 0 || i >= getNumberOfThumbs(), IndexOutOfBoundsException.class, "thumb number %d is out of bounds", i);
        return this.thumbLabels.get(i);
    }

    /**
     * Turn the thumb label display on or off.
     * @param b whether the thumbs are displayed or not
     * @param sizePx the height (for a horizontal slider) or width (for a vertical slider) of the label panel in pixels
     */
    public void setDrawThumbLabels(final boolean b, final int sizePx)
    {
        calculateTrackSize();
        JSlider js = getSlider(0);
        this.drawThumbLabels = b;
        if (b)
        {
            if (isHorizontal())
            {
                this.labelPanel.setPreferredSize(new Dimension(js.getWidth(), sizePx));
            }
            else
            {
                this.labelPanel.setPreferredSize(new Dimension(sizePx, js.getHeight()));
            }
        }
        else
        {
            if (isHorizontal())
            {
                this.labelPanel.setPreferredSize(new Dimension(js.getWidth(), 0));
            }
            else
            {
                this.labelPanel.setPreferredSize(new Dimension(0, js.getHeight()));
            }
        }
        this.labelPanel.revalidate();
        revalidate();
    }

    /**
     * Return whether thumb label display on or off.
     * @return whether the thumbs are displayed or not
     */
    public boolean isDrawThumbLabels()
    {
        return this.drawThumbLabels;
    }

    /**
     * Recalculate the track size (width for horizontal slider; height for vertical slider) after a resize operation.
     */
    protected void calculateTrackSize()
    {
        JSlider js = getSlider(0);
        BasicSliderUI ui = (BasicSliderUI) js.getUI();
        int loValue = getInverted() ? js.getMaximum() : js.getMinimum();
        int hiValue = getInverted() ? js.getMinimum() : js.getMaximum();
        if (isHorizontal())
        {
            this.trackSizeLoPx = 0;
            this.trackSizeHiPx = js.getWidth();
            int i = 0;
            while (i < js.getWidth() && ui.valueForXPosition(i) == loValue)
            {
                this.trackSizeLoPx = i++;
            }
            i = js.getWidth();
            while (i >= 0 && ui.valueForXPosition(i) == hiValue)
            {
                this.trackSizeHiPx = i--;
            }
        }
        else
        {
            this.trackSizeLoPx = 0;
            this.trackSizeHiPx = js.getHeight();
            int i = 0;
            while (i < js.getHeight() && ui.valueForYPosition(i) == hiValue)
            {
                this.trackSizeLoPx = i++;
            }
            i = js.getHeight();
            while (i >= 0 && ui.valueForYPosition(i) == loValue)
            {
                this.trackSizeHiPx = i--;
            }
        }

        // Adjust based on the number of values between minimum and maximum
        int nr = getIndexMaximum() - getIndexMinimum();
        int pxPerNr = (this.trackSizeHiPx - this.trackSizeLoPx) / nr;
        this.trackSizeLoPx = isHorizontal() ? this.trackSizeLoPx - pxPerNr / 2 : this.trackSizeLoPx + pxPerNr / 2;
        this.trackSizeHiPx = isHorizontal() ? this.trackSizeHiPx + pxPerNr / 2 : this.trackSizeHiPx - pxPerNr / 2;
    }

    /**
     * Calculate the track size (width for a horizontal slider; height for a vertical slider).
     * @return the track size (width for a horizontal slider; height for a vertical slider
     */
    protected int trackSize()
    {
        // recalculate the track size of the previous calculation was carried out before 'pack' or 'setSize'.
        if (this.trackSizeHiPx - this.trackSizeLoPx < 2)
        {
            calculateTrackSize();
        }
        return this.trackSizeHiPx - this.trackSizeLoPx;
    }

    /**
     * Return the track size lowest pixel (to calculate width for horizontal slider; height for vertical slider).
     * @return the track size lowest pixel
     */
    public int getTrackSizeLoPx()
    {
        return this.trackSizeLoPx;
    }

    /**
     * Return the track size highest pixel (to calculate width for horizontal slider; height for vertical slider).
     * @return the track size highest pixel
     */
    public int getTrackSizeHiPx()
    {
        return this.trackSizeHiPx;
    }

    /**
     * Calculate x pixel (horizontal) or y pixel (vertical) of thumb[i], relative to the panel of the JSlider.
     * @param i the slider number
     * @return the x pixel (horizontal) or y pixel (vertical) of thumb[i], relative to the panel of the JSlider
     */
    protected int thumbPositionPx(final int i)
    {
        JSlider slider = getSlider(i);
        int value = slider.getValue();
        int min = slider.getMinimum();
        int max = slider.getMaximum();
        int ts = trackSize();
        if (getInverted())
        {
            return this.trackSizeLoPx + (int) (1.0 * ts * (max - value) / (max - min));
        }
        return this.trackSizeLoPx + (int) (1.0 * ts * (value - min) / (max - min));
    }

    /**
     * Return the glass pane on top of the multislider.
     * @return the glass pane on top of the multislider
     */
    protected DispatcherPane getDispatcherPane()
    {
        return this.dispatcherPane;
    }

    /**
     * Gets the UI object which implements the L&amp;F for this component.
     * @return the SliderUI object that implements the Slider L&amp;F
     */
    @Override
    public SliderUI getUI()
    {
        return this.sliders[0].getUI();
    }

    /**
     * Sets the UI object which implements the L&amp;F for all underlying sliders.
     * @param ui the SliderUI L&amp;F object
     */
    public void setUI(final SliderUI ui)
    {
        for (var slider : this.sliders)
        {
            try
            {
                // BasicSliderUI only has a JSlider constructor in recent JDKs; other SliderUIs may only have a no-arg one
                slider.setUI(ui.getClass().getDeclaredConstructor(JSlider.class).newInstance(slider));
            }
            catch (NoSuchMethodException nsme)
            {
                try
                {
                    slider.setUI(ui.getClass().getDeclaredConstructor().newInstance());
                }
                catch (Exception exception)
                {
                    // silently fail
                }
            }
            catch (Exception exception)
            {
                // silently fail
            }
        }
        invalidate();
        calculateTrackSize();
    }

    /**
     * Resets the UI property to a value from the current look and feel for all underlying sliders.
     */
    @Override
    public void updateUI()
    {
        for (var slider : this.sliders)
        {
            slider.updateUI();
        }
        invalidate();
        calculateTrackSize();
    }

    /**
     * Returns the name of the L&amp;F class that renders this component.
     * @return the string "SliderUI"
     */
    @Override
    public String getUIClassID()
    {
        return this.sliders[0].getUIClassID();
    }

    /**
     * Adds a ChangeListener to the multislider.
     * @param listener the ChangeListener to add
     */
    public void addChangeListener(final ChangeListener listener)
    {
        this.listenerList.add(ChangeListener.class, listener);
    }

    /**
     * Removes a ChangeListener from the multislider.
     * @param listener the ChangeListener to remove
     */
    public void removeChangeListener(final ChangeListener listener)
    {
        this.listenerList.remove(ChangeListener.class, listener);
    }

    /**
     * Returns an array of all the <code>ChangeListener</code>s added to this MultiSlider with addChangeListener().
     * @return all of the <code>ChangeListener</code>s added or an empty array if no listeners have been added
     */
    public ChangeListener[] getChangeListeners()
    {
        return this.listenerList.getListeners(ChangeListener.class);
    }

    /**
     * Send a {@code ChangeEvent}, whose source is this {@code MultiSlider}, to all {@code ChangeListener}s that have registered
     * interest in {@code ChangeEvent}s. This method is called each time a {@code ChangeEvent} is received from the model of one
     * of the underlying sliders.
     * <p>
     * The event instance is created if necessary, and stored in {@code changeEvent}.
     * <p>
     */
    protected void fireStateChanged()
    {
        // See if an actual state change has occurred
        boolean changed = false;
        for (int i = 0; i < getNumberOfThumbs(); i++)
        {
            if (this.lastIndexValues[i] != this.sliders[i].getValue())
            {
                changed = true;
                this.lastIndexValues[i] = this.sliders[i].getValue();
            }
        }

        if (changed)
        {
            // Note that the listener array has the classes at the even places, and the listeners at the odd places (yuck).
            Object[] listeners = this.listenerList.getListenerList();
            for (int i = listeners.length - 2; i >= 0; i -= 2)
            {
                if (listeners[i] == ChangeListener.class)
                {
                    if (this.changeEvent == null)
                    {
                        this.changeEvent = new ChangeEvent(this);
                    }
                    ((ChangeListener) listeners[i + 1]).stateChanged(this.changeEvent);
                }
            }
        }
    }

    /**
     * Adds a FinalValueChangeListener to the multislider.
     * @param listener the FinalValueChangeListener to add
     */
    public void addFinalValueChangeListener(final FinalValueChangeListener listener)
    {
        this.listenerList.add(FinalValueChangeListener.class, listener);
    }

    /**
     * Removes a FinalValueChangeListener from the multislider.
     * @param listener the FinalValueChangeListener to remove
     */
    public void removeFinalValueChangeListener(final FinalValueChangeListener listener)
    {
        this.listenerList.remove(FinalValueChangeListener.class, listener);
    }

    /**
     * Returns an array of all the <code>FinalValueChangeListener</code>s added to this MultiSlider with
     * addFinalValueChangeListener().
     * @return all of the <code>FinalValueChangeListener</code>s added or an empty array if no listeners have been added
     */
    public FinalValueChangeListener[] getFinalValueChangeListeners()
    {
        return this.listenerList.getListeners(FinalValueChangeListener.class);
    }

    /**
     * Send a {@code ChangeEvent}, whose source is this {@code MultiSlider}, to all {@code FinalValueChangeListener}s that have
     * registered interest in {@code ChangeEvent}s. This method is called when a change is final, e.g., after setValue(...),
     * setInitialValues(), mouse up, and leaving the slider window after a drag event. Note that the {@code ChangeEvent}s are
     * NOT fired when a value of an underlying slider is changed directly. The regular ChangeListener does fire these changes.
     * <p>
     * The event instance is created if necessary, and stored in {@code changeEvent}.
     * <p>
     */
    protected void fireFinalValueChanged()
    {
        // Note that the listener array has the classes at the even places, and the listeners at the odd places (yuck).
        Object[] listeners = this.listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == FinalValueChangeListener.class)
            {
                if (this.changeEvent == null)
                {
                    this.changeEvent = new ChangeEvent(this);
                }
                ((FinalValueChangeListener) listeners[i + 1]).stateChanged(this.changeEvent);
            }
        }
    }

    /**
     * Translate an index to a value.
     * @param index the index on the slider scale to convert
     * @return the corresponding value
     */
    protected abstract T mapIndexToValue(int index);

    /**
     * Translate a value to an index.
     * @param value the value to convert to an index
     * @return the corresponding index
     * @throws IllegalArgumentException when value cannot be mapped onto an index
     */
    protected abstract int mapValueToIndex(T value);

    /**
     * Returns the slider's current value for slider[i].
     * @param i the thumb to retrieve the value from
     * @return the current value of slider[i]
     * @throws IllegalArgumentException when no value is present for the index
     */
    public T getValue(final int i)
    {
        return mapIndexToValue(getIndexValue(i));
    }

    /**
     * Sets the slider's current value to {@code value}. This method forwards the new value to the model. If the new value is
     * different from the previous value, all change listeners are notified.
     * @param i the thumb to set the value for
     * @param value the new value
     */
    public void setValue(final int i, final T value)
    {
        int n = mapValueToIndex(value);
        setIndexValue(i, n);
    }

    /**
     * Returns the slider's current index value for slider[i] from the {@code BoundedRangeModel}.
     * @param i the thumb to retrieve the value from
     * @return the current index value of slider[i]
     */
    public int getIndexValue(final int i)
    {
        return this.sliders[i].getModel().getValue();
    }

    /**
     * Sets the slider's current index value to {@code n}. This method forwards the new value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel}) handles any mathematical issues arising from assigning faulty
     * values. See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new value is different from the previous value, all change listeners are notified.
     * @param i the thumb to set the value for
     * @param n the new index value
     */
    protected void setIndexValue(final int i, final int n)
    {
        Throw.when(n < getIndexMinimum() || n > getIndexMaximum(), IllegalArgumentException.class,
                "setValue(%d) not in range [%d, %d]", n, getIndexMinimum(), getIndexMaximum());
        this.sliders[i].setValue(n);
        checkRestrictions(i);
        this.sliders[i].invalidate();
        this.sliders[i].repaint();
        fireFinalValueChanged();
    }

    /**
     * Returns the minimum value supported by the slider from the <code>BoundedRangeModel</code>.
     * @return the value of the model's minimum property
     */
    protected int getIndexMinimum()
    {
        return this.sliders[0].getMinimum();
    }

    /**
     * Sets the slider's minimum value to {@code minimum}. This method forwards the new minimum value to the models of all
     * underlying sliders.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel}) handles any mathematical issues arising from assigning faulty
     * values. See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new minimum value is different from the previous minimum value, all change listeners are notified.
     * @param minimum the new minimum
     */
    protected void setIndexMinimum(final int minimum)
    {
        Throw.when(minimum >= getIndexMaximum(), IllegalArgumentException.class, "setMinimum(%d) >= maximum %d", minimum,
                getIndexMaximum());
        int oldMin = getIndexMinimum();
        for (var slider : this.sliders)
        {
            slider.setMinimum(minimum);
        }
        checkRestrictions();
        for (var slider : this.sliders)
        {
            slider.invalidate();
        }
        invalidate();
        fireFinalValueChanged();
        firePropertyChange("minimum", Integer.valueOf(oldMin), Integer.valueOf(minimum));
        calculateTrackSize();
    }

    /**
     * Return the minimum value supported by the multislider.
     * @return the minimum typed value of the multislider
     */
    public T getMinimum()
    {
        return mapIndexToValue(getIndexMinimum());
    }

    /**
     * Set the minimum value supported by the multislider.
     * @param minimum the new minimum typed value of the multislider
     */
    public void setMinimum(final T minimum)
    {
        setIndexMinimum(mapValueToIndex(minimum));
    }

    /**
     * Returns the maximum value supported by the slider from the <code>BoundedRangeModel</code>.
     * @return the value of the model's maximum property
     */
    protected int getIndexMaximum()
    {
        return this.sliders[0].getMaximum();
    }

    /**
     * Sets the slider's maximum value to {@code maximum}. This method forwards the new maximum value to the models of all
     * underlying sliders.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel}) handles any mathematical issues arising from assigning faulty
     * values. See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new maximum value is different from the previous maximum value, all change listeners are notified.
     * @param maximum the new maximum
     */
    protected void setIndexMaximum(final int maximum)
    {
        Throw.when(maximum <= getIndexMinimum(), IllegalArgumentException.class, "setMaximum(%d) >= minimum %d", maximum,
                getIndexMinimum());
        int oldMax = getIndexMaximum();
        for (var slider : this.sliders)
        {
            slider.setMaximum(maximum);
        }
        checkRestrictions();
        for (var slider : this.sliders)
        {
            slider.invalidate();
        }
        fireFinalValueChanged();
        firePropertyChange("maximum", Integer.valueOf(oldMax), Integer.valueOf(maximum));
        invalidate();
        calculateTrackSize();
    }

    /**
     * Return the maximum value supported by the multislider.
     * @return the maximum typed value of the multislider
     */
    public T getMaximum()
    {
        return mapIndexToValue(getIndexMaximum());
    }

    /**
     * Set the maximum value supported by the multislider.
     * @param maximum the new maximum typed value of the multislider
     */
    public void setMaximum(final T maximum)
    {
        setIndexMaximum(mapValueToIndex(maximum));
    }

    /**
     * Returns the "extent" from the <code>BoundedRangeModel</code>. This represents the range of values "covered" by the thumb.
     * @return an int representing the extent
     */
    public int getExtent()
    {
        return this.sliders[0].getExtent();
    }

    /**
     * Sets the size of the range "covered" by the thumb for all underlying slider objects. Most look and feel implementations
     * will change the value by this amount if the user clicks on either side of the thumb. This method just forwards the new
     * extent value to the model.
     * <p>
     * The data model (an instance of {@code BoundedRangeModel}) handles any mathematical issues arising from assigning faulty
     * values. See the {@code BoundedRangeModel} documentation for details.
     * <p>
     * If the new extent value is different from the previous extent value, all change listeners are notified.
     * @param extent the new extent
     */
    public void setExtent(final int extent)
    {
        for (var slider : this.sliders)
        {
            slider.setExtent(extent);
        }
    }

    /**
     * Return this multislider's vertical or horizontal orientation.
     * @return {@code SwingConstants.VERTICAL} or {@code SwingConstants.HORIZONTAL}
     */
    public int getOrientation()
    {
        return this.sliders[0].getOrientation();
    }

    /**
     * Return whether the orientation of the multislider is horizontal or not.
     * @return true if the orientation of the multislider is horizontal, false when not.
     */
    public boolean isHorizontal()
    {
        return getOrientation() == SwingConstants.HORIZONTAL;
    }

    /**
     * Return whether the orientation of the multislider is vertical or not.
     * @return true if the orientation of the multislider is vertical , false when not.
     */
    public boolean isVertical()
    {
        return !isHorizontal();
    }

    /**
     * Set the slider's orientation to either {@code SwingConstants.VERTICAL} or {@code SwingConstants.HORIZONTAL}.
     * @param orientation {@code HORIZONTAL} or {@code VERTICAL}
     * @throws IllegalArgumentException if orientation is not one of {@code VERTICAL}, {@code HORIZONTAL}
     */
    public void setOrientation(final int orientation)
    {
        for (var slider : this.sliders)
        {
            slider.setOrientation(orientation);
            slider.invalidate();
        }
        this.dispatcherPane.setPreferredSize(new Dimension(this.sliders[0].getSize()));
        invalidate();
        calculateTrackSize();
    }

    @Override
    public void setFont(final Font font)
    {
        for (var slider : this.sliders)
        {
            slider.setFont(font);
            slider.invalidate();
        }
        invalidate();
        calculateTrackSize();
    }

    /**
     * Returns the dictionary of what labels to draw at which values.
     * @return the <code>Dictionary</code> containing labels and where to draw them
     */
    @SuppressWarnings("rawtypes")
    public Dictionary getLabelTable()
    {
        return this.sliders[0].getLabelTable();
    }

    /**
     * Specify what label will be drawn at any given value. The key-value pairs are of this format:
     * <code>{ Integer value, java.swing.JComponent label }</code>. An easy way to generate a standard table of value labels is
     * by using the {@code createStandardLabels} method.
     * @param labels new {@code Dictionary} of labels, or {@code null} to remove all labels
     */
    @SuppressWarnings("rawtypes")
    public void setLabelTable(final Dictionary labels)
    {
        for (var slider : this.sliders)
        {
            slider.setLabelTable(labels);
            slider.invalidate();
        }
        invalidate();
        calculateTrackSize();
    }

    /**
     * Creates a {@code Hashtable} of numerical text labels, starting at the slider minimum, and using the increment specified.
     * For example, if you call <code>createStandardLabels( 10 )</code> and the slider minimum is zero, then labels will be
     * created for the values 0, 10, 20, 30, and so on.
     * <p>
     * For the labels to be drawn on the slider, the returned {@code Hashtable} must be passed into {@code setLabelTable}, and
     * {@code setPaintLabels} must be set to {@code true}.
     * <p>
     * For further details on the makeup of the returned {@code Hashtable}, see the {@code setLabelTable} documentation.
     * @param increment distance between labels in the generated hashtable
     * @return a new {@code Hashtable} of labels
     * @throws IllegalArgumentException if {@code increment} is less than or equal to zero
     */
    public Hashtable<Integer, JComponent> createStandardLabels(final int increment)
    {
        return createStandardLabels(increment, getIndexMinimum());
    }

    /**
     * Creates a {@code Hashtable} of text labels, starting at the starting point specified, and using the increment specified.
     * For example, if you call <code>createStandardLabels( 10, 2 )</code>, then labels will be created for the index values 2,
     * 12, 22, 32, and so on.
     * <p>
     * For the labels to be drawn on the slider, the returned {@code Hashtable} must be passed into {@code setLabelTable}, and
     * {@code setPaintLabels} must be set to {@code true}.
     * <p>
     * For further details on the makeup of the returned {@code Hashtable}, see the {@code setLabelTable} documentation.
     * @param increment distance between labels in the generated hashtable
     * @param startIndex value at which the labels will begin
     * @return a new {@code Hashtable} of labels
     * @exception IllegalArgumentException if {@code start} is out of range, or if {@code increment} is less than or equal to
     *                zero
     */
    public Hashtable<Integer, JComponent> createStandardLabels(final int increment, final int startIndex)
    {
        Throw.when(increment <= 0, IllegalArgumentException.class, "increment should be > 0");
        Throw.when(startIndex < getIndexMinimum() || startIndex > getIndexMaximum(), IllegalArgumentException.class,
                "startIndex should be between minimum index and maximum index");
        Hashtable<Integer, JComponent> labels = new Hashtable<>();
        for (int i = startIndex; i <= getIndexMaximum(); i += increment)
        {
            labels.put(i, new JLabel(format(mapIndexToValue(i))));
        }
        return labels;
    }

    /**
     * Format a value for e.g., the labels of the slider. By default, the formatting is done with {@code toString()}, but this
     * can be overridden.
     * @param value the value to format
     * @return a formatted string representation of the value
     */
    protected String format(final T value)
    {
        return value.toString();
    }

    /**
     * Returns true if the value-range shown for the slider is reversed.
     * @return true if the slider values are reversed from their normal order
     */
    public boolean getInverted()
    {
        return this.sliders[0].getInverted();
    }

    /**
     * Specify true to reverse the value-range shown for the slider and false to put the value range in the normal order. The
     * order depends on the slider's <code>ComponentOrientation</code> property. Normal (non-inverted) horizontal sliders with a
     * <code>ComponentOrientation</code> value of <code>LEFT_TO_RIGHT</code> have their maximum on the right. Normal horizontal
     * sliders with a <code>ComponentOrientation</code> value of <code>RIGHT_TO_LEFT</code> have their maximum on the left.
     * Normal vertical sliders have their maximum on the top. These labels are reversed when the slider is inverted.
     * <p>
     * By default, the value of this property is {@code false}.
     * @param b true to reverse the slider values from their normal order
     */
    public void setInverted(final boolean b)
    {
        for (var slider : this.sliders)
        {
            slider.setInverted(b);
            slider.invalidate();
        }
        invalidate();
        calculateTrackSize();
    }

    /**
     * This method returns the major tick spacing. The number that is returned represents the distance, measured in values,
     * between each major tick mark. If you have a slider with a range from 0 to 50 and the major tick spacing is set to 10, you
     * will get major ticks next to the following values: 0, 10, 20, 30, 40, 50.
     * @return the number of values between major ticks
     */
    public int getMajorTickSpacing()
    {
        return this.sliders[0].getMajorTickSpacing();
    }

    /**
     * This method sets the major tick spacing. The number that is passed in represents the distance, measured in values,
     * between each major tick mark. If you have a slider with a range from 0 to 50 and the major tick spacing is set to 10, you
     * will get major ticks next to the following values: 0, 10, 20, 30, 40, 50.
     * <p>
     * In order for major ticks to be painted, {@code setPaintTicks} must be set to {@code true}.
     * <p>
     * This method will also set up a label table for you. If there is not already a label table, and the major tick spacing is
     * {@code > 0}, and {@code getPaintLabels} returns {@code true}, a standard label table will be generated (by calling
     * {@code createStandardLabels}) with labels at the major tick marks. For the example above, you would get text labels: "0",
     * "10", "20", "30", "40", "50". The label table is then set on the slider by calling {@code setLabelTable}.
     * @param n new value for the {@code majorTickSpacing} property
     */
    public void setMajorTickSpacing(final int n)
    {
        int oldValue = getMajorTickSpacing();
        for (var slider : this.sliders)
        {
            slider.setMajorTickSpacing(n);
            slider.invalidate();
        }
        firePropertyChange("majorTickSpacing", oldValue, n);
        invalidate();
    }

    /**
     * This method returns the minor tick spacing. The number that is returned represents the distance, measured in values,
     * between each minor tick mark. If you have a slider with a range from 0 to 50 and the minor tick spacing is set to 10, you
     * will get minor ticks next to the following values: 0, 10, 20, 30, 40, 50.
     * @return the number of values between minor ticks
     */
    public int getMinorTickSpacing()
    {
        return this.sliders[0].getMinorTickSpacing();
    }

    /**
     * This method sets the minor tick spacing. The number that is passed in represents the distance, measured in values,
     * between each minor tick mark. If you have a slider with a range from 0 to 50 and the minor tick spacing is set to 10, you
     * will get minor ticks next to the following values: 0, 10, 20, 30, 40, 50.
     * <p>
     * In order for minor ticks to be painted, {@code setPaintTicks} must be set to {@code true}.
     * @param n new value for the {@code minorTickSpacing} property
     * @see #getMinorTickSpacing
     * @see #setPaintTicks
     */
    public void setMinorTickSpacing(final int n)
    {
        int oldValue = getMinorTickSpacing();
        for (var slider : this.sliders)
        {
            slider.setMinorTickSpacing(n);
            slider.invalidate();
        }
        firePropertyChange("minorTickSpacing", oldValue, n);
        invalidate();
    }

    /**
     * Returns true if the thumb (and the data value it represents) resolve to the closest tick mark next to where the user
     * positioned the thumb.
     * @return true if the value snaps to the nearest tick mark, else false
     */
    public boolean getSnapToTicks()
    {
        return this.sliders[0].getSnapToTicks();
    }

    /**
     * Specifying true makes the thumb (and the data value it represents) resolve to the closest tick mark next to where the
     * user positioned the thumb. By default, this property is {@code false}.
     * @param b true to snap the thumb to the nearest tick mark
     */
    public void setSnapToTicks(final boolean b)
    {
        boolean oldValue = getSnapToTicks();
        for (var slider : this.sliders)
        {
            slider.setSnapToTicks(b);
        }
        firePropertyChange("snapToTicks", oldValue, b);
    }

    /**
     * Tells if tick marks are to be painted.
     * @return true if tick marks are painted, else false
     */
    public boolean getPaintTicks()
    {
        return this.sliders[0].getPaintTicks();
    }

    /**
     * Determines whether tick marks are painted on the slider. By default, this property is {@code false}.
     * @param b whether or not tick marks should be painted
     */
    public void setPaintTicks(final boolean b)
    {
        boolean oldValue = getPaintTicks();
        for (var slider : this.sliders)
        {
            slider.setPaintTicks(b);
            slider.invalidate();
        }
        firePropertyChange("paintTicks", oldValue, b);
        invalidate();
        calculateTrackSize();
    }

    /**
     * Tells if the track (area the slider slides in) is to be painted.
     * @return true if track is painted, else false
     */
    public boolean getPaintTrack()
    {
        return this.sliders[0].getPaintTrack();
    }

    /**
     * Determines whether the track is painted on the slider. By default, this property is {@code true}. It is up to the look
     * and feel to honor this property, some may choose to ignore it.
     * @param b whether or not to paint the slider track
     * @see #getPaintTrack
     */
    public void setPaintTrack(final boolean b)
    {
        boolean oldValue = getPaintTrack();
        this.sliders[0].setPaintTrack(b);
        firePropertyChange("paintTrack", oldValue, b);
        calculateTrackSize();
    }

    /**
     * Tells if labels are to be painted.
     * @return true if labels are painted, else false
     */
    public boolean getPaintLabels()
    {
        return this.sliders[0].getPaintLabels();
    }

    /**
     * Determines whether labels are painted on the slider.
     * <p>
     * This method will also set up a label table for you. If there is not already a label table, and the major tick spacing is
     * {@code > 0}, a standard label table will be generated (by calling {@code createStandardLabels}) with labels at the major
     * tick marks. The label table is then set on the slider by calling {@code setLabelTable}.
     * <p>
     * By default, this property is {@code false}.
     * @param b whether or not to paint labels
     */
    public void setPaintLabels(final boolean b)
    {
        boolean oldValue = getPaintLabels();
        for (var slider : this.sliders)
        {
            slider.setPaintLabels(b);
            slider.invalidate();
        }
        firePropertyChange("paintLabels", oldValue, b);
        invalidate();
        calculateTrackSize();
    }

    /**
     * Set whether passing of the thumbs is allowed, and check whether thumb values are in line with restrictions.
     * @param b whether passing of the thumbs is allowed or not
     */
    public void setPassing(final boolean b)
    {
        this.passing = b;
        if (!this.passing)
        {
            checkRestrictions();
        }
    }

    /**
     * Return whether passing of the thumbs is allowed.
     * @return whether passing of the thumbs is allowed or not
     */
    public boolean getPassing()
    {
        return this.passing;
    }

    /**
     * Set whether overlap of the thumbs is allowed, and check whether thumb values are in line with restrictions.
     * @param b whether overlap of the thumbs is allowed or not
     */
    public void setOverlap(final boolean b)
    {
        this.overlap = b;
        if (!this.overlap)
        {
            checkRestrictions();
        }
    }

    /**
     * Return whether overlap of the thumbs is allowed.
     * @return whether overlap of the thumbs is allowed or not
     */
    public boolean getOverlap()
    {
        return this.overlap;
    }

    /**
     * Check restrictions on all thumb values and correct values where necessary.
     * @return whether compliance with the restrictions is ok; false means violation
     */
    protected boolean checkRestrictions()
    {
        boolean ret = true;
        if (!getPassing())
        {
            for (int i = 1; i < getNumberOfThumbs(); i++)
            {
                // see if we need to push values 'up'
                if (getIndexValue(i) <= getIndexValue(i - 1))
                {
                    getSlider(i).setValue(getIndexValue(i - 1));
                    ret = false;
                }
            }
            for (int i = getNumberOfThumbs() - 1; i >= 1; i--)
            {
                // see if we need to push values 'down'
                if (getIndexValue(i) <= getIndexValue(i - 1))
                {
                    getSlider(i - 1).setValue(getIndexValue(i));
                    ret = false;
                }
            }
        }
        if (!getOverlap())
        {
            for (int i = 1; i < getNumberOfThumbs(); i++)
            {
                // see if we need to push values 'up'
                if (getIndexValue(i) <= getIndexValue(i - 1))
                {
                    getSlider(i).setValue(getIndexValue(i - 1) + 1);
                    ret = false;
                }
            }
            for (int i = getNumberOfThumbs() - 1; i >= 1; i--)
            {
                // see if we need to push values 'down'
                if (getIndexValue(i) <= getIndexValue(i - 1))
                {
                    getSlider(i - 1).setValue(getIndexValue(i) - 1);
                    ret = false;
                }
            }
        }
        if (!ret)
        {
            invalidate();
            repaint();
        }
        return ret;
    }

    /**
     * Check restrictions on the thumb values of thumb 'index' and correct values where necessary.
     * @param index the slider for which to check (the only one whose value should change)
     * @return whether compliance with the restrictions is ok; false means violation
     */
    protected boolean checkRestrictions(final int index)
    {
        boolean ret = true;
        if (!getPassing())
        {
            if (index > 0 && getIndexValue(index) <= getIndexValue(index - 1))
            {
                getSlider(index).setValue(getIndexValue(index - 1));
                ret = false;
            }
            if (index < getNumberOfThumbs() - 1 && getIndexValue(index) >= getIndexValue(index + 1))
            {
                getSlider(index).setValue(getIndexValue(index + 1));
                ret = false;
            }
        }
        if (!getOverlap())
        {
            if (index > 0 && getIndexValue(index) <= getIndexValue(index - 1))
            {
                getSlider(index).setValue(getIndexValue(index - 1) + 1);
                ret = false;
            }
            if (index < getNumberOfThumbs() - 1 && getIndexValue(index) >= getIndexValue(index + 1))
            {
                getSlider(index).setValue(getIndexValue(index + 1) - 1);
                ret = false;
            }
        }
        if (!ret)
        {
            getSlider(index).invalidate();
            getSlider(index).repaint();
        }
        return ret;
    }

    /**
     * The DispatcherPane class, which is a glass pane sitting on top of the sliders to dispatch the mouse event to the correct
     * slider class. Note that the mouse coordinates are relative to the component itself, so a translation might be needed. The
     * <code>SwingUtilities.convertPoint()</code> method can make the conversion.
     */
    protected static class DispatcherPane extends JComponent
    {
        /** */
        private static final long serialVersionUID = 1L;

        /** the pointer to the multislider object; protected to access it by the mouse handlers. */
        @SuppressWarnings("checkstyle:visibilitymodifier")
        protected final AbstractMultiSlider<?> multiSlider;

        /**
         * Return the closest slider number based on x (horizontal) or y (vertical) locations of the thumbs. When two or more
         * thumbs are at the exact same distance (e.g., because they overlap), the first slider found that has a movement option
         * in the direction of the mouse will be returned.
         * @param p the point (e.g., of a mouse position)
         * @return the index(es) of the closest slider(s)
         */
        int closestSliderIndex(final Point p)
        {
            if (this.multiSlider.getBusySlider() >= 0)
            {
                return this.multiSlider.getBusySlider();
            }

            int mindist = Integer.MAX_VALUE; // non-absolute lowest distance of (mouse) position in pixels
            int minIndex = -1; // int scale value of thumb at closest position
            int mini = -1; // thumb index of closest position
            int loi = Integer.MAX_VALUE; // lowest thumb number on closest position
            int hii = -1; // highest thumb number on closest position
            for (int i = 0; i < this.multiSlider.getNumberOfThumbs(); i++)
            {
                int posPx = this.multiSlider.thumbPositionPx(i);
                int dist = this.multiSlider.isHorizontal() ? posPx - p.x : posPx - (getHeight() - p.y);
                if (Math.abs(dist) == Math.abs(mindist))
                {
                    hii = i;
                }
                else if (Math.abs(dist) < Math.abs(mindist))
                {
                    mindist = dist;
                    mini = i;
                    minIndex = this.multiSlider.getIndexValue(i);
                    loi = i;
                    hii = i;
                }
            }

            // if only one closest slider (loi == hii), or no passing restrictions: move any slider!
            if (loi == hii || this.multiSlider.getPassing())
            {
                return mini;
            }
            if (minIndex == this.multiSlider.getIndexMinimum()) // hi movement only
            {
                return hii;
            }
            if (minIndex == this.multiSlider.getIndexMaximum()) // lo movement only
            {
                return loi;
            }
            if (mindist > 0) // mouse to the left
            {
                return loi;
            }
            return hii; // mouse to the right
        }

        /**
         * @param e the MouseEvent to dispatch to the sliders.
         * @param always indicates whether we always need to send the event
         * @return the slider index to which the event was dispatched; -1 if none
         */
        int dispatch(final MouseEvent e, final boolean always)
        {
            var slider = DispatcherPane.this.multiSlider.getSlider(0);
            Point pSlider = SwingUtilities.convertPoint(DispatcherPane.this, e.getPoint(), slider);
            if (always || (pSlider.x >= 0 && pSlider.x <= slider.getSize().width && pSlider.y >= 0
                    && pSlider.y <= slider.getSize().height))
            {
                int index = closestSliderIndex(pSlider);
                MouseEvent meSlider = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiersEx(),
                        pSlider.x, pSlider.y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
                try
                {
                    DispatcherPane.this.multiSlider.getSlider(index).dispatchEvent(meSlider);
                }
                catch (Exception exception)
                {
                    CategoryLogger.always().error(exception, "error dispatching mouseEvent {}", meSlider);
                }
                setBusySlider(index);
                return index;
            }
            return -1;
        }

        /**
         * Reset the busy slider -- action over. Call this method BEFORE processing the MouseEvent. In that way, the
         * ChangeListener will fire a StateChange while the busy slider is -1 -- indicating that there is a final value.
         * @param index the slider number that is busy, or -1 if none
         */
        void setBusySlider(final int index)
        {
            this.multiSlider.setBusySlider(index);
        }

        /**
         * Indicate whether the multislider is busy with handling an action (e.g., drag, mouse down). Note that the busy flag
         * has to be set BEFORE the mouse event is handled, to allow a listener to the ChangeEvent to only react when an action
         * is completed.
         * @param b whether the multislider is busy with handling an action or not
         */
        void setBusy(final boolean b)
        {
            this.multiSlider.setBusy(b);
        }

        /**
         * Check whether minimum, maximum, passing or overlap restrictions were violated, and if so, adjust.
         * @param index the slider number for which to check
         */
        void checkRestrictions(final int index)
        {
            this.multiSlider.checkRestrictions(index);
        }

        /**
         * Create a glass pane on top of the sliders.
         * @param multiSlider the multislider for which this is the glass pane
         */
        public DispatcherPane(final AbstractMultiSlider<?> multiSlider)
        {
            this.multiSlider = multiSlider;
            setOpaque(false);

            addMouseListener(new MouseListener()
            {
                @Override
                public void mouseReleased(final MouseEvent e)
                {
                    setBusy(false);
                    int index = dispatch(e, false);
                    if (index >= 0)
                    {
                        checkRestrictions(index);
                        DispatcherPane.this.multiSlider.fireFinalValueChanged();
                    }
                    setBusySlider(-1);
                }

                @Override
                public void mousePressed(final MouseEvent e)
                {
                    setBusy(true);
                    int index = dispatch(e, false);
                    setBusySlider(index);
                    if (index < 0)
                    {
                        setBusy(false);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e)
                {
                    setBusy(false);
                    // emulate a mouse release somewhere on the current slider to force correct update
                    var ms = DispatcherPane.this.multiSlider;
                    if (ms.getBusySlider() >= 0)
                    {
                        JSlider js = ms.getSlider(ms.getBusySlider());
                        checkRestrictions(ms.getBusySlider());
                        MouseEvent meSlider = new MouseEvent(js, MouseEvent.MOUSE_RELEASED, e.getWhen(), 0, 10, 10, 1, false,
                                MouseEvent.BUTTON1);
                        js.dispatchEvent(meSlider);
                        ms.fireFinalValueChanged();
                    }
                    setBusySlider(-1);
                }

                @Override
                public void mouseEntered(final MouseEvent e)
                {
                    // no action
                }

                @Override
                public void mouseClicked(final MouseEvent e)
                {
                    // completely caught by pressed + released (or pressed + dragged + released)
                }
            });

            addMouseMotionListener(new MouseMotionAdapter()
            {
                @Override
                public void mouseDragged(final MouseEvent e)
                {
                    setBusy(true);
                    int index = dispatch(e, false);
                    setBusySlider(index);
                    if (index < 0)
                    {
                        setBusy(false);
                    }
                    else
                    {
                        checkRestrictions(index);
                    }
                }

                @Override
                public void mouseMoved(final MouseEvent e)
                {
                    // Note: possibly we can trigger an 'mouse entered' when the mouse enters the slider pane,
                    // and a 'mouse exited' when the mouse exits the slider pane.
                }
            });
        }
    }

    /**
     * The LabelPanel is draw above a horizontal slider or left of a vertical slider and displays labels for the thumbs of the
     * slider, so one can see which one is which.
     */
    protected static class LabelPanel extends JPanel
    {
        /** */
        private static final long serialVersionUID = 1L;

        /** a pointer to the multislider. */
        private final AbstractMultiSlider<?> multiSlider;

        /**
         * Default constructor.
         * @param multiSlider the multislider for which this is the LabelPanel.
         */
        public LabelPanel(final AbstractMultiSlider<?> multiSlider)
        {
            this.multiSlider = multiSlider;
            repaint();
            this.multiSlider.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(final ChangeEvent e)
                {
                    repaint();
                }
            });
        }

        @Override
        public void paintComponent(final Graphics g)
        {
            super.paintComponent(g);
            if (LabelPanel.this.multiSlider.isDrawThumbLabels())
            {
                for (int i = 0; i < this.multiSlider.getNumberOfThumbs(); i++)
                {
                    int pos = this.multiSlider.thumbPositionPx(i);
                    String s = this.multiSlider.getThumbLabel(i);
                    int sw = g.getFontMetrics().stringWidth(s);
                    int sh = g.getFontMetrics().getHeight();
                    if (this.multiSlider.isHorizontal())
                    {
                        g.drawString(s, pos - sw / 2, 12);
                    }
                    else
                    {
                        g.drawString(s, getWidth() - sw - 10, getHeight() - pos + sh / 2 - 3);
                    }
                }
            }
        }
    }

    /**
     * The FinalValueChangeListener sends a final value to the listeners after mouse-up, leave focus, setValue(...), and
     * setInitialValues().
     */
    public interface FinalValueChangeListener extends ChangeListener
    {
        // no extra events
    }
}
