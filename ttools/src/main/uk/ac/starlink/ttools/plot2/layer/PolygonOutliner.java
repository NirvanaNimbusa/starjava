package uk.ac.starlink.ttools.plot2.layer;

import gov.fnal.eag.healpix.PixTools;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.AuxScale;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Area;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.geom.CubeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.CubeSurface;
import uk.ac.starlink.ttools.plot2.geom.GPoint3D;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.Rotation;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkySys;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;
import uk.ac.starlink.ttools.plot2.paper.Paper;
import uk.ac.starlink.ttools.plot2.paper.PaperType2D;
import uk.ac.starlink.ttools.plot2.paper.PaperType3D;

/**
 * Outliner implementations for plotting shapes defined by listing
 * three or more vertices in data space.
 * There are lots of implementation details hidden in this class,
 * but external users should just need the public static factory methods.
 *
 * @author   Mark Taylor
 * @since    5 Mar 2019
 */
public class PolygonOutliner extends PixOutliner {

    private final PolygonMode.Glypher polyGlypher_;
    private final VertexReaderFactory vrfact_;
    private final int minSize_;
    private final MarkShape minShape_;
    private final Glyph pointGlyph_;
    private final Icon icon_;

    /** Config key for the replacement marker threshold size. */
    public static final ConfigKey<Integer> MINSIZE_KEY =
        StyleKeys.createMarkSizeKey( 
            new ConfigMeta( "minsize", "Minimal Size" )
           .setStringUsage( "<pixels>" )
           .setShortDescription( "Size of small polygon representation "
                               + "in pixels" )
           .setXmlDescription( new String[] {
                "<p>Defines a threshold size in pixels below which,",
                "instead of the polygon defined by the other parameters,",
                "a replacement marker will be painted instead.",
                "If this is set to zero, then only the shape itself",
                "will be plotted, but if it is small it may appear as",
                "only a single pixel.",
                "By setting a larger value, you can ensure that",
                "the position of even small polygons is easily visible,",
                "at the expense of giving them an artificial shape and size.",
                "This value also defines the size of the replacement markers.",
                "</p>",
            } ),
            1 );

    /** Config key for the replacement marker shape. */
    public static final ConfigKey<MarkShape> MINSHAPE_KEY =
        StyleKeys.createMarkShapeKey(
            new ConfigMeta( "minshape", "Minimal Shape" )
           .setShortDescription( "Marker shape for very small shapes" )
           .setXmlDescription( new String[] {
                "<p>Defines the shape of markers plotted instead of",
                "the actual polygon shape,",
                "for polygons that are smaller than the size threshold",
                "defined by",
                "<code>" + MINSIZE_KEY.getMeta().getShortName() + "</code>.",
                "</p>",
            } ),
            MarkShape.CROXX );

    /* Set up lookup tables for plotting circles. */
    private static final int NVERTEX_CIRCLE = 36;
    private static final double[] COSS;
    private static final double[] SINS;
    static {
        COSS = new double[ NVERTEX_CIRCLE ];
        SINS = new double[ NVERTEX_CIRCLE ];
        double thetaFact = 2 * Math.PI / NVERTEX_CIRCLE;
        for ( int iv = 0; iv < NVERTEX_CIRCLE; iv++ ) {
            double theta = iv * thetaFact;
            COSS[ iv ] = Math.cos( theta );
            SINS[ iv ] = Math.sin( theta );
        }
    }

    /** VertexData instance with no content. */
    private static final VertexData NO_VERTEX_DATA = new VertexData() {
        public int getVertexCount() {
            return 0;
        }
        public boolean readDataPos( int ivert, double[] dpos ) {
            return false;
        }
        public boolean isBreak( int ivert ) {
            return false;
        }
    };

    /**
     * Constructor.
     *
     * @param  polyGlypher  object that knows how to paint a polygon
     * @param  vrfact   object that knows how to get vertex positions from
     *                  a data tuple
     * @param  minSize  threshold size for replacement markers
     * @param  minShape  shape for replacement markers
     */
    private PolygonOutliner( PolygonMode.Glypher polyGlypher,
                             VertexReaderFactory vrfact,
                             int minSize, MarkShape minShape ) {
        polyGlypher_ = polyGlypher;
        vrfact_ = vrfact;
        minSize_ = minSize;
        minShape_ = minShape;
        pointGlyph_ = MarkForm.createMarkGlyph( minShape, minSize, true );
        icon_ = new MultiPosIcon( 4 ) {
            protected void paintPositions( Graphics g, Point[] positions ) {
                int np = positions.length;
                int[] xs = new int[ np ];
                int[] ys = new int[ np ];
                int tx = 0;
                int ty = 0;
                for ( int i = 0; i < np; i++ ) {
                    Point p = positions[ i ];
                    int x = p.x;
                    int y = p.y;
                    xs[ i ] = x;
                    ys[ i ] = y;
                    tx += x;
                    ty += y;
                }
                polyGlypher_.paintPolygon( g, tx / np, ty / np, xs, ys, np );
            }
        };
    }

    public Icon getLegendIcon() {
        return icon_;
    }

    public Map<AuxScale,AuxReader> getAuxRangers( DataGeom geom ) {
        return new HashMap<AuxScale,AuxReader>();
    }

