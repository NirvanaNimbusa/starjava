package uk.ac.starlink.topcat.plot;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.topcat.ColumnSelector;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * PointSelector implementation which queries for spherical polar
 * coordinates and yields 3D Cartesian ones.
 *
 * @author   Mark Taylor
 * @since    23 Dec 2005
 */
public class SphericalPolarPointSelector extends PointSelector {

    private final JComponent colBox_;
    private final JComponent tanerrContainer_;
    private final ColumnSelector phiSelector_;
    private final ColumnSelector thetaSelector_;
    private final AxisDataSelector rSelector_;
    private final ColumnSelector tanerrSelector_;
    private final ToggleButtonModel logToggler_;
    private final ToggleButtonModel tangentErrorModeModel_;
    private final ErrorModeSelectionModel radialErrorModeModel_;

    /** A column data object which contains zeroes. */
    private static final ColumnData ZERO_COLUMN_DATA = createZeroColumnData();

    /**
     * Constructs a point selector with error bar capability.
     *
     * @param  styles  initial style set
     * @param  logToggler model for determining whether the radial coordinate
     *         is to be scaled logarithmically
     * @param  tangentErrorModeModel  model indicating whether tangential
     *         errors will be drawn
     * @param  radialErrorModeModel   model indicating whether/how radial
     *         errors will be drawn
     */
    public SphericalPolarPointSelector( MutableStyleSet styles,
                               ToggleButtonModel logToggler,
                               ToggleButtonModel tangentErrorModeModel,
                               ErrorModeSelectionModel radialErrorModeModel ) {
        super( styles );
        logToggler_ = logToggler;
        tangentErrorModeModel_ = tangentErrorModeModel;
        radialErrorModeModel_ = radialErrorModeModel;

        /* Prepare column selection panel. */
        colBox_ = Box.createVerticalBox();
        String[] axisNames = new String[] { "Longitude", "Latitude" };
        JComponent[] selectors = new JComponent[ axisNames.length ];

        /* Selector for longitude column. */
        phiSelector_ = new ColumnSelector( Tables.RA_INFO, false );
        phiSelector_.addActionListener( actionForwarder_ );
        phiSelector_.setTable( null );
        phiSelector_.setEnabled( false );
        selectors[ 0 ] = phiSelector_;

        /* Selector for latitude column. */
        thetaSelector_ = new ColumnSelector( Tables.DEC_INFO, false );
        thetaSelector_.addActionListener( actionForwarder_ );
        thetaSelector_.setTable( null );
        thetaSelector_.setEnabled( false );
        selectors[ 1 ] = thetaSelector_;

        /* Place longitude and latitude selectors. */
        Box tandatBox = Box.createVerticalBox();
        JLabel[] axLabels = new JLabel[ axisNames.length ];
        for ( int i = 0; i < axisNames.length; i++ ) {
            String aName = axisNames[ i ];
            JComponent cPanel = Box.createHorizontalBox();
            axLabels[ i ] = new JLabel( " " + aName + " Axis: " );
            cPanel.add( axLabels[ i ] );
            cPanel.add( new ShrinkWrapper( selectors[ i ] ) );
            cPanel.add( Box.createHorizontalStrut( 5 ) );
            cPanel.add( Box.createHorizontalGlue() );
            tandatBox.add( Box.createVerticalStrut( 5 ) );
            tandatBox.add( cPanel );
        }

        /* Place long/lat selectors alongside a container for a tangential
         * error column. */
        tanerrContainer_ = Box.createHorizontalBox();
        final Box tanerrBox = Box.createHorizontalBox();
        tanerrBox.add( Box.createVerticalGlue() );
        tanerrBox.add( tanerrContainer_ );
        tanerrBox.add( Box.createVerticalGlue() );

        /* Selector for tangential errors. */
        DefaultValueInfo sizeInfo = 
            new DefaultValueInfo( "Angular Size", Number.class,
                                  "Angular size or error" );
        sizeInfo.setUnitString( "radians" );
        sizeInfo.setNullable( true );
        tanerrSelector_ = new ColumnSelector( sizeInfo, false );
        tanerrSelector_.addActionListener( actionForwarder_ );
        tanerrSelector_.setTable( null );
        tanerrSelector_.setEnabled( false );
        ChangeListener tanerrListener = new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {
                tanerrBox.removeAll();
                if ( tangentErrorModeModel_.isSelected() ) {
                    tanerrBox.add( new JLabel( " +/- " ) );
                    tanerrBox.add( new ShrinkWrapper( tanerrSelector_ ) );
                }
            }
        };
        tangentErrorModeModel_.addChangeListener( tanerrListener );
        tanerrListener.stateChanged( null );
        
