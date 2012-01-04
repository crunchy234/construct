package construct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


/*

# struct.py from the pypy project

# Modified for missing string multiplication
# Modified for missing list addition ([1] + [2])


import math, sys

# TODO: XXX Find a way to get information on native sizes and alignments
def pack_int(number,size,le):
    x=number
    res=[]
    for i in range(size):
        res.append(chr(x&0xff))
        x >>= 8
    if le == 'big':
        res.reverse()
    return ''.join(res)

def pack_signed_int(number,size,le):
    if not isinstance(number, (int,long)):
        raise StructError,"argument for i,I,l,L,q,Q,h,H must be integer"
    if  number > 2**(8*size-1)-1 or number < -1*2**(8*size-1):
        raise OverflowError,"Number:%i too large to convert" % number
    return pack_int(number,size,le)

def pack_unsigned_int(number,size,le):
    if not isinstance(number, (int,long)):
        raise StructError,"argument for i,I,l,L,q,Q,h,H must be integer"
    if number < 0:
        raise TypeError,"can't convert negative long to unsigned"
    if number > 2**(8*size)-1:
        raise OverflowError,"Number:%i too large to convert" % number
    return pack_int(number,size,le)

def pack_char(char,size,le):
    return str(char)

def sane_float(man,e):
    # TODO: XXX Implement checks for floats
    return True

def pack_float(number, size, le):

    if number < 0:
        sign = 1
        number *= -1
    elif number == 0.0:
        #return "\x00" * size
        return "".ljust(size, "\x00")
    else:
        sign = 0
    if size == 4:
        bias = 127
        exp = 8
        prec = 23
    else:
        bias = 1023
        exp = 11
        prec = 52

    man, e = math.frexp(number)
    if 0.5 <= man and man < 1.0:
        man *= 2
        e -= 1
    if sane_float(man,e):
        man -= 1
        e += bias
        mantissa = int(2**prec *(man) +0.5)
        res=[]
        if mantissa >> prec :
            mantissa = 0
            e += 1

        for i in range(size-2):
            #res += [ mantissa & 0xff]
            res.extend([ mantissa & 0xff])
            mantissa >>= 8
        #res += [ (mantissa & (2**(15-exp)-1)) | ((e & (2**(exp-7)-1))<<(15-exp))]
        res.extend([ (mantissa & (2**(15-exp)-1)) | ((e & (2**(exp-7)-1))<<(15-exp))])
        #res += [sign << 7 | e >> (exp - 7)]
        res.extend([sign << 7 | e >> (exp - 7)])
        if le == 'big':
            res.reverse()
        return ''.join([chr(x) for x in res])
    # TODO: What todo with insane floats/doubles. handle in sanefloat?


def getNum(fmt,i):
    num=None
    cur = fmt[i]
    while ('0'<= cur ) and ( cur <= '9'):
        if num == None:
            num = int(cur)
        else:
            num = 10*num + int(cur)
        i += 1
        cur = fmt[i]
    return num,i


def pack_into(fmt, buf, offset, *args):
    raise NotImplementedError("pack_into")
    #data = pack(fmt, *args)
    #buffer(buf)[offset:offset+len(data)] = data

def unpack_from(fmt, buf, offset=0):
    raise NotImplementedError("unpack_from")
    #size = calcsize(fmt)
    #data = buffer(buf)[offset:offset+size]
    #if len(data) != size:
    #    raise error("unpack_from requires a buffer of at least %d bytes"
    #                % (size,))
    #return unpack(fmt, data)
     */
/**
Functions to convert between Python values and C structs.
Python strings are used to hold the data representing the C struct
and also as format strings to describe the layout of data in the C struct.

The optional first format char indicates byte order, size and alignment:
 @: native order, size & alignment (default)
 =: native order, std. size & alignment
 <: little-endian, std. size & alignment
 >: big-endian, std. size & alignment
 !: same as >

The remaining chars indicate types of args and must match exactly;
these can be preceded by a decimal repeat count:
   x: pad byte (no data);
   c:char;
   b:signed byte;
   B:unsigned byte;
   h:short;
   H:unsigned short;
   i:int;
   I:unsigned int;
   l:long;
   L:unsigned long;
   f:float;
   d:double.
Special cases (preceding decimal count indicates length):
   s:string (array of char); p: pascal string (with count byte).
Special case (only available in native format):
   P:an integer type that is wide enough to hold a pointer.
Special case (not in native mode unless 'long long' in platform C):
   q:long long;
   Q:unsigned long long
Whitespace between formats is ignored.

The variable struct.error is an exception raised on errors. *
 */

/**
 * a port of Python's Struct, based on  java.nio.ByteBuffer
 * @author zigliolie - Copyright Sirtrack Ltd.
 *
 */
