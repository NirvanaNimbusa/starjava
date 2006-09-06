/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     20-JUN-2005 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.Color;
import uk.ac.starlink.diva.FigureChangedEvent;
import uk.ac.starlink.diva.XRangeFigure;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Statistics;
import uk.ac.starlink.splat.util.NumericIntegrator;

/**
 * StatsRange extends the {@link XGraphicsRange} class to add four or five new
 * rows that contain statistics values for the spectrum being drawn over (the
 * current spectrum of a {@link PlotControl}).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class StatsRange
    extends XGraphicsRange
{
    /** The PlotControl instance. */
    protected PlotControl control = null;

    /** The Statistics instance. */
    protected Statistics stats = new Statistics( new double[] {0.0} );

    /** The NumericIntegrator instance */
    protected NumericIntegrator integ = new NumericIntegrator();

    /**
     * Create a range interactively or non-interactively.
     *
     * @param control PlotControl that is to display the range.
     * @param model StatsRangesModel model that arranges to have the
     *              properties of the range displayed (may be null).
     * @param colour the colour of any figures.
     * @param constrain whether figures are fixed to move only in X and have
     *                  initial size of the full Y dimension of plot.
     * @param range a pair of doubles containing the range (in physical
     *              coordinates) to be used. Set null if the figure is to be
     *              created interactively.
     */
    public StatsRange( PlotControl control, StatsRangesModel model,
                       Color colour, boolean constrain, double[] range )
    {
        super( control.getPlot(), model, colour, constrain );
        setControl( control );
        
        //  Do this after construction to avoid problems with initialization
        //  order.
        if ( range == null ) {
            startInteraction();
        }
        else {
            createFromRange( range );
        }
    }

    /**
     * Set the PlotControl instance to use.
     *
     * @param control The PlotControl instance.
     */
    protected void setControl( PlotControl control )
    {
        this.control = control;
        DivaPlot newPlot = control.getPlot();
        if ( newPlot != plot ) {
            setPlot( newPlot );
        }
    }

    /**
     * Make the current statistics match the current range for the current
     * spectrum shown in the assocated plot.
     */
    public void updateStats()
    {
        SpecData currentSpectrum = control.getCurrentSpectrum();
        if ( currentSpectrum != null ) {

            //  Extract the data from the spectrum.
            double[] range = getRange();
            int[] lower = currentSpectrum.bound( range[0] );
            int[] higher = currentSpectrum.bound( range[1] );
            if ( lower[1] > higher[0] ) {
                //  Swap when coordinates run right to left.
                int[] temp = lower;
                lower = higher;
                higher = temp;
            }
            int nvals = higher[0] - lower[1] + 1;

            if ( nvals > 0 ) {
                double[] data = currentSpectrum.getYData();
                int low = lower[1];
                int high = higher[0];

                //  Test for presence of BAD values in the data. These are not
                //  allowed in the final data.
                int n = 0;
                int t = 0;
                for ( int i = low; i <= high; i++ ) {
                    if ( data[i] != SpecData.BAD ) n++;
                }

                //  Now allocate the necessary memory and copy in the data.
                boolean showFlux = ((StatsRangesModel)model).getShowFlux();
                boolean monotonic = currentSpectrum.isMonotonic();
                double[] rangeData = new double[n];
                double[] rangeCoords = null;
                if ( showFlux && monotonic ) { 
                    rangeCoords = new double[n];
                    if ( n > 1 ) {
                        double[] coords = currentSpectrum.getXData();
                        n = 0;
                        for ( int i = low; i <= high; i++ ) {
                            if ( data[i] != SpecData.BAD ) {
                                rangeData[n] = data[i];
                                rangeCoords[n] = coords[i];
                                n++;
                            }
                        }
                        
                        //  Set up for flux estimates.
                        integ.setData( rangeCoords, rangeData );
                    }
                    else {
                        // No flux for one point.
                        rangeData = new double[2];
                        rangeCoords = new double[2];
                        rangeData[0] = 0.0;
                        rangeData[1] = 0.0;
                        rangeCoords[0] = 0.0;
                        rangeCoords[1] = 1.0;
                        integ.setData( rangeCoords, rangeData );
                    }
                }
                else {
                    n = 0;
                    for ( int i = low; i <= high; i++ ) {
                        if ( data[i] != SpecData.BAD ) {
                            rangeData[n] = data[i];
                            n++;
                        }
                    }
                }

                //  Perform stats...
                stats.setData( rangeData );
            }
        }
    }

    /**
     * Get mean value of the spectrum in this range.
     */
    public double getMean()
    {
        return stats.getMean();
    }

    /**
     * Get standard deviation of the spectrum in this range.
     */
    public double getStandardDeviation()
    {
        return stats.getStandardDeviation();
    }

    /**
     * Get minimum value of spectrum in this range.
     */
    public double getMin()
    {
        return stats.getMinimum();
    }

    /**
     * Get maximum value of spectrum in this range.
     */
    public double getMax()
    {
        return stats.getMaximum();
    }

    /**
     * Get the flux estimate of the spectrum in this range.
     */
    public double getFlux()
    {
        return integ.getIntegral();
    }

    //
    //  FigureListener interface.
    //

    /**
     * Sent when the figure is created.
     */
    public void figureCreated( FigureChangedEvent e )
    {
        if ( e.getSource() instanceof XRangeFigure ) {
            super.figureCreated( e );
            updateStats();
        }
    }

    /**
     * Sent when the figure is changed (i.e.&nbsp;moved or transformed).
     */
    public void figureChanged( FigureChangedEvent e )
    {
        updateStats();
        super.figureChanged( e );
    }
}
