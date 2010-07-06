package uk.ac.starlink.table.formats;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MultiStarTableWriter;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.ValueInfo;

/**
 * A <tt>StarTableWriter</tt> which outputs text to a human-readable text file.
 * Table parameters (per-table metadata) can optionally be output 
 * as well as the table data themselves.
 *
 * @author   Mark Taylor (Starlink)
 */
public class TextTableWriter extends AbstractTextTableWriter
                             implements MultiStarTableWriter {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.formats" );

    public TextTableWriter() {
        super( true );
    }

    /**
     * Returns "text";
     *
     * @return "text"
     */
    public String getFormatName() {
        return "text";
    }

    public String getMimeType() {
        return "text/plain";
    }

    /**
     * Returns true if the location argument is equal to "-",
     * indicating standard output.
     */
    public boolean looksLikeFile( String location ) {
        return location.equals( "-" );
    }

    public void writeStarTables( TableSequence tableSeq, OutputStream out )
            throws IOException {
        while ( tableSeq.hasNextTable() ) {
            writeStarTable( tableSeq.nextTable(), out );
            if ( tableSeq.hasNextTable() ) {
                out.write( '\n' );
            }
        }
    }

    public int getMaxWidth() {
        return 40;
    }

    protected String formatValue( Object val, ValueInfo vinfo, int width ) {
        return vinfo.formatValue( val, width );
    }

    protected void printSeparator( OutputStream strm, int[] colwidths )
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '+' );
            strm.write( '-' );
            for ( int j = 0; j < colwidths[ i ]; j++ ) {
                strm.write( '-' );
            }
            strm.write( '-' );
        }
        strm.write( '+' );
        strm.write( '\n' );
    }

    protected void printColumnHeads( OutputStream strm, int[] colwidths,
                                     ColumnInfo[] cinfos ) throws IOException {
        int ncol = cinfos.length;
        String[] heads = new String[ ncol ];
        for ( int i = 0; i < ncol; i++ ) {
            heads[ i ] = cinfos[ i ].getName();
        }
        printSeparator( strm, colwidths );
        printLine( strm, colwidths, heads );
        printSeparator( strm, colwidths );
    }

    protected void printLine( OutputStream strm, int[] colwidths,
                              String[] data ) 
            throws IOException {
        for ( int i = 0; i < colwidths.length; i++ ) {
            strm.write( '|' );
            strm.write( ' ' );
            String datum = ( data[ i ] == null ) ? "" : data[ i ];
            int padding = colwidths[ i ] - datum.length();
            strm.write( getBytes( datum ), 0,
                        Math.min( colwidths[ i ], datum.length() ) );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    strm.write( ' ' );
                }
            }
            strm.write( ' ' );
        }
        strm.write( '|' );
        strm.write( '\n' );
    }

    protected void printParam( OutputStream strm, String name, String value )
            throws IOException {
        strm.write( getBytes( name ) );
        strm.write( ':' );
        strm.write( ' ' );
        strm.write( getBytes( value ) );
        strm.write( '\n' );
    }
}