public class Packer {
 /* big_endian_format = {
      'x':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
      'b':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
      'B':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
      'c':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_char, 'unpack' : unpack_char},
      's':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
      'p':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
      'h':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
      'H':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
      'i':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
      'I':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
      'l':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
      'L':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
      'q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
      'Q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
      'f':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},
      'd':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},
      }
  default = big_endian_format
  formatmode={ '<' : (default, 'little'),
               '>' : (default, 'big'),
               '!' : (default, 'big'),
               '=' : (default, sys.byteorder),
               '@' : (default, sys.byteorder)
              }*/
  public class StructError extends RuntimeException{}
//error = StructError

public enum Endianness{
little, big, sys
}

public enum Format{
big_endian_format
}

public Format defaultFormat = Format.big_endian_format;

public class FormatMode{
public Format format;
public Endianness endianness;
public FormatMode( Format format, Endianness endianness )
{
  this.format = format;
  this.endianness = endianness;
  }
}

public Map<Character, FormatMode> formatmode = new HashMap<Character, FormatMode>(){{
    put( '<', new FormatMode( defaultFormat, Endianness.little ));
    put( '>', new FormatMode( defaultFormat, Endianness.big ));
    put( '!', new FormatMode( defaultFormat, Endianness.big ));
    put( '=', new FormatMode( defaultFormat, Endianness.sys ));
    put( '@', new FormatMode( defaultFormat, Endianness.sys ));
  }};

char fmt;
char endianity;
//FormatString[] fsa;

//  public Packer( String name )
public Packer( char endianity, char fmt )
{
  this.endianity = endianity;
  this.fmt = fmt;
  
  // index of last argument referenced
  int last = -1;
  // last ordinary index
  int lasto = -1;

//  FormatString[] fsa = parse(fmt);
//  for (int i = 0; i < fsa.length; i++) {
//      FormatString fs = fsa[i];
//      int index = fs.index();
//      try {
//    switch (index) {
//    case -2:  // fixed string, "%n", or "%%"
//        fs.print(null, l);
//        break;
//    case -1:  // relative index
//        if (last < 0 || (args != null && last > args.length - 1))
//      throw new MissingFormatArgumentException(fs.toString());
//        fs.print((args == null ? null : args[last]), l);
//        break;
//    case 0:  // ordinary index
//        lasto++;
//        last = lasto;
//        if (args != null && lasto > args.length - 1)
//      throw new MissingFormatArgumentException(fs.toString());
//        fs.print((args == null ? null : args[lasto]), l);
//        break;
//    default:  // explicit index
//        last = index - 1;
//        if (args != null && last > args.length - 1)
//      throw new MissingFormatArgumentException(fs.toString());
//        fs.print((args == null ? null : args[last]), l);
//        break;
//    }
//      } catch (IOException x) {
//    lastException = x;
//      }
//  }
  
}

/*def calcsize(fmt):
"""calcsize(fmt) -> int
Return size of C struct described by format string fmt.
See struct.__doc__ for more on format strings."""

formatdef,endianness,i = getmode(fmt)
num = 0
result = 0
while i<len(fmt):
    num,i = getNum(fmt,i)
    cur = fmt[i]
    try:
        format = formatdef[cur]
    except KeyError:
        raise StructError,"%s is not a valid format"%cur
    if num != None :
        result += num*format['size']
    else:
        result += format['size']
    num = 0
    i += 1
return result
*/
public int length()
{
  return 1;
}

public FormatMode getmode()
{
  FormatMode formatMode = formatmode.get( endianity );
  if( formatMode == null )
    throw new RuntimeException("endianity key not found");
  else
    return formatMode;
}

/**
  """unpack(string) -> (v1, v2, ...)
     Unpack the string, containing packed C structure data, according
     to fmt.  Requires len(string)==calcsize(fmt).
     See struct.__doc__ for more on format strings."""
 * @return
 */
//  public Object[] unpack( InputStream data )
  public Object[] unpack( String data )
  {
      Scanner sc;

//    FormatMode formatdef = getmode();
      ArrayList result = new ArrayList<Object>();
      Object obj;
      sc = new Scanner( data );
      sc.useDelimiter( "\\\\x" );
      
      Integer num = null;
      int j = 0;
      
      while( sc.hasNext() )
      {
        if( sc.hasNextByte() )
        {
          if( num != null )
          {
            if( endianity == '>' )
            {
              num = num << 8;
              num += sc.nextByte( 16 );
            } 
            else
            {
              num += sc.nextByte( 16 ) << 8*j;
              j++;
            }
          }
          else
          {
            num = new Integer( sc.nextByte( 16 ) );
            j = 1;
          }

          if( !sc.hasNextByte() )
          {
            result.add( num );
            num = null;
          }
        }
        else if( sc.hasNextBigDecimal() )
          result.add( sc.nextBigDecimal() );
        else if( sc.hasNextBigInteger() )
          result.add( sc.nextBigInteger() );
        else if( sc.hasNextBoolean() )
          result.add( sc.nextBoolean() );
        else if( sc.hasNextDouble())
          result.add(  sc.nextDouble() );
        else if( sc.hasNextFloat() )
          result.add( sc.nextFloat() );
        else if( sc.hasNextInt() )
          result.add( sc.nextInt() );
        else if( sc.hasNextLong())
          result.add( sc.nextLong() );
        else if( sc.hasNextShort() )
          result.add( sc.nextShort() );
        else if( sc.hasNextLine() )
          result.add( sc.nextLine() );
      }
      return result.toArray();
  }

