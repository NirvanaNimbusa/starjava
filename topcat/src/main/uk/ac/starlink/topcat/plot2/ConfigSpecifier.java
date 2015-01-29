package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.SpecifierPanel;

/**
 * Specifier which supplies a ConfigMap.
 * This just aggregates specifiers for a given set of ConfigKeys;
 * the GUI component contains a specifier component for each key,
 * and the specified value is a map containing the values for each key.
 *
 * <p>By default the GUI component is just a stack of the constituent
 * specifier components, but these can be decorated by supplying
 * a suitable {@link ComponentGui} object.
 *
 * @author   Mark Taylor
 * @since    12 Mar 2013
 */
public class ConfigSpecifier extends SpecifierPanel<ConfigMap> {

    private final KSpec[] kspecs_;
    private static final ComponentGui DEFAULT_GUI = new ComponentGui() {
        public <T> Specifier<T> createSpecifier( ConfigKey<T> key ) {
            return key.createSpecifier();
        }
    };

    /**
     * Constructs a config specifier with a default GUI.
     *
     * @param   keys   config keys to gather values for
     */
    public ConfigSpecifier( ConfigKey[] keys ) {
        this( keys, DEFAULT_GUI );
    }

    /**
     * Constructs a config specifier with a custom GUI for constituent keys.
     *
     * @param   keys   config keys to gather values for
     * @param   gui   specifier generation factory that can provide
     *                customised specifiers for different keys
     */
    public ConfigSpecifier( ConfigKey[] keys, ComponentGui gui ) {
        super( true );
        ActionListener forwarder = getActionForwarder();
        int nk = keys.length;
        kspecs_ = new KSpec[ nk ];
        for ( int ik = 0; ik < nk; ik++ ) {
            KSpec<?> kspec = createKSpec( (ConfigKey<?>) keys[ ik ], gui );
            kspec.specifier_.addActionListener( forwarder );
            kspecs_[ ik ] = kspec;
        }
    }

    
    @Override
    protected JComponent createComponent() {
        LabelledComponentStack stack = new LabelledComponentStack();
        for ( int ik = 0; ik < kspecs_.length; ik++ ) {
            final KSpec<?> kspec = kspecs_[ ik ];
            String name = kspec.key_.getMeta().getLongName();
            final JComponent comp = kspec.specifier_.getComponent();

            /* The aim is to get the specifier components to fill the
             * available width, whether it's large or small. */
            boolean xfill = kspec.specifier_.isXFill();
            if ( xfill ) {
                comp.setPreferredSize( comp.getMinimumSize() );
            }
            stack.addLine( name, null, comp, xfill );

            /* Arrange for the component labels to display tooltips giving
             * the current value in a stilts-friendly format. */
            JLabel[] labels = stack.getLabels();
            final JLabel label = labels[ labels.length - 1 ];
            label.addMouseListener( InstantTipper.getInstance() );
            ActionListener tipListener = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    label.setToolTipText( getToolTip( kspec ) );
                }
            };
            kspec.specifier_.addActionListener( tipListener );
            tipListener.actionPerformed( null );
        }
        JPanel panel = new JPanel( new BorderLayout() );
        panel.add( stack, BorderLayout.NORTH );
        return panel;
    }

    public ConfigMap getSpecifiedValue() {
        ConfigMap map = new ConfigMap();
        for ( int ik = 0; ik < kspecs_.length; ik++ ) {
            kspecs_[ ik ].putValue( map );
        }
        return map;
    }

    public void setSpecifiedValue( ConfigMap configMap ) {
        for ( ConfigKey<?> key : configMap.keySet() ) {
            configureSpecifierFromMap( key, configMap );
        }
    }

    /**
     * Configures the current state of the specifier corresponding to a
     * given key from its value in a given map.
     *
     * @param   key   config key
     * @param   map   map holding a value for <code>key</code>
     */
    private <T> void configureSpecifierFromMap( ConfigKey<T> key,
                                                ConfigMap map ) {
        Specifier<T> specifier = getSpecifier( key );
        if ( specifier != null ) {
            specifier.setSpecifiedValue( map.get( key ) );
        }
    }

    /**
     * Returns the keys for which this specifier gathers values.
     *
     * @return   config keys
     */
    public ConfigKey<?>[] getConfigKeys() {
        ConfigKey[] keys = new ConfigKey[ kspecs_.length ];
        for ( int ik = 0; ik < keys.length; ik++ ) {
            keys[ ik ] = kspecs_[ ik ].key_;
        }
        return keys;
    }

    /**
     * Returns the individual specifier used to gather values for
     * a particular key owned by this object. 
     *
     * @param   key   config key
     * @return   specifier for key
     */
    public <T> Specifier<T> getSpecifier( ConfigKey<T> key ) {
        for ( int ik = 0; ik < kspecs_.length; ik++ ) {
            KSpec kspec = kspecs_[ ik ];
            if ( kspec.key_.equals( key ) ) {
                @SuppressWarnings("unchecked")
                KSpec<T> tspec = (KSpec<T>) kspec;
                return tspec.specifier_;
            }
        }
        return null;
    }

    /**
     * May be used by a client of this specifier to report an error
     * associated with one of the config values obtained by this specifier.
     * The error will be presented to the user somehow or other,
     * and the state of the component will be altered so that the same
     * error doesn't keep happening, at least not without further user
     * activity.
     *
     * @param   err   error encountered in using a value supplied by this
     *                specifier
     */
    public void reportError( ConfigException err ) {
        reportConfigError( err, err.getConfigKey() );
    }

    /**
     * Reports an error for a given constituent key.
     *
     * @param   err  error
     * @param   key  config key for which the value caused trouble
     */
    private <T> void reportConfigError( ConfigException err,
                                        ConfigKey<T> key ) {

        /* Report the error to the user with context showing which value
         * caused it. */
        Object msg = new String[] {
            key.getMeta().getLongName() + " error:",
            err.getMessage(),
        };
        JOptionPane.showMessageDialog( getComponent(), msg,
                                       key.getMeta().getShortName() + " Error",
                                       JOptionPane.ERROR_MESSAGE );

        /* Reset the relevant specifier to a default value so the same
         * thing doesn't happen again straight away. */
        getSpecifier( key ).setSpecifiedValue( key.getDefaultValue() );

        /* There might be a better way to do this: perhaps the error
         * should get passed on to the specifier itself (a new method
         * Specifier.reportError would be required) so that it can take
         * specific actions; e.g. in case of text field, grey out the
         * bad text for user editing rather than deleting it altogether. */
    }

    /**
     * Returns tool tip text for a given key.
     * This gives the parameter assignment text that could be used in STILTS
     * to assign the component's current value.
     *
     * @param  kspec  key specifier
     * @return  tool tip text
     */
    private static <T> String getToolTip( KSpec<T> kspec ) {
        ConfigKey<T> key = kspec.key_;
        T value = kspec.specifier_.getSpecifiedValue();
        return key.getMeta().getShortName()
             + "="
             + key.valueToString( value );
    }

    /**
     * Returns a key specifier for a given key.
     *
     * @param  key   config key
     * @param  gui   GUI component policy
     */
    private static <T> KSpec<T> createKSpec( ConfigKey<T> key,
                                             ComponentGui gui ) {
        return new KSpec<T>( key, gui );
    }

    /**
     * GUI component policy.
     * An instance of this interface defines how the specifier component
     * is generated for each key.
     */
    public interface ComponentGui {

        /**
         * Returns a new specifier for a given key.
         *
         * <p>The obvious way to do it is to call
         * <code>key.createSpecifier()</code>,
         * but abstracting the step using this interface provides a hook
         * to decorate or otherwise customise these specifiers.
         *
         * @param  key  config key
         * @return    specifier for key
         */
        <T> Specifier<T> createSpecifier( ConfigKey<T> key );
    }

    /**
     * Associates a key with an specifier for obtaining its value.
     */
    private static class KSpec<T> {
        private final ConfigKey<T> key_;
        private final Specifier<T> specifier_;

        /**
         * Constructor.
         *
         * @param   key   config key
         * @param   gui   specifier generation factory that can provide
         *                customised specifiers for different keys
         */
        KSpec( ConfigKey<T> key, ComponentGui gui ) {
            key_ = key;
            specifier_ = gui.createSpecifier( key_ );
            specifier_.setSpecifiedValue( key_.getDefaultValue() );
        }

        /**
         * Takes the currently specified value and inserts it into a given
         * map under the appropriate key.
         *
         * @param   map  map into which to insert this spec's key-value pair
         */
        void putValue( ConfigMap map ) {
            map.put( key_, specifier_.getSpecifiedValue() );
        }
    }
}