        /* Add long/lat business to main selector panel. */
        Box tanBox = Box.createHorizontalBox();
        tanBox.add( new ShrinkWrapper( tandatBox ) );
        tanBox.add( new ShrinkWrapper( tanerrBox ) );
        tanBox.add( Box.createHorizontalGlue() );
        colBox_.add( tanBox );

        /* Selector for radius column. */
        rSelector_ =
            new AxisDataSelector( "Radial", new String[] { "Log" },
                                  new ToggleButtonModel[] { logToggler } );
        rSelector_.addActionListener( actionForwarder_ );
        rSelector_.setEnabled( false );
        colBox_.add( Box.createVerticalStrut( 5 ) );
        colBox_.add( rSelector_ );
        colBox_.add( Box.createVerticalGlue() );

        /* Align axis labels. */
        Dimension labelSize = new Dimension( 0, 0 );
        for ( int i = 0; i < axisNames.length; i++ ) {
            Dimension s = axLabels[ i ].getPreferredSize();
            labelSize.width = Math.max( labelSize.width, s.width );
            labelSize.height = Math.max( labelSize.height, s.height );
        }
        for ( int i = 0; i < axisNames.length; i++ ) {
            axLabels[ i ].setPreferredSize( labelSize );
        }

        /* Fix for changes to the error mode selections to modify the
         * state of the axis data selectors. */
        if ( radialErrorModeModel_ != null ) {
            ActionListener radialErrorListener = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateAnnotator();
                    rSelector_.setErrorMode( radialErrorModeModel_.getMode() );
                }
            };
            radialErrorModeModel_.addActionListener( radialErrorListener );
            radialErrorListener.actionPerformed( null );
        }
    }

    protected JComponent getColumnSelectorPanel() {
        return colBox_;
    }

    public int getNdim() {
        return 3;
    }

    public boolean isReady() {
        return getTable() != null
            && getPhi() != null
            && getTheta() != null;
    }

    public StarTable getData() {
        return new SphericalPolarTable( getTable(),
                                        getPhi(), getTheta(), getR() );
    }

    public StarTable getErrorData() {
        return createColumnDataTable( getTable(), new ColumnData[ 0 ] );
    }

    public ErrorMode[] getErrorModes() {
        ErrorMode[] modes = new ErrorMode[ 3 ];
        boolean hasTan = tangentErrorModeModel_.isSelected();
        modes[ 0 ] = hasTan ? ErrorMode.SYMMETRIC : ErrorMode.NONE;
        modes[ 1 ] = hasTan ? ErrorMode.SYMMETRIC : ErrorMode.NONE;
        modes[ 2 ] = radialErrorModeModel_.getMode();
        return modes;
    }

    public AxisEditor[] createAxisEditors() {

        /* We only have one axis editor, that for the radial coordinate.
         * Override the default implementation of setAxis so that only
         * the upper bound can be set - the lower bound is always zero. */
        final AxisEditor ed = new AxisEditor( "Radial" ) {
            public void setAxis( ValueInfo axis ) {
                super.setAxis( axis );
                loField_.setText( "" );
                loField_.setEnabled( false );
            }
            protected double getHigh() {
                double hi = super.getHigh();
                return logToggler_.isSelected() ? Math.log( hi ) : hi;
            }
        };
        logToggler_.addChangeListener( new ChangeListener() {
            public void stateChanged( ChangeEvent evt ) {

                /* For some reason the visible range isn't getting set from
                 * the value filled in the editor axis when the log toggle
                 * changes.  I can't work out why.  Until I manage to fix it,
                 * better to cear the editor bounds so they don't say the
                 * wrong thing. */
                // ed.updateRanges();
                // actionForwarder_.stateChanged( evt );
                ed.clearBounds();
            }
        } );
        
        rSelector_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                ColumnData cdata = 
                    (ColumnData) rSelector_.getMainSelector().getSelectedItem();
                ed.setAxis( cdata == null ? null : cdata.getColumnInfo() );
            }
        } );
        return new AxisEditor[] { ed };
    }

    public PointStore createPointStore( int npoint ) {
        return new CartesianPointStore( 3, new ErrorMode[ 0 ] ).init( npoint );
    }

    protected void configureSelectors( TopcatModel tcModel ) {
        if ( tcModel == null ) {
            phiSelector_.getModel().getColumnModel().setSelectedItem( null );
            phiSelector_.getModel().getConverterModel().setSelectedItem( null );
            thetaSelector_.getModel().getColumnModel().setSelectedItem( null );
            thetaSelector_.getModel().getConverterModel()
                                     .setSelectedItem( null );
            tanerrSelector_.getModel().getColumnModel().setSelectedItem( null );
            tanerrSelector_.getModel().getConverterModel()
                                      .setSelectedItem( null );
        }
        else {
            phiSelector_.setTable( tcModel );
            thetaSelector_.setTable( tcModel );
            tanerrSelector_.setTable( tcModel );
        }
        rSelector_.setTable( tcModel );
        phiSelector_.setEnabled( tcModel != null );
        thetaSelector_.setEnabled( tcModel != null );
        tanerrSelector_.setEnabled( tcModel != null );
        rSelector_.setEnabled( tcModel != null );
    }

    protected void initialiseSelectors() {
    }

    /**
     * Return the column of longitude-type values currently selected.
     *
     * @return  phi column data
     */
    private ColumnData getPhi() {
        return phiSelector_.getColumnData();
    }

    /**
     * Return the column of latitude-type values currently selected.
     *
     * @return   theta column data
     */
    private ColumnData getTheta() {
        return thetaSelector_.getColumnData();
    }

    /**
     * Return the column of radius values currently selected.
     * May legitimately be null if you want everything on the surface of
     * a sphere.
     *
     * @return   radius column
     */
    private ColumnData getR() {
        ColumnData cdata =
            (ColumnData) rSelector_.getMainSelector().getSelectedItem();
        if ( cdata == null ) {
            return UnitColumnData.INSTANCE;
        }
        else if ( logToggler_.isSelected() ) {
            return new LogColumnData( cdata );
        }
        else {
            return cdata;
        }
    }

    /**
     * Returns metadata describing the currently selected radial coordinate.
     * If no radial coordinate is selected (all points on the surface of
     * the sphere), <code>null</code> is returned.
     *
     * @return   radial column info
     */
    public ValueInfo getRadialInfo() {
        ColumnData cdata =
            (ColumnData) rSelector_.getMainSelector().getSelectedItem();
        if ( cdata == null ) {
            return null;
        }
        else if ( logToggler_.isSelected() ) {
            return new LogColumnData( cdata ).getColumnInfo();
        }
        else {
            return cdata.getColumnInfo();
        }
    }

    /**
     * ColumnData implementation which returns unity for every entry.
     */
    private static class UnitColumnData extends ColumnData {
        final static UnitColumnData INSTANCE = new UnitColumnData();
        private final Double ONE = new Double( 1.0 );
        private UnitColumnData() {
            super( new DefaultValueInfo( "Unit", Double.class, "Unit value" ) );
        }
        public Object readValue( long irow ) {
            return ONE;
        }
    }

    /**
     * ColumnData implementation which represents the log() values of
     * a base ColumnData.
     * An intelligent implementation of equals() is provided.
     */
    private static class LogColumnData extends ColumnData {

        private final ColumnData base_;

        /**
         * Constructs a new LogColumnData.
         *
         * @param  base  (unlogged) base column data
         */
        LogColumnData( ColumnData base ) {
            base_ = base;
            ColumnInfo cinfo = new ColumnInfo( base.getColumnInfo() );
            String units = cinfo.getUnitString();
            if ( units != null && units.trim().length() > 0 ) {
                cinfo.setUnitString( "log(" + units + ")" );
            }
            cinfo.setName( "log(" + cinfo.getName() + ")" );
            cinfo.setContentClass( Double.class );
            setColumnInfo( cinfo );
        }

        public Object readValue( long irow ) throws IOException {
            Object val = base_.readValue( irow );
            if ( val instanceof Number ) {
                double dval = ((Number) val).doubleValue();
                return dval > 0 ? new Double( Math.log( dval ) )
                                : null;
            }
            else {
                return null;
            }
        }

        public boolean equals( Object o ) {
            return ( o instanceof LogColumnData )
                 ? this.base_.equals( ((LogColumnData) o).base_ )
                 : false;
        }

        public int hashCode() {
            return base_.hashCode() * 999;
        }
    }

    /**
     * StarTable implementation which returns a table with X, Y, Z columns
     * based on the TopcatModel columns selected in this component.
     * This involves a coordinate transformation (spherical polar to
     * Cartesian).
     *
     * <p>Provides a non-trivial implementation of equals().
     *
     * <p>The table is not random-access - it could be made so without 
     * too much effort, but random access is not expected to be required.
     */
    private static class SphericalPolarTable extends AbstractStarTable {

        private final TopcatModel tcModel_;
        private final ColumnData phiData_;
        private final ColumnData thetaData_;
        private final ColumnData rData_;

        /**
         * Constructor.
         *
         * @param   tcModel   table
         * @param   phiData   column of longitude-like values
         * @param   thetaData column of latitude-like values
         * @param   rData     column of radius-like values
         */
        public SphericalPolarTable( TopcatModel tcModel, ColumnData phiData,
                                    ColumnData thetaData, ColumnData rData ) {
            tcModel_ = tcModel;
            phiData_ = phiData;
            thetaData_ = thetaData;
            rData_ = rData;
        }

        public int getColumnCount() {
            return 3;
        }

        public long getRowCount() {
            return tcModel_.getDataModel().getRowCount();
        }

        public ColumnInfo getColumnInfo( int icol ) {
            DefaultValueInfo info =
                new DefaultValueInfo( new String[] { "X", "Y", "Z" }[ icol ],
                                      Double.class,
                                      "Cartesian coordinate " + ( icol + 1 ) );
            info.setUnitString( rData_.getColumnInfo().getUnitString() );
            return new ColumnInfo( info );
        }

        public RowSequence getRowSequence() {
            final long nrow = getRowCount();
            return new RowSequence() {
                long lrow_ = 0;
                Object[] row_;
                public boolean next() throws IOException {
                    if ( lrow_ < nrow ) {
                        row_ = new Object[ 3 ];
                        Object oPhi = phiData_.readValue( lrow_ );
                        Object oTheta = thetaData_.readValue( lrow_ );
                        Object oR = rData_.readValue( lrow_ );
                        if ( oPhi instanceof Number &&
                             oTheta instanceof Number &&
                             oR instanceof Number ) {
                            double r = ((Number) oR).doubleValue(); 
                            if ( r > 0 ) {
                                double phi = ((Number) oPhi).doubleValue();
                                double theta = ((Number) oTheta).doubleValue();

                                double sinTheta = Math.sin( theta );
                                double cosTheta = Math.cos( theta );
                                double sinPhi = Math.sin( phi );
                                double cosPhi = Math.cos( phi );

                                double x = r * cosTheta * cosPhi;
                                double y = r * cosTheta * sinPhi;
                                double z = r * sinTheta;

                                row_[ 0 ] = new Double( x );
                                row_[ 1 ] = new Double( y );
                                row_[ 2 ] = new Double( z );
                            }
                        }
                        lrow_++;
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                public Object[] getRow() {
                    return row_;
                }
                public Object getCell( int icol ) {
                    return row_[ icol ];
                }
                public void close() {
                }
            };
        }

        public boolean equals( Object o ) {
            if ( o instanceof SphericalPolarTable ) {
                SphericalPolarTable other = (SphericalPolarTable) o;
                return other.tcModel_.equals( this.tcModel_ )
                    && other.phiData_.equals( this.phiData_ )
                    && other.thetaData_.equals( this.thetaData_ )
                    && other.rData_.equals( this.rData_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 999;
            code = 23 * code + tcModel_.hashCode();
            code = 23 * code + phiData_.hashCode();
            code = 23 * code + thetaData_.hashCode();
            code = 23 * code + rData_.hashCode();
            return code;
        }
    }
}
