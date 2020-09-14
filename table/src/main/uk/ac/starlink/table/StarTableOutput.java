package uk.ac.starlink.table;

import java.awt.datatransfer.Transferable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.jdbc.JDBCHandler;
import uk.ac.starlink.table.jdbc.WriteMode;
import uk.ac.starlink.table.formats.AsciiTableWriter;
import uk.ac.starlink.table.formats.CsvTableWriter;
import uk.ac.starlink.table.formats.HTMLTableWriter;
import uk.ac.starlink.table.formats.IpacTableWriter;
import uk.ac.starlink.table.formats.LatexTableWriter;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.table.formats.TstTableWriter;
import uk.ac.starlink.util.BeanConfig;
import uk.ac.starlink.util.LoadException;
import uk.ac.starlink.util.Loader;

/**
 * Outputs StarTable objects.
 * This object delegates the actual writing to one of a list of 
 * format-specific writer objects whose content can be configured
 * externally.
 *
 * By default, if the corresponding classes are present, the following
 * handlers are installed:
 * <ul>
 * <li> {@link uk.ac.starlink.votable.FitsPlusTableWriter}
 * <li> {@link uk.ac.starlink.fits.FitsTableWriter}
 * <li> {@link uk.ac.starlink.fits.VariableFitsTableWriter}
 * <li> {@link uk.ac.starlink.fits.HealpixFitsTableWriter}
 * <li> {@link uk.ac.starlink.votable.VOTableWriter}
 * <li> {@link uk.ac.starlink.ecsv.EcsvTableWriter}
 * <li> {@link uk.ac.starlink.feather.FeatherTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.TextTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.AsciiTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.CsvTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.IpacTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.TstTableWriter}
 * <li> {@link uk.ac.starlink.votable.ColFitsPlusTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.HTMLTableWriter}
 * <li> {@link uk.ac.starlink.table.formats.LatexTableWriter}
 * <li> {@link uk.ac.starlink.mirage.MirageTableWriter}
 * </ul>
 * Additionally, any classes named in the <tt>startable.writers</tt>
 * system property (as a colon-separated list) which implement the
 * {@link StarTableWriter} interface and have a no-arg constructor will be
 * instantiated and added to this list of handlers.
 *
 * <p>It can additionally write to JDBC tables.
 *
 * @author   Mark Taylor (Starlink)
 */
public class StarTableOutput {

