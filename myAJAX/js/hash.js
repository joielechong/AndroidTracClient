/**
*
* hash:  Javascript hash string functions
*
* @package      hash string functions
* @category 	hash string functions.
* @access 	public
* @notes        Uses some slightly modified functions from www.webtoolkit.info 
*
*/
/*/////////////////////////////////////////////////////////////////////*/
function crc32(string)
/*/////////////////////////////////////////////////////////////////////*/
{
try {
     var c;
     var CRC32Table = new Array(0x00000000,0x77073096,0xEE0E612C,0x990951BA,0x076DC419,0x706AF48F,0xE963A535,0x9E6495A3,
                                0x0EDB8832,0x79DCB8A4,0xE0D5E91E,0x97D2D988,0x09B64C2B,0x7EB17CBD,0xE7B82D07,0x90BF1D91,
                                0x1DB71064,0x6AB020F2,0xF3B97148,0x84BE41DE,0x1ADAD47D,0x6DDDE4EB,0xF4D4B551,0x83D385C7,
                                0x136C9856,0x646BA8C0,0xFD62F97A,0x8A65C9EC,0x14015C4F,0x63066CD9,0xFA0F3D63,0x8D080DF5,
                                0x3B6E20C8,0x4C69105E,0xD56041E4,0xA2677172,0x3C03E4D1,0x4B04D447,0xD20D85FD,0xA50AB56B,
                                0x35B5A8FA,0x42B2986C,0xDBBBC9D6,0xACBCF940,0x32D86CE3,0x45DF5C75,0xDCD60DCF,0xABD13D59,
                                0x26D930AC,0x51DE003A,0xC8D75180,0xBFD06116,0x21B4F4B5,0x56B3C423,0xCFBA9599,0xB8BDA50F,
                                0x2802B89E,0x5F058808,0xC60CD9B2,0xB10BE924,0x2F6F7C87,0x58684C11,0xC1611DAB,0xB6662D3D,
                                0x76DC4190,0x01DB7106,0x98D220BC,0xEFD5102A,0x71B18589,0x06B6B51F,0x9FBFE4A5,0xE8B8D433,
                                0x7807C9A2,0x0F00F934,0x9609A88E,0xE10E9818,0x7F6A0DBB,0x086D3D2D,0x91646C97,0xE6635C01,
                                0x6B6B51F4,0x1C6C6162,0x856530D8,0xF262004E,0x6C0695ED,0x1B01A57B,0x8208F4C1,0xF50FC457,
                                0x65B0D9C6,0x12B7E950,0x8BBEB8EA,0xFCB9887C,0x62DD1DDF,0x15DA2D49,0x8CD37CF3,0xFBD44C65,
                                0x4DB26158,0x3AB551CE,0xA3BC0074,0xD4BB30E2,0x4ADFA541,0x3DD895D7,0xA4D1C46D,0xD3D6F4FB,
                                0x4369E96A,0x346ED9FC,0xAD678846,0xDA60B8D0,0x44042D73,0x33031DE5,0xAA0A4C5F,0xDD0D7CC9,
                                0x5005713C,0x270241AA,0xBE0B1010,0xC90C2086,0x5768B525,0x206F85B3,0xB966D409,0xCE61E49F,
                                0x5EDEF90E,0x29D9C998,0xB0D09822,0xC7D7A8B4,0x59B33D17,0x2EB40D81,0xB7BD5C3B,0xC0BA6CAD,
                                0xEDB88320,0x9ABFB3B6,0x03B6E20C,0x74B1D29A,0xEAD54739,0x9DD277AF,0x04DB2615,0x73DC1683,
                                0xE3630B12,0x94643B84,0x0D6D6A3E,0x7A6A5AA8,0xE40ECF0B,0x9309FF9D,0x0A00AE27,0x7D079EB1,
                                0xF00F9344,0x8708A3D2,0x1E01F268,0x6906C2FE,0xF762575D,0x806567CB,0x196C3671,0x6E6B06E7,
                                0xFED41B76,0x89D32BE0,0x10DA7A5A,0x67DD4ACC,0xF9B9DF6F,0x8EBEEFF9,0x17B7BE43,0x60B08ED5,
                                0xD6D6A3E8,0xA1D1937E,0x38D8C2C4,0x4FDFF252,0xD1BB67F1,0xA6BC5767,0x3FB506DD,0x48B2364B,
                                0xD80D2BDA,0xAF0A1B4C,0x36034AF6,0x41047A60,0xDF60EFC3,0xA867DF55,0x316E8EEF,0x4669BE79,
                                0xCB61B38C,0xBC66831A,0x256FD2A0,0x5268E236,0xCC0C7795,0xBB0B4703,0x220216B9,0x5505262F,
                                0xC5BA3BBE,0xB2BD0B28,0x2BB45A92,0x5CB36A04,0xC2D7FFA7,0xB5D0CF31,0x2CD99E8B,0x5BDEAE1D,
                                0x9B64C2B0,0xEC63F226,0x756AA39C,0x026D930A,0x9C0906A9,0xEB0E363F,0x72076785,0x05005713,
                                0x95BF4A82,0xE2B87A14,0x7BB12BAE,0x0CB61B38,0x92D28E9B,0xE5D5BE0D,0x7CDCEFB7,0x0BDBDF21,
                                0x86D3D2D4,0xF1D4E242,0x68DDB3F8,0x1FDA836E,0x81BE16CD,0xF6B9265B,0x6FB077E1,0x18B74777,
                                0x88085AE6,0xFF0F6A70,0x66063BCA,0x11010B5C,0x8F659EFF,0xF862AE69,0x616BFFD3,0x166CCF45,
                                0xA00AE278,0xD70DD2EE,0x4E048354,0x3903B3C2,0xA7672661,0xD06016F7,0x4969474D,0x3E6E77DB,
                                0xAED16A4A,0xD9D65ADC,0x40DF0B66,0x37D83BF0,0xA9BCAE53,0xDEBB9EC5,0x47B2CF7F,0x30B5FFE9,
                                0xBDBDF21C,0xCABAC28A,0x53B39330,0x24B4A3A6,0xBAD03605,0xCDD70693,0x54DE5729,0x23D967BF,
                                0xB3667A2E,0xC4614AB8,0x5D681B02,0x2A6F2B94,0xB40BBE37,0xC30C8EA1,0x5A05DF1B,0x2D02EF8D);
     var crc       = 0xFFFFFFFF;
     var tmp       = (typeof(string) == 'undefined')?(this):(string);
     var tmpLength = tmp.length;
  
     for (var i=0; i<tmpLength; i++)
        { 
         c = tmp.charCodeAt(i);
         crc = CRC32Table[(crc ^ c) & 0xFF] ^ ((crc >> 8) & 0xFFFFFF);
        }
     return (crc ^ 0xFFFFFFFF);
    } 
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function utf8Encode(string) 
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.webtoolkit.info */
{
try {
     var c;
     var utf8encoded = '';
     var tmp         = (typeof(string) == 'undefined')?(this):(string);
     tmp = tmp.replace(/\r\n/g,"\n");
     var tmpLength = tmp.length;

     for (var i = 0; i < tmpLength; i++) 
	{
	 c = tmp.charCodeAt(i);
         if (c < 128) utf8encoded += String.fromCharCode(c);
         else 
                {     
		 if((c > 127) && (c < 2048)) 
                        {
                         utf8encoded += String.fromCharCode((c >> 6) | 192);
                         utf8encoded += String.fromCharCode((c & 63) | 128);
			}
                 else 
			{
                         utf8encoded += String.fromCharCode((c >> 12) | 224);
                         utf8encoded += String.fromCharCode(((c >> 6) & 63) | 128);
                         utf8encoded += String.fromCharCode((c & 63) | 128);
			}
                }
	}
     return utf8encoded;
    }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function utf8Decode(string) 
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.webtoolkit.info */
{
try {
     var tmp         = (typeof(string) == 'undefined')?(this):(string);
     var tmpLength   = tmp.length;
     var utf8decoded = '';
     var i = c = c1 = c2 = 0;

     while (i < tmpLength) 
	{
         c = tmp.charCodeAt(i);
         if (c < 128) 
                {
                 utf8decoded += String.fromCharCode(c);
                 i++;
                }
         else 
		{
		 if((c > 191) && (c < 224)) 
			{
                         c2 = tmp.charCodeAt(i+1);
                         utf8decoded += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                         i += 2;
			}
                 else 
			{
                         c2 = tmp.charCodeAt(i+1);
                         c3 = tmp.charCodeAt(i+2);
                         utf8decoded += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                         i += 3;
			}
		}
        }
     return utf8decoded;
    }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function base64Encode(string)
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.webtoolkit.info */
{
try {
     var tmp           = (typeof(string) == 'undefined')?(this):(string);
     var tmpLength     = tmp.length;
     var base64        = new Array('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H','I', 'J', 'K', 'L', 'M', 'N', 'O', 'P','Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X','Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f','g', 'h', 'i', 'j', 'k', 'l', 'm', 'n','o', 'p', 'q', 'r', 's', 't', 'u', 'v','w', 'x', 'y', 'z', '0', '1', '2', '3','4', '5', '6', '7', '8', '9', '+', '/', '=');
     var i             = 0;
     var base64encoded = '';
     var c, c2, c3, e, e2, e3, e4;

     tmp = utf8Encode(tmp);
     while (i < tmpLength) 
	{
         c  = tmp.charCodeAt(i++);
         c2 = tmp.charCodeAt(i++);
         c3 = tmp.charCodeAt(i++);
         e  = c >> 2;
         e2 = ((c & 3) << 4) | (c2 >> 4);
         e3 = ((c2 & 15) << 2) | (c3 >> 6);
         e4 = c3 & 63;
         if (isNaN(c2)) { e3 = e4 = 64; } 
         else { if (isNaN(c3)) { e4 = 64; } }
         base64encoded += base64[e] + base64[e2] + base64[e3] + base64[e4];
        }
     return base64encoded;
    }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function base64Decode(string)
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.webtoolkit.info */
{
try {
     var tmp           = (typeof(string) == 'undefined')?(this):(string);
     var tmpLength     = tmp.length;
     var base64        = new String('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=');
     var i             = 0;
     var base64decoded = '';
     var c, c2, c3, e, e2, e3, e4;

     tmp = tmp.replace(/[^A-Za-z0-9\+\/\=]/g, '');
     while (i < tmpLength) 
	{
         e  = base64.indexOf(tmp.charAt(i++));
         e2 = base64.indexOf(tmp.charAt(i++));
         e3 = base64.indexOf(tmp.charAt(i++));
         e4 = base64.indexOf(tmp.charAt(i++));
         c  = (e << 2) | (e2 >> 4);
         c2 = ((e2 & 15) << 4) | (e3 >> 2);
         c3 = ((e3 & 3) << 6) | e4;
         base64decoded += String.fromCharCode(c);
         if (e3 != 64) { base64decoded += String.fromCharCode(c2); }
         if (e4 != 64) { base64decoded += String.fromCharCode(c3); }
        }
     return utf8Decode(base64decoded);
    }
catch(e) { return false; }    
};
/*/////////////////////////////////////////////////////////////////////*/
function md5(string)
/*/////////////////////////////////////////////////////////////////////*/
/* This function it is a slightly modified function from www.webtoolkit.info */
{
try {
     var tmp = (typeof(string) == 'undefined')?(this):(string);
     /*/////////////////////////////////////////////////////////////////////*/
     function _rotateLeft(lValue, iShiftBits) 
     /*///md5///////////////////////////////////////////////////////////////*/
     { return (lValue<<iShiftBits) | (lValue>>>(32-iShiftBits)); };
     /*/////////////////////////////////////////////////////////////////////*/
     function _addUnsigned(lX,lY) 
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      var lX4,lY4,lX8,lY8,lResult;

      lX8     = (lX & 0x80000000);
      lY8     = (lY & 0x80000000);
      lX4     = (lX & 0x40000000);
      lY4     = (lY & 0x40000000);
      lResult = (lX & 0x3FFFFFFF) + (lY & 0x3FFFFFFF);
      if (lX4 & lY4) { return (lResult ^ 0x80000000 ^ lX8 ^ lY8); }
      if (lX4 | lY4) 
        {
         if (lResult & 0x40000000) { return (lResult ^ 0xC0000000 ^ lX8 ^ lY8); } 
         else { return (lResult ^ 0x40000000 ^ lX8 ^ lY8); }
        } 
      else { return (lResult ^ lX8 ^ lY8); }
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _ff(a,b,c,d,x,s,ac) 
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      a = _addUnsigned(a, _addUnsigned(_addUnsigned(((b & c) | ((~b) & d)), x), ac));
      return _addUnsigned(_rotateLeft(a, s), b);
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _gg(a,b,c,d,x,s,ac)
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      a = _addUnsigned(a, _addUnsigned(_addUnsigned(((b & d) | (c & (~d))), x), ac));
      return _addUnsigned(_rotateLeft(a, s), b);
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _hh(a,b,c,d,x,s,ac) 
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      a = _addUnsigned(a, _addUnsigned(_addUnsigned((b ^ c ^ d), x), ac));
      return _addUnsigned(_rotateLeft(a, s), b);
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _ii(a,b,c,d,x,s,ac)
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      a = _addUnsigned(a, _addUnsigned(_addUnsigned((c ^ (b | (~d))), x), ac));
      return _addUnsigned(_rotateLeft(a, s), b);
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _convertToWordArray(string)
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      var lWordCount;
      var lMessageLength       = string.length;
      var lNumberOfWords_temp1 = lMessageLength + 8;
      var lNumberOfWords_temp2 = (lNumberOfWords_temp1-(lNumberOfWords_temp1 % 64))/64;
      var lNumberOfWords       = (lNumberOfWords_temp2+1)*16;
      var lWordArray           = Array(lNumberOfWords-1);
      var lBytePosition        = 0;
      var lByteCount           = 0;

      while ( lByteCount < lMessageLength ) 
        {
         lWordCount             = (lByteCount-(lByteCount % 4))/4;
         lBytePosition          = (lByteCount % 4)*8;
         lWordArray[lWordCount] = (lWordArray[lWordCount] | (string.charCodeAt(lByteCount)<<lBytePosition));
         lByteCount++;
        }
      lWordCount                   = (lByteCount-(lByteCount % 4))/4;
      lBytePosition                = (lByteCount % 4)*8;
      lWordArray[lWordCount]       = lWordArray[lWordCount] | (0x80<<lBytePosition);
      lWordArray[lNumberOfWords-2] = lMessageLength<<3;
      lWordArray[lNumberOfWords-1] = lMessageLength>>>29;
      return lWordArray;
     };
     /*/////////////////////////////////////////////////////////////////////*/
     function _word2Hex(lValue)
     /*///md5///////////////////////////////////////////////////////////////*/
     {
      var WordToHexValue='', WordToHexValue_temp='', lByte, lCount;

      for (lCount = 0;lCount<=3;lCount++) 
        {
         lByte               = (lValue>>>(lCount*8)) & 255;
         WordToHexValue_temp = '0' + lByte.toString(16);
         WordToHexValue      = WordToHexValue + WordToHexValue_temp.substr(WordToHexValue_temp.length-2,2);
        }
      return WordToHexValue;
     };
     /*/////////////////////////////////////////////////////////////////////*/
     var x = new Array();
     var k, AA, BB, CC, DD, a, b, c, d;
     var S11 = 7, S12 = 12, S13 = 17, S14 = 22;
     var S21 = 5, S22 = 9 , S23 = 14, S24 = 20;
     var S31 = 4, S32 = 11, S33 = 16, S34 = 23;
     var S41 = 6, S42 = 10, S43 = 15, S44 = 21;

     tmp = tmp.utf8Encode();
     x   = _convertToWordArray(tmp); 
     a   = 0x67452301; 
     b   = 0xEFCDAB89; 
     c   = 0x98BADCFE; 
     d   = 0x10325476;
     
     for (k = 0; k<x.length; k += 16) 
        {
         AA = a; 
         BB = b; 
         CC = c; 
         DD = d;
         a = _ff(a,b,c,d,x[k+0], S11,0xD76AA478);
         d = _ff(d,a,b,c,x[k+1], S12,0xE8C7B756);
         c = _ff(c,d,a,b,x[k+2], S13,0x242070DB);
         b = _ff(b,c,d,a,x[k+3], S14,0xC1BDCEEE);
         a = _ff(a,b,c,d,x[k+4], S11,0xF57C0FAF);
         d = _ff(d,a,b,c,x[k+5], S12,0x4787C62A);
         c = _ff(c,d,a,b,x[k+6], S13,0xA8304613);
         b = _ff(b,c,d,a,x[k+7], S14,0xFD469501);
         a = _ff(a,b,c,d,x[k+8], S11,0x698098D8);
         d = _ff(d,a,b,c,x[k+9], S12,0x8B44F7AF);
         c = _ff(c,d,a,b,x[k+10],S13,0xFFFF5BB1);
         b = _ff(b,c,d,a,x[k+11],S14,0x895CD7BE);
         a = _ff(a,b,c,d,x[k+12],S11,0x6B901122);
         d = _ff(d,a,b,c,x[k+13],S12,0xFD987193);
         c = _ff(c,d,a,b,x[k+14],S13,0xA679438E);
         b = _ff(b,c,d,a,x[k+15],S14,0x49B40821);
         a = _gg(a,b,c,d,x[k+1], S21,0xF61E2562);
         d = _gg(d,a,b,c,x[k+6], S22,0xC040B340);
         c = _gg(c,d,a,b,x[k+11],S23,0x265E5A51);
         b = _gg(b,c,d,a,x[k+0], S24,0xE9B6C7AA);
         a = _gg(a,b,c,d,x[k+5], S21,0xD62F105D);
         d = _gg(d,a,b,c,x[k+10],S22,0x2441453);
         c = _gg(c,d,a,b,x[k+15],S23,0xD8A1E681);
         b = _gg(b,c,d,a,x[k+4], S24,0xE7D3FBC8);
         a = _gg(a,b,c,d,x[k+9], S21,0x21E1CDE6);
         d = _gg(d,a,b,c,x[k+14],S22,0xC33707D6);
         c = _gg(c,d,a,b,x[k+3], S23,0xF4D50D87);
         b = _gg(b,c,d,a,x[k+8], S24,0x455A14ED);
         a = _gg(a,b,c,d,x[k+13],S21,0xA9E3E905);
         d = _gg(d,a,b,c,x[k+2], S22,0xFCEFA3F8);
         c = _gg(c,d,a,b,x[k+7], S23,0x676F02D9);
         b = _gg(b,c,d,a,x[k+12],S24,0x8D2A4C8A);
         a = _hh(a,b,c,d,x[k+5], S31,0xFFFA3942);
         d = _hh(d,a,b,c,x[k+8], S32,0x8771F681);
         c = _hh(c,d,a,b,x[k+11],S33,0x6D9D6122);
         b = _hh(b,c,d,a,x[k+14],S34,0xFDE5380C);
         a = _hh(a,b,c,d,x[k+1], S31,0xA4BEEA44);
         d = _hh(d,a,b,c,x[k+4], S32,0x4BDECFA9);
         c = _hh(c,d,a,b,x[k+7], S33,0xF6BB4B60);
         b = _hh(b,c,d,a,x[k+10],S34,0xBEBFBC70);
         a = _hh(a,b,c,d,x[k+13],S31,0x289B7EC6);
         d = _hh(d,a,b,c,x[k+0], S32,0xEAA127FA);
         c = _hh(c,d,a,b,x[k+3], S33,0xD4EF3085);
         b = _hh(b,c,d,a,x[k+6], S34,0x4881D05);
         a = _hh(a,b,c,d,x[k+9], S31,0xD9D4D039);
         d = _hh(d,a,b,c,x[k+12],S32,0xE6DB99E5);
         c = _hh(c,d,a,b,x[k+15],S33,0x1FA27CF8);
         b = _hh(b,c,d,a,x[k+2], S34,0xC4AC5665);
         a = _ii(a,b,c,d,x[k+0], S41,0xF4292244);
         d = _ii(d,a,b,c,x[k+7], S42,0x432AFF97);
         c = _ii(c,d,a,b,x[k+14],S43,0xAB9423A7);
         b = _ii(b,c,d,a,x[k+5], S44,0xFC93A039);
         a = _ii(a,b,c,d,x[k+12],S41,0x655B59C3);
         d = _ii(d,a,b,c,x[k+3], S42,0x8F0CCC92);
         c = _ii(c,d,a,b,x[k+10],S43,0xFFEFF47D);
         b = _ii(b,c,d,a,x[k+1], S44,0x85845DD1);
         a = _ii(a,b,c,d,x[k+8], S41,0x6FA87E4F);
         d = _ii(d,a,b,c,x[k+15],S42,0xFE2CE6E0);
         c = _ii(c,d,a,b,x[k+6], S43,0xA3014314);
         b = _ii(b,c,d,a,x[k+13],S44,0x4E0811A1);
         a = _ii(a,b,c,d,x[k+4], S41,0xF7537E82);
         d = _ii(d,a,b,c,x[k+11],S42,0xBD3AF235);
         c = _ii(c,d,a,b,x[k+2], S43,0x2AD7D2BB);
         b = _ii(b,c,d,a,x[k+9], S44,0xEB86D391);
         a = _addUnsigned(a,AA);
         b = _addUnsigned(b,BB);
         c = _addUnsigned(c,CC);
         d = _addUnsigned(d,DD);
        }

     var temp = _word2Hex(a) + _word2Hex(b) + _word2Hex(c) + _word2Hex(d);
     return temp.toLowerCase();
    }
catch(e) { return false; }    
}
/*/////////////////////////////////////////////////////////////////////*/
String.prototype.crc32               = crc32;
String.prototype.utf8Encode          = utf8Encode;
String.prototype.utf8Decode          = utf8Decode;
String.prototype.base64Encode        = base64Encode;
String.prototype.base64Decode        = base64Decode;
String.prototype.md5                 = md5;
/*/////////////////////////////////////////////////////////////////////*/