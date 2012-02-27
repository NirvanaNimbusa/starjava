/* ********************************************************
 * This file automatically generated by LutMap.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST LutMap class
 *  - transform 1-dimensional coordinates using a lookup table. 
 * A LutMap is a specialised form of Mapping which transforms
 * 1-dimensional coordinates by using linear interpolation in a
 * lookup table.
 * <p>
 * Each input coordinate value is first scaled to give the index of
 * an entry in the table by subtracting a starting value (the input
 * coordinate corresponding to the first table entry) and dividing
 * by an increment (the difference in input coordinate value
 * between adjacent table entries).
 * <p>
 * The resulting index will usually contain a fractional part, so
 * the output coordinate value is then generated by interpolating
 * linearly between the appropriate entries in the table. If the
 * index lies outside the range of the table, linear extrapolation
 * is used based on the two nearest entries (i.e. the two entries
 * at the start or end of the table, as appropriate). If either of the
 * entries used for the interplation has a value of AST__BAD, then the
 * interpolated value is returned as AST__BAD.
 * <p>
 * If the lookup table entries increase or decrease monotonically
 * (ignoring any flat sections), then the inverse transformation may
 * also be performed.
 * <h4>Licence</h4>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public Licence as
 * published by the Free Software Foundation; either version 2 of
 * the Licence, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be
 * useful,but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public Licence for more details.
 * <p>
 * You should have received a copy of the GNU General Public Licence
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street,Fifth Floor, Boston, MA
 * 02110-1301, USA
 * 
 * 
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_LutMap'>AST LutMap</a>  
 */
public class LutMap extends Mapping {
    /** 
     * Create a LutMap.   
     * @param  lut  
     *             An array containing the lookup table entries.  There must be
     *             at least two elements.
     *          
     * @param  start  The input coordinate value which corresponds to the first lookup
     * table entry.
     * 
     * @param  inc  The lookup table spacing (the increment in input coordinate
     * value between successive lookup table entries). This value
     * may be positive or negative, but must not be zero.
     * 
     * @throws  AstException  if an error occurred in the AST library
    */
    public LutMap( double[] lut, double start, double inc ) {
        construct( lut, start, inc );
    }
    private native void construct( double[] lut, double start, double inc );

}