    public ShapePainter create2DPainter( final Surface surf,
                                         final DataGeom geom,
                                         Map<AuxScale,Span> auxSpans,
                                         final PaperType2D paperType ) {
        final VertexReader vertReader = vrfact_.createVertexReader( geom );
        Rectangle bounds = surf.getPlotBounds();
        final int bxMin = bounds.x;
        final int bxMax = bounds.x + bounds.width;
        final int byMin = bounds.y;
        final int byMax = bounds.y + bounds.height;
        int ndim = surf.getDataDimCount();
        final double[] dpos0 = new double[ ndim ];
        final double[] dpos = new double[ ndim ];
        final double[] dposC = new double[ ndim ];
        final Point2D.Double gpos = new Point2D.Double();
        final Point2D.Double gposC = new Point2D.Double();
        final Point igpos = new Point();
        final Point igposC = new Point();
        final int icPos = vertReader.getPosCoordIndex();;
        return new ShapePainter() {
            public void paintPoint( Tuple tuple, Color color, Paper paper ) {
                VertexData vdata = vertReader.readVertexData( tuple );

                /* Get the position of the first vertex in integer
                 * (rounded) graphics coordinates. */
                if ( vdata.readDataPos( 0, dpos0 ) &&
                     surf.dataToGraphics( dpos0, false, gpos ) &&
                     PlotUtil.isPointFinite( gpos ) &&
                     geom.readDataPos( tuple, icPos, dposC ) &&
                     surf.dataToGraphics( dposC, false, gposC ) &&
                     PlotUtil.isPointFinite( gposC ) ) {
                    int np = vdata.getVertexCount();
                    int jp = 0;
                    if ( np > 0 ) {
                        PlotUtil.quantisePoint( gposC, igposC );
                        int gxC = igposC.x;
                        int gyC = igposC.y;
                        int[] gxs = new int[ np ];
                        int[] gys = new int[ np ];
                        PlotUtil.quantisePoint( gpos, igpos );
                        gxs[ jp ] = igpos.x;
                        gys[ jp ] = igpos.y;
                        jp++;

                        /* Get the graphics positions of the other vertices. */
                        for ( int ip = 1; ip < np; ip++ ) {

                            /* If this vertex is a break, draw what we have
                             * and prepare to accumulate vertices for
                             * the next one. */
                            if ( vdata.isBreak( ip ) ) {
                                paintPoly( gxC, gyC, gxs, gys, jp,
                                           color, paper );
                                ip++;
                                if ( ip < np &&
                                     vdata.readDataPos( ip, dpos0 ) &&
                                     surf.dataToGraphics( dpos0, false, gpos )&&
                                     PlotUtil.isPointFinite( gpos ) ) {
                                    jp = 0;
                                    PlotUtil.quantisePoint( gpos, igpos );
                                    gxs[ jp ] = igpos.x;
                                    gys[ jp ] = igpos.y;
                                    jp++;
                                }
                                else {
                                    return;
                                }
                            }

                            /* Get the next graphics position.
                             * Reject the vertex if there is no continuous
                             * line from the first vertex to it.
                             * This is to defend against drawing polygons
                             * going the wrong way around the sphere in
                             * sky plots. */
                            else if ( vdata.readDataPos( ip, dpos )
                                   && surf.dataToGraphics( dpos, false, gpos )
                                   && surf.isContinuousLine( dpos0, dpos )
                                   && PlotUtil.isPointFinite( gpos ) ) {
                                PlotUtil.quantisePoint( gpos, igpos );
                                gxs[ jp ] = igpos.x;
                                gys[ jp ] = igpos.y;
                                jp++;
                            }
                        }

                        /* We have the vertices in graphics space,
                         * paint a figure as appropriate. */
                        if ( jp > 0 ) {
                            paintPoly( gxC, gyC, gxs, gys, jp, color, paper );
                        }
                    }
                }
            }

            /**
             * Does the painting of a single polygon given its vertices
             * in graphics coordinates.
             *
             * @param  gx0  graphics X coordinate of nominal center
             * @param  gy0  graphics Y coordinate of nominal center
             * @param  gxs  graphics X coordinates
             * @param  gys  graphics Y coordinates
             * @param  np  number of vertices forming closed polygon
             * @param  color  plotting colour
             * @param  paper  paper
             */
            private void paintPoly( int gx0, int gy0,
                                    int[] gxs, int[] gys, int np,
                                    Color color, Paper paper ) {

                /* Work out the bounds of the graphics rectangle
                 * enclosing the polygon. */
                int gxMin = bxMax;
                int gxMax = bxMin;
                int gyMin = byMax;
                int gyMax = byMin;
                for ( int ip = 0; ip < np; ip++ ) {
                    int gx = gxs[ ip ];
                    int gy = gys[ ip ];
                    gxMin = Math.min( gxMin, gx );
                    gxMax = Math.max( gxMax, gx );
                    gyMin = Math.min( gyMin, gy );
                    gyMax = Math.max( gyMax, gy );
                }

                /* If the bounds are outside the plot, do nothing.
                 * If the bounds are all the same (in integer
                 * graphics coordinates), the polygon can be
                 * represented cheaply as a single point.
                 * Otherwise, draw it properly. */
                if ( gxMax >= bxMin && gxMin <= bxMax &&
                     gyMax >= byMin && gyMin <= byMax ) {
                    if ( gxMax - gxMin <= minSize_ &&
                         gyMax - gyMin <= minSize_ ) {
                        int gx = ( gxMax + gxMin ) / 2;
                        int gy = ( gyMax + gyMin ) / 2;
                        paperType.placeGlyph( paper, gx, gy,
                                              pointGlyph_, color );
                    }
                    else {
                        polyGlypher_.placeGlyphs2D( paperType, paper,
                                                    gx0, gy0, gxs, gys, np,
                                                    color );
                    }
                }
            }
        };
    }

