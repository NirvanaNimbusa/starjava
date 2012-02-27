
/* ******************************************************************
 *                         DO NOT EDIT!                             *
 *       This file is autogenerated from AstException.pl            *
 *               and built under Ant control                        *
 ********************************************************************/

#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "ast.h"
#include "sae_par.h"
#include "uk_ac_starlink_ast_AstException.h"

#define TRY_CONST(Xident) \
   if ( strcmp( #Xident, ident ) == 0 ) { \
      result = (jint) Xident; \
      success = 1; \
   }

JNIEXPORT jint JNICALL Java_uk_ac_starlink_ast_AstException_getErrConst(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* The class */
   jstring jIdent        /* Name identifying the error constant */
) {
   jint result = (jint) SAI__OK;
   int success = 0;
   const char *ident = (*env)->GetStringUTFChars( env, jIdent, NULL );
   if ( ident != NULL ) {
      TRY_CONST(SAI__OK) 
      else TRY_CONST(SAI__ERROR)
      else TRY_CONST(AST__ATGER)
      else TRY_CONST(AST__ATSER)
      else TRY_CONST(AST__ATTIN)
      else TRY_CONST(AST__AXIIN)
      else TRY_CONST(AST__BADAT)
      else TRY_CONST(AST__BADBX)
      else TRY_CONST(AST__BADIN)
      else TRY_CONST(AST__BADNI)
      else TRY_CONST(AST__BADNO)
      else TRY_CONST(AST__BADPW)
      else TRY_CONST(AST__BADSM)
      else TRY_CONST(AST__BADWM)
      else TRY_CONST(AST__BDBRK)
      else TRY_CONST(AST__BDFMT)
      else TRY_CONST(AST__BDFTS)
      else TRY_CONST(AST__BDOBJ)
      else TRY_CONST(AST__CLPAX)
      else TRY_CONST(AST__CORNG)
      else TRY_CONST(AST__CVBRK)
      else TRY_CONST(AST__DIMIN)
      else TRY_CONST(AST__DTERR)
      else TRY_CONST(AST__ENDIN)
      else TRY_CONST(AST__EOCHN)
      else TRY_CONST(AST__EXPIN)
      else TRY_CONST(AST__FCRPT)
      else TRY_CONST(AST__FMTER)
      else TRY_CONST(AST__FRMIN)
      else TRY_CONST(AST__FRSIN)
      else TRY_CONST(AST__FTCNV)
      else TRY_CONST(AST__GRFER)
      else TRY_CONST(AST__INHAN)
      else TRY_CONST(AST__INNCO)
      else TRY_CONST(AST__INTER)
      else TRY_CONST(AST__INTRD)
      else TRY_CONST(AST__KYCIR)
      else TRY_CONST(AST__LDERR)
      else TRY_CONST(AST__LUTII)
      else TRY_CONST(AST__LUTIN)
      else TRY_CONST(AST__MEMIN)
      else TRY_CONST(AST__MTR23)
      else TRY_CONST(AST__MTRAX)
      else TRY_CONST(AST__MTRML)
      else TRY_CONST(AST__MTRMT)
      else TRY_CONST(AST__NAXIN)
      else TRY_CONST(AST__NCHIN)
      else TRY_CONST(AST__NCOIN)
      else TRY_CONST(AST__NCPIN)
      else TRY_CONST(AST__NELIN)
      else TRY_CONST(AST__NOCTS)
      else TRY_CONST(AST__NODEF)
      else TRY_CONST(AST__NOFTS)
      else TRY_CONST(AST__NOMEM)
      else TRY_CONST(AST__NOPTS)
      else TRY_CONST(AST__NOWRT)
      else TRY_CONST(AST__NPTIN)
      else TRY_CONST(AST__OBJIN)
      else TRY_CONST(AST__OPT)
      else TRY_CONST(AST__PDSIN)
      else TRY_CONST(AST__PLFMT)
      else TRY_CONST(AST__PRMIN)
      else TRY_CONST(AST__PTRIN)
      else TRY_CONST(AST__PTRNG)
      else TRY_CONST(AST__RDERR)
      else TRY_CONST(AST__REGIN)
      else TRY_CONST(AST__REMIN)
      else TRY_CONST(AST__SCSIN)
      else TRY_CONST(AST__SELIN)
      else TRY_CONST(AST__SLAIN)
      else TRY_CONST(AST__TRNND)
      else TRY_CONST(AST__UNMQT)
      else TRY_CONST(AST__VSMAL)
      else TRY_CONST(AST__WCSAX)
      else TRY_CONST(AST__WCSNC)
      else TRY_CONST(AST__WCSPA)
      else TRY_CONST(AST__WCSTY)
      else TRY_CONST(AST__XSOBJ)
      else TRY_CONST(AST__ZOOMI)
      else TRY_CONST(AST__BADCI)
      else TRY_CONST(AST__ILOST)
      else TRY_CONST(AST__ITFER)
      else TRY_CONST(AST__ITFNI)
      else TRY_CONST(AST__MBBNF)
      else TRY_CONST(AST__MRITF)
      else TRY_CONST(AST__OCLUK)
      else TRY_CONST(AST__UNFER)
      else TRY_CONST(AST__URITF)
      else TRY_CONST(AST__GBDIN)
      else TRY_CONST(AST__NGDIN)
      else TRY_CONST(AST__PATIN)
      else TRY_CONST(AST__SISIN)
      else TRY_CONST(AST__SSPIN)
      else TRY_CONST(AST__UINER)
      else TRY_CONST(AST__UK1ER)
      else TRY_CONST(AST__COMIN)
      else TRY_CONST(AST__CONIN)
      else TRY_CONST(AST__DUVAR)
      else TRY_CONST(AST__INNTF)
      else TRY_CONST(AST__MIOPA)
      else TRY_CONST(AST__MIOPR)
      else TRY_CONST(AST__MISVN)
      else TRY_CONST(AST__MLPAR)
      else TRY_CONST(AST__MRPAR)
      else TRY_CONST(AST__NORHS)
      else TRY_CONST(AST__UDVOF)
      else TRY_CONST(AST__VARIN)
      else TRY_CONST(AST__WRNFA)
      else TRY_CONST(AST__BADUN)
      else TRY_CONST(AST__NORSF)
      else TRY_CONST(AST__NOSOR)
      else TRY_CONST(AST__SPCIN)
      else TRY_CONST(AST__XMLNM)
      else TRY_CONST(AST__XMLCM)
      else TRY_CONST(AST__XMLPT)
      else TRY_CONST(AST__XMLIT)
      else TRY_CONST(AST__XMLWF)
      else TRY_CONST(AST__ZERAX)
      else TRY_CONST(AST__BADOC)
      else TRY_CONST(AST__MPGER)
      else TRY_CONST(AST__MPIND)
      else TRY_CONST(AST__REGCN)
      else TRY_CONST(AST__NOVAL)
      else TRY_CONST(AST__INCTS)
      else TRY_CONST(AST__TIMIN)
      else TRY_CONST(AST__STCKEY)
      else TRY_CONST(AST__STCIND)
      else TRY_CONST(AST__CNFLX)
      else TRY_CONST(AST__TUNAM)
      else TRY_CONST(AST__BDPAR)
      else TRY_CONST(AST__3DFSET)
      else TRY_CONST(AST__PXFRRM)
      else TRY_CONST(AST__BADSUB)
      else TRY_CONST(AST__BADFLG)
      else TRY_CONST(AST__LCKERR)
      else TRY_CONST(AST__FUNDEF)
      else TRY_CONST(AST__MPVIN)
      else TRY_CONST(AST__OPRIN)
      else TRY_CONST(AST__NONIN)
      else TRY_CONST(AST__MPKER)
      else TRY_CONST(AST__MPPER)
      else TRY_CONST(AST__BADKEY)
      else TRY_CONST(AST__BADTYP)
      else TRY_CONST(AST__OLDCOL)
      else TRY_CONST(AST__BADNULL)
      else TRY_CONST(AST__BIGKEY)
      else TRY_CONST(AST__BADCOL)
      else TRY_CONST(AST__BIGTAB)
      else TRY_CONST(AST__BADSIZ)
      else TRY_CONST(AST__BADTAB)
      else TRY_CONST(AST__NOTAB)
      else TRY_CONST(AST__LEVMAR)
      else TRY_CONST(AST__NOFIT)
      else TRY_CONST(AST__ISNAN)
      else TRY_CONST(AST__WRERR)
   }
   if ( ! success ) printf( "no such constant %s\n", ident );
   (*env)->ReleaseStringUTFChars( env, jIdent, ident );
   if ( ! success ) {
      jniastThrowIllegalArgumentException( env, "No such constant" );
   }
   return result;
}
#undef TRY_CONST


#define TRY_CONST(Xident) \
   case Xident: result = #Xident; break;

JNIEXPORT jstring JNICALL Java_uk_ac_starlink_ast_AstException_getErrName(
   JNIEnv *env,          /* Interface pointer */
   jclass class,         /* The class */
   jint jCode            /* Status code */
) {
   const char *result = NULL;

   switch ( jCode ) {
      TRY_CONST(SAI__OK)
      TRY_CONST(SAI__ERROR)
      TRY_CONST(AST__ATGER)
      TRY_CONST(AST__ATSER)
      TRY_CONST(AST__ATTIN)
      TRY_CONST(AST__AXIIN)
      TRY_CONST(AST__BADAT)
      TRY_CONST(AST__BADBX)
      TRY_CONST(AST__BADIN)
      TRY_CONST(AST__BADNI)
      TRY_CONST(AST__BADNO)
      TRY_CONST(AST__BADPW)
      TRY_CONST(AST__BADSM)
      TRY_CONST(AST__BADWM)
      TRY_CONST(AST__BDBRK)
      TRY_CONST(AST__BDFMT)
      TRY_CONST(AST__BDFTS)
      TRY_CONST(AST__BDOBJ)
      TRY_CONST(AST__CLPAX)
      TRY_CONST(AST__CORNG)
      TRY_CONST(AST__CVBRK)
      TRY_CONST(AST__DIMIN)
      TRY_CONST(AST__DTERR)
      TRY_CONST(AST__ENDIN)
      TRY_CONST(AST__EOCHN)
      TRY_CONST(AST__EXPIN)
      TRY_CONST(AST__FCRPT)
      TRY_CONST(AST__FMTER)
      TRY_CONST(AST__FRMIN)
      TRY_CONST(AST__FRSIN)
      TRY_CONST(AST__FTCNV)
      TRY_CONST(AST__GRFER)
      TRY_CONST(AST__INHAN)
      TRY_CONST(AST__INNCO)
      TRY_CONST(AST__INTER)
      TRY_CONST(AST__INTRD)
      TRY_CONST(AST__KYCIR)
      TRY_CONST(AST__LDERR)
      TRY_CONST(AST__LUTII)
      TRY_CONST(AST__LUTIN)
      TRY_CONST(AST__MEMIN)
      TRY_CONST(AST__MTR23)
      TRY_CONST(AST__MTRAX)
      TRY_CONST(AST__MTRML)
      TRY_CONST(AST__MTRMT)
      TRY_CONST(AST__NAXIN)
      TRY_CONST(AST__NCHIN)
      TRY_CONST(AST__NCOIN)
      TRY_CONST(AST__NCPIN)
      TRY_CONST(AST__NELIN)
      TRY_CONST(AST__NOCTS)
      TRY_CONST(AST__NODEF)
      TRY_CONST(AST__NOFTS)
      TRY_CONST(AST__NOMEM)
      TRY_CONST(AST__NOPTS)
      TRY_CONST(AST__NOWRT)
      TRY_CONST(AST__NPTIN)
      TRY_CONST(AST__OBJIN)
      TRY_CONST(AST__OPT)
      TRY_CONST(AST__PDSIN)
      TRY_CONST(AST__PLFMT)
      TRY_CONST(AST__PRMIN)
      TRY_CONST(AST__PTRIN)
      TRY_CONST(AST__PTRNG)
      TRY_CONST(AST__RDERR)
      TRY_CONST(AST__REGIN)
      TRY_CONST(AST__REMIN)
      TRY_CONST(AST__SCSIN)
      TRY_CONST(AST__SELIN)
      TRY_CONST(AST__SLAIN)
      TRY_CONST(AST__TRNND)
      TRY_CONST(AST__UNMQT)
      TRY_CONST(AST__VSMAL)
      TRY_CONST(AST__WCSAX)
      TRY_CONST(AST__WCSNC)
      TRY_CONST(AST__WCSPA)
      TRY_CONST(AST__WCSTY)
      TRY_CONST(AST__XSOBJ)
      TRY_CONST(AST__ZOOMI)
      TRY_CONST(AST__BADCI)
      TRY_CONST(AST__ILOST)
      TRY_CONST(AST__ITFER)
      TRY_CONST(AST__ITFNI)
      TRY_CONST(AST__MBBNF)
      TRY_CONST(AST__MRITF)
      TRY_CONST(AST__OCLUK)
      TRY_CONST(AST__UNFER)
      TRY_CONST(AST__URITF)
      TRY_CONST(AST__GBDIN)
      TRY_CONST(AST__NGDIN)
      TRY_CONST(AST__PATIN)
      TRY_CONST(AST__SISIN)
      TRY_CONST(AST__SSPIN)
      TRY_CONST(AST__UINER)
      TRY_CONST(AST__UK1ER)
      TRY_CONST(AST__COMIN)
      TRY_CONST(AST__CONIN)
      TRY_CONST(AST__DUVAR)
      TRY_CONST(AST__INNTF)
      TRY_CONST(AST__MIOPA)
      TRY_CONST(AST__MIOPR)
      TRY_CONST(AST__MISVN)
      TRY_CONST(AST__MLPAR)
      TRY_CONST(AST__MRPAR)
      TRY_CONST(AST__NORHS)
      TRY_CONST(AST__UDVOF)
      TRY_CONST(AST__VARIN)
      TRY_CONST(AST__WRNFA)
      TRY_CONST(AST__BADUN)
      TRY_CONST(AST__NORSF)
      TRY_CONST(AST__NOSOR)
      TRY_CONST(AST__SPCIN)
      TRY_CONST(AST__XMLNM)
      TRY_CONST(AST__XMLCM)
      TRY_CONST(AST__XMLPT)
      TRY_CONST(AST__XMLIT)
      TRY_CONST(AST__XMLWF)
      TRY_CONST(AST__ZERAX)
      TRY_CONST(AST__BADOC)
      TRY_CONST(AST__MPGER)
      TRY_CONST(AST__MPIND)
      TRY_CONST(AST__REGCN)
      TRY_CONST(AST__NOVAL)
      TRY_CONST(AST__INCTS)
      TRY_CONST(AST__TIMIN)
      TRY_CONST(AST__STCKEY)
      TRY_CONST(AST__STCIND)
      TRY_CONST(AST__CNFLX)
      TRY_CONST(AST__TUNAM)
      TRY_CONST(AST__BDPAR)
      TRY_CONST(AST__3DFSET)
      TRY_CONST(AST__PXFRRM)
      TRY_CONST(AST__BADSUB)
      TRY_CONST(AST__BADFLG)
      TRY_CONST(AST__LCKERR)
      TRY_CONST(AST__FUNDEF)
      TRY_CONST(AST__MPVIN)
      TRY_CONST(AST__OPRIN)
      TRY_CONST(AST__NONIN)
      TRY_CONST(AST__MPKER)
      TRY_CONST(AST__MPPER)
      TRY_CONST(AST__BADKEY)
      TRY_CONST(AST__BADTYP)
      TRY_CONST(AST__OLDCOL)
      TRY_CONST(AST__BADNULL)
      TRY_CONST(AST__BIGKEY)
      TRY_CONST(AST__BADCOL)
      TRY_CONST(AST__BIGTAB)
      TRY_CONST(AST__BADSIZ)
      TRY_CONST(AST__BADTAB)
      TRY_CONST(AST__NOTAB)
      TRY_CONST(AST__LEVMAR)
      TRY_CONST(AST__NOFIT)
      TRY_CONST(AST__ISNAN)
      TRY_CONST(AST__WRERR)
   }
   return result ? (*env)->NewStringUTF( env, result ) : NULL;
}
#undef TRY_CONST
