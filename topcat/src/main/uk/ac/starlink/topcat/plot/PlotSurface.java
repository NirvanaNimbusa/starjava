package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Shape;
import java.awt.print.PageFormat;
import javax.swing.JComponent;

/**
 * Defines a surface onto which plots are made.
 * This surface will typically deal with drawing axes and labels
 * and so on.
 *
 * <p>Two coordinate spaces are important when dealing with a PlotSurface:
 * graphics space is referenced in integer coordinates and refers to the
 * coordinates you deal with when you have a {@link java.awt.Graphics} object,
 * and data space is referenced in double coordinates and is
 * the space in which the data points live.
 * PlotSurface defines how to do the necessary conversions between them.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Jun 2004
 */
public interface PlotSurface {

    /**
     * Converts a point in data space to graphics space.
     * If the point does not lie within the currently visible plotting
     * area, <tt>null</tt> should be returned.
     *
     * @param  x  data space X coordinate
     * @param  y  data space Y coordinate
     * @return  point in graphics space corresponding to (x,y), or <tt>null</tt>
     */
    Point dataToGraphics( double x, double y );

    /**
     * Returns the clip region in which points may be plotted.
     * The returned shape should be the sort which can be passed to
     * {@link java.awt.Graphics#setClip(java.awt.Shape)} - i.e. probably
     * a <tt>Rectangle</tt>.
     *
     * @return   clip region representing data zone
     */
    Shape getClip();

    /**
     * Signals to the plot the characteristics of the plot which will 
     * be performed.  Setting this has no immediate effect, but 
     * when the component supplied by {@link #getComponent} 
     * next paints itself it should do so following the specifications
     * made here.
     *
     * @param  state  plot characteristics
     */
    void setState( PlotState state );

    /**
     * Requests a range of data space values to be visible on
     * this plotting surface.  The requested values are a hint which 
     * may (though probably shouldn't) be ignored, and in particular
     * may be overridden by other considerations at a later date,
     * for instance some sort of zoom action initiated by the user and
     * detected by the graphical component.
     * 
     * @param  xlo  (approximate) lower bound of X coordinate
     * @param  ylo  (approximate) lower bound of Y coordinate
     * @param  xhi  (approximate) upper bound of X coordinate
     * @param  yhi  (approximate) upper bound of Y coordinate
     */
    void setDataRange( double xlo, double ylo, double xhi, double yhi );

    /**
     * Returns the graphical component on which the plotting surface is
     * displayed.  This will contain things like axes, grids, labels etc.
     * This component will normally override
     * {@link javax.swing.JComponent#paintComponent}
     * to give a plotting background in accordance with the most recently
     * set <tt>PlotState</tt>.
     *
     * @return  plot surface display component
     */
    JComponent getComponent();

    /**
     * Paints the plotting surface.
     * This should do roughly the same as <tt>getComponent.paintComponent</tt>,
     * except that it's public.
     *
     * <p>Requiring this here isn't very tidy, but following quite a bit of
     * experimentation I can't work out any other way to do scatter plot
     * image caching while still drawing to a potentially 
     * hardware-accelerated graphics context 
     * (see {@link ScatterPlot} implementation).
     *
     * @param  g  graphics context
     */
    void paintSurface( Graphics g );
}
