package uk.ac.starlink.ttools.plot2.layer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.Axis;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.LayerOpt;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ReportKey;
import uk.ac.starlink.ttools.plot2.ReportMap;
import uk.ac.starlink.ttools.plot2.ReportMeta;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.DoubleArrayConfigKey;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;
import uk.ac.starlink.ttools.plot2.geom.PlanarSurface;
import uk.ac.starlink.ttools.plot2.geom.SliceDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType;

/**
 * Plotter to calculate and display univariate statistics
 * of histogram-like data.
 *
 * @author   Mark Taylor
 * @since    30 Sep 2016
 */
public class Stats1Plotter implements Plotter<Stats1Plotter.StatsStyle> {

    private final FloatingCoord xCoord_;
    private final FloatingCoord weightCoord_;
    private final ConfigKey<Normalisation> normKey_;
    private final SliceDataGeom fitDataGeom_;
    private final CoordGroup fitCoordGrp_;
    private final int icX_;
    private final int icWeight_;

    /** Report key for fitted multiplicative constant. */
    public static final ReportKey<Double> CONST_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "c", "Factor" ), true );

    /** Report key for fitted mean. */
    public static final ReportKey<Double> MEAN_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "mu", "Mean" ), true );

    /** Report key for fitted standard deviation. */
    public static final ReportKey<Double> STDEV_KEY =
        ReportKey.createDoubleKey( new ReportMeta( "sigma",
                                                   "Standard Deviation" ),
                                   true );

    /** Report key for gaussian fit function. */
    public static final ReportKey<String> FUNCTION_KEY =
        ReportKey.createStringKey( new ReportMeta( "function", "Function" ),
                                   true );

    /** Config key for equivalent histogram bar width. */
    public static final ConfigKey<BinSizer> BINSIZER_KEY =
        HistogramPlotter.BINSIZER_KEY;

    /** Config key to display a line at the mean value. */
    public static final ConfigKey<Boolean> SHOWMEAN_KEY =
        new BooleanConfigKey(
            new ConfigMeta( "showmean", "Show Mean" )
           .setShortDescription( "Display a line at the mean" )
           .setXmlDescription( new String[] {
                "<p>If true, a line is drawn at the position of",
                "the calculated mean.",
                "</p>",
            } )
        , true );

    /**
     * Constructor.
     *
     * @param  xCoord   X axis coordinate
     * @param  hasWeight  true if weights may be used
     * @param   normKey   config key for normalisation options
     */
    public Stats1Plotter( FloatingCoord xCoord, boolean hasWeight,
                          ConfigKey<Normalisation> normKey ) {
        xCoord_ = xCoord;
        normKey_ = normKey;
        if ( hasWeight ) {
            weightCoord_ = FloatingCoord.WEIGHT_COORD;
            fitCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord, weightCoord_ },
                                         new boolean[] { false, false } );
        }
        else {
            weightCoord_ = null;
            fitCoordGrp_ =
                CoordGroup
               .createPartialCoordGroup( new Coord[] { xCoord },
                                         new boolean[] { false } );
        }
        fitDataGeom_ =
            new SliceDataGeom( new FloatingCoord[] { xCoord_, null }, "X" );

        /* For this plot type, coordinate indices are not sensitive to
         * plot-time geom (the CoordGroup has no point positions),
         * so we can calculate them here. */
        icX_ = fitCoordGrp_.getExtraCoordIndex( 0, null );
        icWeight_ = hasWeight
                  ? fitCoordGrp_.getExtraCoordIndex( 1, null )
                  : -1;
    }

    public String getPlotterName() {
        return "Gaussian";
    }

    public Icon getPlotterIcon() {
        return ResourceIcon.FORM_GAUSSIAN;
    }

    public boolean hasReports() {
        return true;
    }

    public String getPlotterDescription() {
        return PlotUtil.concatLines( new String[] {
            "<p>Plots a best fit Gaussian to the histogram of",
            "a sample of data.",
            "In fact, all this plotter does is to calculate the mean",
            "and standard deviation of the sample,",
            "and plot the corresponding Gaussian curve.",
            "The mean and standard deviation values are reported by the plot.",
            "</p>",
            "<p>The <code>" + normKey_ + "</code> config option,",
            "perhaps in conjunction with <code>" + BINSIZER_KEY + "</code>,",
            "can be used to scale the height of the plotted curve",
            "in data units.",
            "In this case, <code>" + BINSIZER_KEY + "</code>",
            "just describes the bar width of a notional histogram",
            "whose outline the plotted Gaussian should try to fit,",
            "and is only relevant for some of the normalisation options.",
            "</p>",
        } );
    }

    public CoordGroup getCoordGroup() {
        return fitCoordGrp_;
    }

    public ConfigKey[] getStyleKeys() {
        List<ConfigKey> list = new ArrayList<ConfigKey>();
        list.add( StyleKeys.COLOR );
        list.add( SHOWMEAN_KEY );
        list.addAll( Arrays.asList( StyleKeys.getStrokeKeys() ) );
        list.add( StyleKeys.ANTIALIAS );
        list.add( normKey_ );
        list.add( BINSIZER_KEY );
        return list.toArray( new ConfigKey[ 0 ] );
    }

    public StatsStyle createStyle( ConfigMap config ) {
        Color color = config.get( StyleKeys.COLOR );
        boolean showmean = Boolean.TRUE.equals( config.get( SHOWMEAN_KEY ) );
        Stroke stroke = StyleKeys.createStroke( config, BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND );
        boolean antialias = config.get( StyleKeys.ANTIALIAS );
        Normalisation norm = config.get( normKey_ );
        BinSizer sizer = config.get( BINSIZER_KEY );
        return new StatsStyle( color, stroke, antialias, showmean,
                               norm, sizer );
    }

    public PlotLayer createLayer( final DataGeom geom, final DataSpec dataSpec,
                                  final StatsStyle style ) {
        LayerOpt layerOpt = new LayerOpt( style.getColor(), true );
        return new AbstractPlotLayer( this, fitDataGeom_, dataSpec,
                                      style, layerOpt ) {
            public Drawing createDrawing( Surface surface,
                                          Map<AuxScale,Range> auxRanges,
                                          PaperType paperType ) {
                return new StatsDrawing( (PlanarSurface) surface, geom,
                                         dataSpec, style, paperType );
            }
        };
    }

    /**
     * Log function, used for transforming X values to values for fitting.
     *
     * @param  val  value
     * @return  log to base 10 of <code>val</code>
     */
    private static double log( double val ) {
        return Math.log10( val );
    }

    /**
     * Style class associated with this plotter.
     */
    public static class StatsStyle extends LineStyle {

        final boolean showmean_;
        final Normalisation norm_;
        final BinSizer sizer_;

        /**
         * Constructor.
         *
         * @param  color   line colour
         * @param  stroke  line stroke
         * @param  antialias  true to draw line antialiased
         * @param  showmean   true to display a line showing the mean
         * @param  norm  normalisation
         * @param  sizer   histogram equivalent bin sizer,
         *                 may be used in conjunction with norm
         */
        public StatsStyle( Color color, Stroke stroke, boolean antialias,
                           boolean showmean, Normalisation norm,
                           BinSizer sizer ) {
            super( color, stroke, antialias );
            showmean_ = showmean;
            norm_ = norm;
            sizer_ = sizer;
        }

        @Override
        public int hashCode() {
            int code = super.hashCode();
            code = 23 * code + ( showmean_ ? 11 : 17 );
            code = 23 * code + PlotUtil.hashCode( norm_ );
            code = 23 * code + PlotUtil.hashCode( sizer_ );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof StatsStyle ) {
                StatsStyle other = (StatsStyle) o;
                return super.equals( other )
                    && this.showmean_ == other.showmean_
                    && PlotUtil.equals( this.norm_, other.norm_ )
                    && PlotUtil.equals( this.sizer_, other.sizer_ );
            }
            else {
                return false;
            }
        }
    }

    /**
     * Drawing for stats plot.
     */
    private class StatsDrawing implements Drawing {

        private final PlanarSurface surface_;
        private final DataGeom geom_;
        private final DataSpec dataSpec_;
        private final StatsStyle style_;
        private final PaperType paperType_;

        /**
         * Constructor.
         *
         * @param  surface   plotting surface
         * @param  geom      maps position coordinates to graphics positions
         * @param  dataSpec  data points to fit
         * @param  style     line plotting style
         * @param  paperType  paper type
         */
        StatsDrawing( PlanarSurface surface, DataGeom geom, DataSpec dataSpec,
                      StatsStyle style, PaperType paperType ) {
            surface_ = surface;
            geom_ = geom;
            dataSpec_ = dataSpec;
            style_ = style;
            paperType_ = paperType;
        }

        public Object calculatePlan( Object[] knownPlans,
                                     DataStore dataStore ) {
            boolean isLog = surface_.getLogFlags()[ 0 ];

            /* If one of the known plans matches the one we're about
             * to calculate, just return that. */
            for ( Object plan : knownPlans ) {
                if ( plan instanceof StatsPlan &&
                     ((StatsPlan) plan).matches( isLog, dataSpec_ ) ) {
                    return plan;
                }
            }

            /* Otherwise, accumulate statistics and return the result. */
            WStats stats = new WStats();
            TupleSequence tseq = dataStore.getTupleSequence( dataSpec_ );
            if ( weightCoord_ == null || dataSpec_.isCoordBlank( icWeight_ ) ) {
                while ( tseq.next() ) {
                    double x = xCoord_.readDoubleCoord( tseq, icX_ );
                    double s = isLog ? log( x ) : x;
                    if ( PlotUtil.isFinite( s ) ) {
                        stats.addPoint( s );
                    }
                }
            }
            else {
                while ( tseq.next() ) {
                    double x = xCoord_.readDoubleCoord( tseq, icX_ );
                    double s = isLog ? log( x ) : x;
                    if ( PlotUtil.isFinite( s ) ) {
                        double w =
                            weightCoord_.readDoubleCoord( tseq, icWeight_ );
                        stats.addPoint( s, w );
                    }
                } 
            }
            return new StatsPlan( isLog, stats, dataSpec_ );
        }

        public void paintData( Object plan, Paper paper, DataStore dataStore ) {
            final StatsPlan splan = (StatsPlan) plan;
            paperType_.placeDecal( paper, new Decal() {
                public void paintDecal( Graphics g ) {
                    splan.paintLine( g, surface_, style_,
                                     paperType_.isBitmap() );
                }
                public boolean isOpaque() {
                    return ! style_.getAntialias();
                }
            } );
        }

        public ReportMap getReport( Object plan ) {
            return ((StatsPlan) plan).getReport( surface_, style_ );
        }
    }

    /**
     * Plan object encapsulating the inputs and results of a stats plot.
     */
    private static class StatsPlan {
        final boolean isLog_;
        final double mean_;
        final double sigma_;
        final double sum_;
        final DataSpec dataSpec_;

        /**
         * Constructor.
         *
         * @param  isLog   true iff stats are calculated on data logarithms
         * @param  stats   univariate statistics giving fit results
         * @param  dataSpec   characterisation of input data points
         */
        StatsPlan( boolean isLog, WStats stats, DataSpec dataSpec ) {
            isLog_ = isLog;
            mean_ = stats.getMean();
            sigma_ = stats.getSigma();
            sum_ = stats.getSum();
            dataSpec_ = dataSpec;
        }

        /**
         * Indicates whether this object's state will be the same as
         * a plan calculated for the given input values.
         *
         * @param  isLog   true iff stats are calculated on data logarithms
         * @param  dataSpec  characterisation of input data points
         */
        boolean matches( boolean isLog, DataSpec dataSpec ) {
            return isLog == isLog_
                && dataSpec.equals( dataSpec_ );
        }

        /**
         * Plots the fit line for this fitting result.
         *
         * @param  g  graphics context
         * @param  surface  plot surface
         * @param  style   style
         */
        void paintLine( Graphics g, PlanarSurface surface, StatsStyle style,
                        boolean isBitmap ) {
            double factor = getFactor( surface, style );
            Graphics2D g2 = (Graphics2D) g;
            Rectangle box = surface.getPlotBounds();
            int gxlo = box.x - 2;
            int gxhi = box.x + box.width + 2;
            int np = gxhi - gxlo;
            LineTracer tracer = style.createLineTracer( g2, box, np, isBitmap );
            Point2D.Double gpos = new Point2D.Double();
            double[] dpos = new double[ surface.getDataDimCount() ];
            for ( int ip = 0; ip < np; ip++ ) {
                double dx =
                    surface
                   .graphicsToData( new Point( gxlo + ip, box.y ), null )[ 0 ];
                if ( ! Double.isNaN( dx ) ) {
                    dpos[ 0 ] = dx;
                    dpos[ 1 ] = factor * gaussian( dx );
                    if ( surface.dataToGraphics( dpos, false, gpos ) &&
                         PlotUtil.isPointReal( gpos ) ) {
                        tracer.addVertex( gpos.x, gpos.y );
                    }
                }
            }
            tracer.flush();
            if ( style.showmean_ ) {
                double dx = mean_;
                Axis[] axes = surface.getAxes();
                double gx = axes[ 0 ].dataToGraphics( dx );
                double gylo = axes[ 1 ].dataToGraphics( 0 );
                double gyhi = axes[ 1 ].dataToGraphics( factor );
                LineTracer meanTracer =
                    style.createLineTracer( g2, box, 3, isBitmap );
                meanTracer.addVertex( gx, gylo );
                meanTracer.addVertex( gx, gyhi );
                meanTracer.flush();
            }
        }

        /**
         * Returns the multiplicative factor by which the <code>gaussian</code>
         * method should be multiplied to give the plotted value.
         *
         * @param  surface   target plotting surface
         * @param  style     stats style
         */
        private double getFactor( PlanarSurface surface, StatsStyle style ) {
            boolean xlog = surface.getLogFlags()[ 0 ];
            double[] xlims = surface.getDataLimits()[ 0 ];
            Rounding xround =
                Rounding.getRounding( surface.getTimeFlags()[ 0 ] );
            double bw = style.sizer_.getWidth( xlog, xlims[ 0 ], xlims[ 1 ],
                                               xround );
            double binWidth = xlog ? log( bw ) : bw;
            double c = 1.0 / ( sigma_ * Math.sqrt( 2.0 * Math.PI ) );
            double sum = sum_;
            double max = c * sum * binWidth;
            boolean isCumulative = false;
            double normFactor =
                style.norm_.getScaleFactor( sum, max, binWidth, isCumulative );
            return normFactor * c * sum_ * binWidth;
        }

        /**
         * Returns the value of the Gaussian function for this plan,
         * using data coordinates.
         * The result is lacking a scale factor;
         * its value is unity at the mean.
         *
         * @param  x  input value in data coordinates
         * @return  unscaled Gaussian function evaluated at <code>x</code>
         */
        double gaussian( double x ) {
            double s = isLog_ ? log( x ) : x;
            double p = ( s - mean_ ) / sigma_;
            return Math.exp( - 0.5 * p * p );
        }

        /**
         * Returns a plot report based on the state of this plan.
         *
         * @param   surface  target plotting surface
         * @param   style    plot style
         * @return  report
         */
        public ReportMap getReport( PlanarSurface surface, StatsStyle style ) {
            ReportMap report = new ReportMap();
            double factor = getFactor( surface, style );
            report.put( MEAN_KEY, mean_ );
            report.put( STDEV_KEY, sigma_ );
            report.put( CONST_KEY, factor );
            String function = new StringBuffer()
                .append( CONST_KEY.toText( factor ) )
                .append( " * " )
                .append( "exp(-0.5 * pow((" )
                .append( isLog_ ? "log10(x)" : "x" )
                .append( "-" )
                .append( MEAN_KEY.toText( mean_ ) )
                .append( ")/" )
                .append( STDEV_KEY.toText( sigma_ ) )
                .append( ", 2))" )
                .toString();
            report.put( FUNCTION_KEY, function );
            return report;
        }
    }

    /**
     * Accumulates and calculates statistics for an optionally
     * weighted single variable.
     */
    private static class WStats {
        private double sw_;
        private double swX_;
        private double swXX_;

        /**
         * Submits a weighted data value.
         *
         * @param   x  data value
         * @param   w  weighting
         */
        public void addPoint( double x, double w ) {
            if ( w > 0 && ! Double.isInfinite( w ) ) {
                sw_ += w;
                swX_ += w * x;
                swXX_ += w * x * x;
            }
        }

        /**
         * Submits a data value with unit weighting.
         *
         * @param  x  data value
         */
        public void addPoint( double x ) { 
            sw_ += 1;
            swX_ += x;
            swXX_ += x * x;
        }

        /**
         * Returns the mean of the values submitted so far.
         *
         * @return  mean
         */
        public double getMean() {
            return swX_ / sw_;
        }

        /**
         * Returns the standard deviation of the values submitted so far.
         *
         * @return  standard deviation
         */
        public double getSigma() {
            return Math.sqrt( ( swXX_ - swX_ * swX_ / sw_ ) / sw_ );
        }

        /**
         * Returns the sum of the values submitted so far.
         *
         * @return  weighted sum
         */
        public double getSum() {
            return sw_;
        }
    }
}