  public Object[] unpack( byte[] stream )
  {
//    FormatMode formatdef = getmode();
      ArrayList result = new ArrayList<Object>();
      Object obj;
      
      ByteBuffer buf = ByteBuffer.wrap(stream);
      if( endianity == '>' )
        buf.order( ByteOrder.BIG_ENDIAN ); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
      else if( endianity == '<' )
        buf.order( ByteOrder.LITTLE_ENDIAN ); 
      else if( endianity == '=' )
        buf.order( ByteOrder.nativeOrder() ); 
  
      while( buf.hasRemaining() ){
      	switch( fmt ){
      		case 'L':
      			int num = buf.getInt();
            result.add( num );
      		break;
//        'x':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'b':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'B':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'c':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_char, 'unpack' : unpack_char},
//        's':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'p':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'h':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'H':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'i':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'I':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'l':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'L':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'Q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'f':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},
//        'd':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},

       }
      }
     return result.toArray();
  }

  /*
  public int unpack_int(data,index,size,le)
  {
      bytes = [ord(b) for b in data[index:index+size]]
      if le == 'little':
          bytes.reverse()
      number = 0L
      for b in bytes:
          number = number << 8 | b
      return int(number)
  }

  def unpack_signed_int(data,index,size,le):
      number = unpack_int(data,index,size,le)
      max = 2**(size*8)
      if number > 2**(size*8 - 1) - 1:
          number = int(-1*(max - number))
      return number

  def unpack_float(data,index,size,le):
      bytes = [ord(b) for b in data[index:index+size]]
      if len(bytes) != size:
          raise StructError,"Not enough data to unpack"
      if max(bytes) == 0:
          return 0.0
      if le == 'big':
          bytes.reverse()
      if size == 4:
          bias = 127
          exp = 8
          prec = 23
      else:
          bias = 1023
          exp = 11
          prec = 52
      mantissa = long(bytes[size-2] & (2**(15-exp)-1))
      #for b in bytes[size-3::-1]:
      #    mantissa = mantissa << 8 | b
      revbytes = bytes
      revbytes.reverse()
      for b in revbytes[3:]:
          mantissa = mantissa << 8 | b
      mantissa = 1 + (1.0*mantissa)/(2**(prec))
      mantissa /= 2
      e = (bytes[-1] & 0x7f) << (exp - 7)
      e += (bytes[size-2] >> (15 - exp)) & (2**(exp - 7) -1)
      e -= bias
      e += 1
      sign = bytes[-1] & 0x80
      number = math.ldexp(mantissa,e)
      if sign : number *= -1
      return number

  def unpack_char(data,index,size,le):
      return data[index:index+size]

  */
  /*
  """pack(fmt, v1, v2, ...) -> string
     Return string containing values v1, v2, ... packed according to fmt.
     See struct.__doc__ for more on format strings."""
   * @param fmt
   * @param args
   * @return
   */
  public byte[] pack( Object... args )
  {
    ByteBuffer b = ByteBuffer.allocate( 2048 );
    
    if( endianity == '>' )
      b.order( ByteOrder.BIG_ENDIAN ); // optional, the initial order of a byte buffer is always BIG_ENDIAN.
    else if( endianity == '<' )
      b.order( ByteOrder.LITTLE_ENDIAN ); 
    else if( endianity == '=' )
      b.order( ByteOrder.nativeOrder() ); 
    
  //  'L':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
    for( int i = 0; i < args.length; i++ )
    {
//      if( args[i] instanceof Byte )
//        b.put( (Byte)args[i] );
//      else if( args[i] instanceof Character )
//        b.putChar( (Character)args[i] );
//      else if( args[i] instanceof Double )
//        b.putDouble( (Double)args[i] );
//      else if( args[i] instanceof Float )
//        b.putFloat( (Float)args[i] );
//      else if( args[i] instanceof Integer )
//        b.putInt( (Integer)args[i] );
//      else if( args[i] instanceof Long )
//        b.putLong( (Long)args[i] );
//      else if( args[i] instanceof Short )
//        b.putShort( (Short)args[i] );
//      else throw new RuntimeException( "type not supported " + args[i] );
      	switch( fmt ){
      		case 'L':
          if( args[i] instanceof Integer )
          	b.putInt( (Integer)args[i] );
      		break;
//        'x':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'b':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'B':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'c':{ 'size' : 1, 'alignment' : 0, 'pack' : pack_char, 'unpack' : unpack_char},
//        's':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'p':{ 'size' : 1, 'alignment' : 0, 'pack' : None, 'unpack' : None},
//        'h':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'H':{ 'size' : 2, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'i':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'I':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'l':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'L':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_signed_int, 'unpack' : unpack_signed_int},
//        'Q':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_unsigned_int, 'unpack' : unpack_int},
//        'f':{ 'size' : 4, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},
//        'd':{ 'size' : 8, 'alignment' : 0, 'pack' : pack_float, 'unpack' : unpack_float},

       }
      }
    return Arrays.copyOf( b.array(), b.position() );
//    return b.array();
  }
}