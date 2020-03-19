package uk.ac.starlink.ttools.plot2.data;

import java.util.function.Function;
import uk.ac.starlink.table.ValueInfo;

/**
 * Coord implementation for integer values.
 * A selection of integer lengths is available.
 *
 * @author   Mark Taylor
 * @since    1 Dec 2015
 */
public class IntegerCoord extends SingleCoord {

    private final Number badval_;

    /**
     * Constructor.
     *
     * @param   meta  input value metadata
     * @param   isRequired  true if this coordinate is required for plotting
     * @param   itype   defines integer length used
     */
    public IntegerCoord( InputMeta meta, boolean isRequired, IntType itype ) {
        super( meta, isRequired, Number.class, itype.stype_, null );
        badval_ = itype.badval_;
    }

    public Function<Object[],Number> inputStorage( ValueInfo[] infos ) {
        return userValues -> {
            Object c = userValues[ 0 ];
            return c instanceof Number ? ((Number) c) : badval_;
        };
    }

    /**
     * Reads an integer value from an appropriate field
     * in a given Tuple.
     *
     * @param  tuple  tuple
     * @param  icol  index of field in tuple corresponding to this Coord
     * @return  value of integer field
     */
    public int readIntCoord( Tuple tuple, int icol ) {
        return tuple.getIntValue( icol );
    }

    /**
     * Enumerates the avaialable integer types.
     */
    public enum IntType {

        /** 8-bit signed integer. */
        BYTE( StorageType.BYTE, new Byte( Byte.MIN_VALUE ) ),

        /** 16-bit signed integer. */
        SHORT( StorageType.SHORT, new Short( Short.MIN_VALUE ) ),

        /** 32-bit signed integer. */
        INT( StorageType.INT, new Integer( Integer.MIN_VALUE ) );

        private final StorageType stype_;
        private final Number badval_;

        /**
         * Constructor.
         *
         * @param  stype  storage type
         * @param  badval  in-band value used to indicate bad data
         */
        IntType( StorageType stype, Number badval ) {
            stype_ = stype;
            badval_ = badval;
        }

        /**
         * Returns the in-band numeric value which is used to represent
         * null values from the user input data.  Note the presence of
         * this value in the stored data might be either because the
         * input data had this value, or because it was null.
         *
         * @return  bad value
         */
        public Number getBadValue() {
            return badval_;
        }
    }
}
