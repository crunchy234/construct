package construct.protocols.layer3;

import static construct.Core.*;
import static construct.Macros.*;
import static construct.Adapters.*;
import static construct.lib.Containers.*;
import static construct.lib.Binary.*;
import construct.Core.AdapterDecoder;
import construct.Core.AdapterEncoder;
import construct.Core.ValueFunc;
import construct.lib.Containers.Container;

/**
 * Ethernet (TCP/IP protocol stack) 
*/
public class Ipv4 {

    public static Adapter IpAddress( String name ) {
    	return IpAddressAdapter( Field( name, 4));
    }
  
    public static Adapter IpAddressAdapter( Construct field ) {
  	return new Adapter( field ){
			@Override
      public Object encode(Object obj, Container context) {
				String hexStr = (String)obj;
				hexStr = hexStr.replace(".", "");
				return hexStringToByteArray(hexStr);
      }
      public Object decode( Object obj, Container context) {
      	StringBuilder sb = new StringBuilder();
      	for( byte b : (byte[])obj ){
      		if (sb.length() > 0)
            sb.append('.');
      		sb.append(String.format("%03d", b));
      	}
      	return sb.toString();
      }

  	};
  };
  
  static Adapter ProtocolEnum( Construct subcon ){
  	return Enum( subcon,
  							"ICMP", 1,
  							"TCP", 6,
  							"UDP", 17 );
  };
  
  static Construct ipv4_header = 
  		Struct( "ip_header",
  						Const(Nibble("version"), 4 ),
  						ExprAdapter( Nibble("header_length"),
									 new AdapterEncoder() {
										public Object encode(Object obj, Container context) {
											return (Integer)obj / 4;
										}
									},
									new AdapterDecoder() {
										public Object decode(Object obj, Container context) {
											return (Integer)obj * 4;
										}
									}
							),
							BitStruct("tos",
					        Bits("precedence", 3),
					        Flag("minimize_delay"),
					        Flag("high_throuput"),
					        Flag("high_reliability"),
					        Flag("minimize_cost"),
					        Padding(1)
					    ),
					    UBInt16("total_length"),
					    Value("value", new ValueFunc(){public Object get(Container ctx) {
					    	return (Integer)ctx.get("total_length") - (Integer)ctx.get("header_length");
					    }}),
					    UBInt16("identification"),
					    EmbeddedBitStruct(
					        Struct("flags",
					            Padding(1),
					            Flag("dont_fragment"),
					            Flag("more_fragments")
					        ),
					        Bits("frame_offset", 13)
					    ),
					    UBInt8("ttl"),
					    ProtocolEnum(UBInt8("protocol")),
					    UBInt16("checksum"),
					    IpAddress("source"),
					    IpAddress("destination"),
					    Field("options", new LengthFunc(){
                public int length(Container context) {
	                return (Integer)context.get("header_length") - 20;
                }
					    })
  					);
/*
Container({'header_length': 20, 'protocol': 'UDP', 'payload_length': 40, 'tos': Container({'minimize_cost': False, 'high_throuput': False, 'minimize_delay': False, 'precedence': 0, 'high_reliability': False}), 'frame_offset': 0, 'flags': Container({'dont_fragment': False, 'more_fragments': False}), 'source': '192.168.2.5', 'destination': '212.116.161.38', 'version': 4, 'identification': 41187, 'ttl': 128, 'total_length': 60, 'checksum': 24965, 'options': ''})
'E\x00\x00<\xa0\xe3\x00\x00\x80\x11a\x85\xc0\xa8\x02\x05\xd4t\xa1&'

Not working yet:
Exception in thread "main" construct.Adapters$ConstError: expected 4 found 572
	at construct.Adapters$2.decode(Adapters.java:171)
	at construct.Core$Adapter._parse(Core.java:403)
	at construct.Core$Struct._parse(Core.java:741)
	at construct.Core$Construct.parse_stream(Core.java:278)
	at construct.Core$Construct.parse(Core.java:265)
	at construct.protocols.layer3.Ipv4.main(Ipv4.java:103)

 */
  public static void main(String[] args) {
  	byte[] cap = hexStringToByteArray("4500003ca0e3000080116185c0a80205d474a126"); 
  	Container c = ipv4_header.parse(cap);
  	System.out.println(c);
  	byte[] ba = ipv4_header.build(c);
  	System.out.println( byteArrayToHexString(ba) );
  }
  
}
