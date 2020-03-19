package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Function;
import uk.ac.starlink.table.ValueInfo;

/**
 * Coord implementation for single boolean values.
 *
 * @author   Mark Taylor
 * @since    4 Feb 2013
 */
public class BooleanCoord extends SingleCoord {

    /**
     * Constructor.
     *
     * @param   meta   input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     */
    public BooleanCoord( InputMeta meta, boolean isRequired ) {
        super( meta, isRequired, Boolean.class, StorageType.BOOLEAN, null );
    }

    public Function<Object[],Boolean> inputStorage( ValueInfo[] infos ) {
        return userValues -> {
           Object c = userValues[ 0 ];
           return c instanceof Boolean ? (Boolean) c : Boolean.FALSE;
        };
    }

    /**
     * Reads a boolean value from an appropriate tuple column.
     *
     * @param  tuple  tuple
     * @param  icol  index of field in tuple corresponding to this Coord
     * @return  value of boolean field
     */
    public boolean readBooleanCoord( Tuple tuple, int icol ) {
        return tuple.getBooleanValue( icol );
    }
}
