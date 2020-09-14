package uk.ac.starlink.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for configuring dynamically loaded objects.
 *
 * <p>The idea is that objects can be configured from user-supplied text
 * strings that configure bean-like properties.
 * For instance an object may be specified on the command line or using
 * a system property with a syntax like:
 * <pre>
 *    "java.util.Date(year=212,day=1)"
 * </pre>
 * which would construct a Date object using its no-arg constructor,
 * and then configure the bean-like "year" property by reflectively
 * invoking the <code>setYear</code> and <code>setDay</code> methods
 * of the Date class.
 *
 * <p>Where present, a {@link ConfigMethod} annotation on the
 * relevant mutator methods is used to provide aliased property names,
 * as well as to improve LoadException error messages when
 * property setting fails.
 *
 * <p>The configuration syntax is fairly straightforward;
 * the format is a comma-separated list of <code>name=value</code>
 * settings within a pair of parentheses.  Obvious serializations are
 * supported for numeric and boolean values; string values are unquoted;
 * commas may be backslash-escaped; and symbolic values are permitted
 * for Enums and static public members on the value class or
 * created object class.
 *
 * @author   Mark Taylor
 * @since    11 Sep 2020
 * @see      ConfigMethod
 */
public class BeanConfig {

    private final String baseTxt_;
    private final String configTxt_;

    private static final Pattern BEANARG_REGEX =
        Pattern.compile( "[, ]*([A-Za-z][A-Za-z0-9_]*) *= *([^,]*)" );
    private static final Pattern CONFIG_REGEX =
        Pattern.compile( "([^(]*)(?:[(]([^)]*)[)]) *" );
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.util" );

    /**
     * Constructs a BeanConfig with a basic object representation and
     * a string providing configuration information.
     * The baseTxt could be a classname or something else.
     * The configTxt should normally be of the form
     * <code>name1=value1,name2=value2,...</code>;
     * if other forms are used, a LoadException with a helpful
     * error message will be generated as appropriate by relevant method
     * invocations.
     *
     * @param  baseTxt   basic object name or representation
     * @param  configTxt   configuration text, normally as supplied
     *                     in trailing parenthesis
     */
    public BeanConfig( String baseTxt, String configTxt ) {
        baseTxt_ = baseTxt;
        configTxt_ = configTxt;
    }

    /**
     * Returns the basic object name for this object.
     * May be a classname or something else.
     * This should not be null.
     *
     * @return  basic representation
     */
    public String getBaseText() {
        return baseTxt_;
    }

    /**
     * Returns configuration specification.
     * May be null if there was no trailing parenthesis.
     *
     * @return   configuration text, nominally of the form "n1=v1,n2=v2,...",
     */
    public String getConfigText() {
        return configTxt_;
    }

    /**
     * Returns the parsed settings for this object, as extracted
     * from the configuration text.
     * The list may be empty.
     *
     * @return  configuration settings
     * @throws   LoadException  if the config text was badly-formed
     */
    public Setting[] getSettings() throws LoadException {
        return parseSettings( configTxt_ );
    }

