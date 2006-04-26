package uk.ac.starlink.ttools.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.UCD;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.MapGroup;

/**
 * Filter for extracting column metadata.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class MetadataFilter extends BasicFilter {

    /*
     * Metadata for column metadata items.
     */
    private static final ValueInfo NAME_INFO =
        new DefaultValueInfo( "Name", String.class, "Column name" );
    private static final ValueInfo CLASS_INFO =
        new DefaultValueInfo( "Class", String.class,
                              "Data type of objects in column" );
    private static final ValueInfo SHAPE_INFO =
        new DefaultValueInfo( "Shape", int[].class,
                              "Shape of array values" );
    private static final ValueInfo UNIT_INFO =
        new DefaultValueInfo( "Units", String.class,
                              "Unit string" );
    private static final ValueInfo DESCRIPTION_INFO =
        new DefaultValueInfo( "Description", String.class,
                              "Description of data in the column" );
    private static final ValueInfo UCD_INFO =
        new DefaultValueInfo( "UCD", String.class,
                              "Unified Content Descriptor" );
    private static final ValueInfo UCDDESC_INFO =
        new DefaultValueInfo( "UCD_desc", String.class,
                              "Textual description of UCD" );
    private static final List KEY_ORDER = Arrays.asList( new ValueInfo[] {
        NAME_INFO,
        CLASS_INFO,
        SHAPE_INFO,
        UNIT_INFO,
        DESCRIPTION_INFO,
        UCD_INFO,
        UCDDESC_INFO,
    } );

    /**
     * Constructor.
     */
    public MetadataFilter() {
        super( "meta", "[<item> ...]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "Provides information about the metadata for each column.",
            "This filter turns the table sideways, so that each row",
            "of the output corresponds to a column of the input.",
            "The columns of the output give column Name, Units, UCD,",
            "Datatype, Description and other metadata relating to the",
            "columns of the input table.",
            "If one or more <code>&lt;item&gt;</code> headings are given,",
            "only the named items will be listed in the output table;",
            "if no items are listed, columns for all available metadata",
            "will be output.",
            "</p><p>Any table parameters of the input table are propagated",
            "to the output one.",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        final String[] items;
        if ( argIt.hasNext() ) {
            List itemList = new ArrayList();
            while ( argIt.hasNext() ) {
                itemList.add( argIt.next() );
                argIt.remove();
            }
            items = (String[]) itemList.toArray( new String[ 0 ] );
        }
        else {
            items = null;
        }
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {
                AbstractStarTable table = 
                    new ValueInfoMapGroupTable( metadataMapGroup( base ),
                                                items );
                table.setParameters( base.getParameters() );
                return table;
            }
        };
    }

    /**
     * Constructs a MapGroup containing column metadata of a given table.
     *
     * @param  table  the table for which to extract metadata
     */
    private static MapGroup metadataMapGroup( StarTable table ) {

        /* Initialise table with a sensible key order for standard metadata
         * items. */
        MapGroup group = new MapGroup();
        group.setKeyOrder( KEY_ORDER );

        /* Count columns in the original table. */
        int ncol = table.getColumnCount();

        /* Compile a name->ValueInfo map of auxiliary metadata items which
         * appear in any of the columns. */
        Map auxInfos = new HashMap();
        for ( int icol = 0; icol < ncol; icol++ ) {
            for ( Iterator it = table.getColumnInfo( icol ).getAuxData()
                                                           .iterator();
                  it.hasNext(); ) {
                Object item = it.next();
                if ( item instanceof DescribedValue ) {
                    DescribedValue dval = (DescribedValue) item;
                    ValueInfo info = dval.getInfo();
                    String name = info.getName();
                    if ( auxInfos.containsKey( name ) ) {
                        info = DefaultValueInfo
                              .generalise( (ValueInfo) auxInfos.get( name ),
                                           info );
                    }
                    auxInfos.put( name, info );
                }
            }
        }

        /* Prepare a metadata map for each column of the input table and
         * add it to the group. */
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo info = table.getColumnInfo( icol );
            Map map = new HashMap();

            /* Add standard metadata items. */
            map.put( NAME_INFO, info.getName() );
            map.put( CLASS_INFO,
                     DefaultValueInfo.formatClass( info.getContentClass() ) );
            map.put( UNIT_INFO, info.getUnitString() );
            map.put( DESCRIPTION_INFO, info.getDescription() );
            String ucd = info.getUCD();
            map.put( UCD_INFO, ucd );
            if ( ucd != null ) {
                UCD u = UCD.getUCD( ucd );
                if ( u != null ) {
                    map.put( UCDDESC_INFO, u.getDescription() );
                }
            }

            /* Add auxiliary items if there are any. */
            if ( ! auxInfos.isEmpty() ) {
                for ( Iterator it = info.getAuxData().iterator();
                      it.hasNext(); ) {
                    Object item = it.next();
                    if ( item instanceof DescribedValue ) {
                        DescribedValue dval = (DescribedValue) item;
                        ValueInfo auxInfo = (ValueInfo)
                            auxInfos.get( dval.getInfo().getName() );
                        map.put( auxInfo, dval.getValue() );
                    }
                }
            }
            group.addMap( map );
        }

        /* Return the group. */
        return group;
    }
}
