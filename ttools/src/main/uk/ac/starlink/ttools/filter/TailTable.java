package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.LinkedList;
import uk.ac.starlink.table.AccessRowSequence;
import uk.ac.starlink.table.IteratorRowSequence;
import uk.ac.starlink.table.RowAccess;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.WrapperRowAccess;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Wrapper table implementation which only contains the last N rows of
 * its base table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Mar 2005
 */
public class TailTable extends WrapperStarTable {

    private final long ntail_;

    /**
     * Constructor.
     *
     * @param  base  base table
     * @param  ntail  number ofrows at the end of the table to use
     */
    public TailTable( StarTable base, long ntail ) {
        super( base );
        ntail_ = ntail;
    }

    public long getRowCount() {
        long nbase = super.getRowCount();
        return nbase >= 0L ? Math.min( ntail_, nbase )
                           : -1L;
    }

    public Object getCell( long irow, int icol ) throws IOException {
        return super.getCell( translateRow( irow ), icol );
    }

    public Object[] getRow( long irow ) throws IOException {
        return super.getRow( translateRow( irow ) );
    }

    private long translateRow( long irow ) {
        return super.getRowCount() - getRowCount() + irow;
    }

    public RowAccess getRowAccess() throws IOException {
        final long ioff = super.getRowCount() - getRowCount();
        return new WrapperRowAccess( super.getRowAccess() ) {
            @Override
            public void setRowIndex( long irow ) throws IOException {
                super.setRowIndex( ioff + irow );
            }
        };
    }

    public RowSequence getRowSequence() throws IOException {
        if ( isRandom() ) {
            return AccessRowSequence.createInstance( this );
        }
        else {
            long nbase = super.getRowCount();
            final RowSequence baseSeq = super.getRowSequence();
            if ( nbase >= 0 ) {
                long nskip = nbase - ntail_;
                for ( long i = 0; i < nskip && baseSeq.next(); i++ );
                return baseSeq;
            }
            else {
                LinkedList<Object[]> queue = new LinkedList<Object[]>();
                long size = 0;
                while ( baseSeq.next() ) {
                    queue.addLast( baseSeq.getRow() );
                    size++;
                    if ( size > ntail_ ) {
                        queue.removeFirst();
                        size--;
                    }
                }
                assert size <= ntail_;
                assert size == queue.size();
                return new IteratorRowSequence( queue.iterator() );
            }
        }
    }

    public RowSplittable getRowSplittable() throws IOException {
        return Tables.getDefaultRowSplittable( this );
    }
}