    /**
     * Attempts to apply this object's configuration setttings
     * to a supplied target object.
     *
     * @param  target  object to configure
     * @throws  LoadException with a helpful error message
     *          if the settings don't work
     */
    public void configBean( Object target ) throws LoadException {
        final Setting[] settings;
        try {
            settings = getSettings();
        }
        catch ( LoadException e ) {
            String msg = "Badly formed settings - " + getOptionUsage( target );
            throw new LoadException( msg, e );
        }
        for ( Setting setting : settings ) {
            try {
                applySetting( target, setting );
            }
            catch ( ReflectiveOperationException e ) {
                String msg = new StringBuffer()
                    .append( "Failed setting \"" )
                    .append( setting )
                    .append( "\"" )
                    .append( " - " )
                    .append( getOptionUsage( target ) )
                    .toString();
                throw new LoadException( msg, e );
            }
            catch ( RuntimeException e ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "Failed setting \"" )
                    .append( setting )
                    .append( "\"" );
                for ( SettingOpt opt : getSettingOpts( target ) ) {
                    if ( opt.name_.equals( setting.getPropertyName() ) ) {
                        sbuf.append( " - Usage is " )
                            .append( opt );
                    }
                }
                throw new LoadException( sbuf.toString(), e );
            }
        }
    }

    /**
     * Parses an object specification to produce a BeanConfig instance.
     * The specification is of the form <code>base-name(n1=v1,n2=v2,...)</code>.
     * If there is no well-formed parenthesised config-string at the end,
     * the returned object has a base-name that is the whole of the input
     * string, and no config text.
     *
     * @param   txt  input text
     * @return   BaseConfig object, which may or may not have configuration
     *           information
     */
    public static BeanConfig parseSpec( String txt ) {
        Matcher matcher = CONFIG_REGEX.matcher( txt );
        return matcher.matches()
             ? new BeanConfig( matcher.group( 1 ), matcher.group( 2 ).trim() )
             : new BeanConfig( txt, null );
    }

    /**
     * Returns the usage string for the configurable options available
     * on a given object.
     *
     * @param  target  configurable object
     * @return   options list explanation string
     */
    private static String getOptionUsage( Object target ) {
        List<SettingOpt> opts = getSettingOpts( target );
        return opts.size() > 0
             ? new StringBuffer()
                  .append( "Options are " )
                  .append( opts.stream()
                               .map( SettingOpt::toString )
                               .collect( Collectors.joining( ", " ) ) )
                  .toString()
             : "No config options available";
    }

    /**
     * Applies a given setting to an object.
     *
     * @param   target  object to configure
     * @param   setting   setting to apply
     * @throws  RuntimeException  in case of various problems with converting
     *                            the supplied string to the required type
     * @throws  ReflectiveOperationException  in case of some reflection trouble
     */
    public static void applySetting( Object target, Setting setting )
            throws ReflectiveOperationException {
        Class<?> clazz = target.getClass();
        Method method = getMutatorMethod( setting, clazz );
        if ( method == null ) {
            throw new NoSuchMethodException( "No method "
                                           + setting.getMutatorName()
                                           + " in " + clazz.getName() );
        }
        Class<?> argClazz = method.getParameterTypes()[ 0 ];
        Object valueObj =
            decodeTypedValue( argClazz, setting.getPropertyValue(), target );
        method.invoke( target, new Object[] { valueObj } );
    }

    /**
     * Interprets a list of comma-separated name=value pairs as a
     * list of Setting objects.
     *
     * @param  configTxt   list of name=value pairs
     * @return  parsed content
     * @throws  LoadException  if configTxt is not of form n1=v1,n2=v2,...
     */
    private static Setting[] parseSettings( String configTxt )
            throws LoadException {
        List<Setting> settings = new ArrayList<>();
        if ( configTxt != null && configTxt.length() > 0 ) {
            Matcher matcher = BEANARG_REGEX.matcher( configTxt );
            int iend = 0;
            while ( matcher.find() ) {
                settings.add( new Setting( matcher.group( 1 ).trim(),
                                           matcher.group( 2 ).trim() ) );
                iend = matcher.end();
            }
            String over = configTxt.substring( iend );
            if ( over.trim().length() > 0 ) {
                throw new LoadException( "Badly-formed settings text "
                                       + "(not n1=v1,n2=v2,...)" );
            }
        }
        return settings.toArray( new Setting[ 0 ] );
    }

    /**
     * Returns a list of configuration options available for a given target.
     * These are determined by bean-like reflection.
     * This is effectively used for documentation only.
     *
     * @param  target  object to interrogate
     * @return  list of possible property options
     */
    private static List<SettingOpt> getSettingOpts( Object target ) {
        List<SettingOpt> opts = new ArrayList<>();
        if ( target != null ) {
            for ( Method meth : target.getClass().getMethods() ) {
                Class<?> propType = getMutationType( meth );
                if ( propType != null ) {
                    String propName = null;
                    ConfigMethod annotation =
                        meth.getAnnotation( ConfigMethod.class );
                    if ( annotation != null ) {
                        String annProperty = annotation.property();
                        if ( annProperty != null && annProperty.length() > 0 ) {
                            propName = annProperty;
                        }
                    }
                    if ( propName == null ) {
                        String mname = meth.getName();
                        if ( mname.matches( "set[A-Z].*" ) ) {
                            propName = Character.toLowerCase( mname.charAt( 3 ))
                                     + mname.substring( 4 );
                        }
                    }
                    if ( annotation != null && annotation.hide() ) {
                        propName = null;
                    }
                    if ( propName != null ) {
                        String propUsage = getMethodUsage( meth );
                        opts.add( new SettingOpt( propName, propUsage ) );
                    }
                }
            }
        }
        return opts;
    }

    /**
     * Returns a usage string corresponding to a given configuration method.
     *
     * @param  configMethod  mutator method
     * @return  short user-directed usage text
     */
    public static String getMethodUsage( Method configMethod ) {
        ConfigMethod ann = configMethod.getAnnotation( ConfigMethod.class );
        String annUsage = ann == null ? null : ann.usage();
        if ( annUsage != null && annUsage.length() > 0 ) {
            return annUsage;
        }
        Class<?> optClazz = getMutationType( configMethod );
        if ( optClazz == boolean.class || optClazz == Boolean.class ) {
            return "true|false";
        }
        else if ( optClazz.isEnum() ) {
            return Arrays.stream( optClazz.getEnumConstants() )
                         .map( Object::toString )
                         .collect( Collectors.joining( "|" ) );
        }
        else if ( optClazz != null ) {
            return "<" + optClazz.getSimpleName() + ">";
        }
        return "??";
    }

    /**
     * Returns the object type which a given mutation method sets.
     *
     * @param   meth  method
     * @return   type of value set by method, or null
     */
    private static Class<?> getMutationType( Method meth ) {
        int mods = meth.getModifiers();
        if ( Modifier.isPublic( mods ) && ! Modifier.isStatic( mods ) ) {
            Class<?>[] ptypes = meth.getParameterTypes();
            if ( ptypes.length == 1 ) {
                return ptypes[ 0 ];
            }
        }
        return null;
    }

    /**
     * Attempts to interpret a string as an instance of a given class.
     *
     * @param  clazz  required output class
     * @param  txt    text representation of value
     * @param  target  object in the context of which interpretation is done
     * @return  value of required type, or null
     * @throws  RuntimeException  if decoding is unsuccessful
     */
    @SuppressWarnings("unchecked")
    private static <T> T decodeTypedValue( Class<T> clazz, String txt,
                                           Object target ) {
        if ( txt == null || txt.trim().length() == 0 ) {
            if ( ! clazz.isPrimitive() ) {
                return (T) null;
            }
            else if ( clazz.equals( float.class ) ) {
                return (T) Float.valueOf( Float.NaN );
            }
            else if ( clazz.equals( double.class ) ) {
                return (T) Double.valueOf( Double.NaN );
            }
            else {
                throw new NullPointerException();
            }
        }
        else if ( clazz.equals( boolean.class ) ||
                  clazz.equals( Boolean.class ) ) {
            return (T) Boolean.valueOf( txt );
        }
        else if ( clazz.equals( byte.class ) || clazz.equals( Byte.class ) ) {
            return (T) Byte.valueOf( txt );
        }
        else if ( clazz.equals( short.class ) || clazz.equals( Short.class ) ) {
            return (T) Short.valueOf( txt );
        }
        else if ( clazz.equals( int.class ) || clazz.equals( Integer.class ) ) {
            return (T) Integer.valueOf( txt );
        }
        else if ( clazz.equals( long.class ) || clazz.equals( Long.class ) ) {
            return (T) Long.valueOf( txt );
        }
        else if ( clazz.equals( float.class ) || clazz.equals( Float.class ) ) {
            return (T) Float.valueOf( txt );
        }
        else if ( clazz.equals( double.class ) ||
                  clazz.equals( Double.class ) ) {
            return (T) Double.valueOf( txt );
        }
        else if ( clazz.equals( char.class ) ||
                  clazz.equals( Character.class ) ) {
            return (T) Character.valueOf( txt.replaceAll( "\\", "," )
                                             .replaceAll( "\\\\", "\\" )
                                             .charAt( 0 ) );
        }
        else if ( clazz.equals( String.class ) ) {
            return (T) txt.replaceAll( "\\,", "," )
                          .replaceAll( "\\\\", "\\" );
        }
        else if ( Enum.class.isAssignableFrom( clazz ) ) {
            @SuppressWarnings("unchecked")
            Enum<?> evalue = Enum.valueOf( (Class<Enum>) clazz, txt );
            return (T) evalue;
        }
        else {
            T targetMember = getTypedMember( clazz, txt, target.getClass() );
            if ( targetMember != null ) {
                return targetMember;
            }
            T classMember = getTypedMember( clazz, txt, clazz );
            if ( classMember != null ) {
                return classMember;
            }
            else {
                throw new IllegalArgumentException( "Can't convert string "
                                                  + "\"" + txt + "\" to "
                                                  + clazz.getSimpleName() );
            }
        }
    }

    /**
     * Attempts to find a static member of a given class with a given name
     * and type.
     *
     * @param  reqClazz  required output class
     * @param  txt   value representation; name of static member
     * @param  ownerClazz   class to search for static members
     * @return   member value fitting requirements, or null
     */
    private static <T> T getTypedMember( Class<T> reqClazz, String txt,
                                         Class<?> ownerClazz ) {
        try {
            Field field = ownerClazz.getField( txt );
            if ( reqClazz.isAssignableFrom( field.getType() ) ) {
                return reqClazz.cast( field.get( null ) );
            }
            else {
                return null;
            }
        }
        catch ( ReflectiveOperationException | NullPointerException e ) {
            return null;
        }
    }

    /**
     * Returns a method member of a given class to use for
     * applying a given setting.
     *
     * @param  setting  configuration option
     * @param  clazz   class to which config will be applied
     * @return  set* method with one parameter, or null if nothing suitable
     */
    private static Method getMutatorMethod( Setting setting, Class<?> clazz ) {
        String propName = setting.getPropertyName();
        String mutatorName = setting.getMutatorName();
        for ( Method meth : clazz.getMethods() ) {
            if ( getMutationType( meth ) != null ) {
                if ( mutatorName.equalsIgnoreCase( meth.getName() ) ) {
                    return meth;
                }
                ConfigMethod annotation =
                    meth.getAnnotation( ConfigMethod.class );
                if ( annotation != null &&
                     propName.equalsIgnoreCase( annotation.property() ) ) {
                    return meth;
                }
            }
        }
        return null;
    }

    /**
     * Represents an object configuration operation.
     */
    public static class Setting {

        private final String propName_;
        private final String propValue_;

        /**
         * Constructor.
         *
         * @param   propName  property name
         * @param   propValue   property value text representation
         */
        public Setting( String propName, String propValue ) {
            propName_ = propName;
            propValue_ = propValue;
        }

        /**
         * Returns the property name.
         *
         * @return  property name
         */
        public String getPropertyName() {
            return propName_;
        }

        /**
         * Returns the text representation of the property value.
         *
         * @return  property value text representation
         */
        public String getPropertyValue() {
            return propValue_;
        }

        /**
         * Returns the name of the instance mutator method corresponding
         * to this setting's property.
         *
         * @return  set* method name
         */
        public String getMutatorName() {
            return new StringBuffer()
                .append( "set" )
                .append( Character.toUpperCase( propName_.charAt( 0 ) ) )
                .append( propName_.substring( 1 ) )
                .toString();
        }

        @Override
        public String toString() {
            return propName_ + "=" + propValue_;
        }
    }

    /**
     * Utility class that aggregates a setting name and usage text.
     */
    private static class SettingOpt {
        final String name_;
        final String usage_;
        SettingOpt( String name, String usage ) {
            name_ = name;
            usage_ = usage;
        }
        @Override
        public String toString() {
            return name_ + "=" + usage_;
        }
    }

    public static void main( String[] args )
            throws ReflectiveOperationException, LoadException {
        BeanConfig config = BeanConfig.parseSpec( args[ 0 ] );
        Class<?> clazz = Class.forName( config.getBaseText() );
        Object target = clazz.newInstance();
        config.configBean( target );
        System.out.println( target );
    }
}