    public ShapePainter create3DPainter( final CubeSurface surf, DataGeom geom,
                                         Map<AuxScale,Span> auxSpans,
                                         final PaperType3D paperType ) {
        final VertexReader vertReader = vrfact_.createVertexReader( geom );
        Rectangle bounds = surf.getPlotBounds();
        final int bxMin = bounds.x;
        final int bxMax = bounds.x + bounds.width;
        final int byMin = bounds.y;
        final int byMax = bounds.y + bounds.height;
        int ndim = surf.getDataDimCount();
        final double[] dpos = new double[ ndim ];
        final GPoint3D gpos = new GPoint3D();
        final Point igpos = new Point();
        final double[] dposC = new double[ ndim ];
        final GPoint3D gposC = new GPoint3D();
        final Point igposC = new Point();
        final int icPos = vertReader.getPosCoordIndex();
        return new ShapePainter() {
            public void paintPoint( Tuple tuple, Color color, Paper paper ) {
                VertexData vdata = vertReader.readVertexData( tuple );
                int np = vdata.getVertexCount();
                if ( np > 0 &&
                     geom.readDataPos( tuple, icPos, dposC ) &&
                     surf.dataToGraphicZ( dposC, true, gposC ) &&
                     PlotUtil.isPointFinite( gposC ) ) {
                    PlotUtil.quantisePoint( gposC, igposC );
                    int gxC = igposC.x;
                    int gyC = igposC.y;
                    int[] gxs = new int[ np ];
                    int[] gys = new int[ np ];

                    /* Read all the vertex positions in graphics space. */
                    int jp = 0;
                    double sz = 0;
                    for ( int ip = 0; ip < np; ip++ ) {

                        /* If this vertex is a break, draw what we have and
                         * prepare to accumulate vertices for the next one. */
                        if ( vdata.isBreak( ip ) ) {
                            double gz = sz / np;
                            paintPoly( gxC, gyC, gxs, gys, jp, gz,
                                       color, paper );
                            jp = 0;
                            sz = 0;
                        }

                        /* For 3d we only accept polygons for which all
                         * vertices are visible within the 3d plot bounds,
                         * because of the difficulty of partially clipping
                         * polygons in 3d.  This means that polygons near the
                         * edge of the visible cube may not be painted.
                         * 3d plots don't have the possibility of discontinuous
                         * lines, so we don't need to defend against that. */
                        else if ( vdata.readDataPos( ip, dpos ) &&
                                  surf.dataToGraphicZ( dpos, true, gpos ) &&
                                  PlotUtil.isPointFinite( gpos ) ) {
                            PlotUtil.quantisePoint( gpos, igpos );
                            gxs[ jp ] = igpos.x;
                            gys[ jp ] = igpos.y;
                            jp++;
                            sz += gpos.z;
                        }
                        else {
                            return;
                        }
                    }

                    /* We have the vertices in graphics space,
                     * paint a figure as appropriate. */
                    if ( jp > 0 ) {
                        double gz = sz / jp;
                        paintPoly( gxC, gyC, gxs, gys, jp, gz, color, paper );
                    }
                }
            }

            /**
             * Does the painting of a single polygon given its vertices
             * in graphics coordinates.
             *
             * @param  gx0  graphics X coordinate of nominal center
             * @param  gy0  graphics Y coordinate of nominal center
             * @param  gxs  graphics X coordinates
             * @param  gys  graphics Y coordinates
             * @param  np  number of vertices forming closed polygon
             * @param  gz   representative Z coordinate for the polygon;
             *              using the same Z for all the vertices is a fudge,
             *              but it's the best we can do
             * @param  color  plotting colour
             * @param  paper  paper
             */
            private void paintPoly( int gx0, int gy0,
                                    int[] gxs, int[] gys, int np, double gz,
                                    Color color, Paper paper ) {

                /* Work out the bounding box in the two graphics dimensions
                 * for the polygon. */
                int gxMin = bxMax;
                int gxMax = bxMin;
                int gyMin = byMax;
                int gyMax = byMin;
                for ( int ip = 0; ip < np; ip++ ) {
                    int gx = gxs[ ip ];
                    int gy = gys[ ip ];
                    gxMin = Math.min( gxMin, gx );
                    gxMax = Math.max( gxMax, gx );
                    gyMin = Math.min( gyMin, gy );
                    gyMax = Math.max( gyMax, gy );
                }

                /* If it falls within the graphics bounds, plot it at the
                 * mean Z coordinate of all the vertices.  This is a fudge,
                 * but it's the best we can easily do.  Take a short cut
                 * if it's a point. */
                if ( gxMax >= bxMin && gxMin <= bxMax &&
                     gyMax >= byMin && gyMin <= byMax ) {
                    if ( gxMax - gxMin <= minSize_ &&                  
                         gyMax - gyMin <= minSize_ ) {
                        int gx = ( gxMax + gxMin ) / 2;
                        int gy = ( gyMax + gyMin ) / 2;
                        paperType.placeGlyph( paper, gx, gy, gz,
                                              pointGlyph_, color );
                    }
                    else {
                        polyGlypher_.placeGlyphs3D( paperType, paper,
                                                    gx0, gy0, gxs, gys, np, gz,
                                                    color );
                    }
                }
            }
        };
    }

