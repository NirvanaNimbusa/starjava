package uk.ac.starlink.topcat.plot2;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.geom.CubeAspect;
import uk.ac.starlink.ttools.plot2.geom.CubeSurfaceFactory;

/**
 * Axis control for cube plot.
 * This operates in two modes, one isotropic (with geometry specified
 * using spherical polar coordinates) and one at least potentially 
 * anisotropic (with geometry specified using Cartesian coordinates).
 * Which to use is specified at construction time.
 * 
 * @author   Mark Taylor
 * @since    14 Mar 2013
 */
public class CubeAxisController
       extends CartesianAxisController<CubeSurfaceFactory.Profile,CubeAspect> {

    private final boolean isIso_;
    private CubeAspect oldAspect_;

    /**
     * Constructor.
     *
     * @param  isIso   true for isotropic, false for anisotropic
     * @param  stack   plot control stack
     */
    public CubeAxisController( boolean isIso, ControlStack stack ) {
        super( new CubeSurfaceFactory( isIso ), createAxisLabelKeys(), stack );
        isIso_ = isIso;
        final SurfaceFactory<CubeSurfaceFactory.Profile,CubeAspect> surfFact =
            getSurfaceFactory();
        ConfigControl mainControl = getMainControl();

        /* Log/flip config tab - only makes sense for anisotropic mode. */
        if ( ! isIso ) {
            mainControl.addSpecifierTab( "Coords",
                                         new ConfigSpecifier( new ConfigKey[] {
                CubeSurfaceFactory.XLOG_KEY,
                CubeSurfaceFactory.YLOG_KEY,
                CubeSurfaceFactory.ZLOG_KEY,
                CubeSurfaceFactory.XFLIP_KEY,
                CubeSurfaceFactory.YFLIP_KEY,
                CubeSurfaceFactory.ZFLIP_KEY,
            } ) );
        }

        /* Navigator tab. */
        addNavigatorTab();

        /* Provide the aspect configuration in two separate panels.
         * Either can reset the whole aspect, but each takes part of the
         * state from the existing aspect so that adjusting the controls
         * on one panel does not pull in the current values set on the other,
         * which might not reflect the current visible state. */
        final ConfigKey<?>[] rangeKeys = 
            isIso ? new ConfigKey<?>[] {
                        CubeSurfaceFactory.SCALE_KEY,
                        CubeSurfaceFactory.XC_KEY,
                        CubeSurfaceFactory.YC_KEY,
                        CubeSurfaceFactory.ZC_KEY,
                    }
                  : new ConfigKey<?>[] {
                        CubeSurfaceFactory.XMIN_KEY,
                        CubeSurfaceFactory.XMAX_KEY,
                        CubeSurfaceFactory.XSUBRANGE_KEY,
                        CubeSurfaceFactory.YMIN_KEY,
                        CubeSurfaceFactory.YMAX_KEY,
                        CubeSurfaceFactory.YSUBRANGE_KEY,
                        CubeSurfaceFactory.ZMIN_KEY,
                        CubeSurfaceFactory.ZMAX_KEY,
                        CubeSurfaceFactory.ZSUBRANGE_KEY,
                    };
        final ConfigKey<?>[] viewKeys = new ConfigKey<?>[] {
            CubeSurfaceFactory.PHI_KEY,
            CubeSurfaceFactory.THETA_KEY,
            CubeSurfaceFactory.ZOOM_KEY,
            CubeSurfaceFactory.XOFF_KEY,
            CubeSurfaceFactory.YOFF_KEY,
        };
        ConfigSpecifier rangeSpecifier = new ConfigSpecifier( rangeKeys ) {
            @Override
            public ConfigMap getSpecifiedValue() {
                ConfigMap c = super.getSpecifiedValue();
                CubeAspect asp = oldAspect_;
                if ( asp != null ) {
                    c.put( CubeSurfaceFactory.ROTMAT_KEY, asp.getRotation() );
                    c.put( CubeSurfaceFactory.ZOOM_KEY, asp.getZoom() );
                    c.put( CubeSurfaceFactory.XOFF_KEY, asp.getOffsetX() );
                    c.put( CubeSurfaceFactory.YOFF_KEY, asp.getOffsetY() );
                }
                return c;
            }
        };
        addAspectConfigTab( "Range", rangeSpecifier );
        ConfigSpecifier viewSpecifier = new ConfigSpecifier( viewKeys );
        ActionSpecifierPanel viewPanel =
                new ActionSpecifierPanel( viewSpecifier ) {
            protected void doSubmit( ActionEvent evt ) {
                if ( oldAspect_ != null ) {
                    ConfigMap config = super.getSpecifiedValue();
                    double[][] limits = oldAspect_.getLimits();
                    double[] rot = CubeSurfaceFactory.getRotation( config );
                    double zoom = config.get( CubeSurfaceFactory.ZOOM_KEY );
                    double xoff = config.get( CubeSurfaceFactory.XOFF_KEY );
                    double yoff = config.get( CubeSurfaceFactory.YOFF_KEY );
                    CubeAspect aspect =
                        new CubeAspect( limits[ 0 ], limits[ 1 ], limits[ 2 ],
                                        rot, zoom, xoff, yoff );
                    setAspect( aspect );
                }
            }
        };
        viewPanel.addActionListener( getActionForwarder() );
        mainControl.addControlTab( "View", viewPanel.getComponent(), true );

        /* Grid config tab. */
        List<ConfigKey> gridKeyList = new ArrayList<ConfigKey>();
        gridKeyList.add( CubeSurfaceFactory.FRAME_KEY );
        gridKeyList.add( StyleKeys.MINOR_TICKS );
        if ( isIso ) {
            gridKeyList.add( CubeSurfaceFactory.ISOCROWD_KEY );
        }
        else {
            gridKeyList.addAll( Arrays.asList( new ConfigKey[] {
                CubeSurfaceFactory.XCROWD_KEY,
                CubeSurfaceFactory.YCROWD_KEY,
                CubeSurfaceFactory.ZCROWD_KEY,
            } ) );
        }
        gridKeyList.add( StyleKeys.GRID_ANTIALIAS );
        mainControl.addSpecifierTab( "Grid",
                         new ConfigSpecifier( gridKeyList
                                             .toArray( new ConfigKey[ 0 ] ) ) );

        /* Labels config tab. */
        addLabelsTab();

        /* Font config tab. */
        mainControl.addSpecifierTab( "Font",
                         new ConfigSpecifier( StyleKeys.getCaptionerKeys() ) );

        assert assertHasKeys( surfFact.getProfileKeys() );
    }

    @Override
    public void setAspect( CubeAspect aspect ) {

        /* Save last aspect for later use. */
        if ( aspect != null ) {
            oldAspect_ = aspect;
        }
        super.setAspect( aspect );
    }

    @Override
    public ConfigMap getConfig() {
        ConfigMap config = super.getConfig();
        if ( isIso_ ) {
            config.put( CubeSurfaceFactory.XLOG_KEY, false );
            config.put( CubeSurfaceFactory.YLOG_KEY, false );
            config.put( CubeSurfaceFactory.ZLOG_KEY, false );
            config.put( CubeSurfaceFactory.XFLIP_KEY, false );
            config.put( CubeSurfaceFactory.YFLIP_KEY, false );
            config.put( CubeSurfaceFactory.ZFLIP_KEY, false );
        }
        return config;
    }

    @Override
    protected boolean logChanged( CubeSurfaceFactory.Profile prof1,
                                  CubeSurfaceFactory.Profile prof2 ) {
        return ! Arrays.equals( prof1.getLogFlags(), prof2.getLogFlags() );
    }

    private static ConfigKey<String>[] createAxisLabelKeys() {
        List<ConfigKey<String>> list = new ArrayList<ConfigKey<String>>();
        list.add( CubeSurfaceFactory.XLABEL_KEY );
        list.add( CubeSurfaceFactory.YLABEL_KEY );
        list.add( CubeSurfaceFactory.ZLABEL_KEY );
        @SuppressWarnings("unchecked")
        ConfigKey<String>[] keys = list.toArray( new ConfigKey[ 0 ] );
        return keys;
    }
}