    private List<StarTableWriter> handlers_;
    private JDBCHandler jdbcHandler_;
    private static String[] defaultHandlerClasses = {
        "uk.ac.starlink.votable.FitsPlusTableWriter",
        "uk.ac.starlink.fits.FitsTableWriter",
        "uk.ac.starlink.fits.VariableFitsTableWriter",
        "uk.ac.starlink.fits.HealpixFitsTableWriter",
        "uk.ac.starlink.votable.ColFitsPlusTableWriter",
        "uk.ac.starlink.fits.ColFitsTableWriter",
        "uk.ac.starlink.votable.VOTableWriter",
        "uk.ac.starlink.ecsv.EcsvTableWriter",
        "uk.ac.starlink.feather.FeatherTableWriter",
        TextTableWriter.class.getName(),
        AsciiTableWriter.class.getName(),
        CsvTableWriter.class.getName(),
        IpacTableWriter.class.getName(),
        TstTableWriter.class.getName(),
        HTMLTableWriter.class.getName(),
        LatexTableWriter.class.getName(),
        "uk.ac.starlink.mirage.MirageTableWriter",
    };
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );

    private StarTableWriter voWriter_;

    /**
     * Special output handler name indicating automatic format selection.
     */
    public static final String AUTO_HANDLER = "(auto)";

    /**
     * System property which can contain a list of {@link StarTableWriter}
     * classes for addition to the list of known output handlers.
     */
    public static final String EXTRA_WRITERS_PROPERTY = "startable.writers";

    /**
     * Constructs a StarTableOutput with a default list of handlers.
     */
    public StarTableOutput() {
        handlers_ = new ArrayList<StarTableWriter>();

        /* Attempt to add default handlers if they are available. */
        for ( int i = 0; i < defaultHandlerClasses.length; i++ ) {
            String className = defaultHandlerClasses[ i ];
            try {
                @SuppressWarnings("unchecked")
                Class<? extends StarTableWriter> clazz =
                    (Class<? extends StarTableWriter>)
                    Class.forName( className );

                StarTableWriter handler = clazz.newInstance();
                handlers_.add( handler );
                logger.config( "Handler " + handler.getFormatName() +
                               " registered" );
            }
            catch ( ClassNotFoundException e ) {
                logger.config( className + " not found - can't register" );
            }
            catch ( Exception e ) {
                logger.config( "Failed to register " + className + " - " + e );
            }
        }

        /* Add any further handlers specified by system property. */
        handlers_.addAll( Loader.getClassInstances( EXTRA_WRITERS_PROPERTY,
                                                    StarTableWriter.class ) );

        /* Further initialization. */
        voWriter_ =
            getTransferableWriter( handlers_
                                  .toArray( new StarTableWriter[ 0 ] ) );
    }

    /**
     * Gets the list of handlers which can actually do table output.
     * Handlers earlier in the list are given a chance to write the
     * table before ones later in the list.  The returned list may be
     * modified to change this object's behaviour.
     *
     * @return  handlers  a list of <tt>StarTableWriter</tt> objects 
     */
    public List<StarTableWriter> getHandlers() {
        return handlers_;
    }

    /**
     * Sets the list of handlers which can actually do table output.
     * Handlers earlier in the list are given a chance to write the
     * table before ones later in the list.
     *
     * @param  handlers  an array of <tt>StarTableWriter</tt> objects
     */
    public void setHandlers( StarTableWriter[] handlers ) {
        handlers_ =
            new ArrayList<StarTableWriter>( Arrays.asList( handlers ) );
    }

    /**
     * Writes a <tt>StarTable</tt> object out to some external storage.
     * The format in which it is written is determined by some
     * combination of the given output location and a format indicator.
     *
     * @param  startab  the table to output
     * @param  location the location at which to write the new table.
     *         This may be a filename or URL, including a <tt>jdbc:</tt>
     *         protocol if suitable JDBC drivers are installed
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be 
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered 
     *         <tt>StarTableWriter</tt>s (first match is used, 
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> or {@link #AUTO_HANDLER} 
     *         to indicate that a handler should be 
     *         selected based on the value of <tt>location</tt>.
     *         Ignored for <tt>jdbc:</tt>-protocol locations
     * @throws TableFormatException  if no suitable handler is known
     */
    public void writeStarTable( StarTable startab, String location,
                                String format )
            throws TableFormatException, IOException {

        /* Handle the JDBC case. */
        if ( location.startsWith( "jdbc:" ) ) {
            try {
                getJDBCHandler().createJDBCTable( startab, location,
                                                  WriteMode.DROP_CREATE );
                return;
            }
            catch ( SQLException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        /* Otherwise dispatch the job to a suitable handler. */
        else {
            getHandler( format, location )
           .writeStarTable( startab, location, this );
        }
    }

    /**
     * Writes a StarTable to an output stream.
     * This convenience method wraps the stream in a BufferedOutputStream
     * for efficiency and uses the submitted <tt>handler</tt> to perform
     * the write, closing the stream afterwards.
     *
     * @param  startab   table to write
     * @param  out       raw output stream
     * @param  handler   output handler
     * @see  #getHandler
     */
    public void writeStarTable( StarTable startab, OutputStream out,
                                StarTableWriter handler ) throws IOException {
        try {
            if ( ! ( out instanceof BufferedOutputStream ) ) {
                out = new BufferedOutputStream( out );
            }
            handler.writeStarTable( startab, out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    /**
     * Writes an array of StarTable objects to some external storage.
     * The format in which they are written is determined by some
     * combination of the given output location and a format indicator.
     * Note that not all registered output handlers are capable of
     * writing multiple tables; an exception will be thrown if an
     * attempt is made to write multiple tables with a handler which
     * does not implement {@link MultiStarTableWriter}.
     *
     * @param   tables  the tables to output
     * @param   location  the location at which to write the tables;
     *          this may be a filename or URL
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>MultiStarTableWriter</tt> object (which may or may not be
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered
     *         <tt>MultiStarTableWriter</tt>s (first match is used,
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> or {@link #AUTO_HANDLER}
     *         to indicate that a handler should be
     *         selected based on the value of <tt>location</tt>.
     */
    public void writeStarTables( StarTable[] tables, String location,
                                 String format )
            throws TableFormatException, IOException {
        StarTableWriter handler = getHandler( format, location );
        if ( handler instanceof MultiStarTableWriter ) {
            ((MultiStarTableWriter) handler)
                .writeStarTables( Tables.arrayTableSequence( tables ),
                                  location, this );
        }
        else if ( tables.length == 1 ) {
            handler.writeStarTable( tables[ 0 ], location, this );
        }
        else {
            throw new TableFormatException( "Output handler "
                                          + handler.getFormatName()
                                          + " can't write multiple tables" );
        }
    }

    /**
     * Writes an array of StarTables to an output stream.
     * This convenience method wraps the stream in a BufferedOutputStream
     * for efficiency and uses the submitted <tt>handler</tt> to perform
     * the write, closing the stream afterwards.
     *
     * @param  tables  tables to write
     * @param  out   destination stream
     * @param  handler  output handler
     */
    public void writeStarTables( StarTable[] tables, OutputStream out,
                                 MultiStarTableWriter handler )
            throws IOException {
        try {
            if ( ! ( out instanceof BufferedOutputStream ) ) {
                out = new BufferedOutputStream( out );
            }
            handler.writeStarTables( Tables.arrayTableSequence( tables ), out );
            out.flush();
        }
        finally {
            out.close();
        }
    }

    /**
     * Returns a sink which allows you to write data to an output table.
     * Note that this will only work if the <code>handler</code> can write
     * the table using a single pass of the data.  If it requires multiple
     * passes, a <code>UnrepeatableSequenceException</code> will be thrown.
     *
     * @param  out       raw output stream
     * @param  handler   output handler
     * @return  sink whose data will be written to a new table
     */
    public TableSink createOutputSink( final OutputStream out,
                                       final StarTableWriter handler ) {
        return new StreamTableSink() {
            protected void scanTable( StarTable table ) throws IOException {
                writeStarTable( table, out, handler );
            }
        };
    }

    /**
     * Returns a sink which allows you to write data to an output table.
     * Note that this will only work if the <code>handler</code> can write
     * the table using a single pass of the data.  If it requires multiple
     * passes, a <code>UnrepeatableSequenceException</code> will be thrown.
     *
     * @param  location the location at which to write the new table.
     *         This may be a filename or URL, including a <tt>jdbc:</tt>
     *         protocol if suitable JDBC drivers are installed
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered
     *         <tt>StarTableWriter</tt>s (first match is used,
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> or {@link #AUTO_HANDLER}
     *         to indicate that a handler should be
     *         selected based on the value of <tt>location</tt>.
     *         Ignored for <tt>jdbc:</tt>-protocol locations
     * @return  sink whose data will be written to a new table
     */
    public TableSink createOutputSink( final String location,
                                       final String format ) {
        return new StreamTableSink() {
            protected void scanTable( StarTable table ) throws IOException {
                writeStarTable( table, location, format );
            };
        };
    }

    /**
     * Returns an output stream which points to a given location.
     * Typically <tt>location</tt> is a filename and a corresponding
     * <tt>FileOutputStream</tt> is returned, but there may be other
     * possibilities.  The stream returned by this method will not
     * in general be buffered; for high performance writes, wrapping it
     * in a {@link java.io.BufferedOutputStream} may be a good idea.
     *
     * @param   location  name of destination
     * @return   output stream which writes to <tt>location</tt>
     * @throws  IOException  if no stream pointing to <tt>location</tt>
     *          can be opened
     */
    public OutputStream getOutputStream( String location ) throws IOException {

        /* Single minus sign indicates standard output.
         * Wrap it so that a close has no effect, since it is not generally
         * healthy to close stdout. */
        if ( location.equals( "-" ) ) {
            final OutputStream out = System.out;
            return new FilterOutputStream( out ) {
                public void close() throws IOException {
                    out.flush();
                }
            };
        }

        /* Try to interpret it as a URL. */
        try {
            URL url = new URL( location );
            URLConnection uconn = url.openConnection();
            uconn.setDoInput( false );
            uconn.setDoOutput( true );
            uconn.connect();
            return uconn.getOutputStream();
        }
        catch ( MalformedURLException e ) {
            // nope.
        }

        /* Otherwise, assume it's a filename. */
        File file = new File( location );

        /* If the file exists, attempt to delete it before attempting
         * to write to it.  This is potentially important since we 
         * don't want to scribble all over a file which may already 
         * be mapped - quite likely if we're overwriting mapped a 
         * FITS file.
         * On POSIX deleting (unlinking) a mapped file will keep its data
         * safe until it's unmapped.  On Windows, deleting it will 
         * fail - in this case we log a warning. */
        if ( file.exists() ) {
            if ( file.delete() ) {
                logger.info( "Deleting file \"" + location + 
                             "\" prior to overwriting" );
            }
            else {
                logger.warning( "Failed to delete \"" + location +
                                       "\" prior to overwriting" );
            }
        }

        /* Return a new stream which will write to the file. */
        return new FileOutputStream( file );
    }

    /**
     * Returns a StarTableWriter object given an output format name.
     *
     * @param  format  a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered
     *         <tt>StarTableWriter</tt>s (first match is used,
     *         case-insensitive, starting substrings OK).
     * @throws TableFormatException  if no handler suitable for the arguments
     *         can be found
     * @return a suitable output handler
     */
    public StarTableWriter getHandler( String format )
            throws TableFormatException {

        /* See if it's the special value. */
        if ( format.equals( AUTO_HANDLER ) ) {
            throw new TableFormatException( format + " does not name a " +
                                            "specific output handler" );
        }

        /* Parse the format name, and see if it has a configuration
         * parenthesis. */
        BeanConfig config = BeanConfig.parseSpec( format );
        String fname = config.getBaseText();
        boolean isDynamic = config.getConfigText() != null;

        /* If there's no dynamic configuration required, try all the
         * known handlers, matching against prefix of format name. */
        if ( ! isDynamic ) {
            for ( StarTableWriter handler : handlers_ ) {
                if ( handler.getFormatName().toLowerCase()
                            .startsWith( format.toLowerCase() ) ) {
                    return handler;
                }
            }
        }

        /* Otherwise, try to create a handler based on the given name,
         * which may be a handler name or a StarTableWriter classname,
         * and configure it if so required. */
        StarTableWriter handler = createNamedHandler( fname );
        if ( handler != null ) {
            try {
                config.configBean( handler );
            }
            catch ( LoadException e ) {
                throw new TableFormatException( "Handler configuration failed: "
                                              + e, e );
            }
            return handler;
        }

        /* No luck - throw an exception. */
        throw new TableFormatException( "No handler for table format \"" +
                                        format + "\"" );
    }

    /**
     * Returns a newly created TableBuilder instance corresponding to a given
     * specified name.  The given name may be a classname or the name
     * of one of the handlers known to this factory that corresponds to
     * a default instance (created with a no-arg constructor).
     *
     * @param   name  class name or label
     * @return  new and unconfigured StarTableWriter instance,
     *          or null if nothing suitable is found
     * @throws   TableFormatException  if the given name looks like it
     *           references a class but there is a problem with it
     */
    private StarTableWriter createNamedHandler( String fname )
            throws TableFormatException {
        for ( StarTableWriter handler : handlers_ ) {
            if ( handler.getFormatName().toLowerCase().equals( fname ) ) {
                Class<? extends StarTableWriter> hclazz = handler.getClass();
                StarTableWriter handler1;
                try {
                    handler1 = hclazz.newInstance();
                }
                catch ( ReflectiveOperationException e ) {
                    throw new TableFormatException( "Can't instantiate class "
                                                  + hclazz.getName() );
                }
                if ( handler1.getFormatName().equalsIgnoreCase( fname ) ) {
                    return handler1;
                }
                else {
                    return null;
                }
            }
        }
        Class<?> clazz;
        try {
            clazz = Class.forName( fname );
        }
        catch ( ClassNotFoundException e ) {
            return null;
        }
        if ( StarTableWriter.class.isAssignableFrom( clazz ) ) {
            Class<? extends StarTableWriter> hclazz =
                clazz.asSubclass( StarTableWriter.class );
            try {
                return hclazz.newInstance();
            }
            catch ( ReflectiveOperationException e ) {
                throw new TableFormatException( "Can't instantiate class "
                                              + hclazz.getName() );
            }
        }
        else {
            throw new TableFormatException( "Class " + clazz.getName()
                                          + " does not implement"
                                          + " StarTableWriter" );
        }
    }

    /**
     * Returns a StarTableWriter object given an output format name
     * and/or a location to write to.  If the format name is blank,
     * the location is used to guess the type of output required.
     *
     * @param  format   a string which indicates in some way what format
     *         should be used for output.  This may be the class name of
     *         a <tt>StarTableWriter</tt> object (which may or may not be
     *         registered with this <tt>StarTableOutput</tt>), or else
     *         a string which matches the format name of one of the registered
     *         <tt>StarTableWriter</tt>s (first match is used,
     *         case-insensitive, starting substrings OK)
     *         or <tt>null</tt> to indicate that a handler should be
     *         selected based on the value of <tt>location</tt>.
     * @param  location  destination of the table to be written.
     *         If <tt>format</tt> is null, the value of this will be used
     *         to try to determine which handler to use, typically on the
     *         basis of filename extension
     * @throws TableFormatException  if no handler suitable for the arguments
     *         can be found
     * @return a suitable output handler
     */
    public StarTableWriter getHandler( String format, String location ) 
            throws TableFormatException {

        /* Do we have a format string? */
        if ( format != null && format.length() > 0 && 
             ! AUTO_HANDLER.equals( format ) ) {
            return getHandler( format );
        }

        /* If no format has been specified, offer it to the first handler 
         * which likes the look of its filename. */
        else {
            for ( StarTableWriter handler : handlers_ ) {
                if ( handler.looksLikeFile( location ) ) {
                    return handler;
                }
            }

            /* None of them do - failure. */
            StringBuffer msg = new StringBuffer();
            msg.append( "No handler specified for writing table.\n" )
               .append( "Known formats: " );
            for ( Iterator<String> it = getKnownFormats().iterator();
                  it.hasNext(); ) {
                msg.append( it.next() );
                if ( it.hasNext() ) {
                    msg.append( ", " );
                }
            }
            throw new TableFormatException( msg.toString() );
        }
    }

    /**
     * Returns a list of the format strings which are defined by the
     * handlers registered with this object.  The elements of the returned
     * list can be passed as the <tt>format</tt> argument to the 
     * {@link #writeStarTable} method.
     */
    public List<String> getKnownFormats() {
        List<String> kf = new ArrayList<String>();
        kf.add( "jdbc" );
        for ( StarTableWriter handler : handlers_ ) {
            kf.add( handler.getFormatName() );
        }
        return kf;
    }

    /**
     * Returns the JDBCHandler object used for writing tables to JDBC
     * connections.
     *
     * @return  the JDBC handler
     */
    public JDBCHandler getJDBCHandler() {
        if ( jdbcHandler_ == null ) {
            jdbcHandler_ = new JDBCHandler();
        }
        return jdbcHandler_;
    }

    /**
     * Sets the JDBCHandler object used for writing tables to JDBC 
     * connections.
     *
     * @param  handler  the handler to use
     */
    public void setJDBCHandler( JDBCHandler handler ) {
        jdbcHandler_ = handler;
    }

    /**
     * Returns a <tt>Transferable</tt> object associated with a given
     * StarTable, for use at the drag end of a drag and drop operation.
     *
     * @param  startab  the table which is to be dragged
     * @see  StarTableFactory#makeStarTable(java.awt.datatransfer.Transferable)
     */
    public Transferable transferStarTable( final StarTable startab ) {
        if ( voWriter_ != null ) {
            return new StarTableTransferable( this, startab );
        }
        else {
            return null;
        }
    }

    /**
     * Returns a writer suitable for data transfer (drag'n'drop) operations.
     *
     * @return  transfer writer
     */
    StarTableWriter getTransferWriter() {
        return voWriter_;
    }

    /**
     * Sets up one serializer suitable for streaming objects during
     * drag and drop.  In the current implementation a VOTable BINARY
     * is used if available, otherwise VOTable TABLEDATA.
     *
     * @param  handlers  available handler list
     * @return  widely-understandable serializer
     */
    private static StarTableWriter
            getTransferableWriter( StarTableWriter[] handlers ) {

        /* Identify a suitable handler by name.  Can't easily do it by class,
         * since the VOTable classes are probably not available at
         * compile time. */
        StarTableWriter tdWriter = null;
        for ( int ih = 0; ih < handlers.length; ih++ ) {
            StarTableWriter handler = handlers[ ih ];
            String formatName = handler.getFormatName();
            if ( "votable-binary-inline".equals( formatName ) ) {
                return handler;
            }
            if ( "votable-tabledata".equals( formatName ) ) {
                tdWriter = handler;
            }
        }
        return tdWriter;
    }
}
