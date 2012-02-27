/* ********************************************************
 * This file automatically generated by StcResourceProfile.pl.
 *                   Do not edit.                         *
 **********************************************************/

package uk.ac.starlink.ast;


/**
 * Java interface to the AST StcResourceProfile class
 *  - correspond to the IVOA STCResourceProfile class. 
 * The StcResourceProfile class is a sub-class of Stc used to describe
 * the coverage of the datasets contained in some VO resource.
 * <p>
 * See http://hea-www.harvard.edu/~arots/nvometa/STC.html
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
 * @see  <a href='http://star-www.rl.ac.uk/cgi-bin/htxserver/sun211.htx/?xref_StcResourceProfile'>AST StcResourceProfile</a>  
 */
public class StcResourceProfile extends Stc {

   /**
    * Constructs a new StcResourceProfile.
    *
    * @param   region  the encapsulated region
    * @param   coords  the AstroCoords elements associated with this Stc
    */
   public StcResourceProfile( Region region, AstroCoords[] coords ) {
       construct( region, astroCoordsToKeyMaps( coords ) );
   }
   private native void construct( Region region, KeyMap[] coordMaps );
}
