package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Shape;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.OverlayLayout;
import uk.ac.starlink.topcat.RowSubset;

/**
 * Component which can display a scatter plot of points.
 * The details of the plot are determined by a {@link PlotState} object
 * which indicates what the plot will look like and a {@link Points}
 * object which provides the data to plot.  Setting these values does
 * not itself trigger a change in the component, they only take effect
 * when {@link #paintComponent} is called (e.g. following a {@link #repaint}
 * call).  The X and Y ranges of the displayed plot are not specified
 * programatically; they may be changed by user interaction.
 * The drawing of axes and other decorations is done by a decoupled
 * {@link PlotSurface} object (bridge pattern).
 * 
 * @author   Mark Taylor (Starlink)
 * @since    17 Jun 2004
 */
public class ScatterPlot extends JComponent implements Printable {

    private Points points_;
    private PlotState state_;
    private PlotSurface surface_;

    private static final String PRINT_MIME_TYPE = "application/postscript";

    /**
     * Constructs a new scatter plot, specifying the initial plotting surface
     * which provides axis plotting and so on.
     *
     * @param  surface  plotting surface implementation
     */
    public ScatterPlot( PlotSurface surface ) {
        setLayout( new OverlayLayout( this ) );
        add( new ScatterDataPanel() );
        setSurface( surface );
    }
    
    /**
     * Sets the plotting surface which draws axes and other decorations
     * that form the background to the actual plotted points.
     *
     * @param  surface  plotting surface implementation
     */
    public void setSurface( PlotSurface surface ) {
        if ( surface_ != null ) {
            remove( surface_.getComponent() );
        }
        surface_ = surface;
        surface_.setState( state_ );
        add( surface_.getComponent() );
    }

    /**
     * Returns the plotting surface on which this component displays.
     *
     * @return   plotting surface
     */
    public PlotSurface getSurface() {
        return surface_;
    }

    /**
     * Sets the data set for this plot.  These are the points which will
     * be plotted the next time this component is painted.
     *
     * @param   points  data points
     */
    public void setPoints( Points points ) {
        points_ = points;
    }

    /**
     * Returns the data set for this point.
     * 
     * @return  data points
     */
    public Points getPoints() {
        return points_;
    }

    /**
     * Sets the plot state for this plot.  This characterises how the
     * plot will be done next time this component is painted.
     *
     * @param  state  plot state
     */
    public void setState( PlotState state ) {
        state_ = state;
        if ( surface_ != null ) {
            surface_.setState( state_ );
        }
    }

    /**
     * Returns the most recently set state for this plot.
     *
     * @return  plot state
     */
    public PlotState getState() {
        return state_;
    }

    /**
     * Updates the X and Y ranges of the plotting surface so that all the
     * data points which are currently selected for plotting will fit in
     * nicely.
     */
    public void rescale() {
        boolean xlog = state_.xLog_;
        boolean ylog = state_.yLog_;
        double xlo = Double.POSITIVE_INFINITY;
        double xhi = xlog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;
        double ylo = Double.POSITIVE_INFINITY;
        double yhi = ylog ? Double.MIN_VALUE : Double.NEGATIVE_INFINITY;

        /* Go through all points getting max/min values. */
        int nok = 0;
        Points points = points_;
        if ( points != null ) {
            RowSubset[] rsets = getState().usedSubsets_;
            int nrset = rsets.length;
            int np = points.getCount();
            double[] xv = points.getXVector();
            double[] yv = points.getYVector();
            for ( int ip = 0; ip < np; ip++ ) {

                /* First see if this point will be plotted. */
                boolean use = false;
                long lp = (long) ip;
                for ( int is = 0; ! use && is < nrset; is++ ) {
                    use = use || rsets[ is ].isIncluded( lp );
                }
                if ( use ) {
                    double xp = xv[ ip ];
                    double yp = yv[ ip ];
                    if ( ! Double.isNaN( xp ) && 
                         ! Double.isNaN( yp ) &&
                         ! Double.isInfinite( xp ) && 
                         ! Double.isInfinite( yp ) &&
                         ( ! xlog || xp > 0.0 ) &&
                         ( ! ylog || yp > 0.0 ) ) {
                        nok++;
                        if ( xp < xlo ) {
                            xlo = xp;
                        }
                        if ( xp > xhi ) {
                            xhi = xp;
                        }
                        if ( yp < ylo ) {
                            ylo = yp;
                        }
                        if ( yp > yhi ) {
                            yhi = yp;
                        }
                    }
                }
            }
        }

        /* Default to sensible ranges if we didn't find any good data. */
        if ( nok == 0 ) {
            xlo = xlog ? 1e-1 : -1.0;
            xhi = xlog ? 1e+1 : +1.0; 
            ylo = ylog ? 1e-1 : -1.0;
            yhi = ylog ? 1e+1 : +1.0;
        }

        /* Ask the plotting surface to set the new ranges accordingly. */
        surface_.setDataRange( xlo, ylo, xhi, yhi );
    }

    /**
     * Plots the points of this scatter plot onto a given graphics surface.
     *
     * @param  g  graphics context
     * @param  gs  surface which defines the mapping of data to graphics space
     */
    private void drawPoints( Graphics g, GraphicsSurface gs ) {
        Points points = points_;
        PlotState state = state_;

        Shape oldClip = g.getClip();
        g.setClip( gs.getClip() );

        double[] xv = points.getXVector();
        double[] yv = points.getYVector();
        int np = points.getCount();
        RowSubset[] sets = state.usedSubsets_;
        MarkStyle[] styles = state.styles_;
        int nset = sets.length;
        for ( int is = 0; is < nset; is++ ) {
            MarkStyle style = styles[ is ];
            int maxr = style.getMaximumRadius();
            for ( int ip = 0; ip < np; ip++ ) {
                if ( sets[ is ].isIncluded( (long) ip ) ) {
                    Point point = gs.dataToGraphics( xv[ ip ], yv[ ip ] );
                    if ( point != null ) {
                        int xp = point.x;
                        int yp = point.y;
                        if ( g.hitClip( xp, yp, maxr, maxr ) ) {
                            style.drawMarker( g, xp, yp );
                        }
                    }
                }
            }
        }
        g.setClip( oldClip );
    }

    /**
     * Implements {@link java.awt.print.Printable} interface.
     *
     */
    public int print( Graphics g, PageFormat pf, int pageIndex ) {
        if ( pageIndex == 0 ) {
            GraphicsSurface printSurface = surface_.print( g, pf );
            drawPoints( g, printSurface );
            return PAGE_EXISTS;
        }
        else {
            return NO_SUCH_PAGE;
        }
    }


    /**
     * Graphical component which does the actual plotting of the points.
     */
    private class ScatterDataPanel extends JComponent {
        ScatterDataPanel() {
            setOpaque( false );
        }
        protected void paintComponent( Graphics g ) {
            drawPoints( g, surface_ );
        }
    }

}