    @Override
    public int hashCode() {
        int code = 434482;
        code = 23 * code + polyGlypher_.hashCode();
        code = 23 * code + vrfact_.hashCode();
        code = 23 * code + minSize_;
        code = 23 * code + minShape_.hashCode();
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PolygonOutliner ) {
            PolygonOutliner other = (PolygonOutliner) o;
            return this.polyGlypher_.equals( other.polyGlypher_ )
                && this.vrfact_.equals( other.vrfact_ )
                && this.minSize_ == other.minSize_
                && this.minShape_.equals( other.minShape_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns an outliner for polygons with a fixed number of vertices.
     *
     * @param  np  number of vertices
     * @param  polyGlypher  polygon painter
     * @param  minSize  threshold size for replacement markers
     * @param  minShape  shape for replacement markers
     * @return  outliner
     */
    public static PolygonOutliner
            createFixedOutliner( int np, PolygonMode.Glypher polyGlypher,
                                 int minSize, MarkShape minShape ) {
        return new PolygonOutliner( polyGlypher,
                                    new FixedVertexReaderFactory( np ),
                                    minSize, minShape );
    }

    /**
     * Returns an outliner for drawing Area objects to a Plane plot.
     *
     * @param  coord  coordinate for reading area objects
     * @param  icArea   coordinate index in tuple for area coordinate
     * @param  polyGlypher  polygon painter
     * @param  minSize  threshold size for replacement markers
     * @param  minShape  shape for replacement markers
     * @return  outliner
     */
    public static PolygonOutliner
            createPlaneAreaOutliner( AreaCoord<PlaneDataGeom> coord, int icArea,
                                     PolygonMode.Glypher polyGlypher,
                                     int minSize, MarkShape minShape ) {
        return new PolygonOutliner( polyGlypher,
                                    new PlaneAreaVertexReaderFactory( coord,
                                                                      icArea ),
                                    minSize, minShape );
    }

    /**
     * Returns an outliner for drawing Area objects to a Sky plot.
     *
     * @param  coord  coordinate for reading area objects
     * @param  icArea   coordinate index in tuple for area coordinate
     * @param  polyGlypher  polygon painter
     * @param  minSize  threshold size for replacement markers
     * @param  minShape  shape for replacement markers
     * @return  outliner
     */
    public static PolygonOutliner
           createSkyAreaOutliner( AreaCoord<SkyDataGeom> coord, int icArea,
                                  PolygonMode.Glypher polyGlypher,
                                  int minSize, MarkShape minShape ) {
        return new PolygonOutliner( polyGlypher,
                                    new SkyAreaVertexReaderFactory( coord,
                                                                    icArea ),
                                    minSize, minShape );
    }

    /**
     * Returns an outliner for drawing Area objects to a Sphere plot.
     *
     * @param  areaCoord  coordinate for reading area objects
     * @param  icArea   coordinate index in tuple for area coordinate
     * @param  radialCoord  coordinate for reading radial distance of area
     * @param  icRadial  coordinate index in tuple for radial coordinate
     * @param  polyGlypher  polygon painter
     * @param  minSize  threshold size for replacement markers
     * @param  minShape  shape for replacement markers
     * @return  outliner
     */
    public static PolygonOutliner
            createSphereAreaOutliner( AreaCoord<SphereDataGeom> areaCoord,
                                      int icArea,
                                      FloatingCoord radialCoord, int icRadial,
                                      PolygonMode.Glypher polyGlypher,
                                      int minSize, MarkShape minShape ) {
        return new PolygonOutliner(
                polyGlypher,
                new SphereAreaVertexReaderFactory( areaCoord, icArea,
                                                   radialCoord, icRadial ),
                minSize, minShape );
    }

    /**
     * Returns an outliner for polygons defined by an array-valued coordinate
     * providing interleaved coordinates in user data space.
     * Each array instance may be of length N*D, where D is the number of
     * coordinates per point in user space.
     * For instance a triangle in plane coordinates would be
     * (x1,y1, x2,y2, x3,y3),
     * and in sky coordinates
     * (lon1,lat1, lon2,lat2, lon3,lat3).
     *
     * @param  arrayCoord   array-valued coordinate
     * @param  includePos  if true, positional coordinate is included
     *                     as the first vertex, if false it is ignored
     * @param  polyGlypher  polygon painter
     * @return  outliner
     */
    public static PolygonOutliner
            createArrayOutliner( FloatingArrayCoord arrayCoord,
                                 boolean includePos,
                                 PolygonMode.Glypher polyGlypher ) {
        return new PolygonOutliner( polyGlypher,
                                    new ArrayVertexReaderFactory( arrayCoord,
                                                                  includePos ),
                                    0, MarkShape.POINT );
    }

    /**
     * Converts longitude/latitude coordinates to sky data coordinates
     * (3d unit vector).
     *
     * @param  lonDeg  longitude in degrees
     * @param  latDeg  latitude in degrees
     * @param  geom   sky DataGeom, possibly containing rotation information
     * @param  dpos  array into which 3-element unit vector will be written
     * @return  true iff conversion has been successful
     */
    private static boolean toSky( double lonDeg, double latDeg,
                                  SkyDataGeom geom, double[] dpos ) {
        if ( Math.abs( latDeg ) <= 90 && PlotUtil.isFinite( lonDeg ) ) {
            double theta = Math.toRadians( 90 - latDeg );
            double phi = Math.toRadians( lonDeg % 360. );
            double sd = Math.sin( theta );
            dpos[ 0 ] = Math.cos( phi ) * sd;
            dpos[ 1 ] = Math.sin( phi ) * sd;
            dpos[ 2 ] = Math.cos( theta );
            geom.rotate( dpos );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Converts longitude/latitude/radius coordinates to sphere
     * data coordinates (3d vector).
     *
     * @param  lonDeg  longitude in degrees
     * @param  latDeg  latitude in degrees
     * @param  radius  radial distance
     * @param  dpos  array into which 3-element unit vector will be written
     * @return  true iff conversion has been successful
     */
    private static boolean toSphere( double lonDeg, double latDeg,
                                     double radius, double[] dpos ) {
        if ( radius >= 0 &&
             Math.abs( latDeg ) <= 90 &&
             PlotUtil.isFinite( lonDeg ) ) {
            double theta = Math.toRadians( 90 - latDeg );
            double phi = Math.toRadians( lonDeg % 360 );
            double sd = Math.sin( theta );
            dpos[ 0 ] = radius * Math.cos( phi ) * sd;
            dpos[ 1 ] = radius * Math.sin( phi ) * sd;
            dpos[ 2 ] = radius * Math.cos( theta );
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Defines how to acquire vertex coordinates from a plot data tuple.
     */
    @Equality
    private interface VertexReaderFactory {

        /**
         * Constructs a vertex reader appropriate for a given DataGeom.
         *
         * @param  geom  geometry for vertex coordinates
         * @return  vertex reader
         */
        VertexReader createVertexReader( DataGeom geom );
    }

    /**
     * Can acquire vertex coordinates from a plot data tuple.
     */
    private interface VertexReader {

        /**
         * Reads polygon vertex data from a given tuple.
         *
         * @param  tuple  data tuple
         * @return  polygon vertex information
         */
        VertexData readVertexData( Tuple tuple );

        /**
         * Returns the index within a tuple at which the data position
         * can be found.
         *
         * @return  position coordinate index
         */
        int getPosCoordIndex();
    }

    /**
     * Information about the vertices of a polygon.
     */
    private interface VertexData {

        /**
         * Returns the number of vertices.
         *
         * @return  number of vertices
         */
        int getVertexCount();

        /**
         * Acquires the data coordinates of one vertex of a polygon.
         *
         * @param  ivert  vertex index
         * @param  dpos   array to receive vertex coordinates in data space
         * @return   true iff dpos contains a successfully converted
         *           vertex position on exit
         */
        boolean readDataPos( int ivert, double[] dpos );

        /**
         * Indicates whether the given index corresponds to a delimiter
         * between disjoint shapes rather than an actual vertex position.
         * If true, then readDataPos will not give a useful result.
         *
         * <p>Only certain VertexData types (currently Areas with type POLYGON)
         * ever return true from this method.
         *
         * @param  ivert  vertex index
         * @return   true iff this entry corresponds to a break between
         *           disjoint shapes
         */
        boolean isBreak( int ivert );
    }

    /**
     * VertexReaderFactory implementation for polygons with a fixed
     * number of vertices.
     */
    private static class FixedVertexReaderFactory
            implements VertexReaderFactory {
        private final int np_;

        /**
         * Constructor.
         *
         * @param  np  number of vertices per polygon
         */
        FixedVertexReaderFactory( int np ) {
            np_ = np;
        }

        public VertexReader createVertexReader( final DataGeom geom ) {
            final int[] icPos = new int[ np_ ];
            for ( int ip = 0; ip < np_; ip++ ) {
                icPos[ ip ] = getPosCoordIndex( ip, geom );
            }
            return new VertexReader() {
                public VertexData readVertexData( final Tuple tuple ) {
                    return new VertexData() {
                        public int getVertexCount() {
                            return np_;
                        }
                        public boolean readDataPos( int ipos, double[] dpos ) {
                            return geom
                                  .readDataPos( tuple, icPos[ ipos ], dpos );
                        }
                        public boolean isBreak( int ipos ) {
                            return false;
                        }
                    };
                }
                public int getPosCoordIndex() {
                    return icPos[ 0 ];
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 288901;
            code = 23 * code + np_;
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof FixedVertexReaderFactory ) {
                FixedVertexReaderFactory other = (FixedVertexReaderFactory) o;
                return this.np_ == other.np_;
            }
            else {
                return false;
            }
        }

        /**
         * Returns the tuple coordinate index for a given vertex.
         *
         * @param  ivert  vertex index
         * @param  geom   data geom
         * @return  position in tuple at which vertex appears
         */
        private int getPosCoordIndex( int ivert, DataGeom geom ) {
            // Note this is questionable: it should really be
            // CoordGroup.getPosCoordIndex( ivert, geom ), but we don't have
            // the CoordGroup here.
            return geom.getPosCoords().length * ivert;
        }
    }

    /**
     * VertexReaderFactory implementation for polygons with vertices
     * supplied using an array-valued coordinate.
     * Coordinates are interleaved, for example (x1,y1, x2,y2, x3,y3).
     */
    private static class ArrayVertexReaderFactory
            implements VertexReaderFactory {
        private final FloatingArrayCoord arrayCoord_;
        private final boolean includePos_;

        /**
         * Constructor.
         *
         * @param  arrayCoord   array-valued coordinate
         * @param  includePos  if true, positional coordinate is included
         *                     as the first vertex, if false it is ignored
         */
        ArrayVertexReaderFactory( FloatingArrayCoord arrayCoord,
                                  boolean includePos ) {
            arrayCoord_ = arrayCoord;
            includePos_ = includePos;
        }

        public VertexReader createVertexReader( DataGeom geom ) {
            if ( geom instanceof PlaneDataGeom ) {
                return new ArrayVertexReader( arrayCoord_, geom, includePos_ ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double x = array[ icPos ];
                        if ( ! Double.isNaN( x ) ) {
                            double y = array[ icPos + 1 ];
                            if ( ! Double.isNaN( y ) ) {
                                dpos[ 0 ] = x;
                                dpos[ 1 ] = y;
                                return true;
                            }
                        }
                        return false;
                    }
                };
            }
            else if ( geom instanceof CubeDataGeom ) {
                return new ArrayVertexReader( arrayCoord_, geom, includePos_ ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double x = array[ icPos ];
                        if ( ! Double.isNaN( x ) ) {
                            double y = array[ icPos + 1 ];
                            if ( ! Double.isNaN( y ) ) {
                                double z = array[ icPos + 2 ];
                                if ( ! Double.isNaN( z ) ) {
                                    dpos[ 0 ] = x;
                                    dpos[ 1 ] = y;
                                    dpos[ 2 ] = z;
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                };
            }
            else if ( geom instanceof SphereDataGeom ) {
                return new ArrayVertexReader( arrayCoord_, geom, includePos_ ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        double lonDeg = array[ icPos + 0 ];
                        double latDeg = array[ icPos + 1 ];
                        double radius = array[ icPos + 2 ];
                        return toSphere( lonDeg, latDeg, radius, dpos );
                    }
                };
            }
            else if ( geom instanceof SkyDataGeom ) {
                final SkyDataGeom skyGeom = (SkyDataGeom) geom;
                return new ArrayVertexReader( arrayCoord_, geom, includePos_ ) {
                    boolean readArrayPos( double[] array, int icPos,
                                          double[] dpos ) {
                        return toSky( array[ icPos ], array[ icPos + 1 ],
                                      skyGeom, dpos );
                    }
                };
            }
            else {
                assert false;
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public int hashCode() {
            int code = 222389;
            code = 23 * code + arrayCoord_.hashCode();
            code = 23 * code + ( includePos_ ? 17 : 29 );
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof ArrayVertexReaderFactory ) {
                ArrayVertexReaderFactory other = (ArrayVertexReaderFactory) o;
                return this.arrayCoord_.equals( other.arrayCoord_ )
                    && this.includePos_ == other.includePos_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * VertexReader implementation for use with the ArrayVertexReaderFactory.
     */
    private static abstract class ArrayVertexReader implements VertexReader {

        private final FloatingArrayCoord arrayCoord_;
        private final DataGeom geom_;
        private final boolean includePos_;
        private final int nuc_;
        private final int icPos0_;
        private final int icArray_;

        /**
         * Constructor.
         *
         * @param  arrayCoord  vertex array data coordinate
         * @param  geom      geometry for vertices
         * @param  includePos  if true, positional coordinate is included
         *                     as the first vertex, if false it is ignored
         */
        ArrayVertexReader( FloatingArrayCoord arrayCoord, DataGeom geom,
                           boolean includePos ) {
            arrayCoord_ = arrayCoord;
            geom_ = geom;
            includePos_ = includePos;

            /* Work out how many elements of the array coordinate will
             * correspond to a single position. */
            int nuc = 0;
            for ( Coord c : geom.getPosCoords() ) {
                nuc += c.getInputs().length;
            }
            nuc_ = nuc;

            /* Determine where in the tuple to find the initial position
             * coordinates and the array position coordinate.
             * This is not the correct way to do it: really we should use
             * the getPosCoordIndex and getExtraCoordIndex methods of
             * CoordGroup, but we don't have the CoordGroup here.
             * This implementation should work however if the coord group
             * looks as we expect it to for a polygon plotter. */
            icPos0_ = 0;
            icArray_ = geom.getPosCoords().length;
        }

        public VertexData readVertexData( final Tuple tuple ) {
            final double[] array =
                arrayCoord_.readArrayCoord( tuple, icArray_ );
            int nc = array.length;
            final int nv = nc % nuc_ == 0
                         ? nc / nuc_ + ( includePos_ ? 1 : 0 )
                         : 0;
            if ( includePos_ ) {
                return new VertexData() {
                    public int getVertexCount() {
                        return nv;
                    }
                    public boolean readDataPos( int ipos, double[] dpos ) {
                        return ipos == 0
                             ? geom_.readDataPos( tuple, icPos0_, dpos )
                             : readArrayPos( array, ( ipos - 1 ) * nuc_, dpos );
                    }
                    public boolean isBreak( int ipos ) {
                        return false;
                    }
                };
            }
            else {
                return new VertexData() {
                    public int getVertexCount() {
                        return nv;
                    }
                    public boolean readDataPos( int ipos, double[] dpos ) {
                        return readArrayPos( array, ipos * nuc_, dpos );
                    }
                    public boolean isBreak( int ipos ) {
                        return false;
                    }
                };
            }
        }

        public int getPosCoordIndex() {
            return icPos0_;
        }

        /**
         * Reads the position of a single vertex from an array
         * containing multiple interleaved vertex coordinates
         * (for instance x1,y1,x2,y2,...)
         *
         * @param   array  array data
         * @param   icPos  starting index in array for coordinate tuple
         * @param   dpos   destination for vertex coordinates in data space
         * @return   true iff read succeeded; if true the dpos array
         *           will contain usable coordinates on exit
         */
        abstract boolean readArrayPos( double[] array, int icPos,
                                       double[] dpos );
    }

    /**
     * VertexReaderFactory implementation for use with Plane AreaCoord.
     */
    private static class PlaneAreaVertexReaderFactory
            implements VertexReaderFactory {

        private final AreaCoord<PlaneDataGeom> coord_;
        private final int icArea_;
 
        /**
         * Constructor.
         *
         * @param  coord   area coordinate
         * @param  icArea   index within tuple of area coordinate value
         */
        PlaneAreaVertexReaderFactory( AreaCoord<PlaneDataGeom> coord,
                                      int icArea ) {
            coord_ = coord;
            icArea_ = icArea;
        }

        public VertexReader createVertexReader( DataGeom geom0 ) {
            PlaneDataGeom planeGeom =
                coord_.getAreaDataGeom( (PlaneDataGeom) geom0 );
            return new AreaVertexReader( coord_, icArea_ ) {
                public VertexData createVertexData( Area area ) {
                    switch ( area.getType() ) {
                        case POLYGON:
                            final double[] vertices = area.getDataArray();
                            return new VertexData() {
                                public int getVertexCount() {
                                    return vertices.length / 2;
                                }
                                public boolean readDataPos( int ivert,
                                                            double[] dpos ){
                                    dpos[ 0 ] = vertices[ ivert * 2 + 0 ];
                                    dpos[ 1 ] = vertices[ ivert * 2 + 1 ];
                                    return true;
                                }
                                public boolean isBreak( int ivert ) {
                                    int iv2 = ivert * 2;
                                    if ( Double.isNaN( vertices[ iv2 ] ) ) {
                                        assert Double
                                              .isNaN( vertices[ iv2 + 1 ] );
                                        return true;
                                    }
                                    else {
                                        return false;
                                    }
                                }
                            };
                        case CIRCLE:
                            double[] circle = area.getDataArray();
                            final double cx = circle[ 0 ];
                            final double cy = circle[ 1 ];
                            final double r = circle[ 2 ];
                            return new VertexData() {
                                public int getVertexCount() {
                                    return NVERTEX_CIRCLE;
                                }
                                public boolean readDataPos( int ivert,
                                                            double[] dpos ){
                                    dpos[ 0 ] = cx + r * COSS[ ivert ];
                                    dpos[ 1 ] = cy - r * SINS[ ivert ];
                                    return true;
                                }
                                public boolean isBreak( int ivert ) {
                                    return false;
                                }
                            };
                        case POINT:
                            double[] point = area.getDataArray();
                            double[] dpos =
                                new double[] { point[ 0 ], point[ 1 ] };
                            return createPointVertexData( dpos );
                        case MOC:
                            double[] duniqs = area.getDataArray();
                            return new MocVertexData( duniqs ) {
                                void copyVector( double vx, double vy,
                                                 double vz, double[] dpos ) {
                                    double latDeg =
                                        90 - Math.toDegrees( Math.acos( vz ) );
                                    double lonDeg =
                                        Math.toDegrees( Math.atan2( vy, vx ) );
                                    dpos[ 0 ] = lonDeg;
                                    dpos[ 1 ] = latDeg;
                                }
                            };
                        default:
                            assert false;
                            return NO_VERTEX_DATA;
                    }
                }
            };
        }
        @Override
        public int hashCode() {
            int code = 812312;
            code = 23 * code + coord_.hashCode();
            code = 23 * code + icArea_;
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof PlaneAreaVertexReaderFactory ) {
                PlaneAreaVertexReaderFactory other =
                    (PlaneAreaVertexReaderFactory) o;
                return this.coord_.equals( other.coord_ )
                    && this.icArea_ == other.icArea_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * VertexReaderFactory for use with Sky AreaCoord.
     */
    private static class SkyAreaVertexReaderFactory
            implements VertexReaderFactory {
        private final AreaCoord<SkyDataGeom> coord_;
        private final int icArea_;

        /**
         * Constructor.
         *
         * @param  coord   area coordinate
         * @param  icArea   index within tuple of area coordinate value
         */
        SkyAreaVertexReaderFactory( AreaCoord<SkyDataGeom> coord, int icArea ) {
            coord_ = coord;
            icArea_ = icArea;
        }

        public VertexReader createVertexReader( DataGeom geom0 ) {
            SkyDataGeom skyGeom = coord_.getAreaDataGeom( (SkyDataGeom) geom0 );
            return new AreaVertexReader( coord_, icArea_ ) {
                public VertexData createVertexData( Area area ) {
                    switch ( area.getType() ) {
                        case POLYGON:
                            final double[] vertices = area.getDataArray();
                            return new VertexData() {
                                public int getVertexCount() {
                                    return vertices.length / 2;
                                }
                                public boolean readDataPos( int ivert,
                                                            double[] dpos ) {
                                    int iv2 = ivert * 2;
                                    return toSky( vertices[ iv2 ],
                                                  vertices[ iv2 + 1 ],
                                                  skyGeom, dpos );
                                }
                                public boolean isBreak( int ivert ) {
                                    int iv2 = ivert * 2;
                                    if ( Double.isNaN( vertices[ iv2 ] ) ) {
                                        assert Double
                                              .isNaN( vertices[ iv2 + 1 ] );
                                        return true;
                                    }
                                    else {
                                        return false;
                                    }
                                }
                            };
                        case CIRCLE:
                            double[] circle = area.getDataArray();
                            double lonDeg = circle[ 0 ];
                            double latDeg = circle[ 1 ];
                            double rDeg = circle[ 2 ];
                            return createSkyCircleVertexData( lonDeg, latDeg,
                                                              rDeg, skyGeom );
                        case POINT:
                            double[] point = area.getDataArray();
                            double[] dpos = new double[ 3 ];
                            return toSky( point[ 0 ], point[ 1 ], skyGeom, dpos)
                                 ? createPointVertexData( dpos )
                                 : NO_VERTEX_DATA;
                        case MOC:
                            double[] duniqs = area.getDataArray();
                            // MOCs are always equatorial.
                            final Rotation rotation =
                                Rotation
                               .createRotation( SkySys.EQUATORIAL,
                                                skyGeom.getViewSystem() );
                            return new MocVertexData( duniqs ) {
                                void copyVector( double vx, double vy,
                                                 double vz, double[] dpos ) {
                                    dpos[ 0 ] = vx;
                                    dpos[ 1 ] = vy;
                                    dpos[ 2 ] = vz;
                                    rotation.rotate( dpos );
                                }
                            };
                        default:
                            assert false;
                            return NO_VERTEX_DATA;
                    }
                }
            };
        }
        @Override
        public int hashCode() {
            int code = 3188803;
            code = 23 * code + coord_.hashCode();
            code = 23 * code + icArea_;
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SkyAreaVertexReaderFactory ) {
                SkyAreaVertexReaderFactory other =
                    (SkyAreaVertexReaderFactory) o;
                return this.coord_.equals( other.coord_ )
                    && this.icArea_ == other.icArea_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * VertexReaderFactory for use with Sphere AreaCoord.
     */
    private static class SphereAreaVertexReaderFactory
            implements VertexReaderFactory {
        private final AreaCoord<SphereDataGeom> areaCoord_;
        private final int icArea_;
        private final FloatingCoord radialCoord_;
        private final int icRadial_;

        /**
         * Constructor.
         *
         * @param  areaCoord  coordinate for reading area objects
         * @param  icArea   coordinate index in tuple for area coordinate
         * @param  radialCoord  coordinate for reading radial distance of area
         * @param  icRadial  coordinate index in tuple for radial coordinate
         */
        SphereAreaVertexReaderFactory( AreaCoord<SphereDataGeom> areaCoord,
                                       int icArea,
                                       FloatingCoord radialCoord,
                                       int icRadial ) {
            areaCoord_ = areaCoord;
            icArea_ = icArea;
            radialCoord_ = radialCoord;
            icRadial_ = icRadial;
        }

        public VertexReader createVertexReader( DataGeom geom ) {
            return new VertexReader() {
                public VertexData readVertexData( final Tuple tuple ) {
                    double radius =
                        radialCoord_.readDoubleCoord( tuple, icRadial_ );
                    radius = Double.isNaN( radius ) ? 1.0 : radius;
                    if ( radius > 0 ) {
                        Area area = areaCoord_.readAreaCoord( tuple, icArea_ );
                        Area.Type type = area.getType();
                        if ( type != null ) {
                            return createSphereAreaVertexData( area, radius );
                        }
                        else {
                            assert false;
                            return NO_VERTEX_DATA;
                        }
                    }
                    else {
                        return NO_VERTEX_DATA;
                    }
                }
                public int getPosCoordIndex() {
                    return icArea_;
                }
            };
        }

        @Override
        public int hashCode() {
            int code = 322987;
            code = 23 * code + areaCoord_.hashCode();
            code = 23 * code + icArea_;
            code = 23 * code + radialCoord_.hashCode();
            code = 23 * code + icRadial_;
            return code;
        }
        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SphereAreaVertexReaderFactory ) {
                SphereAreaVertexReaderFactory other =
                    (SphereAreaVertexReaderFactory) o;
                return this.areaCoord_.equals( other.areaCoord_ )
                    && this.icArea_ == other.icArea_
                    && this.radialCoord_.equals( other.radialCoord_ )
                    && this.icRadial_ == other.icRadial_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Constructs a VertexData for use with area objects on a sphere plot.
     *
     * @param   area   object defining shape projected onto sphere
     * @param   radius   radial distance of shape from origin
     * @return   vertex data
     */
    private static VertexData createSphereAreaVertexData( Area area,
                                                          double radius ) {
        switch ( area.getType() ) {
            case POLYGON:
                final double[] vertices = area.getDataArray();
                return new VertexData() {
                    public int getVertexCount() {
                        return vertices.length / 2;
                    }
                    public boolean readDataPos( int ivert, double[] dpos ) {
                        int iv2 = ivert * 2;
                        return toSphere( vertices[ iv2 ], vertices[ iv2 + 1 ],
                                         radius, dpos );
                    }
                    public boolean isBreak( int ivert ) {
                        int iv2 = ivert * 2;
                        if ( Double.isNaN( vertices[ iv2 ] ) ) {
                            assert Double.isNaN( vertices[ iv2 + 1 ] );
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                };
            case CIRCLE:
                double[] circle = area.getDataArray();
                double lonDeg = circle[ 0 ];
                double latDeg = circle[ 1 ];
                double rDeg = circle[ 2 ];
                final VertexData unitVertexData =
                    createSkyCircleVertexData( lonDeg, latDeg, rDeg,
                                               SkyDataGeom.GENERIC );
                if ( NO_VERTEX_DATA.equals( unitVertexData ) ) {
                    return NO_VERTEX_DATA;
                }
                else {
                    return new VertexData() {
                        public int getVertexCount() {
                            return unitVertexData.getVertexCount();
                        }
                        public boolean readDataPos( int ivert, double[] dpos ) {
                            if ( unitVertexData.readDataPos( ivert, dpos ) ) {
                                dpos[ 0 ] *= radius;
                                dpos[ 1 ] *= radius;
                                dpos[ 2 ] *= radius;
                                return true;
                            }
                            else {
                                return false;
                            }
                        }
                        public boolean isBreak( int ivert ) {
                            return unitVertexData.isBreak( ivert );
                        }
                    };
                }
            case POINT:
                double[] point = area.getDataArray();
                double[] dpos = new double[ 3 ];
                return toSphere( point[ 0 ], point[ 1 ], radius, dpos )
                     ? createPointVertexData( dpos )
                     : NO_VERTEX_DATA;
            default:
                assert false;
                return NO_VERTEX_DATA;
        }
    }

    /**
     * Returns a VertexData instance that yields a single vertex.
     *
     * @param  dpos0  data coordinates of sole vertex
     * @return   new VertexData
     */
    private static VertexData createPointVertexData( final double[] dpos0 ) {
        return new VertexData() {
            public int getVertexCount() {
                return 1;
            }
            public boolean readDataPos( int ivert, double[] dpos ) {
                if ( ivert == 0 ) {
                    System.arraycopy( dpos0, 0, dpos, 0, dpos0.length );
                    return true;
                }
                else {
                    return false;
                }
            }
            public boolean isBreak( int ivert ) {
                return false;
            }
        };
    }

    /**
     * Returns a VertexData that yields sky positions (unit 3-vectors)
     * on a specified small circle.
     * The small circle does not have to be small :-).
     *
     * @param  lonDeg0  central longitude in degrees
     * @param  latDeg0  central latitude in degrees
     * @param  rDeg    small circle radius in degrees
     * @param  skyGeom  geometry optionally specifying sky system rotation
     */
    private static VertexData
            createSkyCircleVertexData( double lonDeg0, double latDeg0,
                                       double rDeg, SkyDataGeom skyGeom ) {

        /* Convert center to unit vector v0. */
        double[] vec = new double[ 3 ];
        if ( ! toSky( lonDeg0, latDeg0, skyGeom, vec ) ) {
            return NO_VERTEX_DATA;
        }
        final double x0 = vec[ 0 ];
        final double y0 = vec[ 1 ];
        final double z0 = vec[ 2 ];

        /* Locate a unit vector v1 on the circle circumference;
         * we go on radius away from the center either up or down a meridian
         * depending on hemisphere. */
        double latDeg1 = latDeg0 > 0 ? latDeg0 - rDeg : latDeg0 + rDeg;
        if ( ! toSky( lonDeg0, latDeg1, skyGeom, vec ) ) {
            return NO_VERTEX_DATA;
        }
        final double x1 = vec[ 0 ];
        final double y1 = vec[ 1 ];
        final double z1 = vec[ 2 ];

        /* Return an object that will rotate v1 about v0 for each vertex. */
        return new VertexData() {
            public int getVertexCount() {
                return NVERTEX_CIRCLE;
            }
            public boolean readDataPos( int iv, double[] dpos ) {

                /* Calculate an axial rotation matrix which will rotate any
                 * vector around v0 by the angle 2*PI*iv/NVERTEX_CIRCLE;
                 * the algebra is from SAL_DAV2M in SLALIB. */
                double s = SINS[ iv ];
                double c = COSS[ iv ];
                double w = 1.0 - c;
                double r0 = x0 * x0 * w + c;
                double r1 = x0 * y0 * w + z0 * s;
                double r2 = x0 * z0 * w - y0 * s;
                double r3 = x0 * y0 * w - z0 * s;
                double r4 = y0 * y0 * w + c;
                double r5 = y0 * z0 * w + x0 * s;
                double r6 = x0 * z0 * w + y0 * s;
                double r7 = y0 * z0 * w - x0 * s;
                double r8 = z0 * z0 * w + c;

                /* Rotate v1 using the axial rotation matrix. */
                dpos[ 0 ] = r0 * x1 + r1 * y1 + r2 * z1;
                dpos[ 1 ] = r3 * x1 + r4 * y1 + r5 * z1;
                dpos[ 2 ] = r6 * x1 + r7 * y1 + r8 * z1;
                return true;
            }
            public boolean isBreak( int iv ) {
                return false;
            }
        };
    }

    /**
     * VertexData implementation for MOC areas.
     */
    private static abstract class MocVertexData implements VertexData {
        private final double[] duniqs_;
        private final PixTools pixTools_;
        private int iuniq_;
        private double[][] tileVerts_;

        /**
         * Constructor.
         *
         * @param  duniqs   array of double values that need to be equivalenced
         *                  to longs in order to yield MOC NUNIQ tile indices
         */
        MocVertexData( double[] duniqs ) {
            duniqs_ = duniqs;
            pixTools_ = new PixTools();
            iuniq_ = -1;
        }

        public int getVertexCount() {

            /* There are four vertices per tile, plus a break vertex
             * between each pair (but not after the last one). */
            return duniqs_.length * 5 - 1;
        }

        public boolean isBreak( int ivert ) {

            /* Every fifth vertex is a break. */
            return ivert % 5 == 4;
        }

        public boolean readDataPos( int ivert, double[] dpos ) {
            int irot = ivert % 5;
            if ( irot < 4 ) {
                int iuniq = ivert / 5;

                /* First time we see a tile index, calculate and save
                 * its vertices.  Most likely after the first one,
                 * the next three vertices to be requested will be the
                 * othet three for the same tile. */
                if ( iuniq != iuniq_ ) {
                    iuniq_ = iuniq;
                    long uniq = Double.doubleToRawLongBits( duniqs_[ iuniq ] );
                    long order = ( 61 - Long.numberOfLeadingZeros( uniq ) ) >>1;
                    long ipix = uniq - ( 1L << ( 2 + 2 * order ) );
                    long nside = 1L << order;
                    tileVerts_ = pixTools_.pix2vertex_nest( nside, ipix );
                }
                copyVector( tileVerts_[ 0 ][ irot ],
                            tileVerts_[ 1 ][ irot ],
                            tileVerts_[ 2 ][ irot ], dpos );
                return true;
            }
            else {
                return false;
            }
        }

        /**
         * Populates the geometry-specific data-space position array
         * given the elements of a unit vector giving a vertex position
         * on the unit sphere.
         *
         * @param   vx   unit vector X component
         * @param   vx   unit vector Y component
         * @param   vx   unit vector Z component
         * @param  dpos  geometry-specific data-space position array,
         *               to be populated on output
         */
        abstract void copyVector( double vx, double vy, double vz,
                                  double[] dpos );
    }

    /**
     * Partial VertexReader implementation for use with VertexReader.
     */
    private static abstract class AreaVertexReader implements VertexReader {

        private final AreaCoord<?> coord_;
        private final int icArea_;

        /**
         * Constructor.
         *
         * @param  coord  coordinate for reading area values
         * @param  icArea   index in tuple of area value
         */
        AreaVertexReader( AreaCoord<?> coord, int icArea ) {
            coord_ = coord;
            icArea_ = icArea;
        }

        public int getPosCoordIndex() {
            // Area coordinate can also be used as a positional coordinate
            // as far as a suitable DataGeom is concerned,
            // since the position is stashed at the start of it.
            return icArea_;
        }

        public VertexData readVertexData( final Tuple tuple ) {
            Area area = coord_.readAreaCoord( tuple, icArea_ );
            if ( area == null ) {
                return NO_VERTEX_DATA;
            }
            else {
                Area.Type type = area.getType();
                if ( type == null ) {
                    assert false;
                    return NO_VERTEX_DATA;
                }
                else {
                    return createVertexData( area );
                }
            }
        }

        /**
         * Converts an Area object into a VertexData object.
         *
         * @param   area   area object
         * @return   vertexData object, not null
         */
        public abstract VertexData createVertexData( Area area );
    }
}
